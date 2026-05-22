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
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
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
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class UserDetailActivity extends BaseMenuActivity {

    private ImageView imgDetailAvatar;
    private ImageButton btnEditAvatar;
    private EditText etTz, etFirstName, etLastName, etMiddleName, etNewEmail;
    private Spinner spinnerRole;
    private Button btnSaveUserDetails, btnChangePasswordPlaceholder;

    // רכיבי בית ספר
    private EditText etSchoolLocked;
    private Spinner spinnerSchoolSelect;
    private ImageButton btnQuickAddSchool;
    private TextView tvSchoolTitle;
    private View viewSchoolDivider;

    private ArrayList<String> schoolNames = new ArrayList<>();
    private ArrayList<String> schoolIds = new ArrayList<>();
    private DocumentReference selectedSchool;

    // רכיבי תלמיד
    private LinearLayout layoutStudentFields;
    private Spinner spinnerHomeroom, spinnerMathGroup, spinnerEnglishGroup, spinnerMajor1Group, spinnerMajor2Group;

    // רכיבי מורה
    private LinearLayout layoutTeacherFields;
    private LinearLayout layoutSelectedSubjectsList;
    private ImageButton btnQuickAddSubject;
    private TextView tvSelectSubjects;

    private FirebaseFirestore db;
    private String selectedUserId;

    // החזקת ה-Bitmap המעודכן או סימון למחיקה
    private byte[] imageBytesBlob = null;
    private boolean shouldDeletePicture = false;

    private int currentAdminType;
    private String currentAdminId;
    private int targetUserType;

    // מיפוי חכם של תפקידים למניעת בלבול בגלל שינוי דינמי של ה-Spinner
    private ArrayList<Integer> availableRoleIds = new ArrayList<>();

    // רשימות נתונים
    private ArrayList<String> subjectNames = new ArrayList<>();
    private ArrayList<String> subjectIds = new ArrayList<>();
    private boolean[] checkedSubjectsArray;
    private ArrayList<String> chosenSubjectIds = new ArrayList<>();

    private ArrayList<String> homeroomNames = new ArrayList<>(), homeroomIds = new ArrayList<>();
    private ArrayList<String> mathNames = new ArrayList<>(), mathIds = new ArrayList<>();
    private ArrayList<String> englishNames = new ArrayList<>(), englishIds = new ArrayList<>();
    private ArrayList<String> majorNames = new ArrayList<>(), majorIds = new ArrayList<>();

    private final ActivityResultLauncher<Intent> galleryLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                    Uri selectedImageUri = result.getData().getData();
                    imgDetailAvatar.setImageURI(selectedImageUri);
                    try {
                        Bitmap bitmap = MediaStore.Images.Media.getBitmap(this.getContentResolver(), selectedImageUri);
                        prepareImageBlob(bitmap);
                    } catch (IOException e) {
                        Toast.makeText(this, "Failed to read image", Toast.LENGTH_SHORT).show();
                    }
                }
            }
    );

    private final ActivityResultLauncher<Void> cameraLauncher = registerForActivityResult(
            new ActivityResultContracts.TakePicturePreview(),
            bitmap -> {
                if (bitmap != null) {
                    imgDetailAvatar.setImageBitmap(bitmap);
                    prepareImageBlob(bitmap);
                }
            }
    );

    @Override
    protected int getLayoutResourceId() { return R.layout.activity_user_detail; }

    @Override
    protected int[] getAllowedUserTypes() { return new int[]{2, 3}; }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        db = FirebaseFirestore.getInstance();
        selectedUserId = getIntent().getStringExtra("selectedUserId");

        imgDetailAvatar = findViewById(R.id.imgDetailAvatar);
        btnEditAvatar = findViewById(R.id.btnEditAvatar);
        etTz = findViewById(R.id.etNewTz);
        etFirstName = findViewById(R.id.etFirstName);
        etLastName = findViewById(R.id.etLastName);
        etMiddleName = findViewById(R.id.etMiddleName);
        etNewEmail = findViewById(R.id.etNewEmail);
        spinnerRole = findViewById(R.id.spinnerRole);
        btnSaveUserDetails = findViewById(R.id.btnSaveUserDetails);
        btnChangePasswordPlaceholder = findViewById(R.id.btnChangePasswordPlaceholder);

        etSchoolLocked = findViewById(R.id.etSchoolLocked);
        spinnerSchoolSelect = findViewById(R.id.spinnerSchoolSelect);
        btnQuickAddSchool = findViewById(R.id.btnQuickAddSchool);
        tvSchoolTitle = findViewById(R.id.tvSchoolTitle);
        viewSchoolDivider = findViewById(R.id.viewSchoolDivider);

        layoutStudentFields = findViewById(R.id.layoutStudentFields);
        spinnerHomeroom = findViewById(R.id.spinnerHomeroom);
        spinnerMathGroup = findViewById(R.id.spinnerMathGroup);
        spinnerEnglishGroup = findViewById(R.id.spinnerEnglishGroup);
        spinnerMajor1Group = findViewById(R.id.spinnerMajor1Group);
        spinnerMajor2Group = findViewById(R.id.spinnerMajor2Group);

        layoutTeacherFields = findViewById(R.id.layoutTeacherFields);
        layoutSelectedSubjectsList = findViewById(R.id.layoutSelectedSubjectsList);
        tvSelectSubjects = findViewById(R.id.tvSelectSubjects);
        btnQuickAddSubject = findViewById(R.id.btnQuickAddSubject);

        SharedPreferences prefs = getSharedPreferences("SchoolyPrefs", MODE_PRIVATE);
        currentAdminType = prefs.getInt("userType", 2);
        currentAdminId = prefs.getString("userId", "");

        View.OnClickListener avatarClickListener = v -> showImageSourceDialog();
        btnEditAvatar.setOnClickListener(avatarClickListener);
        imgDetailAvatar.setOnClickListener(avatarClickListener);

        btnChangePasswordPlaceholder.setOnClickListener(v ->
                Toast.makeText(this, "Password update coming soon!", Toast.LENGTH_SHORT).show());

        btnSaveUserDetails.setOnClickListener(v -> saveUserEditsToDatabase());

        loadSubjectsDataAndUser();
    }

    // 🔥 התיקון המרכזי: הגדרת רשימת התפקידים הזמינים בצורה דינמית ומאובטחת לפי ה-Target
    private void setupRoleSpinnerStructure(int targetType) {
        ArrayList<String> rolesToDisplay = new ArrayList<>();
        availableRoleIds.clear();

        if (targetType == 0) {
            // תלמיד - לא ניתן להעביר אותו לשום תפקיד אחר
            rolesToDisplay.add("Student");
            availableRoleIds.add(0);
            spinnerRole.setEnabled(false); // חוסם את הספינר לחלוטין
        } else if (targetType == 1 || targetType == 2) {
            // מורים ומנהלי בתי ספר - יכולים לעבור רק בינם לבין עצמם!
            rolesToDisplay.add("Teacher");
            availableRoleIds.add(1);
            rolesToDisplay.add("School Admin");
            availableRoleIds.add(2);
            spinnerRole.setEnabled(true);
        } else if (targetType == 3) {
            // מנהל סקולי על - חסום לשינוי
            rolesToDisplay.add("Schooly Admin");
            availableRoleIds.add(3);
            spinnerRole.setEnabled(false);
        }

        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, rolesToDisplay);
        spinnerRole.setAdapter(adapter);

        // סימון התפקיד הנוכחי בתוך הרשימה המצומצמת החדשה
        int positionToSelect = availableRoleIds.indexOf(targetType);
        if (positionToSelect >= 0) {
            spinnerRole.setSelection(positionToSelect);
        }
    }

    private void showImageSourceDialog() {
        String[] options = {"Open Camera 📷", "Choose from Gallery 🖼️", "Remove Picture 🗑️", "Cancel ❌"};
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Update User Profile Picture");
        builder.setItems(options, (dialog, which) -> {
            if (which == 0) cameraLauncher.launch(null);
            else if (which == 1) {
                Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
                galleryLauncher.launch(intent);
            } else if (which == 2) {
                removeProfilePictureFromView();
            } else dialog.dismiss();
        });
        builder.show();
    }

    private void removeProfilePictureFromView() {
        imageBytesBlob = null;
        shouldDeletePicture = true;
        Glide.with(this).clear(imgDetailAvatar);
        imgDetailAvatar.setImageResource(android.R.drawable.sym_def_app_icon);
        Toast.makeText(this, "Picture removed from preview. Don't forget to Save!", Toast.LENGTH_SHORT).show();
    }

    private void prepareImageBlob(Bitmap originalBitmap) {
        shouldDeletePicture = false;
        int maxWidth = 400;
        int maxHeight = 400;
        float scale = Math.min(((float) maxWidth / originalBitmap.getWidth()), ((float) maxHeight / originalBitmap.getHeight()));
        Bitmap resizedBitmap = Bitmap.createScaledBitmap(originalBitmap, (int) (originalBitmap.getWidth() * scale), (int) (originalBitmap.getHeight() * scale), true);

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        resizedBitmap.compress(Bitmap.CompressFormat.JPEG, 70, baos);
        imageBytesBlob = baos.toByteArray();
    }

    private void loadSubjectsDataAndUser() {
        db.collection("subjects").get().addOnSuccessListener(queryDocumentSnapshots -> {
            subjectNames.clear(); subjectIds.clear();
            for (QueryDocumentSnapshot doc : queryDocumentSnapshots) {
                subjectIds.add(doc.getId());
                subjectNames.add(doc.contains("displayName") ? doc.getString("displayName") : doc.getId());
            }
            checkedSubjectsArray = new boolean[subjectNames.size()];
            loadClassesDataAndUser();
        });
    }

    private void loadClassesDataAndUser() {
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
                    case "homeroom": homeroomNames.add(className); homeroomIds.add(classId); break;
                    case "math": mathNames.add(className); mathIds.add(classId); break;
                    case "english": englishNames.add(className); englishIds.add(classId); break;
                    case "major": majorNames.add(className); majorIds.add(classId); break;
                }
            }

            spinnerHomeroom.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, homeroomNames));
            spinnerMathGroup.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, mathNames));
            spinnerEnglishGroup.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, englishNames));
            spinnerMajor1Group.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, majorNames));
            spinnerMajor2Group.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, majorNames));

            if (selectedUserId != null) {
                fetchTargetUserFullProfile();
            }
        });
    }

    private void initListWithPlaceholder(ArrayList<String> names, ArrayList<String> ids, String placeholder) {
        names.clear(); ids.clear(); names.add(placeholder); ids.add("");
    }

    private void fetchTargetUserFullProfile() {
        db.collection("users").document(selectedUserId).get().addOnSuccessListener(doc -> {
            if (!doc.exists()) return;

            Long typeLong = doc.getLong("type");
            targetUserType = (typeLong != null) ? typeLong.intValue() : 0;

            // 🔥 עדכון הספינר באופן מוגן
            setupRoleSpinnerStructure(targetUserType);

            etTz.setText(selectedUserId);
            etFirstName.setText(doc.getString("firstName"));
            etLastName.setText(doc.getString("lastName"));
            etMiddleName.setText(doc.getString("middleName") != null ? doc.getString("middleName") : "");
            etNewEmail.setText(doc.getString("email"));

            if (doc.getBlob("profileImageBlob") != null) {
                byte[] loadedBytes = doc.getBlob("profileImageBlob").toBytes();
                Glide.with(this).load(loadedBytes).circleCrop().into(imgDetailAvatar);
            }

            selectedSchool = doc.getDocumentReference("school");
            handleSchoolLayoutRendering();

            if (targetUserType == 0) {
                layoutStudentFields.setVisibility(View.VISIBLE);
                layoutTeacherFields.setVisibility(View.GONE);
                List<DocumentReference> classRefs = (List<DocumentReference>) doc.get("classes");
                if (classRefs != null) {
                    for (DocumentReference ref : classRefs) {
                        setTargetSpinnerSelection(ref.getId());
                    }
                }
            } else if (targetUserType == 1 || targetUserType == 2) {
                layoutStudentFields.setVisibility(View.GONE);
                layoutTeacherFields.setVisibility(View.VISIBLE);
                List<DocumentReference> subRefs = (List<DocumentReference>) doc.get("teachableSubjects");
                if (subRefs != null) {
                    for (DocumentReference ref : subRefs) {
                        int idx = subjectIds.indexOf(ref.getId());
                        if (idx >= 0) {
                            checkedSubjectsArray[idx] = true;
                            chosenSubjectIds.add(ref.getId());
                        }
                    }
                }
                updateSubjectsTextView();
            }

            applySecurityRules();
        });
    }

    private void handleSchoolLayoutRendering() {
        if (targetUserType == 3) {
            setSchoolLayoutVisibility(View.GONE, View.GONE, View.GONE);
            return;
        }

        if (currentAdminType == 2) {
            setSchoolLayoutVisibility(View.VISIBLE, View.GONE, View.GONE);
            if (selectedSchool != null) {
                selectedSchool.get().addOnSuccessListener(sDoc -> etSchoolLocked.setText(sDoc.getString("displayName")));
            }
        } else if (currentAdminType == 3) {
            setSchoolLayoutVisibility(View.GONE, View.VISIBLE, View.VISIBLE);
            db.collection("schools").get().addOnSuccessListener(snapshots -> {
                schoolNames.clear(); schoolIds.clear();
                for (QueryDocumentSnapshot d : snapshots) {
                    schoolIds.add(d.getId());
                    schoolNames.add(d.getString("displayName") != null ? d.getString("displayName") : d.getId());
                }
                spinnerSchoolSelect.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, schoolNames));
                if (selectedSchool != null) {
                    int pos = schoolIds.indexOf(selectedSchool.getId());
                    if (pos >= 0) spinnerSchoolSelect.setSelection(pos);
                }
                spinnerSchoolSelect.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                    @Override
                    public void onItemSelected(AdapterView<?> p, View v, int pos, long id) {
                        selectedSchool = db.collection("schools").document(schoolIds.get(pos));
                    }
                    @Override
                    public void onNothingSelected(AdapterView<?> p) {}
                });
            });
        }
    }

    private void setSchoolLayoutVisibility(int lockedVis, int selectVis, int quickAddVis) {
        etSchoolLocked.setVisibility(lockedVis);
        spinnerSchoolSelect.setVisibility(selectVis);
        btnQuickAddSchool.setVisibility(quickAddVis);
        int visibility = (lockedVis == View.GONE && selectVis == View.GONE) ? View.GONE : View.VISIBLE;
        tvSchoolTitle.setVisibility(visibility);
        viewSchoolDivider.setVisibility(visibility);
    }

    private void setTargetSpinnerSelection(String id) {
        if (homeroomIds.contains(id)) spinnerHomeroom.setSelection(homeroomIds.indexOf(id));
        else if (mathIds.contains(id)) spinnerMathGroup.setSelection(mathIds.indexOf(id));
        else if (englishIds.contains(id)) spinnerEnglishGroup.setSelection(englishIds.indexOf(id));
        else if (majorIds.contains(id)) {
            if (spinnerMajor1Group.getSelectedItemPosition() == 0) spinnerMajor1Group.setSelection(majorIds.indexOf(id));
            else spinnerMajor2Group.setSelection(majorIds.indexOf(id));
        }
    }

    private void updateSubjectsTextView() {
        layoutSelectedSubjectsList.removeAllViews();
        if (chosenSubjectIds.isEmpty()) {
            tvSelectSubjects.setText("Select Teachable Subjects *");
        } else {
            tvSelectSubjects.setText(chosenSubjectIds.size() + " Subjects Selected:");
            for (int i = 0; i < checkedSubjectsArray.length; i++) {
                if (checkedSubjectsArray[i]) {
                    TextView tv = new TextView(this);
                    tv.setText("• " + subjectNames.get(i));
                    tv.setTextSize(16.0f);
                    tv.setPadding(0, 6, 0, 6);
                    layoutSelectedSubjectsList.addView(tv);
                }
            }
        }
    }

    private void applySecurityRules() {
        boolean canEdit = false;
        if (currentAdminType == 3 && targetUserType < 3) canEdit = true;
        else if (currentAdminType == 2 && targetUserType < 2) canEdit = true;

        etFirstName.setEnabled(canEdit);
        etLastName.setEnabled(canEdit);
        etMiddleName.setEnabled(canEdit);
        etNewEmail.setEnabled(canEdit);

        spinnerHomeroom.setEnabled(canEdit);
        spinnerMathGroup.setEnabled(canEdit);
        spinnerEnglishGroup.setEnabled(canEdit);
        spinnerMajor1Group.setEnabled(canEdit);
        spinnerMajor2Group.setEnabled(canEdit);

        tvSelectSubjects.setEnabled(canEdit);
        btnQuickAddSubject.setEnabled(canEdit);
        spinnerSchoolSelect.setEnabled(canEdit);
        btnQuickAddSchool.setEnabled(canEdit);

        if (canEdit) {
            btnEditAvatar.setVisibility(View.VISIBLE);
            btnSaveUserDetails.setVisibility(View.VISIBLE);
            tvSelectSubjects.setOnClickListener(v -> showSubjectsMultiChoiceDialog());
            imgDetailAvatar.setClickable(true);
        } else {
            btnEditAvatar.setVisibility(View.GONE);
            btnSaveUserDetails.setVisibility(View.GONE);
            imgDetailAvatar.setClickable(false);
            Toast.makeText(this, "View-only mode", Toast.LENGTH_SHORT).show();
        }

        // 🔥 התיקון: הספינר מקשיב ומחליף את הפנל של מורה/מנהל לפי הבחירה באופן דינמי
        if (canEdit && (targetUserType == 1 || targetUserType == 2)) {
            spinnerRole.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                @Override
                public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                    int realRoleSelected = availableRoleIds.get(position);
                    if (realRoleSelected == 1 || realRoleSelected == 2) {
                        layoutStudentFields.setVisibility(View.GONE);
                        layoutTeacherFields.setVisibility(View.VISIBLE);
                    }
                }
                @Override
                public void onNothingSelected(AdapterView<?> parent) {}
            });
        }
    }

    private void showSubjectsMultiChoiceDialog() {
        String[] items = subjectNames.toArray(new String[0]);
        new AlertDialog.Builder(this)
                .setTitle("Select Teachable Subjects")
                .setMultiChoiceItems(items, checkedSubjectsArray, (dialog, which, isChecked) -> checkedSubjectsArray[which] = isChecked)
                .setPositiveButton("OK", (dialog, which) -> {
                    chosenSubjectIds.clear();
                    for (int i = 0; i < checkedSubjectsArray.length; i++) {
                        if (checkedSubjectsArray[i]) chosenSubjectIds.add(subjectIds.get(i));
                    }
                    updateSubjectsTextView();
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void saveUserEditsToDatabase() {
        String fName = etFirstName.getText().toString().trim();
        String lName = etLastName.getText().toString().trim();
        String mName = etMiddleName.getText().toString().trim();
        String email = etNewEmail.getText().toString().trim();

        // 🔥 שליפת ה-ID האמיתי של התפקיד מתוך המערך הדינמי במקום ה-Position היבש של ה-Spinner
        int finalRole = availableRoleIds.get(spinnerRole.getSelectedItemPosition());

        if (TextUtils.isEmpty(fName) || TextUtils.isEmpty(lName) || TextUtils.isEmpty(email)) {
            Toast.makeText(this, "Please fill all mandatory fields (*)", Toast.LENGTH_SHORT).show();
            return;
        }

        if (!isValidEmail(email)) {
            Toast.makeText(this, "Invalid email format", Toast.LENGTH_SHORT).show();
            return;
        }

        Map<String, Object> updates = new HashMap<>();
        updates.put("firstName", fName);
        updates.put("lastName", lName);
        updates.put("middleName", mName);
        updates.put("name", fName + (mName.isEmpty() ? "" : " " + mName) + " " + lName);
        updates.put("email", email);
        updates.put("type", finalRole);

        if (finalRole != 3) {
            updates.put("school", selectedSchool);
        }

        if (shouldDeletePicture) {
            updates.put("profileImageBlob", FieldValue.delete());
        } else if (imageBytesBlob != null) {
            updates.put("profileImageBlob", Blob.fromBytes(imageBytesBlob));
        }

        if (finalRole == 0) {
            ArrayList<DocumentReference> classRefs = new ArrayList<>();
            classRefs.add(db.collection("classes").document(homeroomIds.get(spinnerHomeroom.getSelectedItemPosition())));
            classRefs.add(db.collection("classes").document(mathIds.get(spinnerMathGroup.getSelectedItemPosition())));
            classRefs.add(db.collection("classes").document(englishIds.get(spinnerEnglishGroup.getSelectedItemPosition())));
            if (spinnerMajor1Group.getSelectedItemPosition() > 0) classRefs.add(db.collection("classes").document(majorIds.get(spinnerMajor1Group.getSelectedItemPosition())));
            if (spinnerMajor2Group.getSelectedItemPosition() > 0) classRefs.add(db.collection("classes").document(majorIds.get(spinnerMajor2Group.getSelectedItemPosition())));
            updates.put("classes", classRefs);
        } else {
            ArrayList<DocumentReference> subRefs = new ArrayList<>();
            for (String subId : chosenSubjectIds) subRefs.add(db.collection("subjects").document(subId));
            updates.put("teachableSubjects", subRefs);
        }

        db.collection("users").document(selectedUserId).update(updates).addOnSuccessListener(aVoid -> {
            Toast.makeText(this, "User details updated successfully! 💾", Toast.LENGTH_SHORT).show();
            setResult(RESULT_OK);
            finish();
        });
    }

    private boolean isValidEmail(String email) {
        return email != null && Patterns.EMAIL_ADDRESS.matcher(email).matches();
    }
}