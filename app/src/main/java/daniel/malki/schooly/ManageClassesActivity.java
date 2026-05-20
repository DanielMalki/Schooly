package daniel.malki.schooly;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import java.util.ArrayList;
import java.util.List;

public class ManageClassesActivity extends BaseMenuActivity {

    private EditText etSearchClass;
    private RecyclerView rvClasses;
    private TextView tvNoClassesResults;

    private ClassAdapter adapter;
    private List<SchoolClass> classList;
    private FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setTitle("Manage Classes & Groups"); // כותרת ל-Toolbar העליון

        db = FirebaseFirestore.getInstance();

        // 1. חיבור רכיבי העיצוב מה-XML
        etSearchClass = findViewById(R.id.etSearchClass);
        rvClasses = findViewById(R.id.rvClasses);
        tvNoClassesResults = findViewById(R.id.tvNoClassesResults);

        // 2. הגדרת ה-RecyclerView והאדאפטר
        classList = new ArrayList<>();
        adapter = new ClassAdapter(classList);
        rvClasses.setLayoutManager(new LinearLayoutManager(this));
        rvClasses.setAdapter(adapter);

        // 3. טעינת הכיתות וקבוצות הלימוד מתוך פיירבייס
        loadClassesFromFirestore();

        // 4. האזנה לטקסט בתיבת החיפוש לסינון בזמן אמת
        etSearchClass.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                // הפעלת פונקציית הסינון באדאפטר
                adapter.filter(s.toString());

                // בדיקה אם הרשימה ריקה כדי להציג הודעת "No classes found"
                if (adapter.getItemCount() == 0) {
                    tvNoClassesResults.setVisibility(View.VISIBLE);
                } else {
                    tvNoClassesResults.setVisibility(View.GONE);
                }
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });
    }

    /**
     * משיכת כל המסמכים מקולקשן classes ב-Firestore
     */
    private void loadClassesFromFirestore() {
        db.collection("classes").get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    classList.clear();
                    for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                        // המרה אוטומטית של המסמך לאובייקט SchoolClass בזכות הטאגים שהגדרנו במודל!
                        SchoolClass schoolClass = document.toObject(SchoolClass.class);
                        schoolClass.setClassId(document.getId()); // שמירת מזהה המסמך
                        classList.add(schoolClass);
                    }
                    // עדכון הרשימה המקורית באדאפטר וריענון המסך
                    adapter.updateList(classList);

                    if (classList.isEmpty()) {
                        tvNoClassesResults.setVisibility(View.VISIBLE);
                    } else {
                        tvNoClassesResults.setVisibility(View.GONE);
                    }
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Error loading classes: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    // מימוש מתודות החובה של BaseMenuActivity
    @Override
    protected int getLayoutResourceId() {
        return R.layout.activity_manage_classes;
    }

    @Override
    protected int[] getAllowedUserTypes() {
        return new int[]{2}; // רק אדמין (type = 2) מורשה להיכנס למסך ניהול כיתות
    }
}