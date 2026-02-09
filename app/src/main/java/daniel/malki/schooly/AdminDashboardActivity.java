package daniel.malki.schooly;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Toast;
import com.google.android.material.card.MaterialCardView;

public class AdminDashboardActivity extends BaseMenuActivity {

    private MaterialCardView cardAddUser, cardStudents, cardTeachers, cardSchedule;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setTitle("Admin Panel"); // הכותרת למעלה

        // חיבור ל-XML
        cardAddUser = findViewById(R.id.cardAddUser);
        cardStudents = findViewById(R.id.cardStudents);
        cardTeachers = findViewById(R.id.cardTeachers);
        cardSchedule = findViewById(R.id.cardSchedule);

        // הגדרת לחיצות ואנימציות

        // זה הכפתור הכי חשוב כרגע - הוספת משתמש
        // נשאיר אותו כרגע עם הודעה עד שנבנה את המסך הבא
        setupCard(cardAddUser, null);

        setupCard(cardStudents, null);
        setupCard(cardTeachers, null);
        setupCard(cardSchedule, null);
    }

    @Override
    protected int getLayoutResourceId() {
        return R.layout.activity_admin_dashboard;
    }

    /**
     * אותה פונקציה מה-MainActivity לאנימציה ומעבר
     */
    @SuppressLint("ClickableViewAccessibility")
    private void setupCard(View card, Class<?> destinationActivity) {

        card.setOnClickListener(v -> {
            if (destinationActivity != null) {
                startActivity(new Intent(this, destinationActivity));
            } else {
                // זמני - עד שנבנה את המסכים
                Toast.makeText(this, "Module under construction 🛠️", Toast.LENGTH_SHORT).show();
            }
        });

        // אנימציית כיווץ
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