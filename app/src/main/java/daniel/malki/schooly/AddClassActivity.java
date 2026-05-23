package daniel.malki.schooly;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
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

    // רכיבי בית ספר דינמיים
    private EditText etSchoolLocked;
    private Spinner spinnerSchoolSelect;
    private TextView tvSchoolTitle;
    private View viewSchoolDivider;

    private FirebaseFirestore db;

    private ArrayList<String> schoolNames = new ArrayList<>();
    private ArrayList<String> schoolIds = new ArrayList<>();
    private DocumentReference selectedSchool;

    private int currentAdminType;
    private String currentAdminId;

    // הרשימות הגלובליות המלאות
    private List<String> subjectNames = new ArrayList<>(), subjectIds = new ArrayList<>();
    private List<String> teacherNames = new ArrayList<>(), teacherIds = new ArrayList<>();

    // מפת קשר בין מורה למקצועותיו
    private Map<String, List<String>> teacherToSubjectsMap = new HashMap<>();

    @Override
    protected int getLayoutResourceId() { return R.layout.activity_add_class; }

    @Override
    protected int[] getAllowedUserTypes() { return new int[]{2, 3}; }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        db = FirebaseFirestore.getInstance();

        etClassName = findViewById(R.id.etClassName);
        spinnerGroupType = findViewById(R.id.spinnerGroupType);
        layoutPairsContainer = findViewById(R.id.layoutPairsContainer);
        btnAddPairRow = findViewById(R.id.btnAddPairRow);
        btnSaveClass = findViewById(R.id.btnSaveClass);

        // אתחול רכיבי בית ספר
        etSchoolLocked = findViewById(R.id.etSchoolLocked);
        spinnerSchoolSelect = findViewById(R.id.spinnerSchoolSelect);
        tvSchoolTitle = findViewById(R.id.tvSchoolTitle);
        viewSchoolDivider = findViewById(R.id.viewSchoolDivider);

        setupTypeSpinner();

        // שליפת נתוני המנהל המחובר
        SharedPreferences prefs = getSharedPreferences("SchoolyPrefs", MODE_PRIVATE);
        currentAdminType = prefs.getInt("userType", 2);
        currentAdminId = prefs.getString("userId", "");

        // טעינת בסיס המקצועות (גלובלי)
        loadSubjectsFromFirestore();

        btnAddPairRow.setOnClickListener(v -> {
            if (selectedSchool == null) {
                Toast.makeText(this, "Please select a school first!", Toast.LENGTH_SHORT).show();
                return;
            }
            addNewPairRow();
        });

        btnSaveClass.setOnClickListener(v -> saveGroupToDatabase());
    }

    private void setupTypeSpinner() {
        String[] types = {"Homeroom", "Math", "English", "PE", "Major A", "Major B"};
        spinnerGroupType.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, types));
    }

    private void checkAdminSchoolStatus() {
        if (currentAdminType == 2) {
            setSchoolLayoutVisibility(View.VISIBLE, View.GONE);
            if (!currentAdminId.isEmpty()) {
                db.collection("users").document(currentAdminId).get().addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        selectedSchool = documentSnapshot.getDocumentReference("school");
                        if (selectedSchool != null) {
                            selectedSchool.get().addOnSuccessListener(schoolDoc -> {
                                if (schoolDoc.exists()) {
                                    etSchoolLocked.setText(schoolDoc.getString("displayName"));
                                    // טעינת מורים רק של בית הספר הזה
                                    loadTeachersForSelectedSchool();
                                }
                            });
                        }
                    }
                });
            }
        } else if (currentAdminType == 3) {
            setSchoolLayoutVisibility(View.GONE, View.VISIBLE);
            loadAllSchoolsForSchoolyAdmin();
        }
    }

    private void setSchoolLayoutVisibility(int lockedVis, int selectVis) {
        if (etSchoolLocked != null) etSchoolLocked.setVisibility(lockedVis);
        if (spinnerSchoolSelect != null) spinnerSchoolSelect.setVisibility(selectVis);
        int generalVisibility = (lockedVis == View.GONE && selectVis == View.GONE) ? View.GONE : View.VISIBLE;
        if (tvSchoolTitle != null) tvSchoolTitle.setVisibility(generalVisibility);
        if (viewSchoolDivider != null) viewSchoolDivider.setVisibility(generalVisibility);
    }

    private void loadAllSchoolsForSchoolyAdmin() {
        db.collection("schools").get().addOnSuccessListener(queryDocumentSnapshots -> {
            schoolNames.clear(); schoolIds.clear();
            for (QueryDocumentSnapshot doc : queryDocumentSnapshots) {
                schoolIds.add(doc.getId());
                schoolNames.add(doc.contains("displayName") ? doc.getString("displayName") : doc.getId());
            }
            spinnerSchoolSelect.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, schoolNames));
            spinnerSchoolSelect.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                @Override
                public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                    selectedSchool = db.collection("schools").document(schoolIds.get(position));
                    // בכל שינוי בית ספר של מנהל על, מנקים שורות קודמות וטוענים מורים מחדש
                    layoutPairsContainer.removeAllViews();
                    loadTeachersForSelectedSchool();
                }
                @Override
                public void onNothingSelected(AdapterView<?> parent) {}
            });
        });
    }

    private void loadSubjectsFromFirestore() {
        db.collection("subjects").get().addOnSuccessListener(queryDocumentSnapshots -> {
            subjectNames.clear(); subjectIds.clear();
            subjectNames.add("Select Subject"); subjectIds.add("");
            for (QueryDocumentSnapshot doc : queryDocumentSnapshots) {
                subjectNames.add(doc.contains("displayName") ? doc.getString("displayName") : doc.getId());
                subjectIds.add(doc.getId());
            }
            // רק אחרי שהמקצועות נטענו, בודקים את סטטוס בית הספר כדי לטעון מורים
            checkAdminSchoolStatus();
        });
    }

    private void loadTeachersForSelectedSchool() {
        if (selectedSchool == null) return;

        // סינון קשוח ב-Firestore: מורים השייכים אך ורק לבית הספר הנבחר
        db.collection("users")
                .whereIn("type", List.of(1, 2))
                .whereEqualTo("school", selectedSchool)
                .get()
                .addOnSuccessListener(users -> {
                    teacherNames.clear(); teacherIds.clear();
                    teacherToSubjectsMap.clear();

                    teacherNames.add("Select Teacher"); teacherIds.add("");

                    for (QueryDocumentSnapshot doc : users) {
                        String teacherId = doc.getId();
                        teacherNames.add(doc.getString("name"));
                        teacherIds.add(teacherId);

                        List<String> teacherSubIds = new ArrayList<>();
                        List<DocumentReference> refs = (List<DocumentReference>) doc.get("teachableSubjects");
                        if (refs != null) {
                            for (DocumentReference ref : refs) {
                                teacherSubIds.add(ref.getId());
                            }
                        }
                        teacherToSubjectsMap.put(teacherId, teacherSubIds);
                    }

                    // הוספת שורה ראשונה אוטומטית אם המכולה ריקה
                    if (layoutPairsContainer.getChildCount() == 0) {
                        addNewPairRow();
                    }
                });
    }

    private void addNewPairRow() {
        View pairView = LayoutInflater.from(this).inflate(R.layout.item_subject_teacher_pair, null);

        Spinner subSpin = pairView.findViewById(R.id.spinnerSubjectInPair);
        Spinner teachSpin = pairView.findViewById(R.id.spinnerTeacherInPair);
        View btnRemove = pairView.findViewById(R.id.btnRemovePair);
        TextView tvWarning = pairView.findViewById(R.id.tvSubjectWarning);

        subSpin.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, new ArrayList<>(subjectNames)));
        teachSpin.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, new ArrayList<>(teacherNames)));
        teachSpin.setEnabled(false);

        if (tvWarning != null) {
            tvWarning.setText("⚠️ Select a subject first");
            tvWarning.setVisibility(View.VISIBLE);
        }

        subSpin.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if (position > 0) {
                    if (tvWarning != null) tvWarning.setVisibility(View.GONE);
                    teachSpin.setEnabled(true);

                    String selectedSubjectId = subjectIds.get(position);
                    ArrayList<String> filteredTeachers = new ArrayList<>();
                    filteredTeachers.add("Select Teacher");

                    for (int i = 1; i < teacherIds.size(); i++) {
                        String tId = teacherIds.get(i);
                        List<String> subs = teacherToSubjectsMap.get(tId);
                        if (subs != null && subs.contains(selectedSubjectId)) {
                            filteredTeachers.add(teacherNames.get(i));
                        }
                    }
                    teachSpin.setAdapter(new ArrayAdapter<>(AddClassActivity.this, android.R.layout.simple_spinner_dropdown_item, filteredTeachers));
                } else {
                    if (tvWarning != null) {
                        tvWarning.setText("⚠️ Select a subject first");
                        tvWarning.setVisibility(View.VISIBLE);
                    }
                    teachSpin.setAdapter(new ArrayAdapter<>(AddClassActivity.this, android.R.layout.simple_spinner_dropdown_item, new ArrayList<>(teacherNames)));
                    teachSpin.setSelection(0);
                    teachSpin.setEnabled(false);
                }
            }
            @Override public void onNothingSelected(AdapterView<?> parent) {}
        });

        btnRemove.setOnClickListener(v -> layoutPairsContainer.removeView(pairView));
        layoutPairsContainer.addView(pairView);
    }

    private void saveGroupToDatabase() {
        String name = etClassName.getText().toString().trim();
        String type = spinnerGroupType.getSelectedItem().toString().toLowerCase().trim();

        if (name.isEmpty()) {
            Toast.makeText(this, "Please enter group name", Toast.LENGTH_SHORT).show();
            return;
        }

        if (selectedSchool == null) {
            Toast.makeText(this, "Error: No school assigned to this class!", Toast.LENGTH_SHORT).show();
            return;
        }

        if (layoutPairsContainer.getChildCount() == 0) {
            Toast.makeText(this, "Please add at least one subject and teacher pair!", Toast.LENGTH_SHORT).show();
            return;
        }

        List<Map<String, Object>> courseAssignments = new ArrayList<>();

        for (int i = 0; i < layoutPairsContainer.getChildCount(); i++) {
            View row = layoutPairsContainer.getChildAt(i);
            Spinner subSpin = row.findViewById(R.id.spinnerSubjectInPair);
            Spinner teachSpin = row.findViewById(R.id.spinnerTeacherInPair);

            int subPos = subSpin.getSelectedItemPosition();
            int teachPos = teachSpin.getSelectedItemPosition();

            if (subPos == 0 || teachPos == 0) {
                Toast.makeText(this, "Error: Row " + (i + 1) + " has missing selections!", Toast.LENGTH_LONG).show();
                return;
            }

            String subName = subSpin.getSelectedItem().toString();
            String teachName = teachSpin.getSelectedItem().toString();

            int realSubPos = subjectNames.indexOf(subName);
            int realTeachPos = teacherNames.indexOf(teachName);

            if (realSubPos > 0 && realTeachPos > 0) {
                Map<String, Object> assignment = new HashMap<>();
                assignment.put("subject", db.collection("subjects").document(subjectIds.get(realSubPos)));
                assignment.put("teacher", db.collection("users").document(teacherIds.get(realTeachPos)));
                courseAssignments.add(assignment);
            }
        }

        Map<String, Object> groupData = new HashMap<>();
        groupData.put("displayName", name);
        groupData.put("type", type);
        groupData.put("school", selectedSchool); // ✨ נשמר כאינדקס / Reference של בית הספר לסינון עתידי
        groupData.put("courseAssignments", courseAssignments);

        db.collection("classes").add(groupData).addOnSuccessListener(documentReference -> {
            Toast.makeText(this, "Group Created successfully! 🚀", Toast.LENGTH_SHORT).show();
            finish();
        });
    }
}