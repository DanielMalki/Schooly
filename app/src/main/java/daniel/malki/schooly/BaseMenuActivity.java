package daniel.malki.schooly;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View; // הוספתי
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
    private void updateHeaderInfo() {
        // גישה ל-Header שנמצא בתוך ה-NavigationView
        View headerView = navigationView.getHeaderView(0);

        if (headerView != null) {
            TextView tvName = headerView.findViewById(R.id.tvHeaderName);
            // TextView tvSubtitle = headerView.findViewById(R.id.tvHeaderSubtitle); // אם תרצה לשנות גם את הטקסט הקטן

            // שליפת השם מהזיכרון
            SharedPreferences prefs = getSharedPreferences("SchoolyPrefs", MODE_PRIVATE);
            String name = prefs.getString("userName", "User");

            // עדכון הטקסט
            tvName.setText("Hello, " + name + " 👋");
        }
    }

    /**
     * הפונקציה האחראית להציג/להסתיר כפתורים בתפריט
     */
    private void updateMenuVisibility() {
        SharedPreferences prefs = getSharedPreferences("SchoolyPrefs", MODE_PRIVATE);
        int type = prefs.getInt("userType", 0); // 0=Student, 1=Teacher, 2=Admin

        android.view.Menu menu = navigationView.getMenu();

        // כפתורי אדמין
        MenuItem itemAddUser = menu.findItem(R.id.menu_add_user);
        MenuItem itemManageUsers = menu.findItem(R.id.menu_manage_users);
        MenuItem titleAdmin = menu.findItem(R.id.title_admin); // הכותרת של הסקשן

        // כפתורי תלמיד/מורה
        MenuItem itemSchedule = menu.findItem(R.id.menu_schedule);
        MenuItem itemGrades = menu.findItem(R.id.menu_grades);
        MenuItem titleAcademic = menu.findItem(R.id.title_academic);

        if (type == 2) {
            // --- מצב מנהל ---
            // מציג אדמין
            if (itemAddUser != null) itemAddUser.setVisible(true);
            if (itemManageUsers != null) itemManageUsers.setVisible(true);
            if (titleAdmin != null) titleAdmin.setVisible(true);

            // מסתיר לימודים (מנהל לא צריך לראות "ציונים שלי")
            if (itemSchedule != null) itemSchedule.setVisible(false);
            if (itemGrades != null) itemGrades.setVisible(false);
            if (titleAcademic != null) titleAcademic.setVisible(false);

        } else {
            // --- מצב תלמיד/מורה ---
            // מסתיר אדמין
            if (itemAddUser != null) itemAddUser.setVisible(false);
            if (itemManageUsers != null) itemManageUsers.setVisible(false);
            if (titleAdmin != null) titleAdmin.setVisible(false);

            // מציג לימודים
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

        // --- כפתורים בפיתוח (Admin) ---
        else if (id == R.id.menu_add_user) {
            Toast.makeText(this, "Add User Module - Coming Soon 🛠️", Toast.LENGTH_SHORT).show();
            // בעתיד: startActivity(new Intent(this, AddUserActivity.class));
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
            if (this instanceof AdminDashboardActivity) return;
            intent = new Intent(this, AdminDashboardActivity.class);
        } else {
            if (this instanceof MainActivity) return;
            intent = new Intent(this, MainActivity.class);
        }
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