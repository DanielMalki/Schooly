// ProfileActivity.java

package daniel.malki.schooly;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.TextView;

public class ProfileActivity extends BaseMenuActivity {

    private TextView tvFullName, tvRole, valueFirstName, valueId;
    // תוסיף כאן את שאר המשתנים של ה-TextViews מה-XML שלך

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setTitle("Profile");

        // קישור לרכיבים ב-XML
        tvFullName = findViewById(R.id.tvFullName);
        tvRole = findViewById(R.id.tvRole);
        valueId = findViewById(R.id.valueId);
        // ... תמשיך לקשר את השאר

        loadUserData();
    }

    // בתוך ProfileActivity.java

    private void loadUserData() {
        SharedPreferences prefs = getSharedPreferences("SchoolyPrefs", MODE_PRIVATE);

        // שליפת הנתונים
        String name = prefs.getString("userName", "Guest");
        String id = prefs.getString("userId", "");
        int type = prefs.getInt("userType", 0); // שליפת המספר

        // המרת המספר לטקסט יפה לתצוגה
        String roleText = getRoleName(type);

        // הצבה ב-TextViews
        tvFullName.setText(name);
        tvRole.setText(roleText);
        valueId.setText(id);

        // אם יש לך מקום לאימייל בפרופיל:
        // String email = prefs.getString("userEmail", "");
    }

    // פונקציית עזר להמרת מספר לתפקיד
    private String getRoleName(int type) {
        switch (type) {
            case 0: return "Student";
            case 1: return "Teacher";
            case 2: return "System Administrator";
            default: return "Unknown";
        }
    }

    @Override
    protected int getLayoutResourceId() {
        return R.layout.activity_profile;
    }
}