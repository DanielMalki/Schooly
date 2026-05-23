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
    private ImageButton btnQuickAddSchool;
    private TextView tvSchoolTitle;
    private View viewSchoolDivider;

    private ArrayList<String> schoolNames = new ArrayList<>();
    private ArrayList<String> schoolIds = new ArrayList<>();
    private DocumentReference selectedSchool;

    // נתונים של המנהל שמחובר כרגע
    private int currentAdminType;
    private String currentAdminId;

    // רכיבי תלמיד (קבוצות למידה)
    private LinearLayout layoutStudentFields;
    private Spinner spinnerHomeroom, spinnerMathGroup, spinnerEnglishGroup, spinnerPeGroup, spinnerMajorAGroup, spinnerMajorBGroup; // ✨ שונה ל-spinnerPeGroup

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

    // רשימות נתונים עבור הספינרים של התלמיד
    private ArrayList<String> homeroomNames = new ArrayList<>(), homeroomIds = new ArrayList<>();
    private ArrayList<String> mathNames = new ArrayList<>(), mathIds = new ArrayList<>();
    private ArrayList<String> englishNames = new ArrayList<>(), englishIds = new ArrayList<>();
    private ArrayList<String> peNames = new ArrayList<>(), peIds = new ArrayList<>(); // ✨ שונה ל-peNames ו-peIds
    private ArrayList<String> majorANames = new ArrayList<>(), majorAIds = new ArrayList<>(); // ✨ מורחב א'
    private ArrayList<String> majorBNames = new ArrayList<>(), majorBIds = new ArrayList<>(); // ✨ מורחב ב'
    @Override
    protected int getLayoutResourceId() { return R.layout.activity_add_user; }

    @Override
    protected int[] getAllowedUserTypes() { return new int[]{2, 3}; }

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

        // אתחול רכיבי בית ספר
        etSchoolLocked = findViewById(R.id.etSchoolLocked);
        spinnerSchoolSelect = findViewById(R.id.spinnerSchoolSelect);
        btnQuickAddSchool = findViewById(R.id.btnQuickAddSchool);
        tvSchoolTitle = findViewById(R.id.tvSchoolTitle);
        viewSchoolDivider = findViewById(R.id.viewSchoolDivider);

        // אתחול שדות תלמיד
        layoutStudentFields = findViewById(R.id.layoutStudentFields);
        spinnerHomeroom = findViewById(R.id.spinnerHomeroom);
        spinnerMathGroup = findViewById(R.id.spinnerMathGroup);
        spinnerEnglishGroup = findViewById(R.id.spinnerEnglishGroup);
        spinnerPeGroup = findViewById(R.id.spinnerPeGroup); // ✨ אתחול עם ה-ID החדש מה-XML
        spinnerMajorAGroup = findViewById(R.id.spinnerMajorAGroup);
        spinnerMajorBGroup = findViewById(R.id.spinnerMajorBGroup);

        // אתחול שדות מורה
        layoutTeacherFields = findViewById(R.id.layoutTeacherFields);
        layoutSelectedSubjectsList = findViewById(R.id.layoutSelectedSubjectsList);
        tvSelectSubjects = findViewById(R.id.tvSelectSubjects);
        btnQuickAddSubject = findViewById(R.id.btnQuickAddSubject);

        tvSelectSubjects.setOnClickListener(v -> showSubjectsMultiChoiceDialog());
        btnQuickAddSubject.setOnClickListener(v -> showQuickAddSubjectDialog());

        if (btnQuickAddSchool != null) {
            btnQuickAddSchool.setOnClickListener(v -> showQuickAddSchoolDialog());
        }

        setupRoleSpinner();

        SharedPreferences prefs = getSharedPreferences("SchoolyPrefs", MODE_PRIVATE);
        currentAdminType = prefs.getInt("userType", 2);
        currentAdminId = prefs.getString("userId", "");

        checkAdminSchoolStatus();

        loadSubjectsFromFirestore();
        loadClassesFromFirestore();

        btnSaveUser.setOnClickListener(v -> saveUserToDatabase());
    }

    private void checkAdminSchoolStatus() {
        if (spinnerRole.getSelectedItemPosition() == 3) {
            setSchoolLayoutVisibility(View.GONE, View.GONE, View.GONE);
            return;
        }

        if (currentAdminType == 2) {
            setSchoolLayoutVisibility(View.VISIBLE, View.GONE, View.GONE);

            if (!currentAdminId.isEmpty()) {
                db.collection("users").document(currentAdminId).get().addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        selectedSchool = documentSnapshot.getDocumentReference("school");
                        if (selectedSchool != null) {
                            selectedSchool.get().addOnSuccessListener(schoolDoc -> {
                                if (schoolDoc.exists()) {
                                    String schoolName = schoolDoc.getString("displayName");
                                    etSchoolLocked.setText(schoolName != null ? schoolName : selectedSchool.getId());
                                }
                            });
                        }
                    }
                });
            }
        } else if (currentAdminType == 3) {
            setSchoolLayoutVisibility(View.GONE, View.VISIBLE, View.VISIBLE);
            loadAllSchoolsForSchoolyAdmin();
        }
    }

    private void setSchoolLayoutVisibility(int lockedVis, int selectVis, int quickAddVis) {
        if (etSchoolLocked != null) etSchoolLocked.setVisibility(lockedVis);
        if (spinnerSchoolSelect != null) spinnerSchoolSelect.setVisibility(selectVis);
        if (btnQuickAddSchool != null) btnQuickAddSchool.setVisibility(quickAddVis);

        int generalVisibility = (lockedVis == View.GONE && selectVis == View.GONE) ? View.GONE : View.VISIBLE;
        if (tvSchoolTitle != null) tvSchoolTitle.setVisibility(generalVisibility);
        if (viewSchoolDivider != null) viewSchoolDivider.setVisibility(generalVisibility);
    }

    private void loadAllSchoolsForSchoolyAdmin() {
        db.collection("schools").get().addOnSuccessListener(queryDocumentSnapshots -> {
            schoolNames.clear();
            schoolIds.clear();
            for (QueryDocumentSnapshot doc : queryDocumentSnapshots) {
                schoolIds.add(doc.getId());
                String name = doc.getString("displayName");
                if (name == null) name = doc.getId();
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
        String[] roles = {"Student", "Teacher", "School Admin", "Schooly Admin"};
        spinnerRole.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, roles));
        spinnerRole.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if (position == 0) {
                    layoutStudentFields.setVisibility(View.VISIBLE);
                    layoutTeacherFields.setVisibility(View.GONE);
                    checkAdminSchoolStatus();
                } else if (position == 1 || position == 2) {
                    layoutStudentFields.setVisibility(View.GONE);
                    layoutTeacherFields.setVisibility(View.VISIBLE);
                    checkAdminSchoolStatus();
                    layoutTeacherFields.requestLayout();
                } else if (position == 3) {
                    layoutStudentFields.setVisibility(View.GONE);
                    layoutTeacherFields.setVisibility(View.GONE);
                    setSchoolLayoutVisibility(View.GONE, View.GONE, View.GONE);
                }
            }
            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });
    }

    private void showQuickAddSchoolDialog() {
        final EditText etInput = new EditText(this);
        etInput.setHint("School Name (e.g., Ironi Alef)");
        etInput.setPadding(40, 32, 40, 32);
        new AlertDialog.Builder(this)
                .setTitle("Quick Add New School")
                .setMessage("Enter the name of the new school:")
                .setView(etInput)
                .setPositiveButton("Save", (dialog, which) -> {
                    String schoolName = etInput.getText().toString().trim();
                    if (!schoolName.isEmpty()) {
                        saveSchoolToFirestoreQuickly(schoolName);
                    } else {
                        Toast.makeText(this, "Name cannot be empty", Toast.LENGTH_SHORT).show();
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void saveSchoolToFirestoreQuickly(String schoolName) {
        Map<String, Object> schoolData = new HashMap<>();
        schoolData.put("displayName", schoolName);

        db.collection("schools")
                .add(schoolData)
                .addOnSuccessListener(documentReference -> {
                    Toast.makeText(this, schoolName + " School Added! 🏫", Toast.LENGTH_SHORT).show();
                    loadAllSchoolsForSchoolyAdmin();
                })
                .addOnFailureListener(e -> Toast.makeText(this, "Failed to add school: " + e.getMessage(), Toast.LENGTH_SHORT).show());
    }

    private void loadSubjectsFromFirestore() {
        db.collection("classes");
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
        Query classesQuery;
        if (currentAdminType == 2 && selectedSchool != null) {
            classesQuery = db.collection("classes").whereEqualTo("school", selectedSchool);
        } else {
            classesQuery = db.collection("classes");
        }

        classesQuery.get().addOnSuccessListener(queryDocumentSnapshots -> {
            // אתחול כל הרשימות עם הפלייסהולדרים המתאימים
            initListWithPlaceholder(homeroomNames, homeroomIds, "Select Homeroom Class *");
            initListWithPlaceholder(mathNames, mathIds, "Select Math Group *");
            initListWithPlaceholder(englishNames, englishIds, "Select English Group *");
            initListWithPlaceholder(peNames, peIds, "Select Physical Education Group *");
            initListWithPlaceholder(majorANames, majorAIds, "Select Major 1 Group (Optional)"); // ✨ פלייסנולדר מורחב א'
            initListWithPlaceholder(majorBNames, majorBIds, "Select Major 2 Group (Optional)"); // ✨ פלייסנולדר מורחב ב'

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
                    case "pe":
                        peNames.add(className); peIds.add(classId);
                        break;
                    case "major a": // ✨ פיצול למורחב א' מתוך ה-DB
                        majorANames.add(className); majorAIds.add(classId);
                        break;
                    case "major b": // ✨ פיצול למורחב ב' מתוך ה-DB
                        majorBNames.add(className); majorBIds.add(classId);
                        break;
                }
            }

            // הצמדת האדפטרים המעודכנים והנפרדים לכל ספינר
            spinnerHomeroom.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, homeroomNames));
            spinnerMathGroup.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, mathNames));
            spinnerEnglishGroup.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, englishNames));
            spinnerPeGroup.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, peNames));

            // ✨ כל ספינר מקבל עכשיו את הרשימה הייעודית שלו
            spinnerMajorAGroup.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, majorANames));
            spinnerMajorBGroup.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, majorBNames));

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

        if (type != 3 && selectedSchool == null) {
            Toast.makeText(this, "Error: No school assigned to this user!", Toast.LENGTH_SHORT).show();
            return;
        }

        Map<String, DocumentReference> studentClassesMap = new HashMap<>();
        ArrayList<DocumentReference> teacherSubjectRefs = new ArrayList<>();

        if (type == 0) { // תלמיד
            // וידוא שכל מקצועות החובה נבחרו כולל PE
            if (spinnerHomeroom.getSelectedItemPosition() == 0 ||
                    spinnerMathGroup.getSelectedItemPosition() == 0 ||
                    spinnerEnglishGroup.getSelectedItemPosition() == 0 ||
                    spinnerPeGroup.getSelectedItemPosition() == 0) {
                Toast.makeText(this, "Please select Homeroom, Math, English, and Physical Education groups!", Toast.LENGTH_LONG).show();
                return;
            }

            // שמירת מקצועות החובה למפה
            studentClassesMap.put("homeroom", db.collection("classes").document(homeroomIds.get(spinnerHomeroom.getSelectedItemPosition())));
            studentClassesMap.put("math", db.collection("classes").document(mathIds.get(spinnerMathGroup.getSelectedItemPosition())));
            studentClassesMap.put("english", db.collection("classes").document(englishIds.get(spinnerEnglishGroup.getSelectedItemPosition())));
            studentClassesMap.put("pe", db.collection("classes").document(peIds.get(spinnerPeGroup.getSelectedItemPosition())));

            // ✨ שמירת מורחב א' (major a) מתוך רשימת ה-IDs הייעודית שלו
            if (spinnerMajorAGroup.getSelectedItemPosition() > 0) {
                studentClassesMap.put("major a", db.collection("classes").document(majorAIds.get(spinnerMajorAGroup.getSelectedItemPosition())));
            } else {
                studentClassesMap.put("major a", null);
            }

            // ✨ שמירת מורחב ב' (major b) מתוך רשימת ה-IDs הייעודית שלו
            if (spinnerMajorBGroup.getSelectedItemPosition() > 0) {
                studentClassesMap.put("major b", db.collection("classes").document(majorBIds.get(spinnerMajorBGroup.getSelectedItemPosition())));
            } else {
                studentClassesMap.put("major b", null);
            }

        } else if (type == 1 || type == 2) {
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

                if (type != 3) {
                    userMap.put("school", selectedSchool);
                }

                if (type == 0) {
                    userMap.put("classes", studentClassesMap);
                } else if (type == 1 || type == 2) {
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
        // איפוס שדות הטקסט
        etTz.setText("");
        etFirstName.setText("");
        etLastName.setText("");
        etMiddleName.setText("");
        etEmail.setText("");
        etPassword.setText("");

        // איפוס בחירת התפקיד
        spinnerRole.setSelection(0);

        // איפוס קבוצות הלימוד של החובה
        spinnerHomeroom.setSelection(0);
        spinnerMathGroup.setSelection(0);
        spinnerEnglishGroup.setSelection(0);
        spinnerPeGroup.setSelection(0); // ✨ איפוס ספינר PE (חינוך גופני)

        // איפוס קבוצות הלימוד של המורחבים (מפוצל לפי מורחב א' ומורחב ב')
        spinnerMajorAGroup.setSelection(0);
        spinnerMajorBGroup.setSelection(0);

        // ניקוי ואיפוס המקצועות שניתן ללמד (עבור מורים)
        chosenSubjectIds.clear();
        if (checkedSubjectsArray != null) {
            for (int i = 0; i < checkedSubjectsArray.length; i++) {
                checkedSubjectsArray[i] = false;
            }
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