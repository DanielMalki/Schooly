package daniel.malki.schooly;

import android.app.AlertDialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.List;

public class EditTeacherFragment extends Fragment {

    private TextView tvSelectSubjects;
    private LinearLayout layoutSelectedSubjectsList;

    private FirebaseFirestore db;
    private ArrayList<String> subjectNames = new ArrayList<>();
    private ArrayList<String> subjectIds = new ArrayList<>();
    private boolean[] checkedSubjectsArray;
    private ArrayList<String> chosenSubjectIds = new ArrayList<>();
    private List<DocumentReference> initialSubjects = new ArrayList<>();

    public EditTeacherFragment() {}

    public static EditTeacherFragment newInstance(List<DocumentReference> subjects) {
        EditTeacherFragment fragment = new EditTeacherFragment();
        if (subjects != null) {
            fragment.initialSubjects = subjects;
        }
        return fragment;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_edit_teacher, container, false);
        db = FirebaseFirestore.getInstance();

        tvSelectSubjects = view.findViewById(R.id.tvSelectSubjects);
        layoutSelectedSubjectsList = view.findViewById(R.id.layoutSelectedSubjectsList);

        tvSelectSubjects.setOnClickListener(v -> showSubjectsMultiSelectDialog());

        loadSubjects();

        return view;
    }

    private void loadSubjects() {
        db.collection("subjects").get().addOnSuccessListener(snapshots -> {
            subjectNames.clear();
            subjectIds.clear();
            for (QueryDocumentSnapshot doc : snapshots) {
                subjectNames.add(doc.getString("displayName"));
                subjectIds.add(doc.getId());
            }

            checkedSubjectsArray = new boolean[subjectNames.size()];
            chosenSubjectIds.clear();

            for (DocumentReference initialRef : initialSubjects) {
                int index = subjectIds.indexOf(initialRef.getId());
                if (index >= 0) {
                    checkedSubjectsArray[index] = true;
                    chosenSubjectIds.add(subjectIds.get(index));
                }
            }

            updateSubjectsTextView();
        });
    }

    private void showSubjectsMultiSelectDialog() {
        if (checkedSubjectsArray == null) return;

        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
        builder.setTitle("Select Teachable Subjects");
        builder.setMultiChoiceItems(subjectNames.toArray(new CharSequence[0]), checkedSubjectsArray, (dialog, which, isChecked) -> {
            checkedSubjectsArray[which] = isChecked;
        });

        builder.setPositiveButton("OK", (dialog, which) -> {
            chosenSubjectIds.clear();
            for (int i = 0; i < checkedSubjectsArray.length; i++) {
                if (checkedSubjectsArray[i]) chosenSubjectIds.add(subjectIds.get(i));
            }
            updateSubjectsTextView();
        });

        builder.setNegativeButton("Cancel", null);
        builder.show();
    }

    private void updateSubjectsTextView() {
        if (layoutSelectedSubjectsList == null) return;
        layoutSelectedSubjectsList.removeAllViews();

        if (chosenSubjectIds.isEmpty()) {
            tvSelectSubjects.setText("Tap to select teachable subjects *");
        } else {
            tvSelectSubjects.setText(chosenSubjectIds.size() + " Subjects Selected:");
            for (int i = 0; i < checkedSubjectsArray.length; i++) {
                if (checkedSubjectsArray[i]) {
                    TextView tv = new TextView(getContext());
                    tv.setText("• " + subjectNames.get(i));
                    tv.setTextSize(16.0f);
                    tv.setPadding(10, 5, 10, 5);
                    layoutSelectedSubjectsList.addView(tv);
                }
            }
        }
    }

    public ArrayList<DocumentReference> getSelectedSubjectsRefs() {
        ArrayList<DocumentReference> refs = new ArrayList<>();
        for (String id : chosenSubjectIds) {
            refs.add(db.collection("subjects").document(id));
        }
        return refs;
    }
}