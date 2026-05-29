package daniel.malki.schooly;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Patterns;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;

import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class AddUserActivity extends BaseMenuActivity {

    private EditText etTz, etFirstName, etLastName, etMiddleName, etEmail, etPassword;
    private Spinner spinnerRole, spinnerSchoolSelect;
    private TextView tvSchoolTitle;
    private ImageButton btnQuickAddSchool;
    private Button btnSaveUser, btnGoToCsvImport;
    private FrameLayout roleSpecificContainer;

    private FirebaseFirestore db;
    private ArrayList<String> schoolNames = new ArrayList<>();
    private ArrayList<DocumentReference> schoolRefs = new ArrayList<>();
    private DocumentReference selectedSchoolRef = null;

    private AddStudentFragment studentFragment;
    private AddTeacherFragment teacherFragment;
    private Fragment currentFragment;

    private int loggedInUserType = 3;
    private String loggedInSchoolPath = null;

    private final String[] ROLES = {"Student", "Teacher", "School Admin", "Schooly Admin"};

    @Override
    protected int getLayoutResourceId() {
        return R.layout.activity_add_user;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        db = FirebaseFirestore.getInstance();

        SharedPreferences prefs = getSharedPreferences("SchoolyPrefs", MODE_PRIVATE);
        loggedInUserType = prefs.getInt("user_type", 3);
        loggedInSchoolPath = prefs.getString("school_path", null);

        initViews();
        setupRoleSpinner();
        loadSchools();

        btnSaveUser.setOnClickListener(v -> saveNewUser());
        btnGoToCsvImport.setOnClickListener(v -> Toast.makeText(this, "CSV Import Screen coming soon! 📂", Toast.LENGTH_SHORT).show());
        btnQuickAddSchool.setOnClickListener(v -> showQuickAddSchoolDialog());

        btnGoToCsvImport.setOnClickListener(v -> {
            if (selectedSchoolRef == null) {
                Toast.makeText(this, "Please select a school from the list first.", Toast.LENGTH_LONG).show();
                return;
            }
            Intent intent = new Intent(AddUserActivity.this, CsvImportActivity.class);
            // מעבירים את ה-ID של בית הספר הנבחר למסך ה-CSV
            intent.putExtra("SCHOOL_ID", selectedSchoolRef.getId());
            startActivity(intent);
        });
    }

    private void initViews() {
        etTz = findViewById(R.id.etTz);
        etFirstName = findViewById(R.id.etFirstName);
        etLastName = findViewById(R.id.etLastName);
        etMiddleName = findViewById(R.id.etMiddleName);
        etEmail = findViewById(R.id.etEmail);
        etPassword = findViewById(R.id.etPassword);

        spinnerRole = findViewById(R.id.spinnerRole);
        spinnerSchoolSelect = findViewById(R.id.spinnerSchoolSelect);
        tvSchoolTitle = findViewById(R.id.tvSchoolTitle);
        btnQuickAddSchool = findViewById(R.id.btnQuickAddSchool);
        btnSaveUser = findViewById(R.id.btnSaveUser);
        btnGoToCsvImport = findViewById(R.id.btnGoToCsvImport);
        roleSpecificContainer = findViewById(R.id.roleSpecificContainer);
    }

    private void setupRoleSpinner() {
        ArrayAdapter<String> roleAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, ROLES);
        spinnerRole.setAdapter(roleAdapter);

        spinnerRole.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                handleRoleSelection(position);
            }
            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });
    }

    private void handleRoleSelection(int position) {
        if (position == 3) {
            tvSchoolTitle.setVisibility(View.GONE);
            spinnerSchoolSelect.setVisibility(View.GONE);
            btnQuickAddSchool.setVisibility(View.GONE);
            selectedSchoolRef = null;
            clearFragmentContainer();
            return;
        }

        tvSchoolTitle.setVisibility(View.VISIBLE);
        spinnerSchoolSelect.setVisibility(View.VISIBLE);
        btnQuickAddSchool.setVisibility(loggedInUserType == 3 ? View.VISIBLE : View.GONE);

        if (position == 0) {
            studentFragment = new AddStudentFragment();
            showFragment(studentFragment);
            studentFragment.setSchoolRefAndLoad(selectedSchoolRef);
        } else if (position == 1) {
            teacherFragment = new AddTeacherFragment();
            showFragment(teacherFragment);
            teacherFragment.setSchoolRefAndLoad(selectedSchoolRef);
        } else if (position == 2) {
            clearFragmentContainer();
        }
    }

    private void showFragment(Fragment fragment) {
        getSupportFragmentManager().beginTransaction().replace(R.id.roleSpecificContainer, fragment).commitNow();
        currentFragment = fragment;
        notifyFragmentSchoolChanged();
    }

    private void clearFragmentContainer() {
        if (currentFragment != null) {
            getSupportFragmentManager().beginTransaction().remove(currentFragment).commitNow();
            currentFragment = null;
        }
    }

    private void loadSchools() {
        db.collection("schools").get().addOnSuccessListener(snapshots -> {
            schoolNames.clear();
            schoolRefs.clear();

            schoolNames.add("Select School...");
            schoolRefs.add(null);

            int autoSelectIndex = -1;

            for (QueryDocumentSnapshot doc : snapshots) {
                // מקפידים רק על displayName, בלי name!
                String displayName = doc.getString("displayName");
                if (displayName == null) displayName = doc.getId();

                schoolNames.add(displayName);
                schoolRefs.add(doc.getReference());

                if (loggedInUserType == 2 && loggedInSchoolPath != null && doc.getReference().getPath().contains(loggedInSchoolPath)) {
                    autoSelectIndex = schoolNames.size() - 1;
                }
            }

            ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, schoolNames);
            spinnerSchoolSelect.setAdapter(adapter);

            if (loggedInUserType == 2 && autoSelectIndex != -1) {
                spinnerSchoolSelect.setSelection(autoSelectIndex);
                spinnerSchoolSelect.setEnabled(false);
                selectedSchoolRef = schoolRefs.get(autoSelectIndex);
                notifyFragmentSchoolChanged();
            }

            spinnerSchoolSelect.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                @Override
                public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                    if (loggedInUserType == 3) {
                        selectedSchoolRef = schoolRefs.get(position);
                        notifyFragmentSchoolChanged();
                    }
                }
                @Override
                public void onNothingSelected(AdapterView<?> parent) {}
            });
        });
    }

    private void notifyFragmentSchoolChanged() {
        if (currentFragment instanceof AddStudentFragment) {
            ((AddStudentFragment) currentFragment).setSchoolRefAndLoad(selectedSchoolRef);
        } else if (currentFragment instanceof AddTeacherFragment) {
            ((AddTeacherFragment) currentFragment).setSchoolRefAndLoad(selectedSchoolRef);
        }
    }

    private void showQuickAddSchoolDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Add New School");
        final EditText input = new EditText(this);
        input.setHint("School Name");
        builder.setView(input);

        builder.setPositiveButton("Add", (dialog, which) -> {
            String newSchool = input.getText().toString().trim();
            if (!newSchool.isEmpty()) {
                Map<String, Object> schoolData = new HashMap<>();
                schoolData.put("displayName", newSchool);
                db.collection("schools").add(schoolData).addOnSuccessListener(docRef -> {
                    Toast.makeText(this, "School added successfully!", Toast.LENGTH_SHORT).show();
                    loadSchools();
                });
            }
        });
        builder.setNegativeButton("Cancel", null);
        builder.create().show();
    }

    private void saveNewUser() {
        String tz = etTz.getText().toString().trim();
        String firstName = etFirstName.getText().toString().trim();
        String lastName = etLastName.getText().toString().trim();
        String email = etEmail.getText().toString().trim();
        String password = etPassword.getText().toString().trim();
        int rolePosition = spinnerRole.getSelectedItemPosition();

        if (TextUtils.isEmpty(tz) || TextUtils.isEmpty(firstName) || TextUtils.isEmpty(lastName) ||
                TextUtils.isEmpty(email) || TextUtils.isEmpty(password)) {
            Toast.makeText(this, "Please fill all required fields", Toast.LENGTH_SHORT).show();
            return;
        }
        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            Toast.makeText(this, "Please enter a valid email address", Toast.LENGTH_SHORT).show();
            return;
        }
        if (password.length() < 6) {
            Toast.makeText(this, "Password must be at least 6 characters", Toast.LENGTH_SHORT).show();
            return;
        }
        if (rolePosition < 3 && selectedSchoolRef == null) {
            Toast.makeText(this, "Please select a school", Toast.LENGTH_SHORT).show();
            return;
        }

        Map<String, Object> userData = new HashMap<>();
        userData.put("tz", tz);
        userData.put("firstName", firstName);
        userData.put("lastName", lastName);
        String middleName = etMiddleName.getText().toString().trim();
        if (!middleName.isEmpty()) userData.put("middleName", middleName);
        userData.put("email", email);
        userData.put("password", password);
        userData.put("type", rolePosition);
        if (selectedSchoolRef != null) userData.put("school", selectedSchoolRef);

        if (rolePosition == 0) {
            if (studentFragment == null) return;
            DocumentReference gradeRef = studentFragment.getSelectedGradeRef();
            if (gradeRef == null) {
                Toast.makeText(this, "Please select a main grade for the student", Toast.LENGTH_SHORT).show();
                return;
            }
            userData.put("grade", gradeRef);
            userData.put("classes", studentFragment.getSelectedClassesMap());

        } else if (rolePosition == 1) {
            if (teacherFragment == null) return;
            ArrayList<String> subjects = teacherFragment.getSelectedSubjects();
            userData.put("teachableSubjects", subjects);
        }

        btnSaveUser.setEnabled(false);
        db.collection("users").add(userData)
                .addOnSuccessListener(documentReference -> {
                    Toast.makeText(AddUserActivity.this, "User created successfully! 🎉", Toast.LENGTH_LONG).show();
                    finish();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(AddUserActivity.this, "Error: " + e.getMessage(), Toast.LENGTH_LONG).show();
                    btnSaveUser.setEnabled(true);
                });
    }
}