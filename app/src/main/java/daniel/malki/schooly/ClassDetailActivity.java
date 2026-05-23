package daniel.malki.schooly;

import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;

import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ClassDetailActivity extends BaseMenuActivity {

    private EditText etClassSchoolLocked, etClassTypeLocked, etClassName;
    private Spinner spinnerClassTeacher;
    // ✨ הוספנו את כפתור המחיקה לכאן
    private Button btnOpenAddStudentDialog, btnSaveClassDetails, btnDeleteClass;
    private LinearLayout layoutClassStudentsList;
    private TextView tvClassDetailTitle, tvTeacherLabel;

    private FirebaseFirestore db;
    private String selectedClassId;
    private DocumentReference classRef;
    private DocumentReference schoolRef;
    private String classType;

    private ArrayList<String> teacherNames = new ArrayList<>();
    private ArrayList<String> teacherIds = new ArrayList<>();
    private String currentTeacherId = "";

    private List<String> currentStudentIds = new ArrayList<>();

    @Override
    protected int getLayoutResourceId() { return R.layout.activity_class_detail; }

    @Override
    protected int[] getAllowedUserTypes() { return new int[]{2, 3}; }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        db = FirebaseFirestore.getInstance();

        tvClassDetailTitle = findViewById(R.id.tvClassDetailTitle);
        etClassSchoolLocked = findViewById(R.id.etClassSchoolLocked);
        etClassTypeLocked = findViewById(R.id.etClassTypeLocked);
        etClassName = findViewById(R.id.etClassName);
        spinnerClassTeacher = findViewById(R.id.spinnerClassTeacher);
        btnOpenAddStudentDialog = findViewById(R.id.btnOpenAddStudentDialog);
        btnSaveClassDetails = findViewById(R.id.btnSaveClassDetails);
        btnDeleteClass = findViewById(R.id.btnDeleteClass); // ✨ קישור לכפתור המחיקה מה-XML
        layoutClassStudentsList = findViewById(R.id.layoutClassStudentsList);
        tvTeacherLabel = findViewById(R.id.tvTeacherLabel);

        selectedClassId = getIntent().getStringExtra("classId");
        if (selectedClassId == null) {
            Toast.makeText(this, "Error: Class ID missing", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        classRef = db.collection("classes").document(selectedClassId);

        loadClassDetails();

        btnSaveClassDetails.setOnClickListener(v -> saveClassChanges());
        btnOpenAddStudentDialog.setOnClickListener(v -> showAvailableStudentsDialog());

        // ✨ מאזין ללחיצה על כפתור המחיקה
        btnDeleteClass.setOnClickListener(v -> showDeleteConfirmationDialog());
    }

    private void loadClassDetails() {
        classRef.get().addOnSuccessListener(doc -> {
            if (doc.exists()) {
                String className = doc.contains("displayName") ? doc.getString("displayName") : doc.getString("name");
                classType = doc.getString("type");
                schoolRef = doc.getDocumentReference("school");
                DocumentReference teacherRef = doc.getDocumentReference("teacher");

                etClassName.setText(className);
                etClassTypeLocked.setText(classType != null ? classType.toUpperCase() : "Unknown");
                tvClassDetailTitle.setText("Manage: " + className);

                if ("homeroom".equals(classType)) {
                    tvTeacherLabel.setText("Assigned Homeroom Teacher (מחנך/ת) *");
                } else {
                    tvTeacherLabel.setText("Assigned Professional Teacher (מורה מקצועי/ת) *");
                }

                if (teacherRef != null) {
                    currentTeacherId = teacherRef.getId();
                }

                if (schoolRef != null) {
                    schoolRef.get().addOnSuccessListener(schoolDoc -> {
                        if (schoolDoc.exists()) {
                            etClassSchoolLocked.setText(schoolDoc.getString("displayName"));
                        }
                    });
                    loadEligibleTeachers();
                }
                loadEnrolledStudents();
            }
        });
    }

    private void loadEligibleTeachers() {
        db.collection("users")
                .whereEqualTo("school", schoolRef)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    teacherNames.clear();
                    teacherIds.clear();

                    teacherNames.add("No Teacher Assigned");
                    teacherIds.add("");

                    int selectedIndex = 0;

                    for (QueryDocumentSnapshot doc : queryDocumentSnapshots) {
                        Long typeLong = doc.getLong("type");
                        if (typeLong != null && (typeLong == 1 || typeLong == 2)) {
                            String id = doc.getId();
                            String name = doc.getString("name");
                            if (name == null) name = id;

                            teacherIds.add(id);
                            teacherNames.add(name);

                            if (id.equals(currentTeacherId)) {
                                selectedIndex = teacherIds.size() - 1;
                            }
                        }
                    }

                    ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, teacherNames);
                    spinnerClassTeacher.setAdapter(adapter);
                    spinnerClassTeacher.setSelection(selectedIndex);
                });
    }

    private void loadEnrolledStudents() {
        db.collection("users")
                .whereEqualTo("type", 0)
                .whereEqualTo("school", schoolRef)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    layoutClassStudentsList.removeAllViews();
                    currentStudentIds.clear();

                    for (QueryDocumentSnapshot doc : queryDocumentSnapshots) {
                        Map<String, Object> classesMap = (Map<String, Object>) doc.get("classes");
                        if (classesMap != null && classesMap.containsKey(classType)) {
                            DocumentReference boundClassRef = (DocumentReference) classesMap.get(classType);
                            if (boundClassRef != null && boundClassRef.getId().equals(selectedClassId)) {

                                String studentId = doc.getId();
                                String studentName = doc.getString("name");
                                currentStudentIds.add(studentId);

                                addStudentRowToUi(studentId, studentName != null ? studentName : studentId);
                            }
                        }
                    }

                    if (currentStudentIds.isEmpty()) {
                        TextView tvEmpty = new TextView(this);
                        tvEmpty.setText("No students enrolled in this group yet.");
                        tvEmpty.setTextColor(Color.GRAY);
                        tvEmpty.setPadding(0, 10, 0, 10);
                        layoutClassStudentsList.addView(tvEmpty);
                    }
                });
    }

    private void addStudentRowToUi(String studentId, String studentName) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setPadding(0, 8, 0, 8);
        row.setGravity(android.view.Gravity.CENTER_VERTICAL);

        TextView tvName = new TextView(this);
        tvName.setText("• " + studentName + " (" + studentId + ")");
        tvName.setTextSize(16f);
        tvName.setTextColor(Color.BLACK);

        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f);
        tvName.setLayoutParams(params);

        Button btnRemove = new Button(this);
        btnRemove.setText("Remove");
        btnRemove.setBackgroundTintList(android.content.res.ColorStateList.valueOf(Color.parseColor("#D32F2F")));
        btnRemove.setTextColor(Color.WHITE);
        btnRemove.setTextSize(11);

        btnRemove.setOnClickListener(v -> removeStudentFromClass(studentId, studentName));

        row.addView(tvName);
        row.addView(btnRemove);
        layoutClassStudentsList.addView(row);
    }

    private void removeStudentFromClass(String studentId, String studentName) {
        new AlertDialog.Builder(this)
                .setTitle("Remove Student 🛑")
                .setMessage("Are you sure you want to remove " + studentName + " from this class?\nTheir schedule for this slot will become a free window (null).")
                .setPositiveButton("Remove", (dialog, which) -> {
                    db.collection("users").document(studentId)
                            .update("classes." + classType, null)
                            .addOnSuccessListener(aVoid -> {
                                Toast.makeText(this, studentName + " removed successfully.", Toast.LENGTH_SHORT).show();
                                loadEnrolledStudents();
                            });
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void showAvailableStudentsDialog() {
        db.collection("users")
                .whereEqualTo("type", 0)
                .whereEqualTo("school", schoolRef)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    ArrayList<String> availableNames = new ArrayList<>();
                    ArrayList<String> availableIds = new ArrayList<>();

                    for (QueryDocumentSnapshot doc : queryDocumentSnapshots) {
                        String studentId = doc.getId();

                        if (currentStudentIds.contains(studentId)) continue;

                        Map<String, Object> classesMap = (Map<String, Object>) doc.get("classes");

                        boolean isFree = false;
                        if (classesMap == null) {
                            isFree = true;
                        } else {
                            if (!classesMap.containsKey(classType) || classesMap.get(classType) == null) {
                                isFree = true;
                            }
                        }

                        if (isFree) {
                            String name = doc.getString("name");
                            availableIds.add(studentId);
                            availableNames.add((name != null ? name : studentId) + " (" + studentId + ")");
                        }
                    }

                    if (availableNames.isEmpty()) {
                        Toast.makeText(this, "No available students with a free window for this subject!", Toast.LENGTH_LONG).show();
                        return;
                    }

                    String[] items = availableNames.toArray(new String[0]);
                    new AlertDialog.Builder(this)
                            .setTitle("Select Student to Add")
                            .setItems(items, (dialog, which) -> {
                                String chosenId = availableIds.get(which);
                                String chosenName = availableNames.get(which);
                                addStudentToClass(chosenId, chosenName);
                            })
                            .setNegativeButton("Cancel", null)
                            .show();
                });
    }

    private void addStudentToClass(String studentId, String studentName) {
        db.collection("users").document(studentId)
                .update("classes." + classType, classRef)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(this, "Added successfully!", Toast.LENGTH_SHORT).show();
                    loadEnrolledStudents();
                });
    }

    private void saveClassChanges() {
        String newName = etClassName.getText().toString().trim();
        if (newName.isEmpty()) {
            Toast.makeText(this, "Class name cannot be empty!", Toast.LENGTH_SHORT).show();
            return;
        }

        int teacherPos = spinnerClassTeacher.getSelectedItemPosition();
        DocumentReference newTeacherRef = null;
        if (teacherPos > 0) {
            newTeacherRef = db.collection("users").document(teacherIds.get(teacherPos));
        }

        Map<String, Object> updates = new HashMap<>();
        updates.put("displayName", newName);
        updates.put("name", newName);
        updates.put("teacher", newTeacherRef);

        classRef.update(updates).addOnSuccessListener(aVoid -> {
            Toast.makeText(this, "Class details updated successfully! 💾", Toast.LENGTH_SHORT).show();
            finish();
        }).addOnFailureListener(e -> Toast.makeText(this, "Failed to update: " + e.getMessage(), Toast.LENGTH_SHORT).show());
    }

    // ✨ הפונקציה החדשה שמקפיצה אזהרה לפני מחיקה
    private void showDeleteConfirmationDialog() {
        new AlertDialog.Builder(this)
                .setTitle("Delete Class ⚠️")
                .setMessage("Are you sure you want to completely delete this class? This action cannot be undone.")
                .setPositiveButton("Delete", (dialog, which) -> deleteClassFromDatabase())
                .setNegativeButton("Cancel", null)
                .show();
    }

    // ✨ הפונקציה שמוחקת בפועל מ-Firestore
    // ✨ הפונקציה שמוחקת בפועל מ-Firestore כולל ניקוי המצביעים אצל התלמידים
    // ✨ הפונקציה שמוחקת בפועל מ-Firestore ומשנה את המצביעים ל-null
    private void deleteClassFromDatabase() {
        // פתיחת Batch כדי לבצע את כל המחיקות יחד כפעולה אחת
        com.google.firebase.firestore.WriteBatch batch = db.batch();

        // 1. הוספת פקודה למחיקת הכיתה עצמה
        batch.delete(classRef);

        // 2. מעבר על כל התלמידים שבכיתה והוספת פקודה לאיפוס המצביע שלהם
        for (String studentId : currentStudentIds) {
            DocumentReference studentRef = db.collection("users").document(studentId);
            // כאן השינוי: אנחנו מעדכנים את השדה ל-null במקום למחוק אותו
            batch.update(studentRef, "classes." + classType, null);
        }

        // 3. שיגור כל הפעולות למסד הנתונים
        batch.commit().addOnSuccessListener(aVoid -> {
            Toast.makeText(this, "Class deleted and references set to null! 🗑️", Toast.LENGTH_SHORT).show();
            finish(); // סוגר את המסך וחוזר לרשימה
        }).addOnFailureListener(e -> {
            Toast.makeText(this, "Error deleting class: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        });
    }
}