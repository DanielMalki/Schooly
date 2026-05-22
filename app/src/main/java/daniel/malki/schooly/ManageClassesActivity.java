package daniel.malki.schooly;

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
    private Spinner spinnerSchoolSelectClasses; // הספינר החדש
    private RecyclerView rvClasses;
    private TextView tvNoClassesResults;

    private ClassAdapter adapter;
    private List<SchoolClass> classList;
    private FirebaseFirestore db;

    private int currentAdminType;
    private String currentAdminId;

    // רשימות לניהול בתי ספר
    private ArrayList<String> schoolNames = new ArrayList<>();
    private ArrayList<String> schoolIds = new ArrayList<>();
    private DocumentReference selectedSchoolRef; // null משמעותו "All Schools"

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setTitle("Manage Classes & Groups");

        db = FirebaseFirestore.getInstance();

        etSearchClass = findViewById(R.id.etSearchClass);
        spinnerSchoolSelectClasses = findViewById(R.id.spinnerSchoolSelectClasses);
        rvClasses = findViewById(R.id.rvClasses);
        tvNoClassesResults = findViewById(R.id.tvNoClassesResults);

        classList = new ArrayList<>();
        adapter = new ClassAdapter(classList);
        rvClasses.setLayoutManager(new LinearLayoutManager(this));
        rvClasses.setAdapter(adapter);

        // קריאת נתוני המנהל המחובר
        SharedPreferences prefs = getSharedPreferences("SchoolyPrefs", MODE_PRIVATE);
        currentAdminType = prefs.getInt("userType", 2);
        currentAdminId = prefs.getString("userId", "");

        // בדיקת סוג המנהל וקביעת לוגיקת טעינה
        if (currentAdminType == 3) {
            spinnerSchoolSelectClasses.setVisibility(View.VISIBLE);
            loadAllSchoolsForSchoolyAdmin();
        } else {
            spinnerSchoolSelectClasses.setVisibility(View.GONE);
            loadSchoolAdminLocation();
        }

        etSearchClass.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                adapter.filter(s.toString());
                checkEmptyState();
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });
    }

    /**
     * מנהל בית ספר (2) - שליפת ה-schoolRef שלו כדי לסנן את הכיתות
     */
    private void loadSchoolAdminLocation() {
        if (currentAdminId.isEmpty()) return;

        db.collection("users").document(currentAdminId).get()
                .addOnSuccessListener(adminDoc -> {
                    if (adminDoc.exists()) {
                        selectedSchoolRef = adminDoc.getDocumentReference("school");
                        if (selectedSchoolRef != null) {
                            fetchClasses();
                        } else {
                            Toast.makeText(this, "No school reference assigned to you!", Toast.LENGTH_SHORT).show();
                        }
                    }
                })
                .addOnFailureListener(e -> Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show());
    }

    /**
     * מנהל על (3) - טעינת כל בתי הספר לספינר כולל אפשרות גלובלית
     */
    private void loadAllSchoolsForSchoolyAdmin() {
        db.collection("schools").get().addOnSuccessListener(queryDocumentSnapshots -> {
            schoolNames.clear();
            schoolIds.clear();

            schoolNames.add("🌍 All Schools");
            schoolIds.add("all");

            for (QueryDocumentSnapshot doc : queryDocumentSnapshots) {
                schoolIds.add(doc.getId());
                String name = doc.getString("displayName");
                if (name == null) name = doc.getId();
                schoolNames.add(name);
            }

            ArrayAdapter<String> schoolAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, schoolNames);
            spinnerSchoolSelectClasses.setAdapter(schoolAdapter);

            spinnerSchoolSelectClasses.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                @Override
                public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                    if (position == 0) {
                        selectedSchoolRef = null; // All Schools
                    } else {
                        selectedSchoolRef = db.collection("schools").document(schoolIds.get(position));
                    }
                    fetchClasses();
                }

                @Override
                public void onNothingSelected(AdapterView<?> parent) {}
            });
        }).addOnFailureListener(e -> Toast.makeText(this, "Failed to load schools", Toast.LENGTH_SHORT).show());
    }

    /**
     * שליפת הכיתות בפועל מתוך Firestore לפי הסינון שנבחר
     */
    private void fetchClasses() {
        Query classesQuery;

        if (selectedSchoolRef != null) {
            // סינון לפי בית ספר ספציפי
            classesQuery = db.collection("classes").whereEqualTo("school", selectedSchoolRef);
        } else {
            // שליפת כל הכיתות של כל בתי הספר יחד
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
                    checkEmptyState();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Error loading classes: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    private void checkEmptyState() {
        if (adapter.getItemCount() == 0) {
            tvNoClassesResults.setVisibility(View.VISIBLE);
        } else {
            tvNoClassesResults.setVisibility(View.GONE);
        }
    }

    @Override
    protected int getLayoutResourceId() {
        return R.layout.activity_manage_classes;
    }

    @Override
    protected int[] getAllowedUserTypes() {
        // מתן הרשאה לשני סוגי המנהלים
        return new int[]{2, 3};
    }
}