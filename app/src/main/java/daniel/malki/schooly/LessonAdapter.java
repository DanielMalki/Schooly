package daniel.malki.schooly; // ודא שזה תואם לשם החבילה שלך

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.util.List;

public class LessonAdapter extends RecyclerView.Adapter<LessonAdapter.LessonViewHolder> {

    private List<Lesson> lessonList;
    private boolean isAdmin; // המשתנה שיקבע אם להציג את כפתור העריכה
    private OnLessonEditClickListener editClickListener;

    // ממשק (Interface) כדי שהאקטיביטי הראשי ידע מתי מנהל לחץ על כפתור העריכה
    public interface OnLessonEditClickListener {
        void onEditClick(Lesson lesson);
    }

    // קונסטרקטור
    public LessonAdapter(List<Lesson> lessonList, boolean isAdmin, OnLessonEditClickListener editClickListener) {
        this.lessonList = lessonList;
        this.isAdmin = isAdmin;
        this.editClickListener = editClickListener;
    }

    @NonNull
    @Override
    public LessonViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        // מחבר את קובץ העיצוב item_lesson לכל שורה ברשימה
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_lesson, parent, false);
        return new LessonViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull LessonViewHolder holder, int position) {
        Lesson lesson = lessonList.get(position);

        // משבץ את הנתונים לתוך ה-TextViews
        holder.tvHourNumber.setText(String.valueOf(lesson.getHourNumber()));
        holder.tvHourTime.setText(lesson.getStartTime() + "\n" + lesson.getEndTime());

        // בודק אם יש שיעור או שזה "חלון" (שיעור חופשי)
        if (lesson.getSubjectName() == null || lesson.getSubjectName().isEmpty()) {
            holder.tvSubjectName.setText("Free Lesson");
            holder.tvTeacherName.setText("-");
        } else {
            holder.tvSubjectName.setText(lesson.getSubjectName());
            holder.tvTeacherName.setText(lesson.getTeacherName());
        }

        // לוגיקת ההרשאות - קסם! ✨
        if (isAdmin) {
            holder.btnEditLesson.setVisibility(View.VISIBLE); // מציג את הכפתור למנהלים
            holder.btnEditLesson.setOnClickListener(v -> {
                if (editClickListener != null) {
                    editClickListener.onEditClick(lesson); // מעביר את הלחיצה לאקטיביטי
                }
            });
        } else {
            // אם זה תלמיד, הכפתור נעלם לחלוטין כאילו לא היה שם מעולם
            holder.btnEditLesson.setVisibility(View.GONE);
        }
    }

    @Override
    public int getItemCount() {
        return lessonList != null ? lessonList.size() : 0;
    }

    // המחלקה הפנימית שתופסת את רכיבי העיצוב
    public static class LessonViewHolder extends RecyclerView.ViewHolder {
        TextView tvHourNumber, tvHourTime, tvSubjectName, tvTeacherName;
        ImageView btnEditLesson;

        public LessonViewHolder(@NonNull View itemView) {
            super(itemView);
            tvHourNumber = itemView.findViewById(R.id.tvHourNumber);
            tvHourTime = itemView.findViewById(R.id.tvHourTime);
            tvSubjectName = itemView.findViewById(R.id.tvSubjectName);
            tvTeacherName = itemView.findViewById(R.id.tvTeacherName);
            btnEditLesson = itemView.findViewById(R.id.btnEditLesson);
        }
    }
}