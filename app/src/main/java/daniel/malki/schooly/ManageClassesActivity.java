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
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ManageClassesActivity extends BaseMenuActivity {

    private EditText etSearchClass;
    private Spinner spinnerSchoolSelectClasses;
    private Spinner spinnerFilterClassType;
    private Spinner spinnerFilterGrade;
    private RecyclerView rvClasses;
    private TextView tvNoClassesResults;

    private ClassAdapter adapter;
    private List<SchoolClass> classList;
    private FirebaseFirestore db;

    private int currentAdminType;
    private String currentAdminId;

    private String currentSearchQuery = "";
    private String currentTypeFilter = "All Types";
    private String currentGradeFilter = "All Grades";

    private ArrayList<String> schoolNames = new ArrayList<>();
    private ArrayList<String> schoolIds = new ArrayList<>();
    private DocumentReference selectedSchoolRef = null;

    private Map<String, String> gradeMap = new HashMap<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setTitle("Manage Classes");

        SharedPreferences prefs = getSharedPreferences("SchoolyPrefs", MODE_PRIVATE);
        currentAdminType = prefs.getInt("userType", 2);
        currentAdminId = prefs.getString("userId", "");

        db = FirebaseFirestore.getInstance();
        classList = new ArrayList<>();

        initViews();
        setupFilters();

        // ✨ התיקון בהשראת ManageUsersActivity: טוענים את השכבות מיד ב-onCreate באופן עצמאי!
        loadGradesData();

        if (currentAdminType == 3) {
            spinnerSchoolSelectClasses.setVisibility(View.VISIBLE);
            loadSchools();
        } else if (currentAdminType == 2) {
            spinnerSchoolSelectClasses.setVisibility(View.GONE);
            loadAdminSchoolAndData();
        }
    }

    private void initViews() {
        etSearchClass = findViewById(R.id.etSearchClass);
        spinnerSchoolSelectClasses = findViewById(R.id.spinnerSchoolSelectClasses);
        spinnerFilterClassType = findViewById(R.id.spinnerFilterClassType);
        spinnerFilterGrade = findViewById(R.id.spinnerFilterGrade);
        rvClasses = findViewById(R.id.rvClasses);
        tvNoClassesResults = findViewById(R.id.tvNoClassesResults);

        rvClasses.setLayoutManager(new LinearLayoutManager(this));

        adapter = new ClassAdapter(classList, schoolClass -> {
            Intent intent = new Intent(ManageClassesActivity.this, ClassDetailActivity.class);
            intent.putExtra("classId", schoolClass.getClassId());
            startActivity(intent);
        });
        rvClasses.setAdapter(adapter);

        etSearchClass.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                currentSearchQuery = s.toString();
                applyFilterAndCheckEmpty();
            }
            @Override
            public void afterTextChanged(Editable s) {}
        });
    }

    private void setupFilters() {
        String[] types = {"All Types", "Homeroom", "Math", "English", "Physical Education", "Major A", "Major B"};
        ArrayAdapter<String> typeAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, types);
        typeAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerFilterClassType.setAdapter(typeAdapter);

        spinnerFilterClassType.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                currentTypeFilter = types[position];
                applyFilterAndCheckEmpty();
            }
            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });

        setupGradeSpinner(new ArrayList<>());
    }

    private void setupGradeSpinner(List<String> gradeNames) {
        List<String> displayGrades = new ArrayList<>();
        displayGrades.add("All Grades");
        displayGrades.addAll(gradeNames);

        ArrayAdapter<String> gradeAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, displayGrades);
        gradeAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerFilterGrade.setAdapter(gradeAdapter);

        spinnerFilterGrade.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                currentGradeFilter = displayGrades.get(position);
                applyFilterAndCheckEmpty();
            }
            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });
    }

    private void loadSchools() {
        db.collection("schools").get().addOnSuccessListener(queryDocumentSnapshots -> {
            schoolNames.clear();
            schoolIds.clear();

            schoolNames.add("All Schools");
            schoolIds.add("");

            for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                String sName = document.getString("displayName") != null ? document.getString("displayName") : document.getId();
                schoolNames.add(sName);
                schoolIds.add(document.getId());
            }

            ArrayAdapter<String> schoolAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, schoolNames);
            schoolAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
            spinnerSchoolSelectClasses.setAdapter(schoolAdapter);

            spinnerSchoolSelectClasses.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                @Override
                public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                    if (position > 0) {
                        selectedSchoolRef = db.collection("schools").document(schoolIds.get(position));
                    } else {
                        selectedSchoolRef = null;
                    }
                    // ✨ קורא רק לטעינת כיתות, השכבות כבר קיימות!
                    loadClassesData();
                }
                @Override
                public void onNothingSelected(AdapterView<?> parent) {}
            });
        });
    }

    private void loadAdminSchoolAndData() {
        db.collection("users").document(currentAdminId).get().addOnSuccessListener(documentSnapshot -> {
            if (documentSnapshot.exists()) {
                selectedSchoolRef = documentSnapshot.getDocumentReference("schoolRef");
                if (selectedSchoolRef != null) {
                    // ✨ קורא רק לטעינת כיתות
                    loadClassesData();
                }
            }
        });
    }

    private int extractGradeNumber(String name) {
        if (name == null) return 9999;
        String numStr = name.replaceAll("\\D+", "");
        if (numStr.isEmpty()) return 9999;
        return Integer.parseInt(numStr);
    }

    // ✨ הפונקציה החדשה והעצמאית לטעינת שכבות (בול כמו ב-ManageUsersActivity)
    private void loadGradesData() {
        db.collection("grades").get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    gradeMap.clear();

                    List<QueryDocumentSnapshot> sortedGrades = new ArrayList<>();
                    for (QueryDocumentSnapshot doc : queryDocumentSnapshots) {
                        sortedGrades.add(doc);
                    }

                    // מיון זהה לחלוטין לקובץ המשתמשים שעובד לך
                    java.util.Collections.sort(sortedGrades, (d1, d2) -> {
                        String n1 = d1.getString("displayName");
                        if (n1 == null) n1 = d1.getId();
                        String n2 = d2.getString("displayName");
                        if (n2 == null) n2 = d2.getId();

                        int num1 = extractGradeNumber(n1);
                        int num2 = extractGradeNumber(n2);

                        if (num1 != num2) {
                            return Integer.compare(num1, num2);
                        }
                        return n1.compareTo(n2);
                    });

                    List<String> currentSchoolGrades = new ArrayList<>();
                    for (QueryDocumentSnapshot doc : sortedGrades) {
                        String gradeId = doc.getId();
                        String gradeName = doc.getString("displayName") != null ? doc.getString("displayName") : doc.getId();

                        gradeMap.put(gradeId, gradeName);
                        currentSchoolGrades.add(gradeName);
                    }

                    setupGradeSpinner(currentSchoolGrades);
                });
    }

    private void loadClassesData() {
        // ✨ התיקון כאן: שינינו מ-schoolRef ל-school לפי הדאטהבייס שלך!
        Query classesQuery = (selectedSchoolRef != null) ?
                db.collection("classes").whereEqualTo("school", selectedSchoolRef) :
                db.collection("classes");

        classesQuery.get().addOnSuccessListener(queryDocumentSnapshots -> {
            classList.clear();
            for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                SchoolClass schoolClass = document.toObject(SchoolClass.class);
                schoolClass.setClassId(document.getId());

                if (schoolClass.getGradeRef() != null) {
                    String gradeId = schoolClass.getGradeRef().getId();
                    String gName = gradeMap.get(gradeId);
                    if (gName != null) {
                        schoolClass.setGradeNameForFilter(gName);
                    } else {
                        schoolClass.setGradeNameForFilter("Unknown");
                    }
                } else {
                    schoolClass.setGradeNameForFilter("Unknown");
                }

                classList.add(schoolClass);
            }
            adapter.updateList(classList);
            applyFilterAndCheckEmpty();
        }).addOnFailureListener(e -> Toast.makeText(this, "Error loading classes: " + e.getMessage(), Toast.LENGTH_SHORT).show());
    }

    private void applyFilterAndCheckEmpty() {
        if (adapter != null) {
            adapter.filter(currentSearchQuery, currentTypeFilter, currentGradeFilter);
            if (adapter.getItemCount() == 0) {
                tvNoClassesResults.setVisibility(View.VISIBLE);
            } else {
                tvNoClassesResults.setVisibility(View.GONE);
            }
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        // ✨ רענון אוטומטי של רשימת הכיתות
        loadClassesData();
    }

    @Override
    protected int getLayoutResourceId() {
        return R.layout.activity_manage_classes;
    }

    @Override
    protected int[] getAllowedUserTypes() {
        return new int[]{2, 3};
    }
}