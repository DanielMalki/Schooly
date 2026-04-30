package daniel.malki.schooly;

import android.os.Bundle;
import android.text.TextUtils;
import android.util.Patterns;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;

import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class AddUserActivity extends BaseMenuActivity {

    // ================== רשימות הנתונים למערכת ==================

    // 1. רשימת כלל המקצועות בבית הספר (לשיוך למורה - יכול לכלול הכל)
    private final String[] allTeachableSubjects = {"Math 3", "Math 4", "Math 5", "English 3", "English 4", "English 5", "History", "Literature", "Physics", "Chemistry", "Biology", "Computer Science", "Software Engineering", "Geography", "Physical Education", "Civics", "Bible", "Sociology", "Theater", "Economy"};

    // 2. רשימת מקצועות ההרחבה בלבד (לבחירת מורחב א' ומורחב ב' לתלמיד)
    private final String[] majorSubjects = {"Physics", "Chemistry", "Biology", "Computer Science", "Geography", "Art", "Music", "Psychology"};

    // 3. רשימות זמניות למתמטיקה ואנגלית (עד שיהיו לנו מורים במסד הנתונים)
    // בעתיד זה יתמלא אוטומטית (למשל: "5 אורלי", "4 רינת"), כרגע רק ברירות המחדל שביקשת.
    private final String[] mathOptions = {"Select Math Level *", "Not decided yet (עדיין לא הוחלט)", "Exempt (פטור)"};
    private final String[] englishOptions = {"Select English Level *", "Not decided yet (עדיין לא הוחלט)", "Exempt (פטור)"};

    // 4. רשימת שכבות
    private final String[] gradesList = {"Select Grade *", "1", "2", "3", "4", "5","6", "7", "8", "9", "10", "11", "12", "13","14"};

    // ============================================================

    private EditText etTz, etFirstName, etLastName, etMiddleName, etEmail, etPassword;
    private Spinner spinnerRole;
    private Button btnSaveUser;

    // שדות של תלמיד
    private LinearLayout layoutStudentFields;
    private Spinner spinnerGrade, spinnerClassNum, spinnerMath, spinnerEnglish, spinnerMajor1, spinnerMajor2;

    // שדות של מורה / מנהל
    private LinearLayout layoutTeacherFields;
    private TextView tvSelectSubjects;

    private boolean[] selectedSubjectsArray;
    private ArrayList<String> teachableSubjects = new ArrayList<>();

    private FirebaseFirestore db;

    @Override
    protected int getLayoutResourceId() {
        return R.layout.activity_add_user;
    }

    @Override
    protected int[] getAllowedUserTypes() {
        return new int[]{2}; // רק אדמין
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        db = FirebaseFirestore.getInstance();

        etTz = findViewById(R.id.etNewTz);
        etFirstName = findViewById(R.id.etFirstName);
        etLastName = findViewById(R.id.etLastName);
        etMiddleName = findViewById(R.id.etMiddleName);
        etEmail = findViewById(R.id.etNewEmail);
        etPassword = findViewById(R.id.etNewPassword);
        spinnerRole = findViewById(R.id.spinnerRole);
        btnSaveUser = findViewById(R.id.btnSaveUser);

        // תלמיד
        layoutStudentFields = findViewById(R.id.layoutStudentFields);
        spinnerGrade = findViewById(R.id.spinnerGrade);
        spinnerClassNum = findViewById(R.id.spinnerClassNum);
        spinnerMath = findViewById(R.id.spinnerMath);
        spinnerEnglish = findViewById(R.id.spinnerEnglish);
        spinnerMajor1 = findViewById(R.id.spinnerMajor1);
        spinnerMajor2 = findViewById(R.id.spinnerMajor2);

        // מורה/מנהל
        layoutTeacherFields = findViewById(R.id.layoutTeacherFields);
        tvSelectSubjects = findViewById(R.id.tvSelectSubjects);
        selectedSubjectsArray = new boolean[allTeachableSubjects.length];

        setupSpinners();
        setupGradeAndClassLogic();

        spinnerRole.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if (position == 0) { // תלמיד
                    layoutStudentFields.setVisibility(View.VISIBLE);
                    layoutTeacherFields.setVisibility(View.GONE);
                } else { // מורה או מנהל
                    layoutStudentFields.setVisibility(View.GONE);
                    layoutTeacherFields.setVisibility(View.VISIBLE);
                }
            }
            @Override
            public void onNothingSelected(AdapterView<?> parent) { }
        });

        tvSelectSubjects.setOnClickListener(v -> showSubjectsDialog());
        btnSaveUser.setOnClickListener(v -> saveUserToDatabase());
    }

    private void setupSpinners() {
        String[] roles = {"Student", "Teacher", "Admin"};
        spinnerRole.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, roles));

        spinnerMath.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, mathOptions));
        spinnerEnglish.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, englishOptions));

        // הכנת רשימת המגמות (נוסיף "Select Major" בהתחלה ונגדיר מורחב א' ומורחב ב')
        String[] major1Options = new String[majorSubjects.length + 1];
        major1Options[0] = "Select Major A (מורחב א')";
        System.arraycopy(majorSubjects, 0, major1Options, 1, majorSubjects.length);

        String[] major2Options = new String[majorSubjects.length + 1];
        major2Options[0] = "Select Major B (מורחב ב')";
        System.arraycopy(majorSubjects, 0, major2Options, 1, majorSubjects.length);

        spinnerMajor1.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, major1Options));
        spinnerMajor2.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, major2Options));
    }

    private void setupGradeAndClassLogic() {
        spinnerGrade.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, gradesList));

        spinnerGrade.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if (position == 0) {
                    // לא נבחרה שכבה - נועלים את הכיתה
                    spinnerClassNum.setEnabled(false);
                    spinnerClassNum.setAdapter(null);
                } else {
                    // נבחרה שכבה - פותחים את הכיתה ומאכלסים בנתונים (1 עד 10 כברירת מחדל)
                    spinnerClassNum.setEnabled(true);
                    String[] classesList = {"Select Class *", "1", "2", "3", "4", "5", "6", "7", "8", "9", "10"};
                    spinnerClassNum.setAdapter(new ArrayAdapter<>(AddUserActivity.this, android.R.layout.simple_spinner_dropdown_item, classesList));
                }
            }
            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });
    }

    private void showSubjectsDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Select Teachable Subjects");
        builder.setCancelable(false);

        builder.setMultiChoiceItems(allTeachableSubjects, selectedSubjectsArray, (dialog, which, isChecked) -> {
            selectedSubjectsArray[which] = isChecked;
        });

        builder.setPositiveButton("OK", (dialog, which) -> {
            teachableSubjects.clear();
            for (int i = 0; i < selectedSubjectsArray.length; i++) {
                if (selectedSubjectsArray[i]) {
                    teachableSubjects.add(allTeachableSubjects[i]);
                }
            }
            if (teachableSubjects.isEmpty()) {
                tvSelectSubjects.setText("Select Teachable Subjects *");
            } else {
                tvSelectSubjects.setText(teachableSubjects.size() + " Subjects Selected");
            }
        });

        builder.setNegativeButton("Cancel", (dialog, which) -> dialog.dismiss());

        builder.setNeutralButton("Clear All", (dialog, which) -> {
            for (int i = 0; i < selectedSubjectsArray.length; i++) {
                selectedSubjectsArray[i] = false;
            }
            teachableSubjects.clear();
            tvSelectSubjects.setText("Select Teachable Subjects *");
        });

        builder.show();
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

        if (!isValidIsraeliID(tz)) {
            etTz.setError("Invalid ID number");
            etTz.requestFocus();
            return;
        }

        if (!isValidEmail(email)) {
            etEmail.setError("Invalid email address format");
            etEmail.requestFocus();
            return;
        }

        String grade = "", classNum = "", math = "", english = "", major1 = "", major2 = "";

        if (type == 0) { // Student Validation
            if (spinnerGrade.getSelectedItemPosition() == 0 || spinnerClassNum.getSelectedItemPosition() == 0) {
                Toast.makeText(this, "Please select Grade and Class No.", Toast.LENGTH_SHORT).show();
                return;
            }

            if (spinnerMath.getSelectedItemPosition() == 0 || spinnerEnglish.getSelectedItemPosition() == 0) {
                Toast.makeText(this, "Please select Math and English levels", Toast.LENGTH_SHORT).show();
                return;
            }

            // חילוץ המספר בלבד מתוך השכבה (למשל מתוך "10 (י)" נשלוף רק "10")
            String fullGradeText = spinnerGrade.getSelectedItem().toString();
            grade = fullGradeText.split(" ")[0];
            classNum = spinnerClassNum.getSelectedItem().toString();

            math = spinnerMath.getSelectedItem().toString();
            english = spinnerEnglish.getSelectedItem().toString();

            major1 = spinnerMajor1.getSelectedItemPosition() > 0 ? spinnerMajor1.getSelectedItem().toString() : "";
            major2 = spinnerMajor2.getSelectedItemPosition() > 0 ? spinnerMajor2.getSelectedItem().toString() : "";

        } else { // Teacher / Admin Validation
            if (teachableSubjects.isEmpty()) {
                Toast.makeText(this, "Please select at least one teachable subject!", Toast.LENGTH_LONG).show();
                return;
            }
        }

        final String finalGrade = grade, finalClassNum = classNum, finalMath = math, finalEnglish = english, finalMajor1 = major1, finalMajor2 = major2;

        db.collection("users").document(tz).get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        etTz.setError("User with this ID already exists!");
                        etTz.requestFocus();
                    } else {
                        String fullName = firstName;
                        if (!middleName.isEmpty()) {
                            fullName += " " + middleName;
                        }
                        fullName += " " + lastName;

                        Map<String, Object> userMap = new HashMap<>();
                        userMap.put("email", email);
                        userMap.put("password", password);
                        userMap.put("type", type);
                        userMap.put("name", fullName);
                        userMap.put("firstName", firstName);
                        userMap.put("lastName", lastName);
                        userMap.put("middleName", middleName);

                        if (type == 0) {
                            userMap.put("grade", Integer.parseInt(finalGrade));
                            userMap.put("classNum", Integer.parseInt(finalClassNum));
                            userMap.put("mathClass", finalMath);
                            userMap.put("englishClass", finalEnglish);
                            userMap.put("major1", finalMajor1);
                            userMap.put("major2", finalMajor2);
                        } else {
                            userMap.put("teachableSubjects", teachableSubjects);
                        }

                        db.collection("users").document(tz)
                                .set(userMap)
                                .addOnSuccessListener(aVoid -> {
                                    Toast.makeText(AddUserActivity.this, "User saved! ✅", Toast.LENGTH_SHORT).show();
                                    clearForm();
                                })
                                .addOnFailureListener(e -> Toast.makeText(AddUserActivity.this, "Error saving user: " + e.getMessage(), Toast.LENGTH_LONG).show());
                    }
                })
                .addOnFailureListener(e -> Toast.makeText(AddUserActivity.this, "Error checking database: " + e.getMessage(), Toast.LENGTH_LONG).show());
    }

    private void clearForm() {
        etTz.setText("");
        etFirstName.setText("");
        etLastName.setText("");
        etMiddleName.setText("");
        etEmail.setText("");
        etPassword.setText("");

        spinnerGrade.setSelection(0);
        spinnerClassNum.setEnabled(false);
        spinnerClassNum.setAdapter(null);

        spinnerMath.setSelection(0);
        spinnerEnglish.setSelection(0);
        spinnerMajor1.setSelection(0);
        spinnerMajor2.setSelection(0);

        for (int i = 0; i < selectedSubjectsArray.length; i++) {
            selectedSubjectsArray[i] = false;
        }
        teachableSubjects.clear();
        tvSelectSubjects.setText("Select Teachable Subjects *");

        spinnerRole.setSelection(0);
        etTz.requestFocus();
    }

    private boolean isValidIsraeliID(String id) {
        if (id == null || id.length() > 9 || !id.matches("\\d+")) { return false; }
        while (id.length() < 9) { id = "0" + id; }
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