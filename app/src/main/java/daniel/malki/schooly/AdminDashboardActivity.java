package daniel.malki.schooly;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Toast;
import com.google.android.material.card.MaterialCardView;

public class AdminDashboardActivity extends BaseMenuActivity {

    // הוספנו את ה-Card של הכיתה החדשה לרשימה
    private MaterialCardView cardAddUser, cardAddClass, cardStudents, cardTeachers, cardSchedule;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setTitle("Admin Panel");

        // חיבור ל-XML המעודכן
        cardAddUser = findViewById(R.id.cardAddUser);
        cardAddClass = findViewById(R.id.cardAddClass); // הקישור החדש
        cardStudents = findViewById(R.id.cardStudents);
        cardTeachers = findViewById(R.id.cardTeachers);
        cardSchedule = findViewById(R.id.cardSchedule);

        // הגדרת הלחיצות והאינטנטים האמיתיים!
        setupCard(cardAddUser, AddUserActivity.class);    // מקשר למסך הוספת משתמש
        setupCard(cardAddClass, AddClassActivity.class);  // מקשר למסך הוספת כיתה/קבוצה

        // שאר המסכים עדיין בפיתוח, נשאיר אותם כרגע כפי שהיו
        setupCard(cardStudents, null);
        setupCard(cardTeachers, null);
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