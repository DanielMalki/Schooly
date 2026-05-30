package daniel.malki.schooly;

import android.app.AlertDialog;
import android.graphics.Color;
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

import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ClassDetailActivity extends BaseMenuActivity {

    private EditText etClassSchoolLocked, etClassTypeLocked, etClassName;
    private Spinner spinnerGradeSelect;
    private Button btnAddPairRow, btnSaveClassDetails, btnDeleteClass;
    private LinearLayout layoutPairsContainer, layoutClassStudentsList;
    private TextView tvClassDetailTitle;

    private FirebaseFirestore db;
    private String selectedClassId;
    private DocumentReference classRef;
    private DocumentReference schoolRef;
    private DocumentReference currentGradeRef;
    private String classType;

    // רשימות עבור ה-Spinner של השכבות
    private ArrayList<String> gradeNames = new ArrayList<>();
    private ArrayList<DocumentReference> gradeRefs = new ArrayList<>();

    // רשימות גלובליות למקצועות ומורים
    private List<String> subjectNames = new ArrayList<>(), subjectIds = new ArrayList<>();
    private List<Map<String, Object>> allTeachersData = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        db = FirebaseFirestore.getInstance();

        etClassSchoolLocked = findViewById(R.id.etClassSchoolLocked);
        etClassTypeLocked = findViewById(R.id.etClassTypeLocked);
        etClassName = findViewById(R.id.etClassName);
        spinnerGradeSelect = findViewById(R.id.spinnerGradeSelect);
        btnAddPairRow = findViewById(R.id.btnAddPairRow);
        btnSaveClassDetails = findViewById(R.id.btnSaveClassDetails);
        btnDeleteClass = findViewById(R.id.btnDeleteClass);
        layoutPairsContainer = findViewById(R.id.layoutPairsContainer);
        layoutClassStudentsList = findViewById(R.id.layoutClassStudentsList);
        tvClassDetailTitle = findViewById(R.id.tvClassDetailTitle);

        selectedClassId = getIntent().getStringExtra("classId");
        if (selectedClassId == null) {
            Toast.makeText(this, "Class ID is missing!", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        classRef = db.collection("classes").document(selectedClassId);

        // טוענים קודם שכבות, מקצועות ומורים, ורק אז את פרטי הכיתה
        loadGrades();
        loadSubjectsAndTeachers();

        if (btnSaveClassDetails != null) {
            btnSaveClassDetails.setOnClickListener(v -> saveClassDetails());
        }
        if (btnDeleteClass != null) {
            btnDeleteClass.setOnClickListener(v -> deleteClass());
        }
        if (btnAddPairRow != null) {
            btnAddPairRow.setOnClickListener(v -> addAssignmentRow(null, null));
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

                loadClassDetails();
            });
        });
    }

    private void addAssignmentRow(DocumentReference preSelectedSub, DocumentReference preSelectedTeach) {
        View rowView = LayoutInflater.from(this).inflate(R.layout.item_class_assignment_row, layoutPairsContainer, false);
        Spinner spinSub = rowView.findViewById(R.id.spinnerRowSubject);
        Spinner spinTeach = rowView.findViewById(R.id.spinnerRowTeacher);
        ImageButton btnRemove = rowView.findViewById(R.id.btnRemoveRow);

        spinSub.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, subjectNames));

        List<String> rowTeacherNames = new ArrayList<>();
        List<String> rowTeacherIds = new ArrayList<>();
        rowTeacherNames.add("Select Teacher...");
        rowTeacherIds.add("");

        ArrayAdapter<String> teacherAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, rowTeacherNames);
        spinTeach.setAdapter(teacherAdapter);
        spinTeach.setEnabled(false);
        spinTeach.setTag(rowTeacherIds);

        final boolean[] isFirstLoad = {true};

        spinSub.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                rowTeacherNames.clear();
                rowTeacherIds.clear();
                rowTeacherNames.add("Select Teacher...");
                rowTeacherIds.add("");

                if (position > 0) {
                    String selectedSubId = subjectIds.get(position);
                    for (Map<String, Object> t : allTeachersData) {
                        List<String> tSubs = (List<String>) t.get("subjects");
                        if (tSubs != null && tSubs.contains(selectedSubId)) {
                            rowTeacherNames.add((String) t.get("name"));
                            rowTeacherIds.add((String) t.get("id"));
                        }
                    }
                    spinTeach.setEnabled(true);
                } else {
                    spinTeach.setEnabled(false);
                }
                teacherAdapter.notifyDataSetChanged();

                if (isFirstLoad[0] && preSelectedTeach != null) {
                    String teachId = preSelectedTeach.getId();
                    for (int i = 0; i < rowTeacherIds.size(); i++) {
                        if (rowTeacherIds.get(i).equals(teachId)) {
                            spinTeach.setSelection(i);
                            break;
                        }
                    }
                    isFirstLoad[0] = false;
                } else {
                    spinTeach.setSelection(0);
                }
            }
            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });

        btnRemove.setOnClickListener(v -> layoutPairsContainer.removeView(rowView));
        layoutPairsContainer.addView(rowView);

        if (preSelectedSub != null) {
            String subId = preSelectedSub.getId();
            for (int i = 0; i < subjectIds.size(); i++) {
                if (subjectIds.get(i).equals(subId)) {
                    spinSub.setSelection(i);
                    break;
                }
            }
        }
    }

    private void loadClassDetails() {
        classRef.get().addOnSuccessListener(documentSnapshot -> {
            if (documentSnapshot.exists()) {
                String displayName = documentSnapshot.getString("displayName");
                if (displayName != null) {
                    etClassName.setText(displayName);
                    if (tvClassDetailTitle != null) {
                        tvClassDetailTitle.setText("Class: " + displayName);
                    }
                }

                classType = documentSnapshot.getString("type");
                if (classType != null) {
                    etClassTypeLocked.setText(classType);

                    if (classType.equalsIgnoreCase("homeroom")) {
                        if (layoutPairsContainer != null) layoutPairsContainer.setVisibility(View.GONE);
                        if (btnAddPairRow != null) btnAddPairRow.setVisibility(View.GONE);
                    } else {
                        if (layoutPairsContainer != null) layoutPairsContainer.setVisibility(View.VISIBLE);
                        if (btnAddPairRow != null) btnAddPairRow.setVisibility(View.VISIBLE);
                    }
                }

                currentGradeRef = documentSnapshot.getDocumentReference("gradeRef");
                syncSelectedGradeInSpinner();

                schoolRef = documentSnapshot.getDocumentReference("school");
                if (schoolRef != null) {
                    schoolRef.get().addOnSuccessListener(schoolDoc -> {
                        if (schoolDoc.exists()) {
                            String schoolName = schoolDoc.getString("displayName");
                            if (schoolName == null) schoolName = schoolDoc.getId();
                            etClassSchoolLocked.setText(schoolName);
                        }
                    });
                }

                loadEnrolledStudents();

                if (layoutPairsContainer != null) {
                    layoutPairsContainer.removeAllViews();
                    List<Map<String, Object>> courseAssignments = (List<Map<String, Object>>) documentSnapshot.get("courseAssignments");

                    if (courseAssignments != null && !courseAssignments.isEmpty()) {
                        for (Map<String, Object> assignment : courseAssignments) {
                            DocumentReference subRef = (DocumentReference) assignment.get("subject");
                            DocumentReference teachRef = (DocumentReference) assignment.get("teacher");
                            addAssignmentRow(subRef, teachRef);
                        }
                    } else if (classType != null && !classType.equalsIgnoreCase("homeroom")) {
                        addAssignmentRow(null, null);
                    }
                }
            }
        }).addOnFailureListener(e -> Toast.makeText(this, "Error loading class details: " + e.getMessage(), Toast.LENGTH_SHORT).show());
    }

    private void saveClassDetails() {
        String newName = etClassName.getText().toString().trim();
        if (newName.isEmpty()) {
            Toast.makeText(this, "Class name cannot be empty", Toast.LENGTH_SHORT).show();
            return;
        }

        Map<String, Object> updates = new HashMap<>();
        updates.put("displayName", newName);

        int selectedGradePos = spinnerGradeSelect.getSelectedItemPosition();
        if (selectedGradePos >= 0 && selectedGradePos < gradeRefs.size()) {
            updates.put("gradeRef", gradeRefs.get(selectedGradePos));
        }

        if (classType != null && !classType.equalsIgnoreCase("homeroom")) {
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

                String selectedSubjectId = subjectIds.get(subPos);
                List<String> currentTeachIds = (List<String>) teachSpin.getTag();
                String selectedTeacherId = currentTeachIds.get(teachPos);

                Map<String, Object> assignment = new HashMap<>();
                assignment.put("subject", db.collection("subjects").document(selectedSubjectId));
                assignment.put("teacher", db.collection("users").document(selectedTeacherId));
                courseAssignments.add(assignment);
            }
            updates.put("courseAssignments", courseAssignments);
        }

        classRef.update(updates).addOnSuccessListener(aVoid -> {
            Toast.makeText(this, "Class details updated successfully! 💾", Toast.LENGTH_SHORT).show();
            finish();
        }).addOnFailureListener(e -> Toast.makeText(this, "Error updating class: " + e.getMessage(), Toast.LENGTH_SHORT).show());
    }

    private int extractGradeNumber(String name) {
        if (name == null) return 9999;
        String numStr = name.replaceAll("\\D+", "");
        if (numStr.isEmpty()) return 9999;
        return Integer.parseInt(numStr);
    }

    private void loadGrades() {
        db.collection("grades").get().addOnSuccessListener(snapshot -> {
            gradeNames.clear();
            gradeRefs.clear();

            List<QueryDocumentSnapshot> sortedGrades = new ArrayList<>();
            for (QueryDocumentSnapshot doc : snapshot) {
                sortedGrades.add(doc);
            }

            java.util.Collections.sort(sortedGrades, (d1, d2) -> {
                String n1 = d1.getString("displayName");
                if (n1 == null) n1 = d1.getId();
                String n2 = d2.getString("displayName");
                if (n2 == null) n2 = d2.getId();
                return Integer.compare(extractGradeNumber(n1), extractGradeNumber(n2));
            });

            for (QueryDocumentSnapshot doc : sortedGrades) {
                String displayName = doc.getString("displayName");
                gradeNames.add(displayName != null ? displayName : doc.getId());
                gradeRefs.add(doc.getReference());
            }

            ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, gradeNames);
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
            spinnerGradeSelect.setAdapter(adapter);

            syncSelectedGradeInSpinner();
        }).addOnFailureListener(e -> Toast.makeText(this, "Error loading grades: " + e.getMessage(), Toast.LENGTH_SHORT).show());
    }

    private void syncSelectedGradeInSpinner() {
        if (currentGradeRef != null && gradeRefs != null && spinnerGradeSelect != null) {
            for (int i = 0; i < gradeRefs.size(); i++) {
                if (gradeRefs.get(i).getId().equals(currentGradeRef.getId())) {
                    spinnerGradeSelect.setSelection(i);
                    break;
                }
            }
        }
    }

    private void loadEnrolledStudents() {
        if (classType == null || layoutClassStudentsList == null) return;
        layoutClassStudentsList.removeAllViews();

        db.collection("users")
                .whereEqualTo("type", 0)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    for (QueryDocumentSnapshot doc : queryDocumentSnapshots) {
                        Map<String, Object> classes = (Map<String, Object>) doc.get("classes");
                        if (classes != null && classes.containsKey(classType)) {
                            Object ref = classes.get(classType);
                            if (ref instanceof DocumentReference && ((DocumentReference) ref).getId().equals(selectedClassId)) {
                                TextView tvStudent = new TextView(this);
                                tvStudent.setText(doc.getString("name") + " (" + doc.getString("email") + ")");
                                tvStudent.setTextSize(16);
                                tvStudent.setPadding(10, 10, 10, 10);
                                tvStudent.setTextColor(Color.BLACK);
                                layoutClassStudentsList.addView(tvStudent);
                            }
                        }
                    }
                });
    }

    private void deleteClass() {
        new AlertDialog.Builder(this)
                .setTitle("Delete Class")
                .setMessage("Are you sure you want to delete this class permanently?")
                .setPositiveButton("Yes, Delete", (dialog, which) -> {
                    classRef.delete().addOnSuccessListener(aVoid -> {
                        Toast.makeText(this, "Class deleted successfully 🗑️", Toast.LENGTH_SHORT).show();
                        finish();
                    }).addOnFailureListener(e -> Toast.makeText(this, "Delete failed: " + e.getMessage(), Toast.LENGTH_SHORT).show());
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    @Override
    protected int getLayoutResourceId() {
        return R.layout.activity_class_detail;
    }

    @Override
    protected int[] getAllowedUserTypes() {
        return new int[]{2, 3};
    }
}