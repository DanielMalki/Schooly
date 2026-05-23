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
import java.util.List;

public class ManageClassesActivity extends BaseMenuActivity {

    private EditText etSearchClass;
    private Spinner spinnerSchoolSelectClasses;
    private Spinner spinnerFilterClassType;
    private RecyclerView rvClasses;
    private TextView tvNoClassesResults;

    private ClassAdapter adapter;
    private List<SchoolClass> classList;
    private FirebaseFirestore db;

    private int currentAdminType;
    private String currentAdminId;

    private String currentSearchQuery = "";
    private String currentTypeFilter = "All Types";

    private ArrayList<String> schoolNames = new ArrayList<>();
    private ArrayList<String> schoolIds = new ArrayList<>();
    private DocumentReference selectedSchoolRef;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setTitle("Manage Classes & Groups");

        db = FirebaseFirestore.getInstance();

        etSearchClass = findViewById(R.id.etSearchClass);
        spinnerSchoolSelectClasses = findViewById(R.id.spinnerSchoolSelectClasses);
        spinnerFilterClassType = findViewById(R.id.spinnerFilterClassType);
        rvClasses = findViewById(R.id.rvClasses);
        tvNoClassesResults = findViewById(R.id.tvNoClassesResults);

        rvClasses.setLayoutManager(new LinearLayoutManager(this));
        classList = new ArrayList<>();

        adapter = new ClassAdapter(classList, schoolClass -> {
            Intent intent = new Intent(ManageClassesActivity.this, ClassDetailActivity.class);
            intent.putExtra("classId", schoolClass.getClassId());
            startActivity(intent);
        });
        rvClasses.setAdapter(adapter);

        // קריאת נתוני המנהל מה-Session המשותף
        SharedPreferences prefs = getSharedPreferences("SchoolyPrefs", MODE_PRIVATE);
        currentAdminType = prefs.getInt("userType", 2);
        currentAdminId = prefs.getString("userId", "");

        setupFilters();
        setupAdminLogic();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (selectedSchoolRef != null || currentAdminType == 3) {
            fetchClasses();
        }
    }

    private void setupAdminLogic() {
        if (currentAdminType == 3) {
            spinnerSchoolSelectClasses.setVisibility(View.VISIBLE);
            loadAllSchoolsForAdmin();
        } else if (currentAdminType == 2) {
            spinnerSchoolSelectClasses.setVisibility(View.GONE);
            loadAdminSchoolAndFetchClasses();
        } else {
            Toast.makeText(this, "Access Denied", Toast.LENGTH_SHORT).show();
            finish();
        }
    }

    private void loadAllSchoolsForAdmin() {
        db.collection("schools").get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    schoolNames.clear();
                    schoolIds.clear();

                    schoolNames.add("🌍 All Schools");
                    schoolIds.add("ALL");

                    for (QueryDocumentSnapshot doc : queryDocumentSnapshots) {
                        schoolIds.add(doc.getId());
                        String sName = doc.getString("displayName");
                        schoolNames.add(sName != null ? sName : doc.getId());
                    }

                    ArrayAdapter<String> schoolAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, schoolNames);
                    schoolAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                    spinnerSchoolSelectClasses.setAdapter(schoolAdapter);

                    spinnerSchoolSelectClasses.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                        @Override
                        public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                            if (position == 0) {
                                selectedSchoolRef = null;
                            } else {
                                selectedSchoolRef = db.collection("schools").document(schoolIds.get(position));
                            }
                            fetchClasses();
                        }

                        @Override
                        public void onNothingSelected(AdapterView<?> parent) {}
                    });
                })
                .addOnFailureListener(e -> Toast.makeText(this, "Error loading schools", Toast.LENGTH_SHORT).show());
    }

    private void loadAdminSchoolAndFetchClasses() {
        if (currentAdminId == null || currentAdminId.isEmpty()) return;

        db.collection("users").document(currentAdminId).get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        selectedSchoolRef = documentSnapshot.getDocumentReference("school");
                        fetchClasses();
                    }
                })
                .addOnFailureListener(e -> Toast.makeText(this, "Error finding admin school", Toast.LENGTH_SHORT).show());
    }

    private void setupFilters() {
        String[] classTypes = {"All Types", "Homeroom", "Math", "English", "Physical Education", "Major A", "Major B"};
        ArrayAdapter<String> typeAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, classTypes);
        typeAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerFilterClassType.setAdapter(typeAdapter);

        spinnerFilterClassType.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                currentTypeFilter = classTypes[position];
                applyFilterAndCheckEmpty();
            }
            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });

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

    private void fetchClasses() {
        Query classesQuery;

        if (selectedSchoolRef != null) {
            classesQuery = db.collection("classes").whereEqualTo("school", selectedSchoolRef);
        } else {
            classesQuery = db.collection("classes");
        }

        classesQuery.get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    classList.clear();
                    for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                        SchoolClass schoolClass = document.toObject(SchoolClass.class);
                        schoolClass.setClassId(document.getId());
                        classList.add(schoolClass);
                    }
                    adapter.updateList(classList);
                    applyFilterAndCheckEmpty();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Error loading classes: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    private void applyFilterAndCheckEmpty() {
        if (adapter != null) {
            adapter.filter(currentSearchQuery, currentTypeFilter);
            if (adapter.getItemCount() == 0) {
                tvNoClassesResults.setVisibility(View.VISIBLE);
            } else {
                tvNoClassesResults.setVisibility(View.GONE);
            }
        }
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