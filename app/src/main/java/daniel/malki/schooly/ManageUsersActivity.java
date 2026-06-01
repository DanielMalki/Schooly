package daniel.malki.schooly;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.List;

public class ManageUsersActivity extends BaseMenuActivity {

    private EditText etSearch;
    private Spinner spinnerSchoolSelectManage, spinnerFilterRole;
    private Spinner spinnerStudentType, spinnerStudentGrade;
    private Spinner spinnerTeacherType, spinnerTeacherSubject;
    private LinearLayout llStudentFilters, llTeacherFilters;

    private RecyclerView rvUsers;
    private TextView tvNoResults;

    private UserAdapter adapter;
    private List<User> fullFetchedUsers; // כל המשתמשים שנשלפו כרגע מהשרת
    private FirebaseFirestore db;

    private int currentAdminType;
    private DocumentReference selectedSchoolRef;

    private ArrayList<String> schoolNames = new ArrayList<>();
    private ArrayList<String> schoolIds = new ArrayList<>();

    private ArrayList<String> gradeNames = new ArrayList<>();
    private ArrayList<String> gradeIds = new ArrayList<>();

    private ArrayList<String> subjectNames = new ArrayList<>();
    private ArrayList<String> subjectIds = new ArrayList<>();

    private final ActivityResultLauncher<Intent> detailActivityLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == RESULT_OK) {
                    refreshCurrentList();
                }
            }
    );

    @Override
    protected int getLayoutResourceId() {
        return R.layout.activity_manage_users;
    }

    @Override
    protected int[] getAllowedUserTypes() {
        return new int[]{2, 3};
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        db = FirebaseFirestore.getInstance();
        fullFetchedUsers = new ArrayList<>();

        initViews();
        setupRecyclerView();
        // ✨ התיקון הקריטי: קודם כל קובעים את הקשר האדמין (טוענים את סוג המשתמש וה-School ID)
        determineAdminContext();
        // רק לאחר מכן טוענים את שאר הפילטרים והמאזינים בצורה בטוחה
        loadDynamicFilterData(); // טוען שכבות ומקצועות
        setupFilters();
    }

    private void initViews() {
        etSearch = findViewById(R.id.etSearch);
        spinnerSchoolSelectManage = findViewById(R.id.spinnerSchoolSelectManage);
        spinnerFilterRole = findViewById(R.id.spinnerFilterRole);

        llStudentFilters = findViewById(R.id.llStudentFilters);
        spinnerStudentType = findViewById(R.id.spinnerStudentType);
        spinnerStudentGrade = findViewById(R.id.spinnerStudentGrade);

        llTeacherFilters = findViewById(R.id.llTeacherFilters);
        spinnerTeacherType = findViewById(R.id.spinnerTeacherType);
        spinnerTeacherSubject = findViewById(R.id.spinnerTeacherSubject);

        rvUsers = findViewById(R.id.rvUsers);
        tvNoResults = findViewById(R.id.tvNoResults);
    }

    private void setupRecyclerView() {
        rvUsers.setLayoutManager(new LinearLayoutManager(this));
        adapter = new UserAdapter(new ArrayList<>());
        rvUsers.setAdapter(adapter);

        adapter.setOnItemClickListener(selectedUser -> {
            if (selectedUser != null) {
                Intent intent = new Intent(ManageUsersActivity.this, UserDetailActivity.class);

                // ✨ התיקון: משנים מ-"USER_ID" ל-"userId" כדי שיתאים בדיוק למסך הבא!
                intent.putExtra("userId", selectedUser.getUserId());

                if (selectedSchoolRef != null) {
                    intent.putExtra("SCHOOL_ID", selectedSchoolRef.getId());
                }
                detailActivityLauncher.launch(intent);
            }
        });
    }

    private void determineAdminContext() {
        SharedPreferences prefs = getSharedPreferences("SchoolyPrefs", MODE_PRIVATE);
        currentAdminType = prefs.getInt("userType", -1);

        if (currentAdminType == 3) {
            spinnerSchoolSelectManage.setVisibility(View.VISIBLE);
            loadSchoolsForAdmin3();
        } else if (currentAdminType == 2) {
            spinnerSchoolSelectManage.setVisibility(View.GONE);
            String schoolId = prefs.getString("currentSchoolId", null);
            if (schoolId != null) {
                selectedSchoolRef = db.collection("schools").document(schoolId);
                loadUsersBySchool();
            } else {
                Toast.makeText(this, "Error: School profile context missing.", Toast.LENGTH_LONG).show();
                finish();
            }
        }
    }

    private void loadSchoolsForAdmin3() {
        db.collection("schools").get().addOnSuccessListener(queryDocumentSnapshots -> {
            schoolNames.clear(); schoolIds.clear();
            schoolNames.add("All Schools"); schoolIds.add("");

            for (QueryDocumentSnapshot doc : queryDocumentSnapshots) {
                String name = doc.getString("displayName");
                if (name == null) name = doc.getString("name");
                schoolNames.add(name != null ? name : "School ID: " + doc.getId());
                schoolIds.add(doc.getId());
            }

            ArrayAdapter<String> schoolAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, schoolNames);
            schoolAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
            spinnerSchoolSelectManage.setAdapter(schoolAdapter);

            spinnerSchoolSelectManage.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                @Override
                public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                    if (position == 0) {
                        selectedSchoolRef = null;
                        loadAllUsers();
                    } else {
                        selectedSchoolRef = db.collection("schools").document(schoolIds.get(position));
                        loadUsersBySchool();
                    }
                }
                @Override
                public void onNothingSelected(AdapterView<?> parent) {}
            });
        });
    }

    // פונקציית עזר לחילוץ המספר מה-displayName לטובת המיון (בדיוק כמו ב-AddStudentFragment)
    private int extractGradeNumber(String name) {
        if (name == null) return 9999;
        String numStr = name.replaceAll("\\D+", ""); // מנקה הכל חוץ מספרות
        if (numStr.isEmpty()) return 9999; // מטפל ב-Graduated
        return Integer.parseInt(numStr);
    }

    private void loadDynamicFilterData() {
        // טעינת שכבות - ללא orderBy! מיון מקומי וחכם לפי displayName
        db.collection("grades").get().addOnSuccessListener(snap -> {
            gradeNames.clear(); gradeIds.clear();
            gradeNames.add("All Grades"); gradeIds.add("");

            // העברה לרשימה זמנית לטובת מיון
            List<QueryDocumentSnapshot> sortedGrades = new ArrayList<>();
            for (QueryDocumentSnapshot doc : snap) {
                sortedGrades.add(doc);
            }

            // המיון המקומי שלך מ-AddStudentFragment
            java.util.Collections.sort(sortedGrades, (d1, d2) -> {
                String n1 = d1.getString("displayName");
                if (n1 == null) n1 = d1.getId();
                String n2 = d2.getString("displayName");
                if (n2 == null) n2 = d2.getId();

                int num1 = extractGradeNumber(n1);
                int num2 = extractGradeNumber(n2);

                if (num1 != num2) {
                    return Integer.compare(num1, num2); // מיון מספרי תקין
                }
                return n1.compareTo(n2);
            });

            // הכנסה לספינר אחרי המיון
            for (QueryDocumentSnapshot doc : sortedGrades) {
                String displayName = doc.getString("displayName");
                gradeNames.add(displayName != null ? displayName : doc.getId());
                gradeIds.add(doc.getId());
            }

            ArrayAdapter<String> gradeAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, gradeNames);
            gradeAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
            spinnerStudentGrade.setAdapter(gradeAdapter);
        });

        // טעינת מקצועות (זה יעבוד מעולה עם orderBy כי כאן באמת לכולם יש displayName)
        db.collection("subjects")
                .orderBy("displayName", com.google.firebase.firestore.Query.Direction.ASCENDING)
                .get()
                .addOnSuccessListener(snap -> {
                    subjectNames.clear(); subjectIds.clear();
                    subjectNames.add("All Subjects"); subjectIds.add("");

                    for (QueryDocumentSnapshot doc : snap) {
                        String displayName = doc.getString("displayName");
                        subjectNames.add(displayName != null ? displayName : doc.getId());
                        subjectIds.add(doc.getId());
                    }

                    ArrayAdapter<String> subjectAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, subjectNames);
                    subjectAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                    spinnerTeacherSubject.setAdapter(subjectAdapter);
                });
    }

    private void setupFilters() {
        // 1. תפקיד ראשי
        String[] roles = {"All Users", "Students", "Teachers & Staff", "Schooly Admins"};
        ArrayAdapter<String> roleAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, roles);
        roleAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerFilterRole.setAdapter(roleAdapter);

        // 2. סוג תלמיד
        String[] studentTypes = {"All Students", "Regular Students", "Exception Students ⚠️"};
        ArrayAdapter<String> stAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, studentTypes);
        stAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerStudentType.setAdapter(stAdapter);

        // 3. סוג מורה/צוות
        String[] teacherTypes = {"All Staff", "Teachers Only", "School Admins"};
        ArrayAdapter<String> ttAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, teacherTypes);
        ttAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerTeacherType.setAdapter(ttAdapter);

        // האזנה לשינויים בספינר הראשי
        spinnerFilterRole.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                // הצגה/הסתרה של תפריטי המשנה
                llStudentFilters.setVisibility(position == 1 ? View.VISIBLE : View.GONE);
                llTeacherFilters.setVisibility(position == 2 ? View.VISIBLE : View.GONE);

                // הגיון של מנהל מערכת (העלמת בתי ספר כשבוחרים Schooly Admins)
                if (currentAdminType == 3) {
                    if (position == 3) {
                        spinnerSchoolSelectManage.setVisibility(View.GONE);
                        loadAllUsers(); // נטען את כולם כדי שנוכל לראות את ה-Schooly Admins
                        return; // הפונקציה תפעיל את applyFilters בסיום השליפה
                    } else if (spinnerSchoolSelectManage.getVisibility() == View.GONE) {
                        spinnerSchoolSelectManage.setVisibility(View.VISIBLE);
                        refreshCurrentList();
                        return;
                    }
                }
                applyFilters();
            }
            @Override public void onNothingSelected(AdapterView<?> parent) {}
        });

        // האזנה לכל שאר הספינרים והחיפוש
        AdapterView.OnItemSelectedListener triggerFilter = new AdapterView.OnItemSelectedListener() {
            @Override public void onItemSelected(AdapterView<?> p, View v, int pos, long id) { applyFilters(); }
            @Override public void onNothingSelected(AdapterView<?> p) {}
        };

        spinnerStudentType.setOnItemSelectedListener(triggerFilter);
        spinnerStudentGrade.setOnItemSelectedListener(triggerFilter);
        spinnerTeacherType.setOnItemSelectedListener(triggerFilter);
        spinnerTeacherSubject.setOnItemSelectedListener(triggerFilter);

        etSearch.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) { applyFilters(); }
            @Override public void afterTextChanged(Editable s) {}
        });
    }

    private void fetchUsersByQuery(Query query) {
        query.get().addOnSuccessListener(queryDocumentSnapshots -> {
            fullFetchedUsers.clear();
            for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                User user = document.toObject(User.class);
                user.setUserId(document.getId());

                Object classesObj = document.get("classes");
                if (classesObj instanceof java.util.Map) {
                    user.setClasses((java.util.Map<String, DocumentReference>) classesObj);
                }
                fullFetchedUsers.add(user);
            }
            applyFilters();
        }).addOnFailureListener(e -> Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show());
    }

    private void loadAllUsers() { fetchUsersByQuery(db.collection("users")); }

    private void loadUsersBySchool() {
        if (selectedSchoolRef != null) {
            fetchUsersByQuery(db.collection("users").whereEqualTo("school", selectedSchoolRef));
        }
    }

    private void refreshCurrentList() {
        if (selectedSchoolRef == null && currentAdminType == 3) {
            loadAllUsers();
        } else {
            loadUsersBySchool();
        }
    }

    // הלב של הסינון - רץ מקומית על הרשימה שנשלפה!
    private void applyFilters() {
        if (adapter == null) return;

        List<User> filteredList = new ArrayList<>();
        String query = etSearch.getText().toString().trim().toLowerCase();
        int mainRole = spinnerFilterRole.getSelectedItemPosition();

        for (User user : fullFetchedUsers) {
            // 1. סינון טקסט (חיפוש)
            if (!query.isEmpty()) {
                boolean matchesName = user.getName() != null && user.getName().toLowerCase().contains(query);
                boolean matchesId = user.getUserId() != null && user.getUserId().toLowerCase().contains(query);
                if (!matchesName && !matchesId) continue;
            }

            // 2. סינון תפקידים וספינרים דינמיים
            if (mainRole == 1) { // תלמידים
                if (user.getType() != 0) continue;

                int studentType = spinnerStudentType.getSelectedItemPosition();
                if (studentType == 1 && user.isExceptionStudent()) continue; // רק רגילים
                if (studentType == 2 && !user.isExceptionStudent()) continue; // רק חריגים

                int gradePos = spinnerStudentGrade.getSelectedItemPosition();
                if (gradePos > 0 && gradeIds != null && gradePos < gradeIds.size()) {
                    String selectedGradeId = gradeIds.get(gradePos);
                    if (user.getGrade() == null || !user.getGrade().getId().equals(selectedGradeId)) continue;
                }

            } else if (mainRole == 2) { // מורים והנהלה
                if (user.getType() != 1 && user.getType() != 2) continue;

                int teacherType = spinnerTeacherType.getSelectedItemPosition();
                if (teacherType == 1 && user.getType() != 1) continue; // רק מורים
                if (teacherType == 2 && user.getType() != 2) continue; // רק הנהלת בית ספר

                int subjectPos = spinnerTeacherSubject.getSelectedItemPosition();
                if (subjectPos > 0 && subjectIds != null && subjectPos < subjectIds.size()) {
                    String selectedSubId = subjectIds.get(subjectPos);
                    boolean hasSubject = false;
                    if (user.getTeachableSubjects() != null) {
                        for (DocumentReference subRef : user.getTeachableSubjects()) {
                            if (subRef.getId().equals(selectedSubId)) {
                                hasSubject = true;
                                break;
                            }
                        }
                    }
                    if (!hasSubject) continue;
                }

            } else if (mainRole == 3) { // מנהלי מערכת
                if (user.getType() != 3) continue;
            }

            filteredList.add(user);
        }

        adapter.updateList(filteredList);
        tvNoResults.setVisibility(filteredList.isEmpty() ? View.VISIBLE : View.GONE);
    }
}