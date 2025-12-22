package daniel.malki.schooly;

import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

public class LoginActivity extends AppCompatActivity {

    private TextInputEditText etId, etPassword;
    private TextInputLayout idLayout, passwordLayout;
    private Button btnLogin;
    private TextView tvForgot;

    private FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        db = FirebaseFirestore.getInstance();

        etId = findViewById(R.id.etId);
        etPassword = findViewById(R.id.etPassword);
        idLayout = findViewById(R.id.idLayout);
        passwordLayout = findViewById(R.id.passwordLayout);
        btnLogin = findViewById(R.id.btnLogin);
        tvForgot = findViewById(R.id.tvForgot);

        btnLogin.setOnClickListener(v -> validateAndLogin());

        tvForgot.setOnClickListener(v ->
                Toast.makeText(this, "Please contact the system administrator", Toast.LENGTH_SHORT).show()
        );
    }

    /* ---------------- VALIDATION ---------------- */

    private void validateAndLogin() {
        String id = etId.getText().toString().trim();
        String password = etPassword.getText().toString().trim();

        idLayout.setError(null);
        passwordLayout.setError(null);

        if (!isValidIsraeliId(id)) {
            idLayout.setError("Invalid ID number");
            return;
        }

        if (!isValidPassword(password)) {
            passwordLayout.setError("Password must be at least 4 characters");
            return;
        }

        loginWithFirestore(id, password);
    }

    /* ---------------- FIRESTORE LOGIN ---------------- */

    private void loginWithFirestore(String id, String inputPassword) {
        db.collection("users")
                .document(id)
                .get()
                .addOnSuccessListener(document ->
                        handleLoginResult(document, inputPassword)
                )
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Database error", Toast.LENGTH_SHORT).show()
                );
    }

    private void handleLoginResult(DocumentSnapshot document, String inputPassword) {
        if (!document.exists()) {
            idLayout.setError("User not found");
            return;
        }

        String savedPassword = document.getString("password");
        String userName = document.getString("name");

        if (!inputPassword.equals(savedPassword)) {
            passwordLayout.setError("Incorrect password");
            return;
        }

        Toast.makeText(this,
                "Welcome " + userName + " 👋",
                Toast.LENGTH_SHORT).show();

        // TODO: Intent למסך הבא
    }

    /* ---------------- ID VALIDATION ---------------- */

    /**
     * Israeli ID validation (length + check digit)
     */
    private boolean isValidIsraeliId(String id) {
        if (id.length() != 9 || !id.matches("\\d+"))
            return false;

        int sum = 0;
        for (int i = 0; i < 9; i++) {
            int digit = Character.getNumericValue(id.charAt(i));
            int factor = (i % 2 == 0) ? 1 : 2;
            int result = digit * factor;

            if (result > 9)
                result -= 9;

            sum += result;
        }

        return sum % 10 == 0;
    }

    /* ---------------- PASSWORD VALIDATION ---------------- */

    private boolean isValidPassword(String password) {
        return password.length() >= 4;
    }
}
