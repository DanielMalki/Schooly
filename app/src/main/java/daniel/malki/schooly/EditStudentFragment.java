package daniel.malki.schooly;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Spinner;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class EditStudentFragment extends Fragment {

    private AutoCompleteTextView spinnerFilterClassesGrade; // הפילטר המעוצב החדש
    private Spinner spinnerHomeroom, spinnerMathGroup, spinnerEnglishGroup, spinnerSportsGroup, spinnerMajor1Group, spinnerMajor2Group;

    private ArrayList<String> filterGradeNames = new ArrayList<>();
    private ArrayList<String> filterGradeIds = new ArrayList<>();
    private Map<String, String> gradeMap = new HashMap<>();

    private List<QueryDocumentSnapshot> rawClassesList = new ArrayList<>();

    private ArrayList<String> homeroomNames = new ArrayList<>(), homeroomIds = new ArrayList<>();
    private ArrayList<String> mathNames = new ArrayList<>(), mathIds = new ArrayList<>();
    private ArrayList<String> englishNames = new ArrayList<>(), englishIds = new ArrayList<>();
    private ArrayList<String> sportsNames = new ArrayList<>(), sportsIds = new ArrayList<>();
    private ArrayList<String> majorNames = new ArrayList<>(), majorIds = new ArrayList<>();

    private FirebaseFirestore db;
    private DocumentReference currentSchoolRef;
    private Map<String, Object> initialClassesData = new HashMap<>();

    private int selectedFilterPosition = 0;

    public EditStudentFragment() {}

    public static EditStudentFragment newInstance(DocumentReference schoolRef, Map<String, Object> initialClasses) {
        EditStudentFragment fragment = new EditStudentFragment();
        fragment.currentSchoolRef = schoolRef;
        fragment.initialClassesData = initialClasses != null ? initialClasses : new HashMap<>();
        return fragment;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_edit_student, container, false);
        db = FirebaseFirestore.getInstance();

        initViews(view);
        loadGrades();

        return view;
    }

    private void initViews(View v) {
        spinnerFilterClassesGrade = v.findViewById(R.id.spinnerFilterClassesGrade);
        spinnerHomeroom = v.findViewById(R.id.spinnerHomeroom);
        spinnerMathGroup = v.findViewById(R.id.spinnerMathGroup);
        spinnerEnglishGroup = v.findViewById(R.id.spinnerEnglishGroup);
        spinnerSportsGroup = v.findViewById(R.id.spinnerSportsGroup);
        spinnerMajor1Group = v.findViewById(R.id.spinnerMajor1Group);
        spinnerMajor2Group = v.findViewById(R.id.spinnerMajor2Group);

        // האזנה לפילטר המעוצב
        spinnerFilterClassesGrade.setOnItemClickListener((parent, view, position, id) -> {
            selectedFilterPosition = position;
            filterClassesByGrade();
        });
    }

    private int extractGradeNumber(String name) {
        if (name == null) return 9999;
        String numberStr = name.replaceAll("\\D+", "");
        if (numberStr.isEmpty()) return 9999;
        return Integer.parseInt(numberStr);
    }

    private void loadGrades() {
        if (db == null) return;
        db.collection("grades").get().addOnSuccessListener(snapshots -> {
            if (!isAdded() || getContext() == null) return;

            List<QueryDocumentSnapshot> sortedGrades = new ArrayList<>();
            for (QueryDocumentSnapshot d : snapshots) {
                sortedGrades.add(d);
            }

            // מיון קשיח לפי הלוגיקה המוצלחת מה-AddFragment
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

            filterGradeNames.clear();
            filterGradeIds.clear();
            gradeMap.clear();

            filterGradeNames.add("All Grades");
            filterGradeIds.add("");

            for (QueryDocumentSnapshot doc : sortedGrades) {
                String gName = doc.getString("displayName");
                if (gName == null) gName = doc.getId();

                String gId = doc.getId();
                filterGradeNames.add(gName);
                filterGradeIds.add(gId);
                gradeMap.put(gId, gName); // מיפוי השמות המלא באנגלית ללא עברית
            }

            if (getContext() != null) {
                ArrayAdapter<String> previewAdapter = new ArrayAdapter<>(getContext(), android.R.layout.simple_spinner_dropdown_item, filterGradeNames);
                spinnerFilterClassesGrade.setAdapter(previewAdapter);
                spinnerFilterClassesGrade.setText(filterGradeNames.get(0), false);
            }
            fetchClassesData();
        });
    }

    private void fetchClassesData() {
        if (currentSchoolRef == null || db == null) return;

        // שלב א': ניסיון שליפה רגיל לפי בית ספר
        db.collection("classes").whereEqualTo("school", currentSchoolRef).get().addOnSuccessListener(snapshots -> {
            if (!isAdded() || getContext() == null) return;

            rawClassesList.clear();
            for (QueryDocumentSnapshot d : snapshots) {
                rawClassesList.add(d);
            }

            // שלב ב' (רשת ביטחון מה-AddFragment): אם חזר ריק, שולפים ומפלטרים ידנית לפי schoolRef / school
            if (rawClassesList.isEmpty()) {
                db.collection("classes").get().addOnSuccessListener(allSnaps -> {
                    if (!isAdded() || getContext() == null) return;
                    rawClassesList.clear();
                    for (QueryDocumentSnapshot doc : allSnaps) {
                        Object schoolObj = doc.get("school");
                        if (schoolObj == null) schoolObj = doc.get("schoolRef");

                        boolean isMatch = false;
                        if (schoolObj instanceof DocumentReference) {
                            isMatch = ((DocumentReference) schoolObj).getId().equals(currentSchoolRef.getId());
                        } else if (schoolObj instanceof String) {
                            isMatch = ((String) schoolObj).contains(currentSchoolRef.getId());
                        }
                        if (isMatch) rawClassesList.add(doc);
                    }
                    filterClassesByGrade();
                });
            } else {
                filterClassesByGrade();
            }
        });
    }

    private void filterClassesByGrade() {
        clearAllSpinnerLists();
        if (selectedFilterPosition < 0) return;
        String filterGradeId = filterGradeIds.get(selectedFilterPosition);

        for (QueryDocumentSnapshot doc : rawClassesList) {
            String type = doc.getString("type");

            // רשת ביטחון לשם הכיתה (displayName או name)
            String rawName = doc.getString("displayName");
            if (rawName == null) rawName = doc.getString("name");
            if (rawName == null) rawName = "Unknown Class";

            // רשת ביטחון לחילוץ מזהה השכבה המדויק (תומך ב-grade וב-gradeRef)
            String classGradeId = "";
            Object gradeObj = doc.get("grade");
            if (gradeObj == null) gradeObj = doc.get("gradeRef");

            if (gradeObj instanceof DocumentReference) {
                classGradeId = ((DocumentReference) gradeObj).getId();
            } else if (gradeObj instanceof String) {
                String gStr = (String) gradeObj;
                classGradeId = gStr.substring(gStr.lastIndexOf("/") + 1);
            }

            // סינון לפי הבחירה בפילטר
            if (!filterGradeId.isEmpty() && !filterGradeId.equals(classGradeId)) {
                continue;
            }

            // שליפת שם השכבה היפה באנגלית מתוך המפה
            String gName = gradeMap.containsKey(classGradeId) ? gradeMap.get(classGradeId) : "Unknown Grade";

            // עיצוב הלייבל החדש לבקשתך: ללא סוג הכיתה, רק שכבה ושם! (למשל: Grade 9 - 1)
            String formattedLabel = gName + " - " + rawName;
            String id = doc.getId();

            if (type == null) continue;
            switch (type.toLowerCase()) {
                case "homeroom":
                    homeroomNames.add(formattedLabel); homeroomIds.add(id);
                    break;
                case "math":
                    mathNames.add(formattedLabel); mathIds.add(id);
                    break;
                case "english":
                    englishNames.add(formattedLabel); englishIds.add(id);
                    break;
                case "sports":
                case "pe":
                    sportsNames.add(formattedLabel); sportsIds.add(id);
                    break;
                case "major a":
                case "major1":
                    majorNames.add(formattedLabel); majorIds.add(id);
                    break;
                case "major b":
                case "major2":
                    majorNames.add(formattedLabel); majorIds.add(id);
                    break;
            }
        }
        setupSpinnerAdapters();
    }

    private void clearAllSpinnerLists() {
        homeroomNames.clear(); homeroomIds.clear(); homeroomNames.add("Select Homeroom..."); homeroomIds.add("");
        mathNames.clear(); mathIds.clear(); mathNames.add("Select Math Group..."); mathIds.add("");
        englishNames.clear(); englishIds.clear(); englishNames.add("Select English Group..."); englishIds.add("");
        sportsNames.clear(); sportsIds.clear(); sportsNames.add("Select P.E. Group..."); sportsIds.add("");
        majorNames.clear(); majorIds.clear(); majorNames.add("Select Elective Major..."); majorIds.add("");
    }

    private void setupSpinnerAdapters() {
        if (!isAdded() || getContext() == null) return;
        spinnerHomeroom.setAdapter(new ArrayAdapter<>(getContext(), android.R.layout.simple_spinner_dropdown_item, homeroomNames));
        spinnerMathGroup.setAdapter(new ArrayAdapter<>(getContext(), android.R.layout.simple_spinner_dropdown_item, mathNames));
        spinnerEnglishGroup.setAdapter(new ArrayAdapter<>(getContext(), android.R.layout.simple_spinner_dropdown_item, englishNames));
        spinnerSportsGroup.setAdapter(new ArrayAdapter<>(getContext(), android.R.layout.simple_spinner_dropdown_item, sportsNames));

        ArrayAdapter<String> majorAdapter = new ArrayAdapter<>(getContext(), android.R.layout.simple_spinner_dropdown_item, majorNames);
        spinnerMajor1Group.setAdapter(majorAdapter);
        spinnerMajor2Group.setAdapter(majorAdapter);

        applyLoadedSelections();
    }

    private void applyLoadedSelections() {
        if (initialClassesData == null) return;

        for (Map.Entry<String, Object> entry : initialClassesData.entrySet()) {
            if (entry.getValue() instanceof DocumentReference) {
                String targetId = ((DocumentReference) entry.getValue()).getId();
                String key = entry.getKey().toLowerCase();

                if (key.contains("homeroom") && homeroomIds.contains(targetId)) {
                    spinnerHomeroom.setSelection(homeroomIds.indexOf(targetId));
                } else if (key.contains("math") && mathIds.contains(targetId)) {
                    spinnerMathGroup.setSelection(mathIds.indexOf(targetId));
                } else if (key.contains("english") && englishIds.contains(targetId)) {
                    spinnerEnglishGroup.setSelection(englishIds.indexOf(targetId));
                } else if ((key.contains("sports") || key.contains("pe")) && sportsIds.contains(targetId)) {
                    spinnerSportsGroup.setSelection(sportsIds.indexOf(targetId));
                } else if ((key.contains("major1") || key.contains("major a")) && majorIds.contains(targetId)) {
                    spinnerMajor1Group.setSelection(majorIds.indexOf(targetId));
                } else if ((key.contains("major2") || key.contains("major b")) && majorIds.contains(targetId)) {
                    spinnerMajor2Group.setSelection(majorIds.indexOf(targetId));
                }
            }
        }
    }

    public Map<String, Object> getSelectedClassesMap() {
        Map<String, Object> classesMap = new HashMap<>();
        if (spinnerHomeroom.getSelectedItemPosition() > 0) classesMap.put("homeroom", db.collection("classes").document(homeroomIds.get(spinnerHomeroom.getSelectedItemPosition())));
        if (spinnerMathGroup.getSelectedItemPosition() > 0) classesMap.put("math", db.collection("classes").document(mathIds.get(spinnerMathGroup.getSelectedItemPosition())));
        if (spinnerEnglishGroup.getSelectedItemPosition() > 0) classesMap.put("english", db.collection("classes").document(englishIds.get(spinnerEnglishGroup.getSelectedItemPosition())));
        if (spinnerSportsGroup.getSelectedItemPosition() > 0) classesMap.put("sports", db.collection("classes").document(sportsIds.get(spinnerSportsGroup.getSelectedItemPosition())));
        if (spinnerMajor1Group.getSelectedItemPosition() > 0) classesMap.put("major1", db.collection("classes").document(majorIds.get(spinnerMajor1Group.getSelectedItemPosition())));
        if (spinnerMajor2Group.getSelectedItemPosition() > 0) classesMap.put("major2", db.collection("classes").document(majorIds.get(spinnerMajor2Group.getSelectedItemPosition())));
        return classesMap;
    }

}