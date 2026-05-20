package daniel.malki.schooly;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import java.util.ArrayList;
import java.util.List;

public class ManageUsersActivity extends BaseMenuActivity {

    private EditText etSearch;
    private Spinner spinnerFilterRole;
    private RecyclerView rvUsers;
    private TextView tvNoResults;

    private UserAdapter adapter;
    private List<User> userList;
    private FirebaseFirestore db;

    // משתנים לשמירת מצב הסינון הנוכחי בזמן אמת
    private String currentQuery = "";
    private int currentRoleFilter = 0; // 0=הכל, 1=תלמידים, 2=מורים, 3=אדמינים

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setTitle("Manage Users"); // קביעת כותרת ל-Toolbar

        db = FirebaseFirestore.getInstance();

        // 1. חיבור רכיבי העיצוב מה-XML
        etSearch = findViewById(R.id.etSearch);
        spinnerFilterRole = findViewById(R.id.spinnerFilterRole);
        rvUsers = findViewById(R.id.rvUsers);
        tvNoResults = findViewById(R.id.tvNoResults);

        // 2. הגדרת ה-RecyclerView
        userList = new ArrayList<>();
        adapter = new UserAdapter(userList);
        rvUsers.setLayoutManager(new LinearLayoutManager(this));
        rvUsers.setAdapter(adapter);

        // 3. הגדרת הספינר (אפשרויות הסינון)
        String[] roles = {"All Roles", "Students", "Teachers", "Admins"};
        ArrayAdapter<String> spinnerAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, roles);
        spinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerFilterRole.setAdapter(spinnerAdapter);

        // 4. טעינת הנתונים מפיירבייס
        loadUsersFromFirestore();

        // 5. האזנה לשינויים בתיבת החיפוש (TextWatcher)
        etSearch.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                currentQuery = s.toString();
                applyFilterAndCheckEmpty();
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });

        // 6. האזנה לשינויים בספינר הסינון
        spinnerFilterRole.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                currentRoleFilter = position;
                applyFilterAndCheckEmpty();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });
    }

    /**
     * משיכת כל המשתמשים מ-Firestore שהם חלק מבית הספר של המנהל
     */
    private void loadUsersFromFirestore() {
        // נניח שזה ה-ID של המנהל המחובר כרגע (למשל ה-ID של רינת מהתמונה: "025484379")
        // בהמשך נדאג שהמשתנה הזה יגיע דינמית מהמסך הקודם
        String currentAdminId = "025484379";

        // שליפת מסמך המנהל ישירות מתוך קולקשן users
        db.collection("users").document(currentAdminId).get()
                .addOnSuccessListener(adminDoc -> {
                    if (adminDoc.exists()) {
                        // שליפת הרפרנס של בית הספר של המנהל
                        com.google.firebase.firestore.DocumentReference adminSchoolRef = adminDoc.getDocumentReference("schoolRef");

                        if (adminSchoolRef != null) {
                            // שאילתה מסוננת שמביאה רק מסמכי משתמשים ששדה ה-schoolRef שלהם שווה לבית הספר של המנהל
                            db.collection("users")
                                    .whereEqualTo("schoolRef", adminSchoolRef)
                                    .get()
                                    .addOnSuccessListener(queryDocumentSnapshots -> {
                                        userList.clear();
                                        for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                                            User user = document.toObject(User.class);
                                            user.setUserId(document.getId()); // מזהה המסמך הוא ה-ID
                                            userList.add(user);
                                        }
                                        adapter.updateList(userList);
                                        applyFilterAndCheckEmpty();
                                    })
                                    .addOnFailureListener(e -> {
                                        Toast.makeText(this, "Error loading users: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                                    });
                        }
                    }
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Error identifying admin school: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    /**
     * הפעלת הסינון המשולב ובדיקה האם הרשימה ריקה כדי להציג הודעת שגיאה
     */
    private void applyFilterAndCheckEmpty() {
        adapter.filter(currentQuery, currentRoleFilter);

        // בדיקה קטנה כדי להציג "No users found" אם הסינון מחק את כל הרשימה מהמסך
        if (adapter.getItemCount() == 0) {
            tvNoResults.setVisibility(View.VISIBLE);
        } else {
            tvNoResults.setVisibility(View.GONE);
        }
    }

    // מימוש מתודות החובה של BaseMenuActivity כדי שהתפריט יעבוד פיקס!
    @Override
    protected int getLayoutResourceId() {
        return R.layout.activity_manage_users;
    }

    @Override
    protected int[] getAllowedUserTypes() {
        return new int[]{2}; // רק מנהל מערכת (type = 2) רשאי להיכנס למסך זה!
    }
}