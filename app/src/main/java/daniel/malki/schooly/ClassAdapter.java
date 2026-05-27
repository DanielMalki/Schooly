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
        SchoolClass currentItem = classList.get(position);

        holder.tvName.setText(currentItem.getDisplayName());

        String rawType = currentItem.getType() != null ? currentItem.getType() : "unknown";
        String capType = rawType;
        if (rawType.length() > 0) {
            capType = rawType.substring(0, 1).toUpperCase() + rawType.substring(1);
        }

        // ✨ שימוש בשדה החדש ששמור ישירות בתוך האובייקט (אין יותר צורך ב-gradeCache)
        String gradeName = currentItem.getGradeNameForFilter();
        if (gradeName == null || gradeName.isEmpty()) {
            gradeName = "Unknown";
        }

        // יצירת המשפט המלא
        String detailsText = capType + ", " + gradeName + " grade, class " + currentItem.getDisplayName() + ".";
        holder.tvClassDetails.setText(detailsText);

        String firstLetter = currentItem.getDisplayName() != null && !currentItem.getDisplayName().isEmpty() ?
                currentItem.getDisplayName().substring(0, 1).toUpperCase() : "C";
        holder.tvClassIconLetter.setText(firstLetter);

        String[] colors = {"#1976D2", "#D32F2F", "#388E3C", "#FBC02D", "#8E24AA", "#F57C00"};
        int colorIndex = position % colors.length;
        holder.cardClassIcon.setCardBackgroundColor(Color.parseColor(colors[colorIndex]));
    }

    @Override
    public int getItemCount() {
        return classList.size();
    }

    public void updateList(List<SchoolClass> newList) {
        List<SchoolClass> temp = new ArrayList<>(newList);
        this.classList.clear();
        this.classList.addAll(temp);
        this.classListFull = new ArrayList<>(temp);
        notifyDataSetChanged();
    }

    // הפילטר עכשיו מקבל את שם השכבה ומסנן לפיו
    public void filter(String text, String typeFilter, String gradeFilterName) {
        classList.clear();

        boolean isSearchEmpty = text.trim().isEmpty();
        boolean isTypeAll = typeFilter.equals("All Types");
        boolean isGradeAll = gradeFilterName.equals("All Grades") || gradeFilterName.isEmpty();

        if (isSearchEmpty && isTypeAll && isGradeAll) {
            classList.addAll(classListFull);
        } else {
            text = text.toLowerCase();
            for (SchoolClass item : classListFull) {
                boolean matchesSearch = item.getDisplayName() != null && item.getDisplayName().toLowerCase().contains(text);
                boolean matchesType = true;
                boolean matchesGrade = true;

                if (!isTypeAll) {
                    String rawType = item.getType() != null ? item.getType().toLowerCase() : "";
                    String mappedType = "";

                    switch (rawType) {
                        case "homeroom": mappedType = "Homeroom"; break;
                        case "math": mappedType = "Math"; break;
                        case "english": mappedType = "English"; break;
                        case "sports": mappedType = "Physical Education"; break;
                        case "pe": mappedType = "Physical Education"; break;
                        case "major a": mappedType = "Major A"; break;
                        case "major b": mappedType = "Major B"; break;
                        default: mappedType = rawType; break;
                    }

                    if (!mappedType.equalsIgnoreCase(typeFilter)) {
                        matchesType = false;
                    }
                }

                // ✨ סינון לפי השכבה שנמצאת באובייקט עצמו
                if (!isGradeAll) {
                    String itemGradeName = item.getGradeNameForFilter();
                    if (itemGradeName == null || !itemGradeName.equals(gradeFilterName)) {
                        matchesGrade = false;
                    }
                }

                if (matchesSearch && matchesType && matchesGrade) {
                    classList.add(item);
                }
            }
        }
        notifyDataSetChanged();
    }

    public class ClassViewHolder extends RecyclerView.ViewHolder {
        TextView tvName, tvClassDetails, tvClassIconLetter;
        CardView cardClassIcon;

        public ClassViewHolder(@NonNull View itemView) {
            super(itemView);
            tvName = itemView.findViewById(R.id.tvClassName);
            tvClassDetails = itemView.findViewById(R.id.tvClassDetails);
            cardClassIcon = itemView.findViewById(R.id.cardClassIcon);
            tvClassIconLetter = itemView.findViewById(R.id.tvClassIconLetter);

            itemView.setOnClickListener(v -> {
                if (clickListener != null && getAdapterPosition() != RecyclerView.NO_POSITION) {
                    clickListener.onClassClick(classList.get(getAdapterPosition()));
                }
            });
        }
    }
}