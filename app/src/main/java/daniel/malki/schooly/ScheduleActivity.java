package daniel.malki.schooly;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.tabs.TabLayout;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ScheduleActivity extends BaseMenuActivity implements TimetableAdapter.OnSlotClickListener {

    private TextInputLayout layoutScheduleSchool, layoutScheduleGrade;
    private AutoCompleteTextView autoSchool, autoType, autoGrade;
    private TabLayout tabLayoutDays;
    private RecyclerView rvTimetable;

    private TimetableAdapter adapter;
    private FirebaseFirestore db;

    private int userType;
    private String selectedDay = "Sunday";
    private String selectedGradeName = "";

    private List<com.google.firebase.firestore.DocumentSnapshot> schoolDocuments = new ArrayList<>();
    private String selectedSchoolId = "";

    // מפה גלובלית ששומרת מזהה שכבה מול השם שלה (למשל: "12th" -> "12th Grade")
    private Map<String, String> gradeMap = new HashMap<>();

    private final String[] daysEng = {"Sunday", "Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday"};
    private final String[] daysHeb = {"Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat"};

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        db = FirebaseFirestore.getInstance();

        SharedPreferences prefs = getSharedPreferences("SchoolyPrefs", MODE_PRIVATE);
        userType = prefs.getInt("userType", 0);

        initViews();
        setupDaysTabs();
        setupDropdowns();
        setupRecyclerView();

        if (userType != 3) {
            loadTimetableSlots();
        } else {
            adapter.setSlots(new ArrayList<>(), true);
        }
    }

    @Override
    protected int getLayoutResourceId() {
        return R.layout.activity_schedule;
    }

    private void initViews() {
        layoutScheduleSchool = findViewById(R.id.layoutScheduleSchool);
        layoutScheduleGrade = findViewById(R.id.layoutScheduleGrade);
        autoSchool = findViewById(R.id.autoCompleteScheduleSchool);
        autoType = findViewById(R.id.autoCompleteScheduleType);
        autoGrade = findViewById(R.id.autoCompleteScheduleGrade);
        tabLayoutDays = findViewById(R.id.tabLayoutDays);
        rvTimetable = findViewById(R.id.rvTimetable);

        if (userType == 3 && layoutScheduleSchool != null) {
            layoutScheduleSchool.setVisibility(View.VISIBLE);
        }
    }

    private void setupDaysTabs() {
        for (String day : daysHeb) {
            tabLayoutDays.addTab(tabLayoutDays.newTab().setText(day));
        }

        tabLayoutDays.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                selectedDay = daysEng[tab.getPosition()];
                loadTimetableSlots();
            }
            @Override
            public void onTabUnselected(TabLayout.Tab tab) {}
            @Override
            public void onTabReselected(TabLayout.Tab tab) {}
        });
    }

    private int extractGradeNumber(String name) {
        if (name == null) return 9999;
        String numStr = name.replaceAll("\\D+", "");
        if (numStr.isEmpty()) return 9999;
        return Integer.parseInt(numStr);
    }

    private void setupDropdowns() {
        ArrayList<String> scheduleTypes = new ArrayList<>();
        scheduleTypes.add("Full Timetable");

        // רק אם המשתמש הוא לא מנהל סקולי (3) נוסיף לו מערכת אישית
        if (userType != 3) {
            scheduleTypes.add("Personal Timetable");
        }

        ArrayAdapter<String> typeAdapter = new ArrayAdapter<>(this, android.R.layout.simple_dropdown_item_1line, scheduleTypes);
        autoType.setAdapter(typeAdapter);
        autoType.setText(scheduleTypes.get(0), false);

        autoType.setOnItemClickListener((parent, view, position, id) -> {
            String selectedType = scheduleTypes.get(position);
            // מראים את ספינר השכבות רק במערכת מלאה
            if (selectedType.equals("Full Timetable")) {
                layoutScheduleGrade.setVisibility(View.VISIBLE);
            } else {
                layoutScheduleGrade.setVisibility(View.INVISIBLE);
                selectedGradeName = "";
            }
            if (userType != 3 || !selectedSchoolId.isEmpty()) {
                loadTimetableSlots();
            }
        });

        // שליפת השכבות ומיון מספרי חכם
        db.collection("grades").get().addOnSuccessListener(queryDocumentSnapshots -> {
            gradeMap.clear();
            List<QueryDocumentSnapshot> sortedGrades = new ArrayList<>();
            for (QueryDocumentSnapshot doc : queryDocumentSnapshots) {
                sortedGrades.add(doc);
            }

            Collections.sort(sortedGrades, (d1, d2) -> {
                String n1 = d1.getString("displayName");
                if (n1 == null) n1 = d1.getId();
                String n2 = d2.getString("displayName");
                if (n2 == null) n2 = d2.getId();

                int num1 = extractGradeNumber(n1);
                int num2 = extractGradeNumber(n2);

                if (num1 != num2) {
                    return Integer.compare(num1, num2);
                }
                return n1.compareTo(n2);
            });

            List<String> realGradesList = new ArrayList<>();

            // מוסיפים את "כל השכבות" כאופציה הראשונה ברשימה
            realGradesList.add("All Grades");

            for (QueryDocumentSnapshot doc : sortedGrades) {
                String displayName = doc.getString("displayName");
                if (displayName == null) displayName = doc.getId();

                realGradesList.add(displayName);
                gradeMap.put(doc.getId(), displayName);
            }

            ArrayAdapter<String> gradeAdapter = new ArrayAdapter<>(this, android.R.layout.simple_dropdown_item_1line, realGradesList);
            autoGrade.setAdapter(gradeAdapter);

            // מגדירים את "כל השכבות" כברירת המחדל
            autoGrade.setText(realGradesList.get(0), false);
            selectedGradeName = "All Grades";

            autoGrade.setOnItemClickListener((parent2, view2, position2, id2) -> {
                selectedGradeName = realGradesList.get(position2);
                if (userType != 3 || !selectedSchoolId.isEmpty()) {
                    loadTimetableSlots();
                }
            });
        });

        if (userType == 3) {
            db.collection("schools").get().addOnSuccessListener(queryDocumentSnapshots -> {
                schoolDocuments = queryDocumentSnapshots.getDocuments();
                List<String> schoolNames = new ArrayList<>();

                for (com.google.firebase.firestore.DocumentSnapshot doc : schoolDocuments) {
                    String name = doc.getString("displayName");
                    if (name == null) name = doc.getString("name");
                    if (name == null) name = doc.getId();
                    schoolNames.add(name);
                }

                ArrayAdapter<String> schoolAdapter = new ArrayAdapter<>(this, android.R.layout.simple_dropdown_item_1line, schoolNames);
                autoSchool.setAdapter(schoolAdapter);
            });

            autoSchool.setOnItemClickListener((parent, view, position, id) -> {
                if (position < schoolDocuments.size()) {
                    selectedSchoolId = schoolDocuments.get(position).getId();
                    loadTimetableSlots();
                }
            });
        }
    }

    private void setupRecyclerView() {
        if (rvTimetable != null) {
            rvTimetable.setLayoutManager(new LinearLayoutManager(this));
            adapter = new TimetableAdapter(this, userType, this);
            rvTimetable.setAdapter(adapter);
        }
    }

    private void loadTimetableSlots() {
        if (userType == 3 && selectedSchoolId.isEmpty()) {
            adapter.setSlots(new ArrayList<>(), autoType.getText().toString().equals("Personal Timetable"));
            return;
        }

        com.google.firebase.firestore.Query query = db.collection("timetableSlots")
                .whereEqualTo("day", selectedDay);

        if (userType == 3 && !selectedSchoolId.isEmpty()) {
            DocumentReference schoolRef = db.collection("schools").document(selectedSchoolId);
            query = query.whereEqualTo("school", schoolRef);
        }

        query.get().addOnSuccessListener(queryDocumentSnapshots -> {
            List<TimetableSlot> allSlots = new ArrayList<>();
            for (QueryDocumentSnapshot doc : queryDocumentSnapshots) {
                TimetableSlot slot = doc.toObject(TimetableSlot.class);
                slot.setSlotId(doc.getId());
                allSlots.add(slot);
            }

            boolean isPersonal = autoType.getText().toString().equals("Personal Timetable");

            // --- סינון מערכת אישית לתלמיד ---
            if (isPersonal && userType == 0) {
                SharedPreferences prefs = getSharedPreferences("SchoolyPrefs", MODE_PRIVATE);
                String currentUserId = prefs.getString("tz", prefs.getString("userId", ""));

                if (!currentUserId.isEmpty()) {
                    db.collection("users").document(currentUserId).get().addOnSuccessListener(userDoc -> {
                        List<TimetableSlot> personalSlots = new ArrayList<>();

                        Object classesObj = userDoc.get("classes");
                        if (classesObj instanceof Map) {
                            Map<String, Object> userClassesMap = (Map<String, Object>) classesObj;
                            List<DocumentReference> studentClassRefs = new ArrayList<>();

                            for (Object val : userClassesMap.values()) {
                                if (val instanceof DocumentReference) {
                                    studentClassRefs.add((DocumentReference) val);
                                }
                            }

                            for (TimetableSlot slot : allSlots) {
                                if (slot.getClassRef() != null && studentClassRefs.contains(slot.getClassRef())) {
                                    personalSlots.add(slot);
                                }
                            }
                        }
                        adapter.setSlots(personalSlots, true);
                    }).addOnFailureListener(e -> adapter.setSlots(new ArrayList<>(), true));
                    return;
                }
            }
            // --- סינון מערכת אישית למורה ---
            else if (isPersonal && userType == 1) {
                SharedPreferences prefs = getSharedPreferences("SchoolyPrefs", MODE_PRIVATE);
                String currentUserId = prefs.getString("tz", prefs.getString("userId", ""));

                List<TimetableSlot> personalSlots = new ArrayList<>();
                for (TimetableSlot slot : allSlots) {
                    if (slot.getTeacherRef() != null && slot.getTeacherRef().getId().equals(currentUserId)) {
                        personalSlots.add(slot);
                    }
                }
                adapter.setSlots(personalSlots, true);
                return;
            }

            // --- במקרה של מבט מלא (מנהל/כל השכבה) ---
            List<TimetableSlot> slotsList = new ArrayList<>();
            for (TimetableSlot slot : allSlots) {
                if (!isPersonal) {
                    // התיקון החכם: אם נבחר "כל השכבות", מכניסים את הכל!
                    if (selectedGradeName.equals("All Grades") || selectedGradeName.isEmpty()) {
                        slotsList.add(slot);
                    }
                    // אחרת, מסננים לפי השכבה שנבחרה
                    else if (slot.getDisplayName() != null && slot.getDisplayName().contains(selectedGradeName)) {
                        slotsList.add(slot);
                    }
                } else {
                    slotsList.add(slot);
                }
            }
            adapter.setSlots(slotsList, isPersonal);
        });
    }

    @Override
    public void onAddSlotClick(int hour) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Add Lesson to Hour " + hour);

        View dialogView = getLayoutInflater().inflate(R.layout.dialog_add_slot, null);
        AutoCompleteTextView spinClass = dialogView.findViewById(R.id.autoDialogClass);
        AutoCompleteTextView spinSubject = dialogView.findViewById(R.id.autoDialogSubject);

        final List<com.google.firebase.firestore.DocumentSnapshot> classDocuments = new ArrayList<>();
        final List<String> formattedClassNames = new ArrayList<>();
        final List<String> subjectList = new ArrayList<>();

        final Map<String, String> subjectToTeacherMap = new HashMap<>();

        if (userType == 3 && !selectedSchoolId.isEmpty()) {
            DocumentReference schoolRef = db.collection("schools").document(selectedSchoolId);
            db.collection("classes")
                    .whereEqualTo("school", schoolRef)
                    .get()
                    .addOnSuccessListener(snapshots -> {
                        classDocuments.clear();
                        formattedClassNames.clear();

                        for (QueryDocumentSnapshot doc : snapshots) {
                            classDocuments.add(doc);

                            String rawType = doc.getString("type");
                            String className = doc.getString("displayName");
                            if (className == null) className = doc.getString("name");
                            if (className == null) className = "Unknown Class";

                            String capType = (rawType != null && !rawType.isEmpty()) ?
                                    rawType.substring(0, 1).toUpperCase() + rawType.substring(1) : "Unknown Type";

                            String gradeId = "";
                            Object gradeObj = doc.get("grade");
                            if (gradeObj == null) gradeObj = doc.get("gradeRef");

                            if (gradeObj instanceof DocumentReference) {
                                gradeId = ((DocumentReference) gradeObj).getId();
                            } else if (gradeObj instanceof String) {
                                String gStr = (String) gradeObj;
                                gradeId = gStr.substring(gStr.lastIndexOf("/") + 1);
                            }

                            String gradeName = gradeMap.containsKey(gradeId) ? gradeMap.get(gradeId) : "Unknown Grade";

                            String formattedName = capType + ", " + gradeName + " " + className;
                            formattedClassNames.add(formattedName);
                        }

                        if (formattedClassNames.isEmpty()) formattedClassNames.add("No classes found");
                        if (spinClass != null) {
                            spinClass.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_dropdown_item_1line, formattedClassNames));
                        }
                    });
        }

        if (spinClass != null) {
            spinClass.setOnItemClickListener((parent, view, position, id) -> {
                subjectList.clear();
                subjectToTeacherMap.clear();
                if (spinSubject != null) spinSubject.setText("");

                if (position < classDocuments.size()) {
                    com.google.firebase.firestore.DocumentSnapshot selectedClassDoc = classDocuments.get(position);
                    List<Map<String, Object>> assignments = (List<Map<String, Object>>) selectedClassDoc.get("courseAssignments");

                    if (assignments != null && !assignments.isEmpty()) {
                        for (Map<String, Object> assignment : assignments) {
                            DocumentReference subRef = (DocumentReference) assignment.get("subject");
                            DocumentReference teaRef = (DocumentReference) assignment.get("teacher");

                            if (subRef != null) {
                                String subName = subRef.getId();
                                if (!subjectList.contains(subName)) {
                                    subjectList.add(subName);
                                }
                                if (teaRef != null) {
                                    subjectToTeacherMap.put(subName, teaRef.getId());
                                }
                            }
                        }
                    }

                    if (subjectList.isEmpty()) subjectList.add("No subjects for this class");
                    if (spinSubject != null) {
                        spinSubject.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_dropdown_item_1line, subjectList));
                    }
                }
            });
        }

        builder.setView(dialogView);

        // כאן נמצא התיקון ששולף את שם המורה מהדאטה-בייס לפני השמירה
        builder.setPositiveButton("Save", (dialog, which) -> {
            if (spinClass == null || spinSubject == null) return;

            int selectedPos = formattedClassNames.indexOf(spinClass.getText().toString());
            String sub = spinSubject.getText().toString();

            if (selectedPos == -1 || sub.isEmpty() || sub.contains("No")) {
                Toast.makeText(this, "Please select valid choices", Toast.LENGTH_SHORT).show();
                return;
            }

            com.google.firebase.firestore.DocumentSnapshot classDoc = classDocuments.get(selectedPos);
            String rawType = classDoc.getString("type");

            // -------- התיקון כאן --------
            String tempClassName = classDoc.getString("displayName");
            if (tempClassName == null) tempClassName = classDoc.getString("name");
            final String finalClassName = tempClassName; // משתנה סופי ובטוח לשימוש בלמבדה!
            // ----------------------------

            String gradeId = "";
            Object gradeObj = classDoc.get("grade");
            if (gradeObj == null) gradeObj = classDoc.get("gradeRef");
            if (gradeObj instanceof DocumentReference) {
                gradeId = ((DocumentReference) gradeObj).getId();
            } else if (gradeObj instanceof String) {
                String gStr = (String) gradeObj;
                gradeId = gStr.substring(gStr.lastIndexOf("/") + 1);
            }
            String finalGradeName = gradeMap.containsKey(gradeId) ? gradeMap.get(gradeId) : "Unknown Grade";
            String finalGradeId = gradeId;

            String autoTeacherId = subjectToTeacherMap.containsKey(sub) ? subjectToTeacherMap.get(sub) : "Unknown Teacher";
            String capType = (rawType != null && !rawType.isEmpty()) ? rawType.substring(0, 1).toUpperCase() + rawType.substring(1) : "Unknown Type";

            TimetableSlot newSlot = new TimetableSlot();
            newSlot.setDay(selectedDay);
            newSlot.setHour(hour);

            DocumentReference schoolRef = db.collection("schools").document(selectedSchoolId);
            newSlot.setSchool(schoolRef);
            newSlot.setGradeRef(db.collection("grades").document(finalGradeId));
            newSlot.setClassRef(classDoc.getReference());
            newSlot.setSubjectRef(db.collection("subjects").document(sub));

            if (!autoTeacherId.equals("Unknown Teacher")) {
                newSlot.setTeacherRef(db.collection("users").document(autoTeacherId));

                db.collection("users").document(autoTeacherId).get().addOnSuccessListener(userDoc -> {
                    String teacherName = userDoc.getString("name");
                    if (teacherName == null || teacherName.isEmpty()) teacherName = "Unknown Teacher";

                    // שימוש במשתנה החדש finalClassName
                    String finalDisplayName = sub.toUpperCase() + ", " + finalGradeName + " " + finalClassName + ", Teacher: " + teacherName + ", (" + capType + ")";
                    newSlot.setDisplayName(finalDisplayName);

                    db.collection("timetableSlots").add(newSlot).addOnSuccessListener(docRef -> {
                        Toast.makeText(this, "Lesson created successfully!", Toast.LENGTH_SHORT).show();
                        loadTimetableSlots();
                    });
                }).addOnFailureListener(e -> {
                    String finalDisplayName = sub.toUpperCase() + ", " + finalGradeName + " " + finalClassName + ", Teacher: Unknown Teacher, (" + capType + ")";
                    newSlot.setDisplayName(finalDisplayName);
                    db.collection("timetableSlots").add(newSlot).addOnSuccessListener(docRef -> {
                        Toast.makeText(this, "Lesson created successfully!", Toast.LENGTH_SHORT).show();
                        loadTimetableSlots();
                    });
                });
            } else {
                String finalDisplayName = sub.toUpperCase() + ", " + finalGradeName + " " + finalClassName + ", Teacher: Unknown Teacher, (" + capType + ")";
                newSlot.setDisplayName(finalDisplayName);
                db.collection("timetableSlots").add(newSlot).addOnSuccessListener(docRef -> {
                    Toast.makeText(this, "Lesson created successfully!", Toast.LENGTH_SHORT).show();
                    loadTimetableSlots();
                });
            }
        });
        builder.setNegativeButton("Cancel", null);
        builder.show();
    }

    @Override
    public void onExistingSlotClick(TimetableSlot slot) {
        CharSequence[] options = new CharSequence[]{"🔄 Replace Lesson", "➕ Add Parallel Lesson", "🗑️ Delete Lesson"};

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Manage Slot");
        builder.setItems(options, (dialog, which) -> {
            if (which == 0) {
                db.collection("timetableSlots").document(slot.getSlotId()).delete()
                        .addOnSuccessListener(aVoid -> onAddSlotClick(slot.getHour()));
            } else if (which == 1) {
                onAddSlotClick(slot.getHour());
            } else if (which == 2) {
                db.collection("timetableSlots").document(slot.getSlotId()).delete()
                        .addOnSuccessListener(aVoid -> {
                            Toast.makeText(this, "Lesson deleted", Toast.LENGTH_SHORT).show();
                            loadTimetableSlots();
                        });
            }
        });
        builder.show();
    }
}