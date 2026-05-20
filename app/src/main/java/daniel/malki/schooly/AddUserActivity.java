package daniel.malki.schooly;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Patterns;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;

import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class AddUserActivity extends BaseMenuActivity {

    private EditText etTz, etFirstName, etLastName, etMiddleName, etEmail, etPassword;
    private Spinner spinnerRole;
    private Button btnSaveUser;

    // רכיבי בית ספר הדינמיים
    private EditText etSchoolLocked;
    private Spinner spinnerSchoolSelect;
    private ArrayList<String> schoolNames = new ArrayList<>();
    private ArrayList<String> schoolIds = new ArrayList<>();
    private DocumentReference selectedSchool; // הרפרנס הסופי שיישמר למשתמש החדש

    // נתונים של המנהל שמחובר כרגע (נשלפים דינמית מ-SharedPreferences)
    private int currentAdminType;
    private String currentAdminId;

    // רכיבי תלמיד (קבוצות למידה)
    private LinearLayout layoutStudentFields;
    private Spinner spinnerHomeroom, spinnerMathGroup, spinnerEnglishGroup, spinnerMajor1Group, spinnerMajor2Group;

    // רכיבי מורה
    private LinearLayout layoutTeacherFields;
    private LinearLayout layoutSelectedSubjectsList;
    private ImageButton btnQuickAddSubject;
    private TextView tvSelectSubjects;

    private FirebaseFirestore db;

    private ArrayList<String> subjectNames = new ArrayList<>();
    private ArrayList<String> subjectIds = new ArrayList<>();
    private boolean[] checkedSubjectsArray;
    private ArrayList<String> chosenSubjectIds = new ArrayList<>();

    private ArrayList<String> homeroomNames = new ArrayList<>(), homeroomIds = new ArrayList<>();
    private ArrayList<String> mathNames = new ArrayList<>(), mathIds = new ArrayList<>();
    private ArrayList<String> englishNames = new ArrayList<>(), englishIds = new ArrayList<>();
    private ArrayList<String> majorNames = new ArrayList<>(), majorIds = new ArrayList<>();

    @Override
    protected int getLayoutResourceId() { return R.layout.activity_add_user; }

    @Override
    protected int[] getAllowedUserTypes() { return new int[]{2, 3}; } // מורשה גם למנהל בית ספר (2) וגם למנהל מערכת (3)

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        db = FirebaseFirestore.getInstance();

        // אתחול שדות כלליים
        etTz = findViewById(R.id.etNewTz);
        etFirstName = findViewById(R.id.etFirstName);
        etLastName = findViewById(R.id.etLastName);
        etMiddleName = findViewById(R.id.etMiddleName);
        etEmail = findViewById(R.id.etNewEmail);
        etPassword = findViewById(R.id.etNewPassword);
        spinnerRole = findViewById(R.id.spinnerRole);
        btnSaveUser = findViewById(R.id.btnSaveUser);

        // אתחול רכיבי בית ספר החדשים
        etSchoolLocked = findViewById(R.id.etSchoolLocked);
        spinnerSchoolSelect = findViewById(R.id.spinnerSchoolSelect);

        // אתחול שדות תלמיד
        layoutStudentFields = findViewById(R.id.layoutStudentFields);
        spinnerHomeroom = findViewById(R.id.spinnerHomeroom);
        spinnerMathGroup = findViewById(R.id.spinnerMathGroup);
        spinnerEnglishGroup = findViewById(R.id.spinnerEnglishGroup);
        spinnerMajor1Group = findViewById(R.id.spinnerMajor1Group);
        spinnerMajor2Group = findViewById(R.id.spinnerMajor2Group);

        // אתחול שדות מורה
        layoutTeacherFields = findViewById(R.id.layoutTeacherFields);
        layoutSelectedSubjectsList = findViewById(R.id.layoutSelectedSubjectsList);
        tvSelectSubjects = findViewById(R.id.tvSelectSubjects);
        btnQuickAddSubject = findViewById(R.id.btnQuickAddSubject);

        tvSelectSubjects.setOnClickListener(v -> showSubjectsMultiChoiceDialog());
        btnQuickAddSubject.setOnClickListener(v -> showQuickAddSubjectDialog());

        setupRoleSpinner();

        // 1. קריאת זהות המנהל המחובר מתוך ה-Session המקומי
        SharedPreferences prefs = getSharedPreferences("SchoolyPrefs", MODE_PRIVATE);
        currentAdminType = prefs.getInt("userType", 2); // ברירת מחדל 2 אם לא נמצא
        currentAdminId = prefs.getString("userId", "");

        // 2. קביעת נראות שדות בית הספר (נעול או ספינר בחירה)
        checkAdminSchoolStatus();

        // 3. טעינת נתונים משלימים מה-DB
        loadSubjectsFromFirestore();
        loadClassesFromFirestore();

        btnSaveUser.setOnClickListener(v -> saveUserToDatabase());
    }

    /**
     * קובע את נראות שדות בית הספר על פי דרגת המנהל המחובר ללא תלות בקוד קשיח
     */
    private void checkAdminSchoolStatus() {
        if (currentAdminType == 2) {
            // מנהל בית ספר (רמה 2) - השדה נעול לצפייה בלבד
            etSchoolLocked.setVisibility(View.VISIBLE);
            spinnerSchoolSelect.setVisibility(View.GONE);

            if (!currentAdminId.isEmpty()) {
                // הבאת ה-school ישירות ממסמך המנהל הנוכחי
                db.collection("users").document(currentAdminId).get().addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        selectedSchool = documentSnapshot.getDocumentReference("school");
                        if (selectedSchool != null) {
                            // שליפת השם הויזואלי של בית הספר שלו
                            selectedSchool.get().addOnSuccessListener(schoolDoc -> {
                                if (schoolDoc.exists()) {
                                    String schoolName = schoolDoc.getString("name");
                                    if (schoolName == null) schoolName = schoolDoc.getString("displayName");
                                    etSchoolLocked.setText(schoolName != null ? schoolName : selectedSchool.getId());
                                }
                            });
                        }
                    }
                });
            }
        } else if (currentAdminType == 3) {
            // מנהל מערכת (רמה 3) - פותח ספינר אינטראקטיבי לבחירת כל מוסד במערכת
            etSchoolLocked.setVisibility(View.GONE);
            spinnerSchoolSelect.setVisibility(View.VISIBLE);
            loadAllSchoolsForSystemAdmin();
        }
    }

    /**
     * טעינת כל המוסדות הקיימים באפליקציה עבור מנהל המערכת הראשי
     */
    private void loadAllSchoolsForSystemAdmin() {
        db.collection("schools").get().addOnSuccessListener(queryDocumentSnapshots -> {
            schoolNames.clear();
            schoolIds.clear();
            for (QueryDocumentSnapshot doc : queryDocumentSnapshots) {
                schoolIds.add(doc.getId());
                String name = doc.contains("name") ? doc.getString("name") : doc.getId();
                schoolNames.add(name);
            }
            ArrayAdapter<String> schoolAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, schoolNames);
            spinnerSchoolSelect.setAdapter(schoolAdapter);

            spinnerSchoolSelect.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                @Override
                public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                    selectedSchool = db.collection("schools").document(schoolIds.get(position));
                }
                @Override
                public void onNothingSelected(AdapterView<?> parent) {}
            });
        });
    }

    private void setupRoleSpinner() {
        String[] roles = {"Student", "Teacher", "School Admin"};
        spinnerRole.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, roles));
        spinnerRole.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if (position == 0) { // תלמיד
                    layoutStudentFields.setVisibility(View.VISIBLE);
                    layoutTeacherFields.setVisibility(View.GONE);
                } else { // מורה או אדמין בית ספר
                    layoutStudentFields.setVisibility(View.GONE);
                    layoutTeacherFields.setVisibility(View.VISIBLE);
                    layoutTeacherFields.requestLayout();
                }
            }
            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });
    }

    private void loadSubjectsFromFirestore() {
        db.collection("subjects").get().addOnSuccessListener(queryDocumentSnapshots -> {
            subjectNames.clear();
            subjectIds.clear();
            for (QueryDocumentSnapshot doc : queryDocumentSnapshots) {
                subjectIds.add(doc.getId());
                String name = doc.contains("displayName") ? doc.getString("displayName") : doc.getId();
                subjectNames.add(name);
            }
            checkedSubjectsArray = new boolean[subjectNames.size()];
            updateSubjectsTextView();
        }).addOnFailureListener(e -> Toast.makeText(this, "Failed to load subjects", Toast.LENGTH_SHORT).show());
    }

    private void showSubjectsMultiChoiceDialog() {
        if (subjectNames.isEmpty()) {
            Toast.makeText(this, "No subjects available. Add one first!", Toast.LENGTH_SHORT).show();
            return;
        }
        String[] items = subjectNames.toArray(new String[0]);
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Select Teachable Subjects");
        builder.setCancelable(false);
        builder.setMultiChoiceItems(items, checkedSubjectsArray, (dialog, which, isChecked) -> checkedSubjectsArray[which] = isChecked);
        builder.setPositiveButton("OK", (dialog, which) -> {
            chosenSubjectIds.clear();
            for (int i = 0; i < checkedSubjectsArray.length; i++) {
                if (checkedSubjectsArray[i]) {
                    chosenSubjectIds.add(subjectIds.get(i));
                }
            }
            updateSubjectsTextView();
        });
        builder.setNegativeButton("Cancel", null);
        builder.show();
    }

    private void updateSubjectsTextView() {
        layoutSelectedSubjectsList.removeAllViews();
        if (chosenSubjectIds.isEmpty()) {
            tvSelectSubjects.setText("Select Teachable Subjects *");
            tvSelectSubjects.setTextColor(android.graphics.Color.parseColor("#666666"));
        } else {
            tvSelectSubjects.setText(chosenSubjectIds.size() + " Subjects Selected:");
            tvSelectSubjects.setTextColor(android.graphics.Color.parseColor("#1A237E"));
            for (int i = 0; i < checkedSubjectsArray.length; i++) {
                if (checkedSubjectsArray[i]) {
                    String currentSubjectName = subjectNames.get(i);
                    TextView tvSubjectRow = new TextView(this);
                    tvSubjectRow.setText("• " + currentSubjectName);
                    tvSubjectRow.setTextSize(16.0f);
                    tvSubjectRow.setTextColor(android.graphics.Color.parseColor("#333333"));
                    tvSubjectRow.setPadding(0, 8, 0, 8);
                    layoutSelectedSubjectsList.addView(tvSubjectRow);
                }
            }
        }
    }

    private void loadClassesFromFirestore() {
        // היררכיית אבטחה: מנהל מוסד (2) רואה ומקבל רק קבוצות לימוד של המוסד שלו!
        Query classesQuery;
        if (currentAdminType == 2 && selectedSchool != null) {
            classesQuery = db.collection("classes").whereEqualTo("school", selectedSchool);
        } else {
            classesQuery = db.collection("classes");
        }

        classesQuery.get().addOnSuccessListener(queryDocumentSnapshots -> {
            initListWithPlaceholder(homeroomNames, homeroomIds, "Select Homeroom Class *");
            initListWithPlaceholder(mathNames, mathIds, "Select Math Group *");
            initListWithPlaceholder(englishNames, englishIds, "Select English Group *");
            initListWithPlaceholder(majorNames, majorIds, "Select Major Group (Optional)");

            for (QueryDocumentSnapshot doc : queryDocumentSnapshots) {
                String classId = doc.getId();
                String className = doc.contains("displayName") ? doc.getString("displayName") : doc.getString("name");
                String type = doc.getString("type");
                if (type == null) continue;

                switch (type) {
                    case "homeroom":
                        homeroomNames.add(className); homeroomIds.add(classId);
                        break;
                    case "math":
                        mathNames.add(className); mathIds.add(classId);
                        break;
                    case "english":
                        englishNames.add(className); englishIds.add(classId);
                        break;
                    case "major":
                        majorNames.add(className); majorIds.add(classId);
                        break;
                }
            }
            spinnerHomeroom.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, homeroomNames));
            spinnerMathGroup.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, mathNames));
            spinnerEnglishGroup.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, englishNames));
            spinnerMajor1Group.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, majorNames));
            spinnerMajor2Group.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, majorNames));
        }).addOnFailureListener(e -> Toast.makeText(this, "Failed to load classes", Toast.LENGTH_SHORT).show());
    }

    private void initListWithPlaceholder(ArrayList<String> names, ArrayList<String> ids, String placeholder) {
        names.clear(); ids.clear();
        names.add(placeholder); ids.add("");
    }

    private void showQuickAddSubjectDialog() {
        final EditText etInput = new EditText(this);
        etInput.setHint("Subject Name (e.g., English 5)");
        etInput.setPadding(40, 32, 40, 32);
        new AlertDialog.Builder(this)
                .setTitle("Quick Add New Subject")
                .setMessage("Enter the name of the new subject:")
                .setView(etInput)
                .setPositiveButton("Save", (dialog, which) -> {
                    String subjectName = etInput.getText().toString().trim();
                    if (!subjectName.isEmpty()) {
                        saveSubjectToFirestoreQuickly(subjectName);
                    } else {
                        Toast.makeText(this, "Name cannot be empty", Toast.LENGTH_SHORT).show();
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void saveSubjectToFirestoreQuickly(String subjectName) {
        String documentId = subjectName.toLowerCase().replace(" ", "-");
        Map<String, Object> subjectData = new HashMap<>();
        subjectData.put("displayName", subjectName);

        db.collection("subjects").document(documentId)
                .set(subjectData)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(this, subjectName + " Added! 🎉", Toast.LENGTH_SHORT).show();
                    subjectNames.add(subjectName);
                    subjectIds.add(documentId);
                    boolean[] newCheckedArray = new boolean[subjectNames.size()];
                    System.arraycopy(checkedSubjectsArray, 0, newCheckedArray, 0, checkedSubjectsArray.length);
                    newCheckedArray[newCheckedArray.length - 1] = true;
                    checkedSubjectsArray = newCheckedArray;
                    chosenSubjectIds.add(documentId);
                    updateSubjectsTextView();
                }).addOnFailureListener(e -> Toast.makeText(this, "Failed to add subject: " + e.getMessage(), Toast.LENGTH_SHORT).show());
    }

    private void saveUserToDatabase() {
        String tz = etTz.getText().toString().trim();
        String firstName = etFirstName.getText().toString().trim();
        String lastName = etLastName.getText().toString().trim();
        String middleName = etMiddleName.getText().toString().trim();
        String email = etEmail.getText().toString().trim();
        String password = etPassword.getText().toString().trim();
        int type = spinnerRole.getSelectedItemPosition();

        if (TextUtils.isEmpty(tz) || TextUtils.isEmpty(firstName) || TextUtils.isEmpty(lastName) ||
                TextUtils.isEmpty(email) || TextUtils.isEmpty(password)) {
            Toast.makeText(this, "Please fill all mandatory fields (*)", Toast.LENGTH_SHORT).show();
            return;
        }

        if (!isValidIsraeliID(tz) || !isValidEmail(email)) {
            Toast.makeText(this, "Please check ID or Email format", Toast.LENGTH_SHORT).show();
            return;
        }

        if (selectedSchool == null) {
            Toast.makeText(this, "Error: No school assigned to this user!", Toast.LENGTH_SHORT).show();
            return;
        }

        ArrayList<DocumentReference> studentClassRefs = new ArrayList<>();
        ArrayList<DocumentReference> teacherSubjectRefs = new ArrayList<>();

        if (type == 0) { // תלמיד
            if (spinnerHomeroom.getSelectedItemPosition() == 0 ||
                    spinnerMathGroup.getSelectedItemPosition() == 0 ||
                    spinnerEnglishGroup.getSelectedItemPosition() == 0) {
                Toast.makeText(this, "Please select Homeroom, Math, and English groups!", Toast.LENGTH_LONG).show();
                return;
            }
            studentClassRefs.add(db.collection("classes").document(homeroomIds.get(spinnerHomeroom.getSelectedItemPosition())));
            studentClassRefs.add(db.collection("classes").document(mathIds.get(spinnerMathGroup.getSelectedItemPosition())));
            studentClassRefs.add(db.collection("classes").document(englishIds.get(spinnerEnglishGroup.getSelectedItemPosition())));

            if (spinnerMajor1Group.getSelectedItemPosition() > 0) {
                studentClassRefs.add(db.collection("classes").document(majorIds.get(spinnerMajor1Group.getSelectedItemPosition())));
            }
            if (spinnerMajor2Group.getSelectedItemPosition() > 0) {
                studentClassRefs.add(db.collection("classes").document(majorIds.get(spinnerMajor2Group.getSelectedItemPosition())));
            }
        } else { // מורה או אדמין בית ספר
            if (chosenSubjectIds.isEmpty()) {
                Toast.makeText(this, "Please select at least one teachable subject!", Toast.LENGTH_LONG).show();
                return;
            }
            for (String subId : chosenSubjectIds) {
                teacherSubjectRefs.add(db.collection("subjects").document(subId));
            }
        }

        db.collection("users").document(tz).get().addOnSuccessListener(documentSnapshot -> {
            if (documentSnapshot.exists()) {
                etTz.setError("User already exists!");
                new AlertDialog.Builder(this)
                        .setTitle("שגיאה ביצירת משתמש 🚫")
                        .setMessage("תעודת הזהות שהזנת (" + tz + ") כבר קיימת במערכת.")
                        .setPositiveButton("הבנתי", null)
                        .show();
            } else {
                String fullName = firstName + (middleName.isEmpty() ? "" : " " + middleName) + " " + lastName;

                Map<String, Object> userMap = new HashMap<>();
                userMap.put("email", email);
                userMap.put("password", password);
                userMap.put("type", type);
                userMap.put("name", fullName);
                userMap.put("firstName", firstName);
                userMap.put("lastName", lastName);

                // שיוך הרפרנס הנבחר/האוטומטי למסמך המשתמש החדש
                userMap.put("school", selectedSchool);

                if (type == 0) {
                    userMap.put("classes", studentClassRefs);
                } else {
                    userMap.put("teachableSubjects", teacherSubjectRefs);
                }

                db.collection("users").document(tz).set(userMap).addOnSuccessListener(aVoid -> {
                    Toast.makeText(AddUserActivity.this, "User saved successfully! ✅", Toast.LENGTH_SHORT).show();
                    clearForm();
                });
            }
        });
    }

    private void clearForm() {
        etTz.setText(""); etFirstName.setText(""); etLastName.setText(""); etMiddleName.setText(""); etEmail.setText(""); etPassword.setText("");
        spinnerRole.setSelection(0);
        spinnerHomeroom.setSelection(0); spinnerMathGroup.setSelection(0); spinnerEnglishGroup.setSelection(0);
        spinnerMajor1Group.setSelection(0); spinnerMajor2Group.setSelection(0);
        chosenSubjectIds.clear();
        if (checkedSubjectsArray != null) {
            for (int i = 0; i < checkedSubjectsArray.length; i++) checkedSubjectsArray[i] = false;
        }
        if (layoutSelectedSubjectsList != null) {
            layoutSelectedSubjectsList.removeAllViews();
        }
        updateSubjectsTextView();
    }

    private boolean isValidIsraeliID(String id) {
        if (id == null || id.length() > 9 || !id.matches("\\d+")) return false;
        while (id.length() < 9) id = "0" + id;
        int sum = 0;
        for (int i = 0; i < 9; i++) {
            int digit = id.charAt(i) - '0';
            int step = digit * ((i % 2) + 1);
            if (step > 9) step -= 9;
            sum += step;
        }
        return sum % 10 == 0;
    }

    private boolean isValidEmail(String email) {
        return email != null && Patterns.EMAIL_ADDRESS.matcher(email).matches();
    }
}