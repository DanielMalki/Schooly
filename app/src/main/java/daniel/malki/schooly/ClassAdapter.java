package daniel.malki.schooly;

import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.util.ArrayList;
import java.util.List;

public class ClassAdapter extends RecyclerView.Adapter<ClassAdapter.ClassViewHolder> {

    private List<SchoolClass> classListFiltered;
    private List<SchoolClass> classListFull;

    public ClassAdapter(List<SchoolClass> classList) {
        this.classListFiltered = classList;
        this.classListFull = new ArrayList<>(classList);
    }

    @NonNull
    @Override
    public ClassViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_class, parent, false);
        return new ClassViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ClassViewHolder holder, int position) {
        SchoolClass schoolClass = classListFiltered.get(position);

        holder.tvClassName.setText(schoolClass.getDisplayName());

        // הצגת סוג הכיתה/קבוצה באותיות קטנות מתחת לשם
        String typeText = schoolClass.getType() != null ? schoolClass.getType().toUpperCase() : "GENERAL";
        holder.tvTeacherName.setText("Type: " + typeText);

        // שינוי צבע הריבוע בצד לפי סוג הקבוצה כדי שיהיה מעוצב ויפה
        GradientDrawable bgShape = new GradientDrawable();
        bgShape.setCornerRadius(15); // עיגול פינות קל לאייקון

        if (schoolClass.getType() != null) {
            switch (schoolClass.getType().toLowerCase()) {
                case "math":
                    bgShape.setColor(Color.parseColor("#1E88E5")); // כחול למתמטיקה
                    break;
                case "english":
                    bgShape.setColor(Color.parseColor("#4CAF50")); // ירוק לאנגלית
                    break;
                case "homeroom":
                    bgShape.setColor(Color.parseColor("#9C27B0")); // סגול לכיתת אם
                    break;
                default:
                    bgShape.setColor(Color.parseColor("#78909C")); // אפור לכל השאר
                    break;
            }
        } else {
            bgShape.setColor(Color.parseColor("#78909C"));
        }
        holder.viewClassIcon.setBackground(bgShape);
    }

    @Override
    public int getItemCount() {
        return classListFiltered != null ? classListFiltered.size() : 0;
    }

    public void updateList(List<SchoolClass> newList) {
        this.classListFiltered = newList;
        this.classListFull = new ArrayList<>(newList);
        notifyDataSetChanged();
    }

    // מנגנון חיפוש כיתות לפי ה-displayName
    public void filter(String query) {
        List<SchoolClass> filteredList = new ArrayList<>();
        for (SchoolClass sc : classListFull) {
            if (sc.getDisplayName() != null && sc.getDisplayName().toLowerCase().contains(query.toLowerCase())) {
                filteredList.add(sc);
            }
        }
        classListFiltered = filteredList;
        notifyDataSetChanged();
    }

    public static class ClassViewHolder extends RecyclerView.ViewHolder {
        View viewClassIcon;
        TextView tvClassName, tvTeacherName;

        public ClassViewHolder(@NonNull View itemView) {
            super(itemView);
            viewClassIcon = itemView.findViewById(R.id.viewClassIcon);
            tvClassName = itemView.findViewById(R.id.tvClassName);
            tvTeacherName = itemView.findViewById(R.id.tvTeacherName);
        }
    }
}