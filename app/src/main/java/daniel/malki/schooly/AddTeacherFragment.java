package daniel.malki.schooly;

import android.app.AlertDialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.fragment.app.Fragment;

import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class AddTeacherFragment extends Fragment {

    private TextView tvSelectSubjects;
    private ImageButton btnQuickAddSubject;
    private LinearLayout layoutSelectedSubjectsList;

    private FirebaseFirestore db;
    private DocumentReference currentSchoolRef;

    private ArrayList<String> allSubjectNames = new ArrayList<>();
    private ArrayList<String> selectedSubjects = new ArrayList<>(); // ⚠️ זו הרשימה הישנה שמחזיקה שמות (טקסט)
    // ✨ התוספת שלך: רשימות שיחזיקו את הרפרנסים
    private ArrayList<DocumentReference> allSubjectRefs = new ArrayList<>();
    private ArrayList<DocumentReference> selectedSubjectRefs = new ArrayList<>();

    private boolean[] checkedSubjects;

    public AddTeacherFragment() { }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_add_teacher, container, false);
        db = FirebaseFirestore.getInstance();
        initViews(view);
        loadSubjects();
        return view;
    }

    private void initViews(View view) {
        tvSelectSubjects = view.findViewById(R.id.tvSelectSubjects);
        btnQuickAddSubject = view.findViewById(R.id.btnQuickAddSubject);
        layoutSelectedSubjectsList = view.findViewById(R.id.layoutSelectedSubjectsList);

        tvSelectSubjects.setOnClickListener(v -> showSubjectsDialog());
        btnQuickAddSubject.setOnClickListener(v -> showQuickAddSubjectDialog());
    }

    public void setSchoolRefAndLoad(DocumentReference schoolRef) {
        this.currentSchoolRef = schoolRef;
        if (isAdded() && getView() != null) {
            if (schoolRef != null) {
                loadSubjects();
            } else {
                clearTeacherData();
            }
        }
    }

    private void loadSubjects() {
        if (db == null) return;

        db.collection("subjects").get().addOnSuccessListener(queryDocumentSnapshots -> {
            if (!isAdded() || getContext() == null) return;

            allSubjectNames.clear();
            allSubjectRefs.clear(); // ✨ חובה לנקות את רשימת הרפרנסים

            for (QueryDocumentSnapshot doc : queryDocumentSnapshots) {
                String displayName = doc.getString("displayName");
                if (displayName != null) {
                    allSubjectNames.add(displayName);
                    allSubjectRefs.add(doc.getReference()); // ✨ חובה! כדי שלא יקרוס כשנשלוף ממנה
                }
            }

            checkedSubjects = new boolean[allSubjectNames.size()];

            if (allSubjectNames.isEmpty()) {
                tvSelectSubjects.setText("No subjects found in database");
            } else {
                tvSelectSubjects.setText("Tap to select subjects...");
            }
        });
    }

    private void showSubjectsDialog() {
        if (!isAdded() || getContext() == null) return;

        if (allSubjectNames.isEmpty()) {
            Toast.makeText(getContext(), "No subjects found. Add one first!", Toast.LENGTH_SHORT).show();
            return;
        }

        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
        builder.setTitle("Select Teacher's Subjects");

        CharSequence[] items = allSubjectNames.toArray(new CharSequence[0]);
        builder.setMultiChoiceItems(items, checkedSubjects, (dialog, indexSelected, isChecked) -> {
            checkedSubjects[indexSelected] = isChecked;
        });

        builder.setPositiveButton("OK", (dialog, id) -> {
            selectedSubjects.clear();
            selectedSubjectRefs.clear(); // ✨ תוספת 1: מנקים את רשימת הרפרנסים כדי שלא יצטברו כפילויות
            layoutSelectedSubjectsList.removeAllViews();

            for (int i = 0; i < checkedSubjects.length; i++) {
                if (checkedSubjects[i]) {
                    selectedSubjects.add(allSubjectNames.get(i));
                    selectedSubjectRefs.add(allSubjectRefs.get(i)); // ✨ תוספת 2: מוסיפים את הרפרנס האמיתי לרשימה!
                    addSubjectTextViewToList(allSubjectNames.get(i));
                }
            }
        });

        builder.setNegativeButton("Cancel", null);
        builder.create().show();
    }

    private void showQuickAddSubjectDialog() {
        if (getContext() == null) return;

        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
        builder.setTitle("Quick Add Subject");

        final EditText input = new EditText(getContext());
        input.setHint("Subject Name (e.g., History)");
        builder.setView(input);

        builder.setPositiveButton("Add", (dialog, which) -> {
            String subjectName = input.getText().toString().trim();
            if (!subjectName.isEmpty()) {
                Map<String, Object> subjectData = new HashMap<>();
                subjectData.put("displayName", subjectName);
                // תיקון: הוסר הקישור לבית ספר, המקצוע נשאר אוניברסלי לכולם!

                db.collection("subjects").add(subjectData).addOnSuccessListener(documentReference -> {
                    Toast.makeText(getContext(), subjectName + " added! 🎉", Toast.LENGTH_SHORT).show();
                    loadSubjects();
                });
            }
        });
        builder.setNegativeButton("Cancel", null);
        builder.create().show();
    }

    private void addSubjectTextViewToList(String subjectName) {
        if (getContext() == null) return;
        TextView tv = new TextView(getContext());
        tv.setText("• " + subjectName);
        tv.setTextSize(16);
        tv.setPadding(10, 5, 10, 5);
        layoutSelectedSubjectsList.addView(tv);
    }

    private void clearTeacherData() {
        allSubjectNames.clear();
        selectedSubjects.clear();
        if (layoutSelectedSubjectsList != null) {
            layoutSelectedSubjectsList.removeAllViews();
        }
        if (tvSelectSubjects != null) {
            tvSelectSubjects.setText("Tap to select subjects...");
        }
    }

    // נשנה את טיפוס ההחזרה מ-String ל-DocumentReference
    public ArrayList<DocumentReference> getSelectedSubjects() {
        return selectedSubjectRefs;
    }
}