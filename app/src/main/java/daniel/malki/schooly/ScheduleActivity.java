package daniel.malki.schooly;

import android.os.Bundle;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.tabs.TabLayout;
import java.util.ArrayList;
import java.util.List;

public class ScheduleActivity extends AppCompatActivity {

    private TabLayout tabLayoutDays;
    private RecyclerView recyclerSchedule;
    private LessonAdapter lessonAdapter;
    private List<Lesson> lessonList;

    // משתנים מדומים כרגע (בפועל נמשוך אותם מה-SharedPreferences או מ-Firestore)
    private String currentUserType = "Admin"; // יכול להיות "Student", "Teacher", "Admin"
    private String currentTeacherName = "John Doe"; // רלוונטי רק אם המשתמש הוא מורה

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_schedule);

        initViews();
        setupDaysTabs();
        setupRecyclerView();

        // טעינת מערכת השעות ליום הראשון (ראשון)
        loadScheduleForDay("Sunday");
    }

    private void initViews() {
        tabLayoutDays = findViewById(R.id.tabLayoutDays);
        recyclerSchedule = findViewById(R.id.recyclerSchedule);
    }

    private void setupDaysTabs() {
        String[] days = {"Sunday", "Monday", "Tuesday", "Wednesday", "Thursday", "Friday"};
        for (String day : days) {
            tabLayoutDays.addTab(tabLayoutDays.newTab().setText(day));
        }

        tabLayoutDays.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                loadScheduleForDay(tab.getText().toString());
            }

            @Override
            public void onTabUnselected(TabLayout.Tab tab) {}

            @Override
            public void onTabReselected(TabLayout.Tab tab) {}
        });
    }

    private void setupRecyclerView() {
        lessonList = new ArrayList<>();
        // מעבירים לאדפטר "true" רק אם המשתמש הוא מנהל
        boolean isAdmin = currentUserType.equals("Admin");

        lessonAdapter = new LessonAdapter(lessonList, isAdmin, lesson -> {
            // כאן תיפתח תיבת הדו-שיח (Dialog) לעריכת השיעור שהמנהל לחץ עליו
            openEditLessonDialog(lesson);
        });

        recyclerSchedule.setLayoutManager(new LinearLayoutManager(this));
        recyclerSchedule.setAdapter(lessonAdapter);
    }

    private void loadScheduleForDay(String day) {
        // כאן תהיה השליפה מ-Firestore לפי היום שנבחר.
        // כרגע נשים נתוני דמה כדי לראות שהעיצוב עובד.
        lessonList.clear();

        if (currentUserType.equals("Teacher")) {
            // לוגיקה למורה: שולפים רק שיעורים שבהם teacherName שווה למורה המחובר
            lessonList.add(new Lesson(1, "08:00", "08:45", "Math", currentTeacherName, "layer"));
            // שעה 2 חופשית למורה הזה, אז לא נוסיף כלום או שנוסיף חלון ריק
        } else {
            // לוגיקה לתלמיד/מנהל: שולפים את כל המערכת של הכיתה
            lessonList.add(new Lesson(1, "08:00", "08:45", "Math", "Mr. Smith", "layer"));
            lessonList.add(new Lesson(2, "08:50", "09:35", "History", "Mrs. Cohen", "class"));
        }

        lessonAdapter.notifyDataSetChanged();
    }

    private void openEditLessonDialog(Lesson lesson) {
        // הפונקציה הזו תבנה את הדיאלוג עם רשימת המקצועות, ואז רשימת המורים (עם האפורים)
        Toast.makeText(this, "Editing hour " + lesson.getHourNumber(), Toast.LENGTH_SHORT).show();
    }
}