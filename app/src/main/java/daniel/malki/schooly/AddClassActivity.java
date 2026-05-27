package daniel.malki.schooly;

import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;

import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.WriteBatch;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class AddClassActivity extends BaseMenuActivity {

    private EditText etClassName;
    private Spinner spinnerGroupType;
    private Spinner spinnerGradeSelect;
    private LinearLayout layoutPairsContainer;
    private Button btnAddPairRow, btnSaveClass, btnImportCsv;

    // רכיבי בית ספר דינמיים
    private EditText etSchoolLocked;
    private Spinner spinnerSchoolSelect;
    private TextView tvSchoolTitle;
    private View viewSchoolDivider;

    private FirebaseFirestore db;

    private ArrayList<String> schoolNames = new ArrayList<>();
    private ArrayList<String> schoolIds = new ArrayList<>();
    private DocumentReference selectedSchool;

    // נתוני השכבות
    private ArrayList<String> gradeNames = new ArrayList<>();
    private ArrayList<String> gradeIds = new ArrayList<>();
    private DocumentReference selectedGrade;

    private int currentAdminType;
    private String currentAdminId;

    // הרשימות הגלובליות
    private List<String> subjectNames = new ArrayList<>(), subjectIds = new ArrayList<>();

    // ✨ רשימה מורכבת למורים כדי לשמור גם את המקצועות שהם מלמדים לטובת הסינון
    private List<Map<String, Object>> allTeachersData = new ArrayList<>();

    // לאנצ'ר לבחירת קובץ CSV
    private final ActivityResultLauncher<Intent> csvPickerLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                    Uri csvUri = result.getData().getData();
                    if (csvUri != null) {
                        processCsvFile(csvUri);
                    }
                }
            }
    );

    @Override
    protected int getLayoutResourceId() {
        return R.layout.activity_add_class;
    }

    @Override
    protected int[] getAllowedUserTypes() {
        return new int[]{2, 3};
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setTitle("Create Learning Group");

        db = FirebaseFirestore.getInstance();

        etClassName = findViewById(R.id.etClassName);
        spinnerGroupType = findViewById(R.id.spinnerGroupType);
        spinnerGradeSelect = findViewById(R.id.spinnerGradeSelect);
        layoutPairsContainer = findViewById(R.id.layoutPairsContainer);
        btnAddPairRow = findViewById(R.id.btnAddPairRow);
        btnSaveClass = findViewById(R.id.btnSaveClass);
        btnImportCsv = findViewById(R.id.btnImportCsv);

        etSchoolLocked = findViewById(R.id.etSchoolLocked);
        spinnerSchoolSelect = findViewById(R.id.spinnerSchoolSelect);
        tvSchoolTitle = findViewById(R.id.tvSchoolTitle);
        viewSchoolDivider = findViewById(R.id.viewSchoolDivider);

        SharedPreferences prefs = getSharedPreferences("SchoolyPrefs", MODE_PRIVATE);
        currentAdminType = prefs.getInt("userType", 2);
        currentAdminId = prefs.getString("userId", "");

        String[] types = {"Select Type...", "homeroom", "math", "english", "sports", "major a", "major b"};
        spinnerGroupType.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, types));

        handleSchoolSelection();
        loadSubjectsAndTeachers();
        loadGrades();

        btnAddPairRow.setOnClickListener(v -> addAssignmentRow());
        btnSaveClass.setOnClickListener(v -> saveGroupToDatabase());

        btnImportCsv.setOnClickListener(v -> {
            if (selectedSchool == null) {
                Toast.makeText(this, "Please select a school first!", Toast.LENGTH_SHORT).show();
                return;
            }
            Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
            intent.setType("*/*");
            csvPickerLauncher.launch(intent);
        });
    }

    private void loadGrades() {
        db.collection("grades").orderBy("displayName", Query.Direction.ASCENDING).get().addOnSuccessListener(snapshots -> {
            gradeNames.clear();
            gradeIds.clear();
            gradeNames.add("Select Grade...");
            gradeIds.add("");

            for (QueryDocumentSnapshot doc : snapshots) {
                gradeIds.add(doc.getId());
                gradeNames.add(doc.getString("displayName") != null ? doc.getString("displayName") : doc.getId());
            }

            spinnerGradeSelect.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, gradeNames));

            spinnerGradeSelect.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                @Override
                public void onItemSelected(AdapterView<?> p, View v, int pos, long id) {
                    if (pos > 0) {
                        selectedGrade = db.collection("grades").document(gradeIds.get(pos));
                    } else {
                        selectedGrade = null;
                    }
                }

                @Override
                public void onNothingSelected(AdapterView<?> p) {}
            });
        }).addOnFailureListener(e -> {
            Toast.makeText(this, "Failed to load grades", Toast.LENGTH_SHORT).show();
        });
    }

    private void handleSchoolSelection() {
        if (currentAdminType == 3) {
            tvSchoolTitle.setVisibility(View.VISIBLE);
            spinnerSchoolSelect.setVisibility(View.VISIBLE);
            viewSchoolDivider.setVisibility(View.VISIBLE);

            db.collection("schools").get().addOnSuccessListener(snapshots -> {
                schoolNames.clear();
                schoolIds.clear();
                schoolNames.add("Select School...");
                schoolIds.add("");

                for (QueryDocumentSnapshot d : snapshots) {
                    schoolIds.add(d.getId());
                    schoolNames.add(d.getString("displayName") != null ? d.getString("displayName") : d.getId());
                }
                spinnerSchoolSelect.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, schoolNames));

                spinnerSchoolSelect.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                    @Override
                    public void onItemSelected(AdapterView<?> p, View v, int pos, long id) {
                        if (pos > 0) {
                            selectedSchool = db.collection("schools").document(schoolIds.get(pos));
                        } else {
                            selectedSchool = null;
                        }
                    }

                    @Override
                    public void onNothingSelected(AdapterView<?> p) {}
                });
            });
        } else if (currentAdminType == 2) {
            tvSchoolTitle.setVisibility(View.VISIBLE);
            etSchoolLocked.setVisibility(View.VISIBLE);
            viewSchoolDivider.setVisibility(View.VISIBLE);

            db.collection("users").document(currentAdminId).get().addOnSuccessListener(doc -> {
                if (doc.exists() && doc.getDocumentReference("school") != null) {
                    selectedSchool = doc.getDocumentReference("school");
                    selectedSchool.get().addOnSuccessListener(sDoc -> {
                        etSchoolLocked.setText(sDoc.getString("displayName"));
                    });
                }
            });
        }
    }

    private void loadSubjectsAndTeachers() {
        db.collection("subjects").get().addOnSuccessListener(snapshots -> {
            subjectNames.clear();
            subjectIds.clear();
            subjectNames.add("Select Subject...");
            subjectIds.add("");
            for (QueryDocumentSnapshot doc : snapshots) {
                subjectIds.add(doc.getId());
                subjectNames.add(doc.getString("displayName"));
            }

            db.collection("users").whereIn("type", java.util.Arrays.asList(1, 2)).get().addOnSuccessListener(userSnaps -> {
                allTeachersData.clear();

                for (QueryDocumentSnapshot doc : userSnaps) {
                    Map<String, Object> teacher = new HashMap<>();
                    teacher.put("id", doc.getId());
                    String name = doc.getString("name") != null ? doc.getString("name") : doc.getString("firstName") + " " + doc.getString("lastName");
                    teacher.put("name", name);

                    // משיכת המקצועות שהמורה יודע ללמד
                    List<String> teachableSubIds = new ArrayList<>();
                    List<DocumentReference> subRefs = (List<DocumentReference>) doc.get("teachableSubjects");
                    if (subRefs != null) {
                        for (DocumentReference ref : subRefs) {
                            teachableSubIds.add(ref.getId());
                        }
                    }
                    teacher.put("subjects", teachableSubIds);
                    allTeachersData.add(teacher);
                }

                if (layoutPairsContainer.getChildCount() == 0) addAssignmentRow();
            });
        });
    }

    private void addAssignmentRow() {
        View rowView = LayoutInflater.from(this).inflate(R.layout.item_class_assignment_row, layoutPairsContainer, false);
        Spinner spinSub = rowView.findViewById(R.id.spinnerRowSubject);
        Spinner spinTeach = rowView.findViewById(R.id.spinnerRowTeacher);
        ImageButton btnRemove = rowView.findViewById(R.id.btnRemoveRow);

        spinSub.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, subjectNames));

        // ✨ רשימת מורים דינמית לשורה הזו ספציפית
        List<String> rowTeacherNames = new ArrayList<>();
        List<String> rowTeacherIds = new ArrayList<>();
        rowTeacherNames.add("Select Teacher...");
        rowTeacherIds.add("");

        ArrayAdapter<String> teacherAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, rowTeacherNames);
        spinTeach.setAdapter(teacherAdapter);
        spinTeach.setEnabled(false); // המורה חסום לבחירה עד שבוחרים מקצוע!

        // נשמור את הרשימה של ה-IDs כתגית בתוך הספינר כדי לשלוף אותה בקלות בשמירה
        spinTeach.setTag(rowTeacherIds);

        // ✨ מאזין לבחירת מקצוע שמסנן את המורים
        spinSub.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                rowTeacherNames.clear();
                rowTeacherIds.clear();
                rowTeacherNames.add("Select Teacher...");
                rowTeacherIds.add("");

                if (position > 0) {
                    String selectedSubId = subjectIds.get(position);

                    // סינון המורים: רק מי שיש לו את המקצוע במערך
                    for (Map<String, Object> t : allTeachersData) {
                        List<String> tSubs = (List<String>) t.get("subjects");
                        if (tSubs != null && tSubs.contains(selectedSubId)) {
                            rowTeacherNames.add((String) t.get("name"));
                            rowTeacherIds.add((String) t.get("id"));
                        }
                    }
                    spinTeach.setEnabled(true); // עכשיו אפשר לבחור מורה
                } else {
                    spinTeach.setEnabled(false); // נעל בחזרה אם בחרו "Select Subject..."
                }

                teacherAdapter.notifyDataSetChanged();
                spinTeach.setSelection(0);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });

        btnRemove.setOnClickListener(v -> layoutPairsContainer.removeView(rowView));
        layoutPairsContainer.addView(rowView);
    }

    private void saveGroupToDatabase() {
        String name = etClassName.getText().toString().trim();
        int typePos = spinnerGroupType.getSelectedItemPosition();

        if (selectedSchool == null) {
            Toast.makeText(this, "School must be selected!", Toast.LENGTH_SHORT).show();
            return;
        }
        if (name.isEmpty() || typePos == 0) {
            Toast.makeText(this, "Please enter group name and select type.", Toast.LENGTH_SHORT).show();
            return;
        }
        if (selectedGrade == null) {
            Toast.makeText(this, "Please select a grade.", Toast.LENGTH_SHORT).show();
            return;
        }

        String type = spinnerGroupType.getSelectedItem().toString();
        List<Map<String, Object>> courseAssignments = new ArrayList<>();

        for (int i = 0; i < layoutPairsContainer.getChildCount(); i++) {
            View row = layoutPairsContainer.getChildAt(i);
            Spinner subSpin = row.findViewById(R.id.spinnerRowSubject);
            Spinner teachSpin = row.findViewById(R.id.spinnerRowTeacher);

            int subPos = subSpin.getSelectedItemPosition();
            int teachPos = teachSpin.getSelectedItemPosition();

            if (subPos == 0 || teachPos == 0) {
                Toast.makeText(this, "Error: Row " + (i + 1) + " has missing selections!", Toast.LENGTH_LONG).show();
                return;
            }

            // ✨ שולפים את ה-ID של המקצוע מהרשימה הגלובלית ואת ה-ID של המורה מהרשימה המסוננת ששמרנו ב-Tag
            String selectedSubjectId = subjectIds.get(subPos);
            List<String> currentTeachIds = (List<String>) teachSpin.getTag();
            String selectedTeacherId = currentTeachIds.get(teachPos);

            Map<String, Object> assignment = new HashMap<>();
            assignment.put("subject", db.collection("subjects").document(selectedSubjectId));
            assignment.put("teacher", db.collection("users").document(selectedTeacherId));
            courseAssignments.add(assignment);
        }

        Map<String, Object> groupData = new HashMap<>();
        groupData.put("displayName", name);
        groupData.put("type", type);
        groupData.put("school", selectedSchool);
        groupData.put("gradeRef", selectedGrade);
        groupData.put("courseAssignments", courseAssignments);

        db.collection("classes").add(groupData).addOnSuccessListener(documentReference -> {
            Toast.makeText(this, "Group Created successfully! 🚀", Toast.LENGTH_SHORT).show();
            finish();
        });
    }

    private void processCsvFile(Uri uri) {
        db.collection("grades").get().addOnSuccessListener(gradeSnaps -> {
            Map<String, DocumentReference> gradeDict = new HashMap<>();
            for (QueryDocumentSnapshot doc : gradeSnaps) {
                String gName = doc.getString("displayName");
                if (gName != null) {
                    gradeDict.put(gName.trim().toLowerCase(), doc.getReference());
                }
            }

            try {
                InputStream inputStream = getContentResolver().openInputStream(uri);
                BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
                String line;

                WriteBatch batch = db.batch();
                int count = 0;
                boolean isFirstRow = true;

                while ((line = reader.readLine()) != null) {
                    if (isFirstRow && line.toLowerCase().contains("name")) {
                        isFirstRow = false;
                        continue;
                    }
                    isFirstRow = false;

                    String[] columns = line.split(",");
                    if (columns.length >= 3) {
                        String className = columns[0].trim();
                        String classType = columns[1].trim().toLowerCase();
                        String gradeName = columns[2].trim().toLowerCase();

                        if (className.isEmpty() || (!classType.equals("homeroom") && !classType.equals("math")
                                && !classType.equals("english") && !classType.equals("sports") && !classType.equals("major a") && !classType.equals("major b"))) {
                            continue;
                        }

                        DocumentReference gradeRef = gradeDict.get(gradeName);
                        if (gradeRef == null) {
                            continue;
                        }

                        DocumentReference newClassRef = db.collection("classes").document();

                        Map<String, Object> groupData = new HashMap<>();
                        groupData.put("displayName", className);
                        groupData.put("type", classType);
                        groupData.put("school", selectedSchool);
                        groupData.put("gradeRef", gradeRef);
                        groupData.put("courseAssignments", new ArrayList<>());

                        batch.set(newClassRef, groupData);
                        count++;
                    }
                }
                reader.close();

                if (count > 0) {
                    final int finalCount = count;
                    batch.commit().addOnSuccessListener(aVoid -> {
                        Toast.makeText(this, "Successfully imported " + finalCount + " groups! 🎉", Toast.LENGTH_LONG).show();
                        finish();
                    }).addOnFailureListener(e -> {
                        Toast.makeText(this, "Failed to import groups: " + e.getMessage(), Toast.LENGTH_LONG).show();
                    });
                } else {
                    Toast.makeText(this, "No valid groups found in CSV. Make sure grade names match exactly.", Toast.LENGTH_LONG).show();
                }

            } catch (Exception e) {
                Toast.makeText(this, "Error reading CSV file.", Toast.LENGTH_SHORT).show();
                e.printStackTrace();
            }
        });
    }
}