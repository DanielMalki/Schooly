package daniel.malki.schooly;

import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.RecyclerView;
import java.util.ArrayList;
import java.util.List;

public class ClassAdapter extends RecyclerView.Adapter<ClassAdapter.ClassViewHolder> {

    public interface OnClassClickListener {
        void onClassClick(SchoolClass schoolClass);
    }

    private List<SchoolClass> classList;
    private List<SchoolClass> classListFull;
    private OnClassClickListener clickListener;

    public ClassAdapter(List<SchoolClass> classList) {
        this.classList = classList;
        this.classListFull = new ArrayList<>(classList);
        this.clickListener = null;
    }

    public ClassAdapter(List<SchoolClass> classList, OnClassClickListener clickListener) {
        this.classList = classList;
        this.classListFull = new ArrayList<>(classList);
        this.clickListener = clickListener;
    }

    @NonNull
    @Override
    public ClassViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_class, parent, false);
        return new ClassViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ClassViewHolder holder, int position) {
        SchoolClass currentClass = classList.get(position);

        String className = currentClass.getDisplayName() != null ? currentClass.getDisplayName() : currentClass.getClassId();
        holder.tvName.setText(className);

        String rawType = currentClass.getType();
        String displayType = "Unknown";
        String colorHex = "#9E9E9E"; // צבע אפור כברירת מחדל
        String letter = "C"; // אות ברירת מחדל

        if (rawType != null) {
            switch (rawType) {
                case "homeroom":
                    displayType = "Homeroom";
                    colorHex = "#4CAF50"; // ירוק
                    letter = "H";
                    break;
                case "math":
                    displayType = "Math";
                    colorHex = "#2196F3"; // כחול
                    letter = "M";
                    break;
                case "english":
                    displayType = "English";
                    colorHex = "#FF9800"; // כתום
                    letter = "E";
                    break;
                case "pe":
                    displayType = "Physical Education";
                    colorHex = "#E91E63"; // ורוד
                    letter = "P";
                    break;
                case "major a":
                    displayType = "Major A";
                    colorHex = "#9C27B0"; // סגול
                    letter = "A";
                    break;
                case "major b":
                    displayType = "Major B";
                    colorHex = "#00BCD4"; // תכלת
                    letter = "B";
                    break;
                default:
                    displayType = rawType;
                    letter = rawType.substring(0, 1).toUpperCase();
                    break;
            }
        }

        holder.tvType.setText("Type: " + displayType);

        // הגדרת הצבע והאות בעיגול
        holder.cardClassIcon.setCardBackgroundColor(Color.parseColor(colorHex));
        holder.tvClassIconLetter.setText(letter);

        holder.itemView.setOnClickListener(v -> {
            if (clickListener != null) {
                clickListener.onClassClick(currentClass);
            }
        });
    }

    @Override
    public int getItemCount() {
        return classList.size();
    }

    public void updateList(List<SchoolClass> newList) {
        this.classList = newList;
        this.classListFull = new ArrayList<>(newList);
        notifyDataSetChanged();
    }

    // ✨ הפונקציה המעודכנת שמקבלת גם טקסט לחיפוש וגם סוג מקצוע
    public void filter(String text, String typeFilter) {
        classList.clear();
        String query = text.toLowerCase().trim();

        for (SchoolClass item : classListFull) {
            boolean matchesSearch = true;
            boolean matchesType = true;

            // 1. בדיקת חיפוש טקסט
            if (!query.isEmpty()) {
                String name = item.getDisplayName() != null ? item.getDisplayName() : item.getClassId();
                if (name == null || !name.toLowerCase().contains(query)) {
                    matchesSearch = false;
                }
            }

            // 2. בדיקת סינון לפי סוג מקצוע
            if (!"All Types".equals(typeFilter)) {
                String rawType = item.getType() != null ? item.getType() : "";
                String mappedType = "Unknown";

                // מתרגם את מה שיש ב-DB למה שהמשתמש רואה בספינר
                switch (rawType) {
                    case "homeroom": mappedType = "Homeroom"; break;
                    case "math": mappedType = "Math"; break;
                    case "english": mappedType = "English"; break;
                    case "pe": mappedType = "Physical Education"; break;
                    case "major a": mappedType = "Major A"; break;
                    case "major b": mappedType = "Major B"; break;
                    default: mappedType = rawType; break;
                }

                if (!mappedType.equalsIgnoreCase(typeFilter)) {
                    matchesType = false;
                }
            }

            // אם השורה עומדת גם בחיפוש וגם בסינון, נוסיף אותה
            if (matchesSearch && matchesType) {
                classList.add(item);
            }
        }
        notifyDataSetChanged();
    }

    public static class ClassViewHolder extends RecyclerView.ViewHolder {
        TextView tvName, tvType, tvClassIconLetter;
        CardView cardClassIcon;

        public ClassViewHolder(@NonNull View itemView) {
            super(itemView);
            tvName = itemView.findViewById(R.id.tvClassName);
            tvType = itemView.findViewById(R.id.tvTeacherName);
            cardClassIcon = itemView.findViewById(R.id.cardClassIcon);
            tvClassIconLetter = itemView.findViewById(R.id.tvClassIconLetter);
        }
    }
}