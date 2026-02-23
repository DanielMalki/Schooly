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
import android.widget.Toast;

import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

public class AddUserActivity extends BaseMenuActivity {

    private EditText etTz, etFirstName, etLastName, etMiddleName, etEmail, etPassword;
    private Spinner spinnerRole;
    private Button btnSaveUser;

    // שדות של תלמיד בלבד
    private LinearLayout layoutStudentFields;
    private EditText etGrade, etClassNum, etMath, etEnglish, etMajor1, etMajor2;

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

        layoutStudentFields = findViewById(R.id.layoutStudentFields);
        etGrade = findViewById(R.id.etGrade);
        etClassNum = findViewById(R.id.etClassNum);
        etMath = findViewById(R.id.etMath);
        etEnglish = findViewById(R.id.etEnglish);
        etMajor1 = findViewById(R.id.etMajor1);
        etMajor2 = findViewById(R.id.etMajor2);

        String[] roles = {"Student", "Teacher", "Admin"};
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, roles);
        spinnerRole.setAdapter(adapter);

        spinnerRole.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if (position == 0) {
                    layoutStudentFields.setVisibility(View.VISIBLE);
                } else {
                    layoutStudentFields.setVisibility(View.GONE);
                }
            }
            @Override
            public void onNothingSelected(AdapterView<?> parent) { }
        });

        btnSaveUser.setOnClickListener(v -> saveUserToDatabase());
    }

    private void saveUserToDatabase() {
        String tz = etTz.getText().toString().trim();
        String firstName = etFirstName.getText().toString().trim();
        String lastName = etLastName.getText().toString().trim();
        String middleName = etMiddleName.getText().toString().trim();
        String email = etEmail.getText().toString().trim();
        String password = etPassword.getText().toString().trim();
        int type = spinnerRole.getSelectedItemPosition();

        String grade = etGrade.getText().toString().trim();
        String classNum = etClassNum.getText().toString().trim();
        String math = etMath.getText().toString().trim();
        String english = etEnglish.getText().toString().trim();
        String major1 = etMajor1.getText().toString().trim();
        String major2 = etMajor2.getText().toString().trim();

        // 1. בדיקת חובה כללית לכל המשתמשים
        if (TextUtils.isEmpty(tz) || TextUtils.isEmpty(firstName) || TextUtils.isEmpty(lastName) ||
                TextUtils.isEmpty(email) || TextUtils.isEmpty(password)) {
            Toast.makeText(this, "Please fill all mandatory fields (*)", Toast.LENGTH_SHORT).show();
            return;
        }

        // 2. בדיקת תקינות תעודת זהות
        if (!isValidIsraeliID(tz)) {
            etTz.setError("Invalid ID number");
            etTz.requestFocus();
            return;
        }

        // 3. בדיקת תקינות אימייל
        if (!isValidEmail(email)) {
            etEmail.setError("Invalid email address format");
            etEmail.requestFocus();
            return;
        }

        // 4. בדיקות חובה ספציפיות לתלמיד
        if (type == 0) {
            if (TextUtils.isEmpty(grade) || TextUtils.isEmpty(classNum) ||
                    TextUtils.isEmpty(math) || TextUtils.isEmpty(english)) {
                Toast.makeText(this, "Please fill all Student Academic Info fields (*)", Toast.LENGTH_SHORT).show();
                return;
            }
        }

        // --- השלב החדש: בדיקה האם המשתמש כבר קיים במסד הנתונים ---
        db.collection("users").document(tz).get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        // המשתמש כבר קיים! עוצרים הכל ומציגים שגיאה.
                        etTz.setError("User with this ID already exists!");
                        etTz.requestFocus();
                    } else {
                        // המשתמש לא קיים - ממשיכים לשמירה!
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
                            userMap.put("grade", grade);
                            userMap.put("classNum", classNum);
                            userMap.put("mathClass", math);
                            userMap.put("englishClass", english);
                            userMap.put("major1", major1);
                            userMap.put("major2", major2);
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
        etGrade.setText("");
        etClassNum.setText("");
        etMath.setText("");
        etEnglish.setText("");
        etMajor1.setText("");
        etMajor2.setText("");
        etTz.requestFocus();
    }

    // --- פונקציות עזר לוולידציה ---

    /**
     * בודק האם תעודת הזהות תקינה לפי אלגוריתם ספרת ביקורת ישראלי
     */
    private boolean isValidIsraeliID(String id) {
        if (id == null || id.length() > 9 || !id.matches("\\d+")) {
            return false;
        }
        // הוספת אפסים מובילים אם המספר קצר מ-9 ספרות
        while (id.length() < 9) {
            id = "0" + id;
        }
        int sum = 0;
        for (int i = 0; i < 9; i++) {
            int digit = id.charAt(i) - '0';
            int step = digit * ((i % 2) + 1);
            if (step > 9) {
                step -= 9;
            }
            sum += step;
        }
        return sum % 10 == 0;
    }

    /**
     * בודק האם כתובת האימייל בפורמט תקין בעזרת הכלים המובנים של אנדרואיד
     */
    private boolean isValidEmail(String email) {
        return email != null && Patterns.EMAIL_ADDRESS.matcher(email).matches();
    }
}