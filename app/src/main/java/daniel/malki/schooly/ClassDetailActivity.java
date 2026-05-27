package daniel.malki.schooly;

import android.app.AlertDialog;
import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
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
    private Button btnAddPairRow, btnOpenAddStudentDialog, btnSaveClassDetails, btnDeleteClass;
    private LinearLayout layoutPairsContainer, layoutClassStudentsList;
    private TextView tvClassDetailTitle;

    private FirebaseFirestore db;
    private String selectedClassId;
    private DocumentReference classRef;
    private DocumentReference schoolRef;
    private DocumentReference currentGradeRef;
    private String classType;

    // Data lists for Spinners
    private ArrayList<String> gradeNames = new ArrayList<>();
    private ArrayList<DocumentReference> gradeRefs = new ArrayList<>();

    private ArrayList<String> subjectNames = new ArrayList<>();
    private ArrayList<DocumentReference> subjectRefs = new ArrayList<>();

    private ArrayList<String> teacherNames = new ArrayList<>();
    private ArrayList<DocumentReference> teacherRefs = new ArrayList<>();

    private List<String> currentStudentIds = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        selectedClassId = getIntent().getStringExtra("classId");
        if (selectedClassId == null || selectedClassId.isEmpty()) {
            Toast.makeText(this, "Error: No class ID provided.", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        db = FirebaseFirestore.getInstance();
        classRef = db.collection("classes").document(selectedClassId);

        initViews();
        loadClassData();
    }

    private void initViews() {
        tvClassDetailTitle = findViewById(R.id.tvClassDetailTitle);
        etClassSchoolLocked = findViewById(R.id.etClassSchoolLocked);
        etClassTypeLocked = findViewById(R.id.etClassTypeLocked);
        etClassName = findViewById(R.id.etClassName);
        spinnerGradeSelect = findViewById(R.id.spinnerGradeSelect);

        layoutPairsContainer = findViewById(R.id.layoutPairsContainer);
        layoutClassStudentsList = findViewById(R.id.layoutClassStudentsList);

        btnAddPairRow = findViewById(R.id.btnAddPairRow);
        btnOpenAddStudentDialog = findViewById(R.id.btnOpenAddStudentDialog);
        btnSaveClassDetails = findViewById(R.id.btnSaveClassDetails);
        btnDeleteClass = findViewById(R.id.btnDeleteClass);

        btnAddPairRow.setOnClickListener(v -> addPairRow(null));
        btnSaveClassDetails.setOnClickListener(v -> saveClassDetails());
        btnDeleteClass.setOnClickListener(v -> confirmDeleteClass());
        btnOpenAddStudentDialog.setOnClickListener(v -> openAddStudentDialog());
    }

    private void loadClassData() {
        classRef.get().addOnSuccessListener(documentSnapshot -> {
            if (documentSnapshot.exists()) {
                SchoolClass schoolClass = documentSnapshot.toObject(SchoolClass.class);
                if (schoolClass == null) return;

                etClassName.setText(schoolClass.getDisplayName());
                tvClassDetailTitle.setText(schoolClass.getDisplayName() + " Details");

                classType = schoolClass.getType();
                etClassTypeLocked.setText(classType != null ? classType : "N/A");

                schoolRef = documentSnapshot.getDocumentReference("school");
                currentGradeRef = schoolClass.getGradeRef();

                if (schoolRef != null) {
                    schoolRef.get().addOnSuccessListener(schoolDoc -> {
                        if (schoolDoc.exists()) {
                            // ✨ תוקן ל-displayName
                            String sName = schoolDoc.getString("displayName") != null ? schoolDoc.getString("displayName") : schoolDoc.getId();
                            etClassSchoolLocked.setText(sName);
                        }
                    });
                }

                loadDropdownData(schoolClass.getCourseAssignments());
                loadEnrolledStudents();
            }
        }).addOnFailureListener(e -> Toast.makeText(this, "Failed to load class data.", Toast.LENGTH_SHORT).show());
    }

    private void loadDropdownData(List<SchoolClass.CourseAssignment> existingAssignments) {
        if (schoolRef == null) return;

        db.collection("grades").whereEqualTo("school", schoolRef).get().addOnSuccessListener(gradesSnap -> {
            gradeNames.clear(); gradeRefs.clear();
            int selectedGradeIndex = 0;

            for (QueryDocumentSnapshot g : gradesSnap) {
                // ✨ תוקן ל-displayName
                String gName = g.getString("displayName") != null ? g.getString("displayName") : g.getId();
                gradeNames.add(gName);
                gradeRefs.add(g.getReference());
                if (currentGradeRef != null && g.getId().equals(currentGradeRef.getId())) {
                    selectedGradeIndex = gradeNames.size() - 1;
                }
            }

            ArrayAdapter<String> gradeAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, gradeNames);
            gradeAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
            spinnerGradeSelect.setAdapter(gradeAdapter);
            if (gradeNames.size() > 0) {
                spinnerGradeSelect.setSelection(selectedGradeIndex);
            }

            db.collection("subjects").get().addOnSuccessListener(subjSnap -> {
                subjectNames.clear(); subjectRefs.clear();
                subjectNames.add("Select Subject..."); subjectRefs.add(null);

                for (QueryDocumentSnapshot s : subjSnap) {
                    // ✨ תוקן ל-displayName
                    String subjName = s.getString("displayName") != null ? s.getString("displayName") : s.getId();
                    subjectNames.add(subjName);
                    subjectRefs.add(s.getReference());
                }

                db.collection("users").whereIn("type", java.util.Arrays.asList(1, 2)).whereEqualTo("school", schoolRef).get().addOnSuccessListener(teachSnap -> {                    teacherNames.clear(); teacherRefs.clear();
                    teacherNames.add("Select Teacher..."); teacherRefs.add(null);

                    for (QueryDocumentSnapshot t : teachSnap) {
                        String fName = t.getString("firstName") != null ? t.getString("firstName") : "";
                        String lName = t.getString("lastName") != null ? t.getString("lastName") : "";
                        String fullName = t.getString("name") != null ? t.getString("name") : (fName + " " + lName).trim();
                        teacherNames.add(fullName);
                        teacherRefs.add(t.getReference());
                    }

                    layoutPairsContainer.removeAllViews();
                    if (existingAssignments != null && !existingAssignments.isEmpty()) {
                        for (SchoolClass.CourseAssignment assignment : existingAssignments) {
                            addPairRow(assignment);
                        }
                    } else {
                        addPairRow(null);
                    }
                });
            });
        });
    }

    private void addPairRow(SchoolClass.CourseAssignment assignment) {
        View rowView = LayoutInflater.from(this).inflate(R.layout.item_class_assignment_row, layoutPairsContainer, false);

        Spinner spinnerSubj = rowView.findViewById(R.id.spinnerRowSubject);
        Spinner spinnerTeach = rowView.findViewById(R.id.spinnerRowTeacher);
        ImageButton btnRemove = rowView.findViewById(R.id.btnRemoveRow);

        ArrayAdapter<String> subjAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, subjectNames);
        subjAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerSubj.setAdapter(subjAdapter);

        ArrayAdapter<String> teachAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, teacherNames);
        teachAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerTeach.setAdapter(teachAdapter);

        // בחירת המקצוע והמורה השמורים (השוואה חסינה באמצעות Path)
        if (assignment != null) {
            if (assignment.getSubject() != null) {
                for (int i = 0; i < subjectRefs.size(); i++) {
                    if (subjectRefs.get(i) != null &&
                            subjectRefs.get(i).getPath().equals(assignment.getSubject().getPath())) {
                        spinnerSubj.setSelection(i);
                        break;
                    }
                }
            }
            if (assignment.getTeacher() != null) {
                for (int i = 0; i < teacherRefs.size(); i++) {
                    if (teacherRefs.get(i) != null &&
                            teacherRefs.get(i).getPath().equals(assignment.getTeacher().getPath())) {
                        spinnerTeach.setSelection(i);
                        break;
                    }
                }
            }
        }

        btnRemove.setOnClickListener(v -> layoutPairsContainer.removeView(rowView));
        layoutPairsContainer.addView(rowView);
    }

    private void saveClassDetails() {
        String newName = etClassName.getText().toString().trim();
        if (newName.isEmpty()) {
            Toast.makeText(this, "Class name cannot be empty.", Toast.LENGTH_SHORT).show();
            return;
        }

        DocumentReference selectedGrade = null;
        if (spinnerGradeSelect.getSelectedItemPosition() >= 0 && gradeRefs.size() > 0) {
            selectedGrade = gradeRefs.get(spinnerGradeSelect.getSelectedItemPosition());
        }

        List<SchoolClass.CourseAssignment> assignmentsToSave = new ArrayList<>();

        for (int i = 0; i < layoutPairsContainer.getChildCount(); i++) {
            View row = layoutPairsContainer.getChildAt(i);
            Spinner spinnerSubj = row.findViewById(R.id.spinnerRowSubject);
            Spinner spinnerTeach = row.findViewById(R.id.spinnerRowTeacher);

            int subjPos = spinnerSubj.getSelectedItemPosition();
            int teachPos = spinnerTeach.getSelectedItemPosition();

            if (subjPos > 0 && teachPos > 0) {
                DocumentReference subjRef = subjectRefs.get(subjPos);
                DocumentReference teachRef = teacherRefs.get(teachPos);
                assignmentsToSave.add(new SchoolClass.CourseAssignment(subjRef, teachRef));
            }
        }

        Map<String, Object> updates = new HashMap<>();
        updates.put("displayName", newName);
        updates.put("gradeRef", selectedGrade);
        // ✨ התיקון לשמירה בפיירבייס!
        updates.put("courseAssignments", assignmentsToSave);

        classRef.update(updates).addOnSuccessListener(aVoid -> {
            Toast.makeText(this, "Class updated successfully! ✅", Toast.LENGTH_SHORT).show();
            finish();
        }).addOnFailureListener(e -> Toast.makeText(this, "Failed to update class.", Toast.LENGTH_SHORT).show());
    }

    private void loadEnrolledStudents() {
        if (classType == null || classType.isEmpty()) return;

        db.collection("users")
                .whereEqualTo("type", 0)
                .whereEqualTo("classes." + classType, classRef)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    layoutClassStudentsList.removeAllViews();
                    currentStudentIds.clear();

                    if (queryDocumentSnapshots.isEmpty()) {
                        TextView tvEmpty = new TextView(this);
                        tvEmpty.setText("No students enrolled yet.");
                        tvEmpty.setPadding(0, 10, 0, 10);
                        layoutClassStudentsList.addView(tvEmpty);
                        return;
                    }

                    for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                        String studentId = document.getId();
                        currentStudentIds.add(studentId);
                        String firstName = document.getString("firstName") != null ? document.getString("firstName") : "";
                        String lastName = document.getString("lastName") != null ? document.getString("lastName") : "";

                        TextView tvStudent = new TextView(this);
                        tvStudent.setText("• " + firstName + " " + lastName);
                        tvStudent.setTextSize(16f);
                        tvStudent.setTextColor(Color.parseColor("#333333"));
                        tvStudent.setPadding(0, 8, 0, 8);
                        layoutClassStudentsList.addView(tvStudent);
                    }
                });
    }

    private void confirmDeleteClass() {
        new AlertDialog.Builder(this)
                .setTitle("Delete Class")
                .setMessage("Are you sure you want to completely delete this class? This action cannot be undone.")
                .setPositiveButton("Delete", (dialog, which) -> deleteClassFromDatabase())
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void deleteClassFromDatabase() {
        com.google.firebase.firestore.WriteBatch batch = db.batch();
        batch.delete(classRef);

        for (String studentId : currentStudentIds) {
            DocumentReference studentRef = db.collection("users").document(studentId);
            batch.update(studentRef, "classes." + classType, null);
        }

        batch.commit().addOnSuccessListener(aVoid -> {
            Toast.makeText(this, "Class deleted! 🗑️", Toast.LENGTH_SHORT).show();
            finish();
        }).addOnFailureListener(e -> Toast.makeText(this, "Failed to delete class.", Toast.LENGTH_SHORT).show());
    }

    private void openAddStudentDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Add Student to Class");

        final EditText input = new EditText(this);
        input.setHint("Enter Student Email");
        builder.setView(input);

        builder.setPositiveButton("Add", (dialog, which) -> {
            String email = input.getText().toString().trim();
            if (!email.isEmpty()) {
                addStudentToClass(email);
            }
        });
        builder.setNegativeButton("Cancel", (dialog, which) -> dialog.cancel());
        builder.show();
    }

    private void addStudentToClass(String email) {
        db.collection("users")
                .whereEqualTo("email", email)
                .whereEqualTo("type", 0)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    if (!queryDocumentSnapshots.isEmpty()) {
                        DocumentReference studentRef = queryDocumentSnapshots.getDocuments().get(0).getReference();
                        studentRef.update("classes." + classType, classRef)
                                .addOnSuccessListener(aVoid -> {
                                    Toast.makeText(this, "Student added! ✅", Toast.LENGTH_SHORT).show();
                                    loadEnrolledStudents();
                                });
                    } else {
                        Toast.makeText(this, "Student not found.", Toast.LENGTH_SHORT).show();
                    }
                });
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