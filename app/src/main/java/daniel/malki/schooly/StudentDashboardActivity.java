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

    private MaterialCardView cardProfile, cardSchedule, cardGrades, cardAttendance, cardHomework, cardMessages;

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

// עכשיו אפשר להמשיך להשתמש ב-db כרגיל
        FirebaseFirestore db = FirebaseFirestore.getInstance();

        // 1. מציאת הכרטיסים לפי ה-ID
        cardProfile = findViewById(R.id.cardProfile);
        cardSchedule = findViewById(R.id.cardSchedule);
        cardGrades = findViewById(R.id.cardGrades);
        cardAttendance = findViewById(R.id.cardAttendance);
        cardHomework = findViewById(R.id.cardHomework);
        cardMessages = findViewById(R.id.cardMessages);

        // 2. הפעלת האנימציה והלחיצה לכל כרטיס

        // כרטיס פרופיל - עובר למסך פרופיל
        setupCard(cardProfile, ProfileActivity.class);

        // שאר הכרטיסים (כרגע אין להם מסך אז שמתי הודעה זמנית)
        setupCard(cardSchedule, null);
        setupCard(cardGrades, null);
        setupCard(cardAttendance, null);
        setupCard(cardHomework, null);
        setupCard(cardMessages, null);
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
                    // כשעוזבים - מחזיר לגודל מקורי
                    v.animate().scaleX(1f).scaleY(1f).setDuration(100).start();
                    break;
            }
            // מחזיר false כדי לאפשר ל-OnClickListener לעבוד גם
            return false;
        });
    }
}