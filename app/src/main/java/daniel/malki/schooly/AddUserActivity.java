package daniel.malki.schooly;

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
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class AddUserActivity extends BaseMenuActivity {

    private EditText etTz, etFirstName, etLastName, etMiddleName, etEmail, etPassword;
    private Spinner spinnerRole;
    private Button btnSaveUser;

    // רכיבי תלמיד (קבוצות למידה)
    private LinearLayout layoutStudentFields;
    private Spinner spinnerHomeroom, spinnerMathGroup, spinnerEnglishGroup, spinnerMajor1Group, spinnerMajor2Group;

    // רכיבי מורה
    // רכיבי מורה
    private LinearLayout layoutTeacherFields;
    private LinearLayout layoutSelectedSubjectsList; // המיכל החדש לרשימה שורה-אחר-שורה
    private ImageButton btnQuickAddSubject;
    private TextView tvSelectSubjects;

    private FirebaseFirestore db;

    // רשימות דינמיות למקצועות (IDs ושמות לתצוגה)
    private ArrayList<String> subjectNames = new ArrayList<>();
    private ArrayList<String> subjectIds = new ArrayList<>();
    private boolean[] checkedSubjectsArray; // שומר אילו מקצועות מסומנים ב-V
    private ArrayList<String> chosenSubjectIds = new ArrayList<>(); // ה-IDs שהמנהל בחר למורה בפועל

    // רשימות מופרדות לקבוצות הלמידה של התלמיד
    private ArrayList<String> homeroomNames = new ArrayList<>(), homeroomIds = new ArrayList<>();
    private ArrayList<String> mathNames = new ArrayList<>(), mathIds = new ArrayList<>();
    private ArrayList<String> englishNames = new ArrayList<>(), englishIds = new ArrayList<>();
    private ArrayList<String> majorNames = new ArrayList<>(), majorIds = new ArrayList<>();

    @Override
    protected int getLayoutResourceId() { return R.layout.activity_add_user; }

    @Override
    protected int[] getAllowedUserTypes() { return new int[]{2}; } // אדמין בלבד

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

        // בלחיצה על הטקסט-ויו ייפתח הדיאלוג עם ה-Checkboxes
        tvSelectSubjects.setOnClickListener(v -> showSubjectsMultiChoiceDialog());

        // הגדרת מאזין לכפתור הפלוס המהיר
        btnQuickAddSubject.setOnClickListener(v -> showQuickAddSubjectDialog());

        setupRoleSpinner();

        // טעינת כל המידע מ-Firestore ברקע
        loadSubjectsFromFirestore();
        loadClassesFromFirestore();

        btnSaveUser.setOnClickListener(v -> saveUserToDatabase());
    }

    private void setupRoleSpinner() {
        String[] roles = {"Student", "Teacher", "Admin"};
        spinnerRole.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, roles));
        spinnerRole.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if (position == 0) { // תלמיד
                    layoutStudentFields.setVisibility(View.VISIBLE);
                    layoutTeacherFields.setVisibility(View.GONE);
                } else { // מורה או אדמין
                    layoutStudentFields.setVisibility(View.GONE);
                    layoutTeacherFields.setVisibility(View.VISIBLE);

                    // הוקוס פוקוס: מכריח את ה-Layout לחשב גבהים מחדש ולא להישאר תקוע
                    layoutTeacherFields.requestLayout();
                }
            }
            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });
    }

    // שליפת המקצועות עבור המורים מה-Firestore
    private void loadSubjectsFromFirestore() {
        db.collection("subjects").get().addOnSuccessListener(queryDocumentSnapshots -> {
            subjectNames.clear();
            subjectIds.clear();

            for (QueryDocumentSnapshot doc : queryDocumentSnapshots) {
                subjectIds.add(doc.getId());
                String name = doc.contains("displayName") ? doc.getString("displayName") : doc.getId();
                subjectNames.add(name);
            }
            // אתחול מערך הבוליאנים לפי כמות המקצועות שחזרו מהשרת
            checkedSubjectsArray = new boolean[subjectNames.size()];
            updateSubjectsTextView();
        }).addOnFailureListener(e -> Toast.makeText(this, "Failed to load subjects", Toast.LENGTH_SHORT).show());
    }

    // פתיחת חלונית בחירה מרובה (Multi-Choice Checkboxes)
    private void showSubjectsMultiChoiceDialog() {
        if (subjectNames.isEmpty()) {
            Toast.makeText(this, "No subjects available. Add one first!", Toast.LENGTH_SHORT).show();
            return;
        }

        String[] items = subjectNames.toArray(new String[0]);

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Select Teachable Subjects");
        builder.setCancelable(false);

        // הגדרת הצי'קבוקסים עם המצב הנוכחי שלהם
        builder.setMultiChoiceItems(items, checkedSubjectsArray, (dialog, which, isChecked) -> {
            checkedSubjectsArray[which] = isChecked;
        });

        builder.setPositiveButton("OK", (dialog, which) -> {
            chosenSubjectIds.clear();
            for (int i = 0; i < checkedSubjectsArray.length; i++) {
                if (checkedSubjectsArray[i]) {
                    chosenSubjectIds.add(subjectIds.get(i)); // שומר את ה-IDs שנבחרו
                }
            }
            updateSubjectsTextView();
        });

        builder.setNegativeButton("Cancel", null);
        builder.show();
    }

    // עדכון הטקסט המוצג בתיבה לפי כמות המקצועות שנבחרו
    // עדכון הטקסט המוצג בתיבה - מציג את שמות המקצועות שנבחרו בפועל!
    // עדכון כמות המקצועות ויצירת רשימה שורה-אחר-שורה מתחתיה
    private void updateSubjectsTextView() {
        // 1. מנקים את הרשימה הויזואלית הקודמת כדי שלא ישוכפלו שורות
        layoutSelectedSubjectsList.removeAllViews();

        if (chosenSubjectIds.isEmpty()) {
            tvSelectSubjects.setText("Select Teachable Subjects *");
            tvSelectSubjects.setTextColor(android.graphics.Color.parseColor("#666666"));
        } else {
            // מעדכנים את הכותרת הראשית עם כמות המקצועות שנבחרו
            tvSelectSubjects.setText(chosenSubjectIds.size() + " Subjects Selected:");
            tvSelectSubjects.setTextColor(android.graphics.Color.parseColor("#1A237E")); // צבע בולט יותר

            // 2. רצים על כל המקצועות ומייצרים שורה לכל אחד שנבחר
            for (int i = 0; i < checkedSubjectsArray.length; i++) {
                if (checkedSubjectsArray[i]) {
                    String currentSubjectName = subjectNames.get(i);

                    // יצירת TextView חדש לחלוטין בקוד עבור השורה הנוכחית
                    TextView tvSubjectRow = new TextView(this);
                    tvSubjectRow.setText("• " + currentSubjectName); // נקודה קטנה בתחילת השורה (Bullet point)
                    tvSubjectRow.setTextSize(16.0f);
                    tvSubjectRow.setTextColor(android.graphics.Color.parseColor("#333333"));
                    tvSubjectRow.setPadding(0, 8, 0, 8); // רווח עדין בין השורות

                    // דוחפים את השורה החדשה לתוך הבלוק מתחת לכפתור
                    layoutSelectedSubjectsList.addView(tvSubjectRow);
                }
            }
        }
    }

    // שליפת קבוצות הלמידה לתלמיד (מתוך אוסף classes) וחלוקה לפי סוגים
    private void loadClassesFromFirestore() {
        db.collection("classes").get().addOnSuccessListener(queryDocumentSnapshots -> {
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

    /**
     * פונקציה שמקפיצה חלונית דיאלוג להוספה מהירה של מקצוע
     */
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

    /**
     * שמירת המקצוע החדש ל-Firestore, עדכון הרשימות וסימונו כאוטומטי!
     */
    private void saveSubjectToFirestoreQuickly(String subjectName) {
        String documentId = subjectName.toLowerCase().replace(" ", "-");

        Map<String, Object> subjectData = new HashMap<>();
        subjectData.put("displayName", subjectName);

        db.collection("subjects").document(documentId)
                .set(subjectData)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(this, subjectName + " Added! 🎉", Toast.LENGTH_SHORT).show();

                    // 1. הוספה לרשימות הכלליות
                    subjectNames.add(subjectName);
                    subjectIds.add(documentId);

                    // 2. הגדלת מערך הצי'קבוקסים וסימון המקצוע החדש כ-True אוטומטית!
                    boolean[] newCheckedArray = new boolean[subjectNames.size()];
                    System.arraycopy(checkedSubjectsArray, 0, newCheckedArray, 0, checkedSubjectsArray.length);
                    newCheckedArray[newCheckedArray.length - 1] = true; // המקצוע החדש מסומן ב-V
                    checkedSubjectsArray = newCheckedArray;

                    // 3. הוספה לרשימת המקצועות הנבחרים של המורה הנוכחי
                    chosenSubjectIds.add(documentId);
                    updateSubjectsTextView();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Failed to add subject: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
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

        } else { // מורה או מנהל מערכת
            if (chosenSubjectIds.isEmpty()) { // וידוא שנבחר לפחות מקצוע אחד
                Toast.makeText(this, "Please select at least one teachable subject!", Toast.LENGTH_LONG).show();
                return;
            }

            // הפיכת כל רשימת ה-IDs שנבחרו לרפרנסים של פיירבייס!
            for (String subId : chosenSubjectIds) {
                teacherSubjectRefs.add(db.collection("subjects").document(subId));
            }
        }

        db.collection("users").document(tz).get().addOnSuccessListener(documentSnapshot -> {
            if (documentSnapshot.exists()) {
                etTz.setError("User already exists!");
            } else {
                String fullName = firstName + (middleName.isEmpty() ? "" : " " + middleName) + " " + lastName;

                Map<String, Object> userMap = new HashMap<>();
                userMap.put("email", email);
                userMap.put("password", password);
                userMap.put("type", type);
                userMap.put("name", fullName);
                userMap.put("firstName", firstName);
                userMap.put("lastName", lastName);

                if (type == 0) {
                    userMap.put("classes", studentClassRefs);
                } else {
                    userMap.put("teachableSubjects", teacherSubjectRefs); // נשמר כמערך רפרנסים מלא
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

        // מנקה גם את התצוגה הויזואלית של השורות
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