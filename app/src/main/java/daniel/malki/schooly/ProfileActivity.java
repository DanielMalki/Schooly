package daniel.malki.schooly;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.TextView;

public class ProfileActivity extends BaseMenuActivity {

    // 1. הגדרת המשתנים (חובה להגדיר אותם כאן כדי שיהיו מוכרים בכל המחלקה)
    private TextView tvFullName, tvRole;
    private TextView valueFirstName, valueLastName, valueId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setTitle("Profile"); // כותרת בסרגל העליון

        // 2. קישור המשתנים לרכיבים ב-XML (לפי ה-IDs שנתת ב-XML שלך)
        tvFullName = findViewById(R.id.tvFullName);
        tvRole = findViewById(R.id.tvRole);

        valueFirstName = findViewById(R.id.valueFirstName);
        valueLastName = findViewById(R.id.valueLastName);
        valueId = findViewById(R.id.valueId);

        // 3. קריאה לפונקציה שטוענת את המידע (בלעדיה המסך יישאר ריק)
        loadUserData();
    }

    private void loadUserData() {
        SharedPreferences prefs = getSharedPreferences("SchoolyPrefs", MODE_PRIVATE);

        // שליפת הנתונים מהזיכרון
        String fullName = prefs.getString("userName", "Guest"); // ברירת מחדל Guest
        String id = prefs.getString("userId", "");
        int type = prefs.getInt("userType", 0);

        // המרת המספר לטקסט תפקיד
        String roleText = getRoleName(type);

        // הצבה ב-TextViews הראשיים
        tvFullName.setText(fullName);
        tvRole.setText(roleText);
        valueId.setText(id);

        // --- לוגיקה לפיצול שם פרטי ושם משפחה ---
        // זה נועד למלא את השדות הקטנים למטה ב-Info Card
        if (fullName != null && !fullName.isEmpty()) {
            String[] parts = fullName.split(" "); // מפצל לפי רווח

            // שם פרטי (החלק הראשון)
            if (parts.length > 0) {
                valueFirstName.setText(parts[0]);
            }

            // שם משפחה (כל מה שנשאר)
            if (parts.length > 1) {
                // לוקח את כל המחרוזת החל מהסוף של השם הראשון
                String lastName = fullName.substring(parts[0].length()).trim();
                valueLastName.setText(lastName);
            } else {
                valueLastName.setText(""); // אם אין שם משפחה
            }
        }
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