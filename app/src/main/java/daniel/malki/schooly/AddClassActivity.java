package daniel.malki.schooly;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.Toast;

import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class AddClassActivity extends BaseMenuActivity {

    private EditText etClassName;
    private Spinner spinnerGroupType;
    private LinearLayout layoutPairsContainer;
    private Button btnAddPairRow, btnSaveClass;

    private FirebaseFirestore db;

    // רשימות לשליפה מהדאטה בייס
    private List<String> subjectNames = new ArrayList<>(), subjectIds = new ArrayList<>();
    private List<String> teacherNames = new ArrayList<>(), teacherIds = new ArrayList<>();

    @Override
    protected int getLayoutResourceId() { return R.layout.activity_add_class; }

    @Override
    protected int[] getAllowedUserTypes() { return new int[]{2}; } // אדמין

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        db = FirebaseFirestore.getInstance();

        etClassName = findViewById(R.id.etClassName);
        spinnerGroupType = findViewById(R.id.spinnerGroupType);
        layoutPairsContainer = findViewById(R.id.layoutPairsContainer);
        btnAddPairRow = findViewById(R.id.btnAddPairRow);
        btnSaveClass = findViewById(R.id.btnSaveClass);

        setupTypeSpinner();
        loadDataFromFirestore();

        btnAddPairRow.setOnClickListener(v -> addNewPairRow());
        btnSaveClass.setOnClickListener(v -> saveGroupToDatabase());
    }

    private void setupTypeSpinner() {
        String[] types = {"homeroom", "math", "english", "PE", "major a", "major b"};
        spinnerGroupType.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, types));
    }

    private void loadDataFromFirestore() {
        // 1. שליפת מקצועות
        db.collection("subjects").get().addOnSuccessListener(queryDocumentSnapshots -> {
            subjectNames.add("Select Subject"); subjectIds.add("");
            for (QueryDocumentSnapshot doc : queryDocumentSnapshots) {
                subjectNames.add(doc.contains("displayName") ? doc.getString("displayName") : doc.getId());
                subjectIds.add(doc.getId());
            }
            // 2. שליפת מורים (רק משתמשים מסוג 1 או 2)
            db.collection("users").whereIn("type", List.of(1, 2)).get().addOnSuccessListener(users -> {
                teacherNames.add("Select Teacher"); teacherIds.add("");
                for (QueryDocumentSnapshot doc : users) {
                    teacherNames.add(doc.getString("name"));
                    teacherIds.add(doc.getId());
                }
                // כשכל המידע כאן - מוסיפים שורה ראשונה אוטומטית
                addNewPairRow();
            });
        });
    }

    private void addNewPairRow() {
        // פונקציה שמנפחת (Inflate) שורה חדשה לתוך הקונטיינר
        View pairView = LayoutInflater.from(this).inflate(R.layout.item_subject_teacher_pair, null);

        Spinner subSpin = pairView.findViewById(R.id.spinnerSubjectInPair);
        Spinner teachSpin = pairView.findViewById(R.id.spinnerTeacherInPair);
        View btnRemove = pairView.findViewById(R.id.btnRemovePair);

        subSpin.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, subjectNames));
        teachSpin.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, teacherNames));

        btnRemove.setOnClickListener(v -> layoutPairsContainer.removeView(pairView));

        layoutPairsContainer.addView(pairView);
    }

    private void saveGroupToDatabase() {
        String name = etClassName.getText().toString().trim();
        String type = spinnerGroupType.getSelectedItem().toString();

        if (name.isEmpty()) {
            Toast.makeText(this, "Please enter group name", Toast.LENGTH_SHORT).show();
            return;
        }

        // בניית מערך המפות (Array of Maps)
        List<Map<String, Object>> courseAssignments = new ArrayList<>();

        for (int i = 0; i < layoutPairsContainer.getChildCount(); i++) {
            View row = layoutPairsContainer.getChildAt(i);
            Spinner subSpin = row.findViewById(R.id.spinnerSubjectInPair);
            Spinner teachSpin = row.findViewById(R.id.spinnerTeacherInPair);

            int subPos = subSpin.getSelectedItemPosition();
            int teachPos = teachSpin.getSelectedItemPosition();

            if (subPos > 0 && teachPos > 0) {
                Map<String, Object> assignment = new HashMap<>();
                assignment.put("subject", db.collection("subjects").document(subjectIds.get(subPos)));
                assignment.put("teacher", db.collection("users").document(teacherIds.get(teachPos)));
                courseAssignments.add(assignment);
            }
        }

        if (courseAssignments.isEmpty()) {
            Toast.makeText(this, "Assign at least one teacher!", Toast.LENGTH_SHORT).show();
            return;
        }

        Map<String, Object> groupData = new HashMap<>();
        groupData.put("displayName", name);
        groupData.put("type", type);
        groupData.put("courseAssignments", courseAssignments);

        db.collection("classes").add(groupData).addOnSuccessListener(documentReference -> {
            Toast.makeText(this, "Group Created! 🚀", Toast.LENGTH_SHORT).show();
            finish();
        });
    }
}