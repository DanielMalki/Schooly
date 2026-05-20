package daniel.malki.schooly;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Toast;
import com.google.android.material.card.MaterialCardView;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreSettings;
import com.google.firebase.firestore.PersistentCacheSettings;

public class AdminDashboardActivity extends BaseMenuActivity {

    // עודכן: החלפנו את cardStudents ו-cardTeachers בשמות החדשים והנכונים
    private MaterialCardView cardAddUser, cardAddClass, cardManageUsers, cardManageClasses, cardSchedule;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setTitle("Admin Panel");

        // הגדרת זיכרון מטמון מקומי (Cache) בפיירבייס עם הגנה מכפילויות
        try {
            FirebaseFirestoreSettings settings = new FirebaseFirestoreSettings.Builder()
                    .setLocalCacheSettings(PersistentCacheSettings.newBuilder().build())
                    .build();
            FirebaseFirestore.getInstance().setFirestoreSettings(settings);
        } catch (IllegalStateException e) {
            // ההגדרות כבר נקבעו במסך קודם, אין צורך לעשות כלום
            android.util.Log.d("FirebaseSetup", "Settings already initialized: " + e.getMessage());
        }

        // עכשיו אפשר להמשיך להשתמש ב-db כרגיל
        FirebaseFirestore db = FirebaseFirestore.getInstance();

        // חיבור ל-XML המעודכן עם ה-IDs החדשים
        cardAddUser = findViewById(R.id.cardAddUser);
        cardAddClass = findViewById(R.id.cardAddClass);
        cardManageUsers = findViewById(R.id.cardManageUsers);     // עודכן מ-cardStudents
        cardManageClasses = findViewById(R.id.cardManageClasses); // עודכן מ-cardTeachers
        cardSchedule = findViewById(R.id.cardSchedule);

        // הגדרת הלחיצות והאינטנטים האמיתיים!
        setupCard(cardAddUser, AddUserActivity.class);    // מקשר למסך הוספת משתמש
        setupCard(cardAddClass, AddClassActivity.class);  // מקשר למסך הוספת כיתה/קבוצה
        setupCard(cardManageUsers, ManageUsersActivity.class); // עודכן: מקשר ישירות למסך ניהול המשתמשים החדש! 🔥
        setupCard(cardManageClasses, ManageClassesActivity.class);

        // שאר המסכים עדיין בפיתוח, נשאיר אותם כרגע כפי שהיו
        setupCard(cardSchedule, null);
    }

    @Override
    protected int getLayoutResourceId() {
        return R.layout.activity_admin_dashboard;
    }

    @Override
    protected int[] getAllowedUserTypes() {
        return new int[]{2}; // רק אדמין מורשה
    }

    @SuppressLint("ClickableViewAccessibility")
    private void setupCard(View card, Class<?> destinationActivity) {
        card.setOnClickListener(v -> {
            if (destinationActivity != null) {
                startActivity(new Intent(this, destinationActivity));
            } else {
                Toast.makeText(this, "Module under construction 🛠️", Toast.LENGTH_SHORT).show();
            }
        });

        // אנימציית הכיווץ היפה בלחיצה
        card.setOnTouchListener((v, event) -> {
            switch (event.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    v.animate().scaleX(0.95f).scaleY(0.95f).setDuration(100).start();
                    break;
                case MotionEvent.ACTION_UP:
                case MotionEvent.ACTION_CANCEL:
                    v.animate().scaleX(1f).scaleY(1f).setDuration(100).start();
                    break;
            }
            return false;
        });
    }
}