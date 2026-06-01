package daniel.malki.schooly;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Spinner;

import androidx.fragment.app.Fragment;

import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class AddStudentFragment extends Fragment {

    private Spinner spinnerStudentGrade, spinnerFilterClassesGrade;
    private Spinner spinnerHomeroom, spinnerMathGroup, spinnerEnglishGroup, spinnerPeGroup, spinnerMajorAGroup, spinnerMajorBGroup;

    private ArrayList<String> gradeNames = new ArrayList<>();
    private ArrayList<DocumentReference> gradeRefs = new ArrayList<>();
    private ArrayList<String> filterGradeNames = new ArrayList<>();
    private ArrayList<String> filterGradeIds = new ArrayList<>();
    private Map<String, String> gradeMap = new HashMap<>();

    private List<QueryDocumentSnapshot> rawClassesList = new ArrayList<>();

    private ArrayList<String> homeroomNames = new ArrayList<>(), homeroomIds = new ArrayList<>();
    private ArrayList<String> mathNames = new ArrayList<>(), mathIds = new ArrayList<>();
    private ArrayList<String> englishNames = new ArrayList<>(), englishIds = new ArrayList<>();
    private ArrayList<String> peNames = new ArrayList<>(), peIds = new ArrayList<>();
    private ArrayList<String> majorANames = new ArrayList<>(), majorAIds = new ArrayList<>();
    private ArrayList<String> majorBNames = new ArrayList<>(), majorBIds = new ArrayList<>();

    private FirebaseFirestore db;
    private DocumentReference currentSchoolRef;

    public AddStudentFragment() { }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_add_student, container, false);
        db = FirebaseFirestore.getInstance();
        initViews(view);

        if (currentSchoolRef != null) {
            loadGradesAndClasses();
        }
        return view;
    }

    private void initViews(View view) {
        spinnerStudentGrade = view.findViewById(R.id.spinnerStudentGrade);
        spinnerFilterClassesGrade = view.findViewById(R.id.spinnerFilterClassesGrade);
        spinnerHomeroom = view.findViewById(R.id.spinnerHomeroom);
        spinnerMathGroup = view.findViewById(R.id.spinnerMathGroup);
        spinnerEnglishGroup = view.findViewById(R.id.spinnerEnglishGroup);
        spinnerPeGroup = view.findViewById(R.id.spinnerPeGroup);
        spinnerMajorAGroup = view.findViewById(R.id.spinnerMajorAGroup);
        spinnerMajorBGroup = view.findViewById(R.id.spinnerMajorBGroup);
    }

    public void setSchoolRefAndLoad(DocumentReference schoolRef) {
        this.currentSchoolRef = schoolRef;
        if (isAdded() && getView() != null) {
            if (schoolRef != null) {
                loadGradesAndClasses();
            } else {
                clearStudentSpinners();
            }
        }
    }

    // =========================================================================
    // פונקציית עזר: מחלצת מספרים מהטקסט לטובת סידור הגיוני (1, 2... 10... Graduated)
    // =========================================================================
        private int extractGradeNumber(String name) {
            if (name == null) return 9999;
            String numStr = name.replaceAll("\\D+", ""); // מנקה כל מה שהוא לא ספרה
            if (numStr.isEmpty()) return 9999; // אם זה "Graduated", ניתן לו מספר ענק שיופיע בסוף
            return Integer.parseInt(numStr);
        }

        private void loadGradesAndClasses() {
            if (db == null) return;

            db.collection("grades").get().addOnSuccessListener(gradesSnap -> {
                if (!isAdded() || getContext() == null) return;

                gradeNames.clear(); gradeRefs.clear();
                filterGradeNames.clear(); filterGradeIds.clear();
                gradeMap.clear();

                filterGradeNames.add("All Grades");
                filterGradeIds.add("");

                // 1. שומרים הכל ברשימה זמנית כדי למיין
                List<QueryDocumentSnapshot> sortedGrades = new ArrayList<>();
                for (QueryDocumentSnapshot doc : gradesSnap) {
                    sortedGrades.add(doc);
                }

                // 2. ממיינים לפי המספר שחילצנו
                Collections.sort(sortedGrades, (d1, d2) -> {
                    String n1 = d1.getString("displayName");
                    if (n1 == null) n1 = d1.getId();
                    String n2 = d2.getString("displayName");
                    if (n2 == null) n2 = d2.getId();

                    int num1 = extractGradeNumber(n1);
                    int num2 = extractGradeNumber(n2);

                    if (num1 != num2) {
                        return Integer.compare(num1, num2); // מיון מספרי
                    }
                    return n1.compareTo(n2); // אם משום מה יש שניים באותו מספר, ימוין לפי א' ב'
                });

                // 3. מכניסים לספינרים לפי הסדר הנכון
                for (QueryDocumentSnapshot doc : sortedGrades) {
                    String gName = doc.getString("displayName");
                    if (gName == null) gName = doc.getId();

                    String gId = doc.getId();
                    gradeNames.add(gName);
                    gradeRefs.add(doc.getReference());

                    filterGradeNames.add(gName);
                    filterGradeIds.add(gId);
                    gradeMap.put(gId, gName);
                }

                ArrayAdapter<String> gradeAdapter = new ArrayAdapter<>(getContext(), android.R.layout.simple_spinner_dropdown_item, gradeNames);
                spinnerStudentGrade.setAdapter(gradeAdapter);

                ArrayAdapter<String> filterAdapter = new ArrayAdapter<>(getContext(), android.R.layout.simple_spinner_dropdown_item, filterGradeNames);
                spinnerFilterClassesGrade.setAdapter(filterAdapter);

                spinnerFilterClassesGrade.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                    @Override
                    public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                        filterAndDisplayClasses();
                    }
                    @Override
                    public void onNothingSelected(AdapterView<?> parent) {}
                });

                fetchAllClassesForSchool();
            });
        }

    private void fetchAllClassesForSchool() {
        if (currentSchoolRef == null || db == null) return;

        db.collection("classes").whereEqualTo("school", currentSchoolRef).get().addOnSuccessListener(snapshots -> {
            if (!isAdded() || getContext() == null) return;

            rawClassesList.clear();
            for (QueryDocumentSnapshot doc : snapshots) {
                rawClassesList.add(doc);
            }

            if (rawClassesList.isEmpty()) {
                db.collection("classes").get().addOnSuccessListener(allSnaps -> {
                    if (!isAdded() || getContext() == null) return;
                    rawClassesList.clear();
                    for (QueryDocumentSnapshot doc : allSnaps) {
                        Object schoolObj = doc.get("school");
                        if (schoolObj == null) schoolObj = doc.get("schoolRef"); // רשת ביטחון לשם השדה

                        boolean isMatch = false;
                        if (schoolObj instanceof DocumentReference) {
                            isMatch = ((DocumentReference) schoolObj).getId().equals(currentSchoolRef.getId());
                        } else if (schoolObj instanceof String) {
                            isMatch = ((String) schoolObj).contains(currentSchoolRef.getId());
                        }
                        if (isMatch) rawClassesList.add(doc);
                    }
                    filterAndDisplayClasses();
                });
            } else {
                filterAndDisplayClasses();
            }
        });
    }

    private void filterAndDisplayClasses() {
        clearStudentLists();

        String selectedFilterId = "";
        if (spinnerFilterClassesGrade.getSelectedItemPosition() > 0) {
            selectedFilterId = filterGradeIds.get(spinnerFilterClassesGrade.getSelectedItemPosition());
        }

        for (QueryDocumentSnapshot doc : rawClassesList) {
            String type = doc.getString("type");
            String rawName = doc.getString("displayName");
            if (rawName == null) rawName = doc.getString("name");
            if (rawName == null) rawName = "Unknown Class";

            // רשת ביטחון: בודק גם grade וגם gradeRef כדי למנוע את באג ה-Unknown Grade!
            String classGradeId = "";
            Object gradeObj = doc.get("grade");
            if (gradeObj == null) gradeObj = doc.get("gradeRef");

            if (gradeObj instanceof DocumentReference) {
                classGradeId = ((DocumentReference) gradeObj).getId();
            } else if (gradeObj instanceof String) {
                String gStr = (String) gradeObj;
                classGradeId = gStr.substring(gStr.lastIndexOf("/") + 1);
            }

            if (!selectedFilterId.isEmpty() && !selectedFilterId.equals(classGradeId)) {
                continue;
            }

            if (type != null) {
                String gName = gradeMap.containsKey(classGradeId) ? gradeMap.get(classGradeId) : "Unknown Grade";
                String capType = type.substring(0, 1).toUpperCase() + type.substring(1);
                String formattedName = capType + ", " + gName + ", " + rawName;
                String id = doc.getId();

                switch (type.toLowerCase()) {
                    case "homeroom": homeroomNames.add(formattedName); homeroomIds.add(id); break;
                    case "math": mathNames.add(formattedName); mathIds.add(id); break;
                    case "english": englishNames.add(formattedName); englishIds.add(id); break;
                    case "sports": case "pe": peNames.add(formattedName); peIds.add(id); break;
                    case "major a": majorANames.add(formattedName); majorAIds.add(id); break;
                    case "major b": majorBNames.add(formattedName); majorBIds.add(id); break;
                }
            }
        }
        updateClassSpinners();
    }

    private void clearStudentLists() {
        homeroomNames.clear(); homeroomIds.clear();
        mathNames.clear(); mathIds.clear();
        englishNames.clear(); englishIds.clear();
        peNames.clear(); peIds.clear();
        majorANames.clear(); majorAIds.clear();
        majorBNames.clear(); majorBIds.clear();

        homeroomNames.add("Select Homeroom..."); homeroomIds.add("");
        mathNames.add("Select Math Group..."); mathIds.add("");
        englishNames.add("Select English Group..."); englishIds.add("");
        peNames.add("Select P.E. Group..."); peIds.add("");
        majorANames.add("Select Major A..."); majorAIds.add("");
        majorBNames.add("Select Major B..."); majorBIds.add("");
    }

    private void clearStudentSpinners() {
        clearStudentLists();
        updateClassSpinners();
    }

    private void updateClassSpinners() {
        if (!isAdded() || getContext() == null) return;
        spinnerHomeroom.setAdapter(new ArrayAdapter<>(getContext(), android.R.layout.simple_spinner_dropdown_item, homeroomNames));
        spinnerMathGroup.setAdapter(new ArrayAdapter<>(getContext(), android.R.layout.simple_spinner_dropdown_item, mathNames));
        spinnerEnglishGroup.setAdapter(new ArrayAdapter<>(getContext(), android.R.layout.simple_spinner_dropdown_item, englishNames));
        spinnerPeGroup.setAdapter(new ArrayAdapter<>(getContext(), android.R.layout.simple_spinner_dropdown_item, peNames));
        spinnerMajorAGroup.setAdapter(new ArrayAdapter<>(getContext(), android.R.layout.simple_spinner_dropdown_item, majorANames));
        spinnerMajorBGroup.setAdapter(new ArrayAdapter<>(getContext(), android.R.layout.simple_spinner_dropdown_item, majorBNames));
    }

    public DocumentReference getSelectedGradeRef() {
        if (spinnerStudentGrade.getSelectedItemPosition() >= 0 && gradeRefs.size() > 0) {
            return gradeRefs.get(spinnerStudentGrade.getSelectedItemPosition());
        }
        return null;
    }

    public Map<String, Object> getSelectedClassesMap() {
        Map<String, Object> classesMap = new HashMap<>();
        if (spinnerHomeroom.getSelectedItemPosition() > 0) classesMap.put("homeroom", db.collection("classes").document(homeroomIds.get(spinnerHomeroom.getSelectedItemPosition())));
        if (spinnerMathGroup.getSelectedItemPosition() > 0) classesMap.put("math", db.collection("classes").document(mathIds.get(spinnerMathGroup.getSelectedItemPosition())));
        if (spinnerEnglishGroup.getSelectedItemPosition() > 0) classesMap.put("english", db.collection("classes").document(englishIds.get(spinnerEnglishGroup.getSelectedItemPosition())));
        if (spinnerPeGroup.getSelectedItemPosition() > 0) classesMap.put("sports", db.collection("classes").document(peIds.get(spinnerPeGroup.getSelectedItemPosition())));
        if (spinnerMajorAGroup.getSelectedItemPosition() > 0) classesMap.put("major a", db.collection("classes").document(majorAIds.get(spinnerMajorAGroup.getSelectedItemPosition())));
        if (spinnerMajorBGroup.getSelectedItemPosition() > 0) classesMap.put("major b", db.collection("classes").document(majorBIds.get(spinnerMajorBGroup.getSelectedItemPosition())));
        return classesMap;
    }
}