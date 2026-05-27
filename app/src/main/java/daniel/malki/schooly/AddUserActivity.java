package daniel.malki.schooly;

import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
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

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AlertDialog;

import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.WriteBatch;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class AddUserActivity extends BaseMenuActivity {

    private EditText etTz, etFirstName, etLastName, etMiddleName, etEmail, etPassword;
    private Spinner spinnerRole;
    private Button btnSaveUser;

    // כפתורי הייבוא
    private Button btnImportStudents, btnImportTeachers;

    // רכיבי בית ספר הדינמיים
    private EditText etSchoolLocked;
    private Spinner spinnerSchoolSelect;
    private ImageButton btnQuickAddSchool;
    private TextView tvSchoolTitle;
    private View viewSchoolDivider;

    private ArrayList<String> schoolNames = new ArrayList<>();
    private ArrayList<String> schoolIds = new ArrayList<>();
    private DocumentReference selectedSchoolRef;

    private int currentAdminType;
    private String currentSchoolId;

    // אזור תלמיד - ספינרים
    private LinearLayout layoutStudentGroups;
    private Spinner spinnerHomeroom, spinnerMathGroup, spinnerEnglishGroup, spinnerPeGroup, spinnerMajorAGroup, spinnerMajorBGroup;

    // רשימות נתונים לספינרים (תלמיד)
    private ArrayList<String> homeroomNames = new ArrayList<>(), homeroomIds = new ArrayList<>();
    private ArrayList<String> mathNames = new ArrayList<>(), mathIds = new ArrayList<>();
    private ArrayList<String> englishNames = new ArrayList<>(), englishIds = new ArrayList<>();
    private ArrayList<String> peNames = new ArrayList<>(), peIds = new ArrayList<>();
    private ArrayList<String> majorANames = new ArrayList<>(), majorAIds = new ArrayList<>();
    private ArrayList<String> majorBNames = new ArrayList<>(), majorBIds = new ArrayList<>();

    // אזור מורה
    private LinearLayout layoutTeacherSubjects;
    private TextView tvSelectSubjects;
    private ImageButton btnQuickAddSubject;
    private LinearLayout layoutSelectedSubjectsList;

    private String[] allSubjectNames;
    private String[] allSubjectIds;
    private boolean[] checkedSubjectsArray;
    private ArrayList<String> chosenSubjectIds = new ArrayList<>();

    private FirebaseFirestore db;

    // לאנצ'רים לייבוא קבצים
    private final ActivityResultLauncher<Intent> csvTeachersPickerLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                    Uri csvUri = result.getData().getData();
                    if (csvUri != null) processTeachersCsvFile(csvUri);
                }
            }
    );

    private final ActivityResultLauncher<Intent> csvStudentsPickerLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                    Uri csvUri = result.getData().getData();
                    if (csvUri != null) processStudentsCsvFile(csvUri);
                }
            }
    );

    @Override
    protected int getLayoutResourceId() {
        return R.layout.activity_add_user;
    }

    @Override
    protected int[] getAllowedUserTypes() {
        return new int[]{2, 3};
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setTitle("Add New User");

        db = FirebaseFirestore.getInstance();

        SharedPreferences prefs = getSharedPreferences("SchoolyPrefs", MODE_PRIVATE);
        currentAdminType = prefs.getInt("userType", 2);
        currentSchoolId = prefs.getString("schoolId", "");

        etTz = findViewById(R.id.etTz);
        etFirstName = findViewById(R.id.etFirstName);
        etLastName = findViewById(R.id.etLastName);
        etMiddleName = findViewById(R.id.etMiddleName);
        etEmail = findViewById(R.id.etEmail);
        etPassword = findViewById(R.id.etPassword);
        spinnerRole = findViewById(R.id.spinnerRole);
        btnSaveUser = findViewById(R.id.btnSaveUser);

        btnImportStudents = findViewById(R.id.btnImportStudents);
        btnImportTeachers = findViewById(R.id.btnImportTeachers);

        etSchoolLocked = findViewById(R.id.etSchoolLocked);
        spinnerSchoolSelect = findViewById(R.id.spinnerSchoolSelect);
        btnQuickAddSchool = findViewById(R.id.btnQuickAddSchool);
        tvSchoolTitle = findViewById(R.id.tvSchoolTitle);
        viewSchoolDivider = findViewById(R.id.viewSchoolDivider);

        layoutStudentGroups = findViewById(R.id.layoutStudentGroups);
        spinnerHomeroom = findViewById(R.id.spinnerHomeroom);
        spinnerMathGroup = findViewById(R.id.spinnerMathGroup);
        spinnerEnglishGroup = findViewById(R.id.spinnerEnglishGroup);
        spinnerPeGroup = findViewById(R.id.spinnerPeGroup);
        spinnerMajorAGroup = findViewById(R.id.spinnerMajorAGroup);
        spinnerMajorBGroup = findViewById(R.id.spinnerMajorBGroup);

        layoutTeacherSubjects = findViewById(R.id.layoutTeacherSubjects);
        tvSelectSubjects = findViewById(R.id.tvSelectSubjects);
        btnQuickAddSubject = findViewById(R.id.btnQuickAddSubject);
        layoutSelectedSubjectsList = findViewById(R.id.layoutSelectedSubjectsList);

        setupRoleSpinner();
        setupSchoolLogic();

        btnSaveUser.setOnClickListener(v -> saveUser());

        btnImportStudents.setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
            intent.setType("*/*");
            csvStudentsPickerLauncher.launch(intent);
        });

        btnImportTeachers.setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
            intent.setType("*/*");
            csvTeachersPickerLauncher.launch(intent);
        });

        tvSelectSubjects.setOnClickListener(v -> showSubjectsDialog());

        btnQuickAddSchool.setOnClickListener(v -> showQuickAddSchoolDialog());

        btnQuickAddSubject.setOnClickListener(v -> showQuickAddSubjectDialog());
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (currentAdminType == 3) {
            loadSchoolsForSuperAdmin();
        }
        loadAllSubjectsForTeacher();

        if (spinnerRole.getSelectedItemPosition() == 1) {
            loadStudentClasses();
        }
    }

    private void setupRoleSpinner() {
        String[] roles = {"Select Role...", "Student", "Teacher"};
        ArrayAdapter<String> roleAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, roles);
        spinnerRole.setAdapter(roleAdapter);

        spinnerRole.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if (position == 1) { // Student
                    layoutStudentGroups.setVisibility(View.VISIBLE);
                    layoutTeacherSubjects.setVisibility(View.GONE);
                    loadStudentClasses();
                } else if (position == 2) { // Teacher
                    layoutTeacherSubjects.setVisibility(View.VISIBLE);
                    layoutStudentGroups.setVisibility(View.GONE);
                } else {
                    layoutStudentGroups.setVisibility(View.GONE);
                    layoutTeacherSubjects.setVisibility(View.GONE);
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });
    }

    private void setupSchoolLogic() {
        if (currentAdminType == 3) {
            tvSchoolTitle.setVisibility(View.VISIBLE);
            spinnerSchoolSelect.setVisibility(View.VISIBLE);
            btnQuickAddSchool.setVisibility(View.VISIBLE);
            viewSchoolDivider.setVisibility(View.VISIBLE);
            loadSchoolsForSuperAdmin();

            spinnerSchoolSelect.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                @Override
                public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                    if (position > 0) {
                        selectedSchoolRef = db.collection("schools").document(schoolIds.get(position));
                        if (spinnerRole.getSelectedItemPosition() == 1) {
                            loadStudentClasses();
                        }
                    } else {
                        selectedSchoolRef = null;
                        clearStudentSpinners();
                    }
                }
                @Override
                public void onNothingSelected(AdapterView<?> parent) {}
            });
        } else if (currentAdminType == 2) {
            tvSchoolTitle.setVisibility(View.VISIBLE);
            etSchoolLocked.setVisibility(View.VISIBLE);
            viewSchoolDivider.setVisibility(View.VISIBLE);

            if (!TextUtils.isEmpty(currentSchoolId)) {
                selectedSchoolRef = db.collection("schools").document(currentSchoolId);
                selectedSchoolRef.get().addOnSuccessListener(doc -> {
                    if (doc.exists()) {
                        etSchoolLocked.setText(doc.getString("displayName"));
                    }
                });
            } else {
                Toast.makeText(this, "Error: Admin is not assigned to a school.", Toast.LENGTH_LONG).show();
                btnSaveUser.setEnabled(false);
            }
        }
    }

    private void loadSchoolsForSuperAdmin() {
        String currentSelectionId = null;
        if (spinnerSchoolSelect.getSelectedItemPosition() > 0 && schoolIds.size() > spinnerSchoolSelect.getSelectedItemPosition()) {
            currentSelectionId = schoolIds.get(spinnerSchoolSelect.getSelectedItemPosition());
        }

        final String finalCurrentSelectionId = currentSelectionId;

        db.collection("schools").orderBy("displayName", Query.Direction.ASCENDING).get().addOnSuccessListener(snapshots -> {
            schoolNames.clear();
            schoolIds.clear();
            schoolNames.add("Select School...");
            schoolIds.add("");

            int newPosition = 0;
            int currentIndex = 1;

            for (QueryDocumentSnapshot doc : snapshots) {
                schoolIds.add(doc.getId());
                schoolNames.add(doc.getString("displayName") != null ? doc.getString("displayName") : doc.getId());

                if (finalCurrentSelectionId != null && finalCurrentSelectionId.equals(doc.getId())) {
                    newPosition = currentIndex;
                }
                currentIndex++;
            }

            ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, schoolNames);
            spinnerSchoolSelect.setAdapter(adapter);

            if (newPosition > 0) {
                spinnerSchoolSelect.setSelection(newPosition);
            }
        });
    }

    private DocumentReference getSelectedSchoolRef() {
        if (currentAdminType == 3) {
            int pos = spinnerSchoolSelect.getSelectedItemPosition();
            if (pos > 0) {
                return db.collection("schools").document(schoolIds.get(pos));
            }
            return null;
        } else {
            return selectedSchoolRef;
        }
    }

    private void loadStudentClasses() {
        DocumentReference schoolRef = getSelectedSchoolRef();
        if (schoolRef == null) {
            clearStudentSpinners();
            return;
        }

        db.collection("classes").whereEqualTo("school", schoolRef).get()
                .addOnSuccessListener(snapshots -> {
                    clearClassLists();

                    for (QueryDocumentSnapshot doc : snapshots) {
                        String type = doc.getString("type");
                        String name = doc.getString("displayName");
                        String id = doc.getId();

                        if (type != null && name != null) {
                            switch (type.toLowerCase()) {
                                case "homeroom": homeroomNames.add(name); homeroomIds.add(id); break;
                                case "math": mathNames.add(name); mathIds.add(id); break;
                                case "english": englishNames.add(name); englishIds.add(id); break;
                                case "sports": peNames.add(name); peIds.add(id); break;
                                case "major a": majorANames.add(name); majorAIds.add(id); break;
                                case "major b": majorBNames.add(name); majorBIds.add(id); break;
                            }
                        }
                    }
                    updateClassSpinners();
                });
    }

    private void clearClassLists() {
        homeroomNames.clear(); homeroomIds.clear(); homeroomNames.add("None"); homeroomIds.add("");
        mathNames.clear(); mathIds.clear(); mathNames.add("None"); mathIds.add("");
        englishNames.clear(); englishIds.clear(); englishNames.add("None"); englishIds.add("");
        peNames.clear(); peIds.clear(); peNames.add("None"); peIds.add("");
        majorANames.clear(); majorAIds.clear(); majorANames.add("None (Optional)"); majorAIds.add("");
        majorBNames.clear(); majorBIds.clear(); majorBNames.add("None (Optional)"); majorBIds.add("");
    }

    private void updateClassSpinners() {
        spinnerHomeroom.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, homeroomNames));
        spinnerMathGroup.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, mathNames));
        spinnerEnglishGroup.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, englishNames));
        spinnerPeGroup.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, peNames));
        spinnerMajorAGroup.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, majorANames));
        spinnerMajorBGroup.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, majorBNames));
    }

    private void clearStudentSpinners() {
        clearClassLists();
        updateClassSpinners();
    }

    private void loadAllSubjectsForTeacher() {
        db.collection("subjects").orderBy("displayName", Query.Direction.ASCENDING).get().addOnSuccessListener(snapshots -> {
            int size = snapshots.size();
            allSubjectNames = new String[size];
            allSubjectIds = new String[size];
            checkedSubjectsArray = new boolean[size];

            int i = 0;
            for (QueryDocumentSnapshot doc : snapshots) {
                allSubjectIds[i] = doc.getId();
                allSubjectNames[i] = doc.getString("displayName");
                if (chosenSubjectIds.contains(doc.getId())) {
                    checkedSubjectsArray[i] = true;
                }
                i++;
            }
            updateSubjectsTextView();
        });
    }

    private void showSubjectsDialog() {
        if (allSubjectNames == null || allSubjectNames.length == 0) {
            Toast.makeText(this, "No subjects found in database.", Toast.LENGTH_SHORT).show();
            return;
        }

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Select Subjects");
        builder.setMultiChoiceItems(allSubjectNames, checkedSubjectsArray, (dialog, which, isChecked) -> {
            checkedSubjectsArray[which] = isChecked;
        });

        builder.setPositiveButton("OK", (dialog, which) -> {
            chosenSubjectIds.clear();
            for (int i = 0; i < checkedSubjectsArray.length; i++) {
                if (checkedSubjectsArray[i]) {
                    chosenSubjectIds.add(allSubjectIds[i]);
                }
            }
            updateSubjectsTextView();
        });

        builder.setNegativeButton("Cancel", null);
        builder.show();
    }

    private void updateSubjectsTextView() {
        if (chosenSubjectIds.isEmpty()) {
            tvSelectSubjects.setText("Select Subjects...");
            layoutSelectedSubjectsList.removeAllViews();
        } else {
            tvSelectSubjects.setText(chosenSubjectIds.size() + " subjects selected");
            layoutSelectedSubjectsList.removeAllViews();

            for (int i = 0; i < allSubjectIds.length; i++) {
                if (checkedSubjectsArray[i]) {
                    TextView tv = new TextView(this);
                    tv.setText("• " + allSubjectNames[i]);
                    tv.setTextColor(getResources().getColor(android.R.color.black));
                    tv.setTextSize(14f);
                    tv.setPadding(0, 4, 0, 4);
                    layoutSelectedSubjectsList.addView(tv);
                }
            }
        }
    }

    private void showQuickAddSchoolDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Quick Add School");

        final EditText input = new EditText(this);
        input.setHint("Enter school name");
        input.setPadding(40, 40, 40, 40);
        builder.setView(input);

        builder.setPositiveButton("Add", (dialog, which) -> {
            String newSchoolName = input.getText().toString().trim();
            if (!newSchoolName.isEmpty()) {
                Map<String, Object> schoolData = new HashMap<>();
                schoolData.put("displayName", newSchoolName);

                db.collection("schools").add(schoolData).addOnSuccessListener(documentReference -> {
                    Toast.makeText(this, "School added successfully!", Toast.LENGTH_SHORT).show();
                    loadSchoolsForSuperAdmin(); // רענון הספינר של בתי הספר
                }).addOnFailureListener(e -> {
                    Toast.makeText(this, "Error adding school.", Toast.LENGTH_SHORT).show();
                });
            } else {
                Toast.makeText(this, "School name cannot be empty", Toast.LENGTH_SHORT).show();
            }
        });

        builder.setNegativeButton("Cancel", (dialog, which) -> dialog.cancel());
        builder.show();
    }

    private void showQuickAddSubjectDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Quick Add Subject");

        final EditText input = new EditText(this);
        input.setHint("Enter subject name");
        input.setPadding(40, 40, 40, 40);
        builder.setView(input);

        builder.setPositiveButton("Add", (dialog, which) -> {
            String newSubjectName = input.getText().toString().trim();
            if (!newSubjectName.isEmpty()) {
                Map<String, Object> subjectData = new HashMap<>();
                subjectData.put("displayName", newSubjectName);

                db.collection("subjects").add(subjectData).addOnSuccessListener(documentReference -> {
                    Toast.makeText(this, "Subject added successfully!", Toast.LENGTH_SHORT).show();
                    loadAllSubjectsForTeacher(); // רענון רשימת המקצועות לבחירה
                }).addOnFailureListener(e -> {
                    Toast.makeText(this, "Error adding subject.", Toast.LENGTH_SHORT).show();
                });
            } else {
                Toast.makeText(this, "Subject name cannot be empty", Toast.LENGTH_SHORT).show();
            }
        });

        builder.setNegativeButton("Cancel", (dialog, which) -> dialog.cancel());
        builder.show();
    }

    private void saveUser() {
        String tz = etTz.getText().toString().trim();
        String firstName = etFirstName.getText().toString().trim();
        String lastName = etLastName.getText().toString().trim();
        String middleName = etMiddleName.getText().toString().trim();
        String email = etEmail.getText().toString().trim();
        String password = etPassword.getText().toString();
        int rolePos = spinnerRole.getSelectedItemPosition();

        if (TextUtils.isEmpty(tz) || TextUtils.isEmpty(firstName) || TextUtils.isEmpty(lastName) ||
                TextUtils.isEmpty(email) || TextUtils.isEmpty(password) || rolePos == 0) {
            Toast.makeText(this, "Please fill all mandatory fields and select role.", Toast.LENGTH_SHORT).show();
            return;
        }

        if (!isValidIsraeliID(tz)) {
            Toast.makeText(this, "Invalid ID (TZ) number.", Toast.LENGTH_SHORT).show();
            return;
        }

        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            Toast.makeText(this, "Invalid email address.", Toast.LENGTH_SHORT).show();
            return;
        }

        if (password.length() < 6) {
            Toast.makeText(this, "Password must be at least 6 characters.", Toast.LENGTH_SHORT).show();
            return;
        }

        DocumentReference schoolRef = getSelectedSchoolRef();
        if (schoolRef == null) {
            Toast.makeText(this, "Please select a school.", Toast.LENGTH_SHORT).show();
            return;
        }

        db.collection("users").whereEqualTo("tz", tz).get().addOnSuccessListener(snapshots -> {
            if (!snapshots.isEmpty()) {
                Toast.makeText(this, "User with this ID already exists!", Toast.LENGTH_SHORT).show();
                return;
            }

            db.collection("users").whereEqualTo("email", email).get().addOnSuccessListener(emailSnaps -> {
                if (!emailSnaps.isEmpty()) {
                    Toast.makeText(this, "User with this email already exists!", Toast.LENGTH_SHORT).show();
                    return;
                }

                Map<String, Object> userData = new HashMap<>();
                userData.put("tz", tz);
                userData.put("firstName", firstName);
                userData.put("lastName", lastName);
                if (!middleName.isEmpty()) userData.put("middleName", middleName);
                userData.put("email", email);
                userData.put("password", password);
                userData.put("school", schoolRef);

                if (rolePos == 1) { // Student
                    userData.put("type", 0);

                    int hrPos = spinnerHomeroom.getSelectedItemPosition();
                    int mathPos = spinnerMathGroup.getSelectedItemPosition();
                    int engPos = spinnerEnglishGroup.getSelectedItemPosition();
                    int pePos = spinnerPeGroup.getSelectedItemPosition();

                    if (hrPos == 0 || mathPos == 0 || engPos == 0 || pePos == 0) {
                        Toast.makeText(this, "Homeroom, Math, English and PE groups are mandatory for students.", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    Map<String, DocumentReference> classesMap = new HashMap<>();
                    classesMap.put("homeroom", db.collection("classes").document(homeroomIds.get(hrPos)));
                    classesMap.put("math", db.collection("classes").document(mathIds.get(mathPos)));
                    classesMap.put("english", db.collection("classes").document(englishIds.get(engPos)));
                    classesMap.put("sports", db.collection("classes").document(peIds.get(pePos)));

                    int majorAPos = spinnerMajorAGroup.getSelectedItemPosition();
                    if (majorAPos > 0) {
                        classesMap.put("major a", db.collection("classes").document(majorAIds.get(majorAPos)));
                    }

                    int majorBPos = spinnerMajorBGroup.getSelectedItemPosition();
                    if (majorBPos > 0) {
                        classesMap.put("major b", db.collection("classes").document(majorBIds.get(majorBPos)));
                    }

                    userData.put("classes", classesMap);

                } else if (rolePos == 2) { // Teacher
                    userData.put("type", 1);
                    ArrayList<DocumentReference> subjectsRefs = new ArrayList<>();
                    for (String sid : chosenSubjectIds) {
                        subjectsRefs.add(db.collection("subjects").document(sid));
                    }
                    userData.put("teachableSubjects", subjectsRefs);
                }

                db.collection("users").add(userData).addOnSuccessListener(docRef -> {
                    Toast.makeText(this, "User created successfully!", Toast.LENGTH_SHORT).show();
                    clearFields();
                }).addOnFailureListener(e -> {
                    Toast.makeText(this, "Error creating user: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });

            });
        });
    }

    private void clearFields() {
        etTz.setText("");
        etFirstName.setText("");
        etLastName.setText("");
        etMiddleName.setText("");
        etEmail.setText("");
        etPassword.setText("");
        spinnerRole.setSelection(0);
        spinnerHomeroom.setSelection(0);
        spinnerMathGroup.setSelection(0);
        spinnerEnglishGroup.setSelection(0);
        spinnerPeGroup.setSelection(0);
        spinnerMajorAGroup.setSelection(0);
        spinnerMajorBGroup.setSelection(0);

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
            sum += (step > 9) ? (step - 9) : step;
        }
        return sum % 10 == 0;
    }

    // ==========================================
    // קוד הייבוא מ-CSV
    // ==========================================

    private void processStudentsCsvFile(Uri uri) {
        DocumentReference selectedSchool = getSelectedSchoolRef();
        if (selectedSchool == null) {
            Toast.makeText(this, "Please select a school first!", Toast.LENGTH_SHORT).show();
            return;
        }

        // שלב 1: Pre-fetch למילון הכיתות
        db.collection("classes").whereEqualTo("school", selectedSchool).get().addOnSuccessListener(snapshots -> {
            Map<String, DocumentReference> classDict = new HashMap<>();
            for (QueryDocumentSnapshot doc : snapshots) {
                String className = doc.getString("displayName");
                if (className != null) {
                    classDict.put(className.trim(), doc.getReference());
                }
            }

            try {
                InputStream inputStream = getContentResolver().openInputStream(uri);
                BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
                String line;
                WriteBatch batch = db.batch();
                int count = 0;
                boolean isFirstRow = true;

                while ((line = reader.readLine()) != null) {
                    if (isFirstRow && line.toLowerCase().contains("email")) {
                        isFirstRow = false;
                        continue;
                    }
                    isFirstRow = false;

                    String[] columns = line.split(",");
                    if (columns.length >= 11) {
                        String tz = columns[0].trim();
                        String firstName = columns[1].trim();
                        String lastName = columns[2].trim();
                        String email = columns[3].trim();
                        String password = columns[4].trim();
                        String middleName = columns[5].trim();

                        String homeroom = columns[6].trim();
                        String math = columns[7].trim();
                        String english = columns[8].trim();
                        String sports = columns[9].trim();
                        String majorA = columns[10].trim();
                        String majorB = columns.length >= 12 ? columns[11].trim() : "";

                        Map<String, Object> userData = new HashMap<>();
                        userData.put("tz", tz);
                        userData.put("firstName", firstName);
                        userData.put("lastName", lastName);
                        if (!middleName.isEmpty()) userData.put("middleName", middleName);
                        userData.put("email", email);
                        userData.put("password", password);
                        userData.put("type", 0);
                        userData.put("school", selectedSchool);

                        // המרה לרפרנסים לפי המילון שהורדנו
                        Map<String, DocumentReference> classesMap = new HashMap<>();
                        if (classDict.containsKey(homeroom)) classesMap.put("homeroom", classDict.get(homeroom));
                        if (classDict.containsKey(math)) classesMap.put("math", classDict.get(math));
                        if (classDict.containsKey(english)) classesMap.put("english", classDict.get(english));
                        if (classDict.containsKey(sports)) classesMap.put("sports", classDict.get(sports));
                        if (classDict.containsKey(majorA)) classesMap.put("major a", classDict.get(majorA));
                        if (classDict.containsKey(majorB)) classesMap.put("major b", classDict.get(majorB));

                        userData.put("classes", classesMap);

                        DocumentReference newUserRef = db.collection("users").document();
                        batch.set(newUserRef, userData);
                        count++;
                    }
                }
                reader.close();

                if (count > 0) {
                    final int finalCount = count;
                    batch.commit().addOnSuccessListener(aVoid -> {
                        Toast.makeText(this, "Successfully imported " + finalCount + " students! 🎉", Toast.LENGTH_LONG).show();
                    }).addOnFailureListener(e -> {
                        Toast.makeText(this, "Failed to import students: " + e.getMessage(), Toast.LENGTH_LONG).show();
                    });
                } else {
                    Toast.makeText(this, "No valid students found in CSV.", Toast.LENGTH_SHORT).show();
                }

            } catch (Exception e) {
                Toast.makeText(this, "Error reading students CSV file.", Toast.LENGTH_SHORT).show();
                e.printStackTrace();
            }
        });
    }

    private void processTeachersCsvFile(Uri uri) {
        DocumentReference selectedSchool = getSelectedSchoolRef();
        if (selectedSchool == null) {
            Toast.makeText(this, "Please select a school first!", Toast.LENGTH_SHORT).show();
            return;
        }

        try {
            InputStream inputStream = getContentResolver().openInputStream(uri);
            BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
            String line;
            WriteBatch batch = db.batch();
            int count = 0;
            boolean isFirstRow = true;

            while ((line = reader.readLine()) != null) {
                if (isFirstRow && line.toLowerCase().contains("email")) {
                    isFirstRow = false;
                    continue;
                }
                isFirstRow = false;

                String[] columns = line.split(",");
                if (columns.length >= 5) {
                    String tz = columns[0].trim();
                    String firstName = columns[1].trim();
                    String lastName = columns[2].trim();
                    String email = columns[3].trim();
                    String password = columns[4].trim();
                    String middleName = columns.length >= 6 ? columns[5].trim() : "";

                    Map<String, Object> userData = new HashMap<>();
                    userData.put("tz", tz);
                    userData.put("firstName", firstName);
                    userData.put("lastName", lastName);
                    if (!middleName.isEmpty()) userData.put("middleName", middleName);
                    userData.put("email", email);
                    userData.put("password", password);
                    userData.put("type", 1);
                    userData.put("school", selectedSchool);
                    userData.put("teachableSubjects", new ArrayList<>()); // ריק

                    DocumentReference newUserRef = db.collection("users").document();
                    batch.set(newUserRef, userData);
                    count++;
                }
            }
            reader.close();

            if (count > 0) {
                final int finalCount = count;
                batch.commit().addOnSuccessListener(aVoid -> {
                    Toast.makeText(this, "Successfully imported " + finalCount + " teachers! 🎉", Toast.LENGTH_LONG).show();
                }).addOnFailureListener(e -> {
                    Toast.makeText(this, "Failed to import teachers: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
            } else {
                Toast.makeText(this, "No valid teachers found in CSV.", Toast.LENGTH_SHORT).show();
            }

        } catch (Exception e) {
            Toast.makeText(this, "Error reading teachers CSV file.", Toast.LENGTH_SHORT).show();
            e.printStackTrace();
        }
    }
}