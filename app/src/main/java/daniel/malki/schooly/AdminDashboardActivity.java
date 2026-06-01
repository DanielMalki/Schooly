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

    // עדכון שמות המשתנים שיתאימו בדיוק ל-XML החדש
    private MaterialCardView cardAddUsers, cardAddClasses, cardManageUsers, cardManageClasses, cardSchedule;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setTitle("Admin Panel");

        try {
            FirebaseFirestoreSettings settings = new FirebaseFirestoreSettings.Builder()
                    .setLocalCacheSettings(PersistentCacheSettings.newBuilder().build())
                    .build();
            FirebaseFirestore.getInstance().setFirestoreSettings(settings);
        } catch (IllegalStateException e) {
            android.util.Log.d("FirebaseSetup", "Settings already initialized: " + e.getMessage());
        }

        // קישור הרכיבים מה-XML (כולל ה-IDs המתוקנים בלשון רבים)
        cardAddUsers = findViewById(R.id.cardAddUsers);
        cardAddClasses = findViewById(R.id.cardAddClasses);
        cardManageUsers = findViewById(R.id.cardManageUsers);
        cardManageClasses = findViewById(R.id.cardManageClasses);
        cardSchedule = findViewById(R.id.cardSchedule);

        // הגדרת הפעולות והאנימציות לכל כרטיס
        setupCard(cardAddUsers, AddUserActivity.class);
        setupCard(cardAddClasses, AddClassActivity.class);
        setupCard(cardManageUsers, ManageUsersActivity.class);
        setupCard(cardManageClasses, ManageClassesActivity.class);
        setupCard(cardSchedule, ScheduleActivity.class); // כרגע פותח הודעת בקרוב (או החלף במסך המתאים במידת הצורך)
    }

    @Override
    protected int getLayoutResourceId() {
        return R.layout.activity_admin_dashboard;
    }

    @Override
    protected int[] getAllowedUserTypes() {
        return new int[]{2, 3};
    }

    @SuppressLint("ClickableViewAccessibility")
    private void setupCard(View card, Class<?> destinationActivity) {
        if (card == null) return;

        // 1. מה קורה בלחיצה (מעבר מסך)
        card.setOnClickListener(v -> {
            if (destinationActivity != null) {
                startActivity(new Intent(this, destinationActivity));
            } else {
                Toast.makeText(this, "Module under construction 🛠️", Toast.LENGTH_SHORT).show();
            }
        });

        // 2. אפקט האנימציה המגניב (התכווצות קלה בלחיצה)
        card.setOnTouchListener((v, event) -> {
            switch (event.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    v.animate().scaleX(0.95f).scaleY(0.95f).setDuration(100).start();
                    break;
                case MotionEvent.ACTION_UP:
                case MotionEvent.ACTION_CANCEL:
                    v.animate().scaleX(1.0f).scaleY(1.0f).setDuration(100).start();
                    break;
            }
            return false; // מאפשר ל-OnClickListener להמשיך לעבוד כרגיל
        });
    }
}