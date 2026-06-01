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

public class StudentDashboardActivity extends BaseMenuActivity {

    private MaterialCardView cardProfile, cardSchedule;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setTitle("Home");

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

        // מקשרים רק את שני הכפתורים שנשארו
        cardProfile = findViewById(R.id.cardProfile);
        cardSchedule = findViewById(R.id.cardSchedule);

        setupCard(cardProfile, ProfileActivity.class);
        // שים לב: אם עדיין אין לך מחלקה ScheduleActivity, שנה את זה ל-null לעת עתה
        setupCard(cardSchedule, ScheduleActivity.class);
    }

    @Override
    protected int getLayoutResourceId() {
        return R.layout.activity_student_dashboard;
    }

    /**
     * פונקציית עזר שמגדירה גם את המעבר למסך הבא וגם את האנימציה המגניבה
     */
    @SuppressLint("ClickableViewAccessibility")
    private void setupCard(View card, Class<?> destinationActivity) {

        // 1. מה קורה כשלוחצים (מעבר מסך)
        card.setOnClickListener(v -> {
            if (destinationActivity != null) {
                startActivity(new Intent(StudentDashboardActivity.this, destinationActivity));
            } else {
                Toast.makeText(this, "Coming Soon...", Toast.LENGTH_SHORT).show();
            }
        });

        // 2. האנימציה (כיווץ בלחיצה)
        card.setOnTouchListener((v, event) -> {
            switch (event.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    // כשלוחצים - מקטין את הכרטיס ל-95% מהגודל
                    v.animate().scaleX(0.95f).scaleY(0.95f).setDuration(100).start();
                    break;

                case MotionEvent.ACTION_UP:
                case MotionEvent.ACTION_CANCEL:
                    // כשעוזבים - חוזר לגודל המקורי
                    v.animate().scaleX(1f).scaleY(1f).setDuration(100).start();
                    break;
            }
            return false;
        });
    }
}