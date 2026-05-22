package daniel.malki.schooly;

import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.text.SpannableString;
import android.text.style.ForegroundColorSpan;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.ContextCompat;
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

        // 4. סינון התפריט לפי סוג משתמש וצביעתו
        updateMenuVisibility();

        // 5. עדכון השם והתפקיד למעלה
        updateHeaderInfo();
    }

    public void updateHeaderInfo() {
        View headerView = navigationView.getHeaderView(0);

        if (headerView != null) {
            TextView tvName = headerView.findViewById(R.id.tvHeaderName);
            TextView tvSubtitle = headerView.findViewById(R.id.tvHeaderSubtitle);
            ImageView imgHeaderAvatar = headerView.findViewById(R.id.imgHeaderAvatar);

            SharedPreferences prefs = getSharedPreferences("SchoolyPrefs", MODE_PRIVATE);
            String name = prefs.getString("userName", "User");
            String userId = prefs.getString("userId", "");
            int type = prefs.getInt("userType", 0);

            tvName.setText("Hello, " + name + " 👋");
            tvSubtitle.setText(getRoleName(type));

            if (!userId.isEmpty() && imgHeaderAvatar != null) {
                com.google.firebase.firestore.FirebaseFirestore.getInstance()
                        .collection("users").document(userId).get()
                        .addOnSuccessListener(doc -> {
                            if (doc.exists() && doc.getBlob("profileImageBlob") != null) {
                                byte[] imageBytes = doc.getBlob("profileImageBlob").toBytes();

                                com.bumptech.glide.Glide.with(this)
                                        .load(imageBytes)
                                        .circleCrop()
                                        .into(imgHeaderAvatar);
                            } else {
                                imgHeaderAvatar.setImageResource(android.R.drawable.sym_def_app_icon);
                            }
                        });
            }
        }
    }

    private void updateMenuVisibility() {
        SharedPreferences prefs = getSharedPreferences("SchoolyPrefs", MODE_PRIVATE);
        int type = prefs.getInt("userType", 0);

        android.view.Menu menu = navigationView.getMenu();

        MenuItem itemAddUser = menu.findItem(R.id.menu_add_user);
        MenuItem itemAddClass = menu.findItem(R.id.menu_add_class);
        MenuItem itemManageUsers = menu.findItem(R.id.menu_manage_users);
        MenuItem itemManageClasses = menu.findItem(R.id.menu_manage_classes);
        MenuItem titleAdmin = menu.findItem(R.id.title_admin);

        MenuItem itemSchedule = menu.findItem(R.id.menu_schedule);
        MenuItem itemGrades = menu.findItem(R.id.menu_grades);
        MenuItem titleAcademic = menu.findItem(R.id.title_academic);

        // 🟥 צביעת כפתור ההתנתקות באדום בולט (טקסט + אייקון)
        MenuItem itemLogout = menu.findItem(R.id.menu_logout);
        if (itemLogout != null) {
            // צביעת הטקסט לאדום
            SpannableString logoutTitle = new SpannableString(itemLogout.getTitle());
            logoutTitle.setSpan(new ForegroundColorSpan(Color.parseColor("#D32F2F")), 0, logoutTitle.length(), 0);
            itemLogout.setTitle(logoutTitle);

            // צביעת האייקון לאדום
            if (itemLogout.getIcon() != null) {
                itemLogout.getIcon().setTint(Color.parseColor("#D32F2F"));
            }
        }

        boolean isAdminVisible = (type == 2 || type == 3);
        boolean isAcademicVisible = (type == 0 || type == 1 || type == 2);

        if (titleAdmin != null) titleAdmin.setVisible(isAdminVisible);
        if (itemAddUser != null) itemAddUser.setVisible(isAdminVisible);
        if (itemAddClass != null) itemAddClass.setVisible(isAdminVisible);
        if (itemManageUsers != null) itemManageUsers.setVisible(isAdminVisible);
        if (itemManageClasses != null) itemManageClasses.setVisible(isAdminVisible);

        if (titleAcademic != null) titleAcademic.setVisible(isAcademicVisible);
        if (itemSchedule != null) itemSchedule.setVisible(isAcademicVisible);
        if (itemGrades != null) itemGrades.setVisible(isAcademicVisible);
    }

    @Override
    public boolean onNavigationItemSelected(@NonNull MenuItem item) {
        drawerLayout.closeDrawers();
        int id = item.getItemId();

        if (id == R.id.menu_home) {
            navigateToHomeByType();
        }
        else if (id == R.id.menu_profile) {
            if (!(this instanceof ProfileActivity)) {
                startActivity(new Intent(this, ProfileActivity.class));
            }
        }
        else if (id == R.id.menu_logout) {
            // 🔥 שינוי: במקום להתנתק מיד, נציג דיאלוג וידוא חמור
            showLogoutConfirmationDialog();
        }
        else if (id == R.id.menu_add_user) {
            if (!(this instanceof AddUserActivity)) {
                startActivity(new Intent(this, AddUserActivity.class));
            }
        }
        else if (id == R.id.menu_add_class) {
            if (!(this instanceof AddClassActivity)) {
                startActivity(new Intent(this, AddClassActivity.class));
            }
        }
        else if (id == R.id.menu_manage_users) {
            if (!(this instanceof ManageUsersActivity)) {
                startActivity(new Intent(this, ManageUsersActivity.class));
            }
        }
        else if (id == R.id.menu_manage_classes) {
            if (!(this instanceof ManageClassesActivity)) {
                startActivity(new Intent(this, ManageClassesActivity.class));
            }
        }
        else if (id == R.id.menu_schedule) {
            Toast.makeText(this, "Schedule - Coming Soon 📅", Toast.LENGTH_SHORT).show();
        }
        else if (id == R.id.menu_grades) {
            Toast.makeText(this, "Grades - Coming Soon 💯", Toast.LENGTH_SHORT).show();
        }

        return true;
    }

    // 🔥 דיאלוג וידוא התנתקות מעוצב ואחראי
    private void showLogoutConfirmationDialog() {
        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle("Confirm Logout 🚪")
                .setMessage("Are you sure you want to log out of your Schooly account?\nYou will need to sign in again to access your data.")
                .setPositiveButton("Log Out", (dialogInterface, which) -> logout())
                .setNegativeButton("Stay Connected", (dialogInterface, which) -> dialogInterface.dismiss())
                .create();

        dialog.setOnShowListener(dialogInterface -> {
            // הפיכת כפתור האישור לאדום כדי להדגיש שמדובר בפעולה בלתי הפיכה
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setTextColor(Color.parseColor("#D32F2F"));
            // כפתור הביטול יישאר בצבע נייטרלי כהה
            dialog.getButton(AlertDialog.BUTTON_NEGATIVE).setTextColor(Color.parseColor("#424242"));
        });

        dialog.show();
    }

    private String getRoleName(int type) {
        switch (type) {
            case 0: return "Student Status";
            case 1: return "Teacher Status";
            case 2: return "School Administrator";
            case 3: return "Global Schooly Admin";
            default: return "User";
        }
    }

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
        if (type == 2 || type == 3) {
            if (this instanceof AdminDashboardActivity) return;
            intent = new Intent(this, AdminDashboardActivity.class);
        } else {
            if (this instanceof StudentDashboardActivity) return;
            intent = new Intent(this, StudentDashboardActivity.class);
        }

        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        startActivity(intent);
    }

    protected void logout() {
        getSharedPreferences("SchoolyPrefs", MODE_PRIVATE)
                .edit()
                .clear()
                .apply();
        Toast.makeText(this, "Logged out successfully", Toast.LENGTH_SHORT).show();
        redirectToLogin();
    }
}