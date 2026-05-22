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

    private MaterialCardView cardAddUser, cardAddClass, cardManageUsers, cardManageClasses, cardSchedule;

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

        cardAddUser = findViewById(R.id.cardAddUser);
        cardAddClass = findViewById(R.id.cardAddClass);
        cardManageUsers = findViewById(R.id.cardManageUsers);
        cardManageClasses = findViewById(R.id.cardManageClasses);
        cardSchedule = findViewById(R.id.cardSchedule);

        setupCard(cardAddUser, AddUserActivity.class);
        setupCard(cardAddClass, AddClassActivity.class);
        setupCard(cardManageUsers, ManageUsersActivity.class);
        setupCard(cardManageClasses, ManageClassesActivity.class);

        setupCard(cardSchedule, null);
    }

    @Override
    protected int getLayoutResourceId() {
        return R.layout.activity_admin_dashboard;
    }

    @Override
    protected int[] getAllowedUserTypes() {
        // תיקון: מתן גישה גם ל-School Admin (2) וגם ל-Schooly Admin (3)
        return new int[]{2, 3};
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