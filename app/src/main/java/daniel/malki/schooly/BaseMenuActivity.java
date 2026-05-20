package daniel.malki.schooly;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View; // הוספתי
import android.widget.ImageView;
import android.widget.TextView; // הוספתי
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.drawerlayout.widget.DrawerLayout;

import com.google.android.material.navigation.NavigationView;

public abstract class BaseMenuActivity extends AppCompatActivity
        implements NavigationView.OnNavigationItemSelectedListener {

    protected DrawerLayout drawerLayout;
    protected NavigationView navigationView;
    protected Toolbar toolbar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // 1. אבטחה: האם מחובר?
        if (!isSessionValid()) {
            redirectToLogin();
            return;
        }

        // 2. אבטחה: האם מורשה?
        if (!checkPermission()) {
            Toast.makeText(this, "Access Denied 🚫", Toast.LENGTH_LONG).show();
            navigateToHomeByType();
            finish();
            return;
        }

        // 3. טעינת תצוגה
        setContentView(R.layout.activity_base_menu);

        toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        drawerLayout = findViewById(R.id.drawerLayout);
        navigationView = findViewById(R.id.navigationView);

        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawerLayout, toolbar,
                R.string.open_drawer, R.string.close_drawer
        );

        drawerLayout.addDrawerListener(toggle);
        toggle.syncState();

        navigationView.setNavigationItemSelectedListener(this);

        getLayoutInflater().inflate(getLayoutResourceId(), findViewById(R.id.contentFrame));

        // 4. סינון התפריט לפי סוג משתמש
        updateMenuVisibility();

        // 5. עדכון השם למעלה (החדש!)
        updateHeaderInfo();
    }

    /**
     * פונקציה חדשה: מעדכנת את השם והכותרת בתפריט הצד
     */
    /**
     * פונקציה מעודכנת: מעדכנת את השם ואת תמונת הפרופיל בתפריט הצד
     */
    public void updateHeaderInfo() {
        // גישה ל-Header שנמצא בתוך ה-NavigationView
        View headerView = navigationView.getHeaderView(0);

        if (headerView != null) {
            TextView tvName = headerView.findViewById(R.id.tvHeaderName);
            ImageView imgHeaderAvatar = headerView.findViewById(R.id.imgHeaderAvatar); // <-- הוספנו את ה-ImageView של התפריט

            SharedPreferences prefs = getSharedPreferences("SchoolyPrefs", MODE_PRIVATE);
            String name = prefs.getString("userName", "User");
            String userId = prefs.getString("userId", "");

            // 1. עדכון השם
            tvName.setText("Hello, " + name + " 👋");

            // 2. משכיחת תמונת הפרופיל מה-FireStore והצגתה בתפריט
            if (!userId.isEmpty() && imgHeaderAvatar != null) {
                com.google.firebase.firestore.FirebaseFirestore.getInstance()
                        .collection("users").document(userId).get()
                        .addOnSuccessListener(doc -> {
                            if (doc.exists() && doc.getBlob("profileImageBlob") != null) {
                                byte[] imageBytes = doc.getBlob("profileImageBlob").toBytes();

                                // טעינת התמונה בעיגול לתוך ה-Header של התפריט
                                com.bumptech.glide.Glide.with(this)
                                        .load(imageBytes)
                                        .circleCrop()
                                        .into(imgHeaderAvatar);
                            } else {
                                // תמונת ברירת מחדל אם אין תמונה בבסיס הנתונים
                                imgHeaderAvatar.setImageResource(android.R.drawable.ic_menu_gallery);
                            }
                        });
            }
        }
    }

    /**
     * הפונקציה האחראית להציג/להסתיר כפתורים בתפריט
     */
    private void updateMenuVisibility() {
        SharedPreferences prefs = getSharedPreferences("SchoolyPrefs", MODE_PRIVATE);
        int type = prefs.getInt("userType", 0); // 0=Student, 1=Teacher, 2=SchoolAdmin, 3=SystemAdmin

        android.view.Menu menu = navigationView.getMenu();

        // כפתורי אדמין
        MenuItem itemAddUser = menu.findItem(R.id.menu_add_user);
        MenuItem itemAddClass = menu.findItem(R.id.menu_add_class);
        MenuItem itemManageUsers = menu.findItem(R.id.menu_manage_users);
        MenuItem titleAdmin = menu.findItem(R.id.title_admin);

        // כפתורי תלמיד/מורה (אקדמי)
        MenuItem itemSchedule = menu.findItem(R.id.menu_schedule);
        MenuItem itemGrades = menu.findItem(R.id.menu_grades);
        MenuItem titleAcademic = menu.findItem(R.id.title_academic);

        if (type == 3) {
            // --- מצב מנהל מערכת על (System Admin) ---
            // מציג ניהול בלבד, מסתיר חלק אקדמי
            if (itemAddUser != null) itemAddUser.setVisible(true);
            if (itemAddClass != null) itemAddClass.setVisible(true);
            if (itemManageUsers != null) itemManageUsers.setVisible(true);
            if (titleAdmin != null) titleAdmin.setVisible(true);

            if (itemSchedule != null) itemSchedule.setVisible(false);
            if (itemGrades != null) itemGrades.setVisible(false);
            if (titleAcademic != null) titleAcademic.setVisible(false);

        } else if (type == 2) {
            // --- מצב מנהל בית ספר (School Admin שהוא גם מורה) ---
            // מציג גם ניהול וגם אקדמי
            if (itemAddUser != null) itemAddUser.setVisible(true);
            if (itemAddClass != null) itemAddClass.setVisible(true);
            if (itemManageUsers != null) itemManageUsers.setVisible(true);
            if (titleAdmin != null) titleAdmin.setVisible(true);

            if (itemSchedule != null) itemSchedule.setVisible(true);
            if (itemGrades != null) itemGrades.setVisible(true);
            if (titleAcademic != null) titleAcademic.setVisible(true);

        } else if (type == 1) {
            // --- מצב מורה רגיל ---
            // מסתיר ניהול, מציג אקדמי
            if (itemAddUser != null) itemAddUser.setVisible(false);
            if (itemAddClass != null) itemAddClass.setVisible(false);
            if (itemManageUsers != null) itemManageUsers.setVisible(false);
            if (titleAdmin != null) titleAdmin.setVisible(false);

            if (itemSchedule != null) itemSchedule.setVisible(true);
            if (itemGrades != null) itemGrades.setVisible(true);
            if (titleAcademic != null) titleAcademic.setVisible(true);

        } else {
            // --- מצב תלמיד ---
            // מסתיר ניהול, מציג אקדמי
            if (itemAddUser != null) itemAddUser.setVisible(false);
            if (itemAddClass != null) itemAddClass.setVisible(false);
            if (itemManageUsers != null) itemManageUsers.setVisible(false);
            if (titleAdmin != null) titleAdmin.setVisible(false);

            if (itemSchedule != null) itemSchedule.setVisible(true);
            if (itemGrades != null) itemGrades.setVisible(true);
            if (titleAcademic != null) titleAcademic.setVisible(true);
        }
    }

    @Override
    public boolean onNavigationItemSelected(@NonNull MenuItem item) {
        drawerLayout.closeDrawers();
        int id = item.getItemId();

        // --- כפתורים פעילים ---
        if (id == R.id.menu_home) {
            navigateToHomeByType();
        }
        else if (id == R.id.menu_profile) {
            if (!(this instanceof ProfileActivity)) {
                startActivity(new Intent(this, ProfileActivity.class));
            }
        }
        else if (id == R.id.menu_logout) {
            logout();
        }

        // --- כפתורים פעילים (Admin) ---
        else if (id == R.id.menu_add_user) {
            if (!(this instanceof AddUserActivity)) {
                startActivity(new Intent(this, AddUserActivity.class));
            }
        }
        // הבלוק החדש שיוצר את המעבר למסך יצירת כיתה!
        else if (id == R.id.menu_add_class) {
            // ודא ששם ה-Activity כאן (CreateClassActivity) תואם ב-100% לשם הקובץ שיצרת!
            if (!(this instanceof AddClassActivity)) {
                startActivity(new Intent(this, AddClassActivity.class));
            }
        }
        else if (id == R.id.menu_manage_users) {
            Toast.makeText(this, "Manage Users - Coming Soon 🛠️", Toast.LENGTH_SHORT).show();
        }

        // --- כפתורים בפיתוח (Academic) ---
        else if (id == R.id.menu_schedule) {
            Toast.makeText(this, "Schedule - Coming Soon 📅", Toast.LENGTH_SHORT).show();
        }
        else if (id == R.id.menu_grades) {
            Toast.makeText(this, "Grades - Coming Soon 💯", Toast.LENGTH_SHORT).show();
        }

        return true;
    }

    // --- שאר הפונקציות ללא שינוי ---

    protected abstract int getLayoutResourceId();

    protected int[] getAllowedUserTypes() {
        return null;
    }

    private boolean checkPermission() {
        int[] allowedTypes = getAllowedUserTypes();
        if (allowedTypes == null) return true;

        SharedPreferences prefs = getSharedPreferences("SchoolyPrefs", MODE_PRIVATE);
        int currentUserType = prefs.getInt("userType", -1);

        for (int type : allowedTypes) {
            if (type == currentUserType) return true;
        }
        return false;
    }

    private boolean isSessionValid() {
        SharedPreferences prefs = getSharedPreferences("SchoolyPrefs", MODE_PRIVATE);
        return prefs.getBoolean("isLoggedIn", false);
    }

    private void redirectToLogin() {
        Intent intent = new Intent(this, LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    private void navigateToHomeByType() {
        SharedPreferences prefs = getSharedPreferences("SchoolyPrefs", MODE_PRIVATE);
        int type = prefs.getInt("userType", 0);

        Intent intent;
        if (type == 2) {
            // אם זה אדמין - לך לדאשבורד מנהל
            if (this instanceof AdminDashboardActivity) return;
            intent = new Intent(this, AdminDashboardActivity.class);
        } else {
            // אם זה תלמיד (או מורה כרגע) - לך לדאשבורד תלמיד
            if (this instanceof StudentDashboardActivity) return;
            intent = new Intent(this, StudentDashboardActivity.class);
        }

        // מנקה את היסטוריית המסכים כדי שהמשתמש לא יוכל ללחוץ "אחורה" ולחזור למסך לא קשור
        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        startActivity(intent);
    }

    protected void logout() {
        getSharedPreferences("SchoolyPrefs", MODE_PRIVATE)
                .edit()
                .clear()
                .apply();
        Toast.makeText(this, "Logged out", Toast.LENGTH_SHORT).show();
        redirectToLogin();
    }
}