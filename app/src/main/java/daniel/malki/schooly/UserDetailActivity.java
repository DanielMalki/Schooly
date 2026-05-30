package daniel.malki.schooly;

import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.text.TextUtils;
import android.util.Patterns;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AlertDialog;

import com.bumptech.glide.Glide;
import com.google.firebase.firestore.Blob;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class UserDetailActivity extends BaseMenuActivity {

    private ImageView imgDetailAvatar;
    private ImageButton btnEditAvatar;
    private EditText etTz, etFirstName, etMiddleName, etLastName, etNewEmail;
    private Spinner spinnerRole, spinnerSchoolSelect;
    private TextView tvSchoolTitle;
    private EditText etSchoolLocked;

    // רכיבי התיכון המעוצבים
    private AutoCompleteTextView autoCompleteSchool;
    private com.google.android.material.textfield.TextInputLayout layoutSchoolSelect;
    private ArrayList<String> schoolNames = new ArrayList<>();
    private ArrayList<DocumentReference> schoolRefs = new ArrayList<>();
    private DocumentReference selectedSchoolRef;

    private ImageButton btnQuickAddSchool;
    private View viewSchoolDivider;
    private Button btnSaveUserDetails, btnDeleteUser, btnChangePasswordPlaceholder;

    private FirebaseFirestore db;
    private String selectedUserId;
    private int userTypeInt = 0; // 0=Student, 1=Teacher, 2=SchoolAdmin, 3=SchoolyAdmin

    private EditStudentFragment editStudentFragment;
    private EditTeacherFragment editTeacherFragment;

    private ArrayList<String> roleNames = new ArrayList<>();
    private Map<String, Object> loadedClassesMap = new HashMap<>();
    private List<DocumentReference> loadedSubjectsList = new ArrayList<>();

    private Bitmap selectedBitmap = null;

    private final ActivityResultLauncher<Intent> pickImageLauncher =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
                if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                    Uri imageUri = result.getData().getData();
                    if (imageUri != null) {
                        try {
                            InputStream is = getContentResolver().openInputStream(imageUri);
                            selectedBitmap = BitmapFactory.decodeStream(is);
                            imgDetailAvatar.setImageBitmap(selectedBitmap);
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                }
            });

    @Override
    protected int getLayoutResourceId() {
        return R.layout.activity_user_detail;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        db = FirebaseFirestore.getInstance();
        selectedUserId = getIntent().getStringExtra("userId");

        if (selectedUserId == null) {
            Toast.makeText(this, "No user ID provided", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        initViews();
        loadRoles();
    }

    private void initViews() {
        imgDetailAvatar = findViewById(R.id.imgDetailAvatar);
        btnEditAvatar = findViewById(R.id.btnEditAvatar);
        etTz = findViewById(R.id.etTz);
        etFirstName = findViewById(R.id.etFirstName);
        etMiddleName = findViewById(R.id.etMiddleName);
        etLastName = findViewById(R.id.etLastName);
        etNewEmail = findViewById(R.id.etNewEmail);
        spinnerRole = findViewById(R.id.spinnerRole);
        spinnerSchoolSelect = findViewById(R.id.spinnerSchoolSelect);
        tvSchoolTitle = findViewById(R.id.tvSchoolTitle);
        etSchoolLocked = findViewById(R.id.etSchoolLocked);
        btnQuickAddSchool = findViewById(R.id.btnQuickAddSchool);
        viewSchoolDivider = findViewById(R.id.viewSchoolDivider);
        btnSaveUserDetails = findViewById(R.id.btnSaveUserDetails);
        btnDeleteUser = findViewById(R.id.btnDeleteUser);
        btnChangePasswordPlaceholder = findViewById(R.id.btnChangePasswordPlaceholder);

        autoCompleteSchool = findViewById(R.id.autoCompleteSchool);
        layoutSchoolSelect = findViewById(R.id.layoutSchoolSelect);

        btnEditAvatar.setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
            pickImageLauncher.launch(intent);
        });

        btnSaveUserDetails.setOnClickListener(v -> saveUserData());
        btnDeleteUser.setOnClickListener(v -> deleteUser());
    }

    private void loadRoles() {
        roleNames.clear();
        roleNames.add("Student");
        roleNames.add("Teacher");
        roleNames.add("School Admin");
        roleNames.add("Schooly Admin");

        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, roleNames);
        spinnerRole.setAdapter(adapter);
        spinnerRole.setEnabled(false);

        loadSchools();
    }

    private void loadSchools() {
        db.collection("schools").get().addOnSuccessListener(snapshots -> {
            schoolNames.clear();
            schoolRefs.clear();
            for (QueryDocumentSnapshot doc : snapshots) {
                String name = doc.getString("name");
                if (name == null) name = doc.getString("displayName");
                if (name == null) name = doc.getId();

                schoolNames.add(name);
                schoolRefs.add(doc.getReference());
            }
            loadUserData();
        });
    }

    private void loadUserData() {
        db.collection("users").document(selectedUserId).get().addOnSuccessListener(doc -> {
            if (!doc.exists()) return;

            etTz.setText(doc.getId());
            etFirstName.setText(doc.getString("firstName"));
            etMiddleName.setText(doc.getString("middleName"));
            etLastName.setText(doc.getString("lastName"));
            etNewEmail.setText(doc.getString("email"));

            Long typeLong = doc.getLong("type");
            userTypeInt = (typeLong != null) ? typeLong.intValue() : 0;
            if (userTypeInt >= 0 && userTypeInt < roleNames.size()) {
                spinnerRole.setSelection(userTypeInt);
            }

            selectedSchoolRef = doc.getDocumentReference("school");
            if (selectedSchoolRef == null) {
                selectedSchoolRef = doc.getDocumentReference("schoolRef");
            }

            SharedPreferences sp = getSharedPreferences("SchoolyPrefs", MODE_PRIVATE);
            int loggedInType = sp.getInt("userType", -1);

            // בדיקה אם המשתמש המחובר הוא Schooly Admin (רמה 3)
            if (loggedInType == 3) {
                layoutSchoolSelect.setEnabled(true);

                int initialPosition = 0;
                if (selectedSchoolRef != null) {
                    for (int i = 0; i < schoolRefs.size(); i++) {
                        if (schoolRefs.get(i).getId().equals(selectedSchoolRef.getId())) {
                            initialPosition = i;
                            break;
                        }
                    }
                }

                ArrayAdapter<String> schoolAdapter = new ArrayAdapter<>(UserDetailActivity.this, android.R.layout.simple_spinner_dropdown_item, schoolNames);
                autoCompleteSchool.setAdapter(schoolAdapter);
                if (!schoolNames.isEmpty()) {
                    autoCompleteSchool.setText(schoolNames.get(initialPosition), false);
                }

                // מאזין חכם שמחליף את הפרגמנט בצורה נקייה ובטוחה ללא קריסות
                autoCompleteSchool.setOnItemClickListener((parent, view1, position, id) -> {
                    selectedSchoolRef = schoolRefs.get(position);

                    if (userTypeInt == 0) { // Student
                        // מייצרים מופע חדש של הפרגמנט עם רפרנס בית הספר המעודכן
                        editStudentFragment = EditStudentFragment.newInstance(selectedSchoolRef, loadedClassesMap);
                        getSupportFragmentManager().beginTransaction()
                                .replace(R.id.dynamicFieldsContainer, editStudentFragment)
                                .commitAllowingStateLoss();
                    }
                });

            } else {
                // נעול לצפייה בלבד עבור משתמשים אחרים
                layoutSchoolSelect.setEnabled(false);

                if (selectedSchoolRef != null) {
                    String displayName = selectedSchoolRef.getId();
                    for (int i = 0; i < schoolRefs.size(); i++) {
                        if (schoolRefs.get(i).getId().equals(selectedSchoolRef.getId())) {
                            displayName = schoolNames.get(i);
                            break;
                        }
                    }
                    autoCompleteSchool.setText(displayName, false);
                } else {
                    autoCompleteSchool.setText("No School Assigned", false);
                }
            }

            if (doc.contains("avatarBlob")) {
                Blob blob = doc.getBlob("avatarBlob");
                if (blob != null) {
                    byte[] bytes = blob.toBytes();
                    Bitmap bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
                    imgDetailAvatar.setImageBitmap(bitmap);
                }
            }

            // טעינה ראשונית של הפרגמנטים הדינמיים לפי סוג המשתמש
            if (userTypeInt == 0) { // Student
                Map<String, Object> classes = (Map<String, Object>) doc.get("classes");
                if (classes != null) loadedClassesMap = classes;

                editStudentFragment = EditStudentFragment.newInstance(selectedSchoolRef, loadedClassesMap);
                getSupportFragmentManager().beginTransaction()
                        .replace(R.id.dynamicFieldsContainer, editStudentFragment)
                        .commitAllowingStateLoss();
            } else if (userTypeInt == 1) { // Teacher
                List<DocumentReference> subjects = (List<DocumentReference>) doc.get("teachableSubjects");
                if (subjects != null) loadedSubjectsList = subjects;

                editTeacherFragment = EditTeacherFragment.newInstance(loadedSubjectsList);
                getSupportFragmentManager().beginTransaction()
                        .replace(R.id.dynamicFieldsContainer, editTeacherFragment)
                        .commitAllowingStateLoss();
            }

            if (loggedInType == 2 || loggedInType == 3) {
                btnSaveUserDetails.setVisibility(View.VISIBLE);
                btnDeleteUser.setVisibility(View.VISIBLE);
            }
        });
    }

    private void saveUserData() {
        String fName = etFirstName.getText().toString().trim();
        String lName = etLastName.getText().toString().trim();
        String email = etNewEmail.getText().toString().trim();

        if (TextUtils.isEmpty(fName) || TextUtils.isEmpty(lName) || TextUtils.isEmpty(email)) {
            Toast.makeText(this, "Please fill all required fields (*)", Toast.LENGTH_SHORT).show();
            return;
        }

        if (!isValidEmail(email)) {
            Toast.makeText(this, "Invalid email address structure", Toast.LENGTH_SHORT).show();
            return;
        }

        Map<String, Object> updates = new HashMap<>();
        updates.put("firstName", fName);
        updates.put("middleName", etMiddleName.getText().toString().trim());
        updates.put("lastName", lName);
        updates.put("email", email);
        updates.put("school", selectedSchoolRef);

        if (selectedBitmap != null) {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            selectedBitmap.compress(Bitmap.CompressFormat.JPEG, 70, baos);
            updates.put("avatarBlob", Blob.fromBytes(baos.toByteArray()));
        }

        if (userTypeInt == 0 && editStudentFragment != null) {
            updates.put("classes", editStudentFragment.getSelectedClassesMap());
        } else if (userTypeInt == 1 && editTeacherFragment != null) {
            updates.put("teachableSubjects", editTeacherFragment.getSelectedSubjectsRefs());
        }

        db.collection("users").document(selectedUserId).update(updates).addOnSuccessListener(aVoid -> {
            Toast.makeText(this, "User details updated successfully! 💾", Toast.LENGTH_SHORT).show();
            setResult(RESULT_OK);
            finish();
        }).addOnFailureListener(e -> {
            Toast.makeText(this, "Update failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        });
    }

    private void deleteUser() {
        new AlertDialog.Builder(this)
                .setTitle("Delete User")
                .setMessage("Are you sure you want to permanently delete this user profile?")
                .setPositiveButton("Delete", (dialog, which) -> {
                    db.collection("users").document(selectedUserId).delete().addOnSuccessListener(aVoid -> {
                        Toast.makeText(UserDetailActivity.this, "User has been deleted.", Toast.LENGTH_SHORT).show();
                        setResult(RESULT_OK);
                        finish();
                    });
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private boolean isValidEmail(String email) {
        return email != null && Patterns.EMAIL_ADDRESS.matcher(email).matches();
    }
}