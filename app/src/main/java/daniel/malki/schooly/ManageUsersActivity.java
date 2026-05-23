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
    private Spinner spinnerFilterRole;
    private Spinner spinnerSchoolSelectManage;
    private RecyclerView rvUsers;
    private TextView tvNoResults;

    private UserAdapter adapter;
    private List<User> userList;
    private FirebaseFirestore db;

    private int currentAdminType;
    private String currentAdminId;

    private ArrayList<String> schoolNames = new ArrayList<>();
    private ArrayList<String> schoolIds = new ArrayList<>();
    private DocumentReference selectedSchoolRef;

    private String currentQuery = "";
    private int currentRoleFilter = 0;
    // הוספנו לתיעוד: 0=הכל, 1=תלמידים, 2=מורים, 3=School Admins, 4=Schooly Admins, 5=Exception Students

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setTitle("Manage Users");

        db = FirebaseFirestore.getInstance();

        etSearch = findViewById(R.id.etSearch);
        spinnerFilterRole = findViewById(R.id.spinnerFilterRole);
        spinnerSchoolSelectManage = findViewById(R.id.spinnerSchoolSelectManage);
        rvUsers = findViewById(R.id.rvUsers);
        tvNoResults = findViewById(R.id.tvNoResults);

        userList = new ArrayList<>();
        adapter = new UserAdapter(userList);
        rvUsers.setLayoutManager(new LinearLayoutManager(this));
        rvUsers.setAdapter(adapter);

        // קריאת נתוני המנהל המחובר מה-Session
        SharedPreferences prefs = getSharedPreferences("SchoolyPrefs", MODE_PRIVATE);
        currentAdminType = prefs.getInt("userType", 2);
        currentAdminId = prefs.getString("userId", "");

        // 🛠️ הגדרת ספינר התפקידים דינמית לפי סוג המנהל המחובר
        ArrayList<String> roleOptions = new ArrayList<>();
        roleOptions.add("All Roles");      // 0
        roleOptions.add("Students");       // 1
        roleOptions.add("Teachers");       // 2
        roleOptions.add("School Admins");  // 3

        // אם המשתמש הוא Schooly Admin, נוסיף לו את האופציה
        if (currentAdminType == 3) {
            roleOptions.add("Schooly Admins"); // 4
        } else {
            // אם הוא רק מנהל בית ספר, אנחנו חייבים להוסיף "מקום ריק" לאינדקס 4 כדי ש-5 יישאר 5.
            roleOptions.add("---"); // 4 (מוסתר או לא לשימוש)
        }

        // ✨ הוספת תלמידים חריגים לאינדקס 5!
        roleOptions.add("Exception Students ⚠️"); // 5

        ArrayAdapter<String> spinnerAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, roleOptions);
        spinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerFilterRole.setAdapter(spinnerAdapter);

        // קביעת הלוגיקה בהתאם לסוג המנהל
        if (currentAdminType == 3) {
            spinnerSchoolSelectManage.setVisibility(View.VISIBLE);
            loadAllSchoolsForSchoolyAdmin();
        } else {
            spinnerSchoolSelectManage.setVisibility(View.GONE);
            loadUsersForSchoolAdmin();
        }

        etSearch.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                currentQuery = s.toString();
                applyFilterAndCheckEmpty();
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });

        spinnerFilterRole.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                // אם בחרנו את המקום הריק (אינדקס 4 אצל מנהל בית ספר), נתעלם
                if (position == 4 && currentAdminType != 3) {
                    return;
                }

                currentRoleFilter = position;
                applyFilterAndCheckEmpty();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });
    }

    final ActivityResultLauncher<Intent> editUserLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == RESULT_OK) {
                    if (currentAdminType == 3) {
                        fetchUsers();
                    } else {
                        loadUsersForSchoolAdmin();
                    }
                }
            }
    );

    private void loadUsersForSchoolAdmin() {
        if (currentAdminId.isEmpty()) return;

        db.collection("users").document(currentAdminId).get()
                .addOnSuccessListener(adminDoc -> {
                    if (adminDoc.exists()) {
                        selectedSchoolRef = adminDoc.getDocumentReference("school");
                        if (selectedSchoolRef != null) {
                            fetchUsers();
                        } else {
                            Toast.makeText(this, "No school reference assigned to you!", Toast.LENGTH_SHORT).show();
                        }
                    }
                })
                .addOnFailureListener(e -> Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show());
    }

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
            spinnerSchoolSelectManage.setAdapter(schoolAdapter);

            spinnerSchoolSelectManage.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                @Override
                public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                    if (position == 0) {
                        selectedSchoolRef = null;
                    } else {
                        selectedSchoolRef = db.collection("schools").document(schoolIds.get(position));
                    }
                    fetchUsers();
                }

                @Override
                public void onNothingSelected(AdapterView<?> parent) {}
            });
        }).addOnFailureListener(e -> Toast.makeText(this, "Failed to load schools", Toast.LENGTH_SHORT).show());
    }

    private void fetchUsers() {
        Query usersQuery;

        if (selectedSchoolRef != null) {
            usersQuery = db.collection("users").whereEqualTo("school", selectedSchoolRef);
        } else {
            usersQuery = db.collection("users");
        }

        usersQuery.get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    userList.clear();
                    for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                        User user = document.toObject(User.class);
                        user.setUserId(document.getId());

                        // קריאת מפת הכיתות ושמירתה למודל המשתמש
                        Object classesObj = document.get("classes");
                        if (classesObj instanceof java.util.Map) {
                            user.setClasses((java.util.Map<String, DocumentReference>) classesObj);
                        }

                        userList.add(user);
                    }
                    adapter.updateList(userList);
                    applyFilterAndCheckEmpty();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Error loading users: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    private void applyFilterAndCheckEmpty() {
        if (adapter != null) {
            adapter.filter(currentQuery, currentRoleFilter);
            if (adapter.getItemCount() == 0) {
                tvNoResults.setVisibility(View.VISIBLE);
            } else {
                tvNoResults.setVisibility(View.GONE);
            }
        }
    }

    @Override
    protected int getLayoutResourceId() {
        return R.layout.activity_manage_users;
    }

    @Override
    protected int[] getAllowedUserTypes() {
        return new int[]{2, 3};
    }
}