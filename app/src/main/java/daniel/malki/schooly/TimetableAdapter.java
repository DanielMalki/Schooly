package daniel.malki.schooly;

import android.content.Context;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;
import java.util.ArrayList;
import java.util.List;

public class TimetableAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private static final int TYPE_FULL_VIEW = 1;
    private static final int TYPE_PERSONAL_VIEW = 2;

    private final Context context;
    private final int totalHours = 12;
    private List<TimetableSlot> allSlotsForDay = new ArrayList<>();
    private final int userType; // 0 = Student, 1 = Teacher, 2 = Admin, 3 = Schooly Admin
    private boolean isPersonalView = true;
    private final OnSlotClickListener listener;

    private final String[][] hourTimes = {
            {"08:00", "08:45"}, {"08:55", "09:40"}, {"10:00", "10:45"},
            {"10:55", "11:40"}, {"11:50", "12:35"}, {"12:45", "13:30"},
            {"13:50", "14:35"}, {"14:40", "15:25"}, {"15:30", "16:15"},
            {"16:20", "17:05"}, {"17:10", "17:55"}, {"18:00", "18:45"}
    };

    public interface OnSlotClickListener {
        void onAddSlotClick(int hour);
        void onExistingSlotClick(TimetableSlot slot);
    }

    public TimetableAdapter(Context context, int userType, OnSlotClickListener listener) {
        this.context = context;
        this.userType = userType;
        this.listener = listener;
    }

    public void setSlots(List<TimetableSlot> slots, boolean isPersonal) {
        this.allSlotsForDay = slots != null ? slots : new ArrayList<>();
        this.isPersonalView = isPersonal;
        notifyDataSetChanged();
    }

    @Override
    public int getItemViewType(int position) {
        return isPersonalView ? TYPE_PERSONAL_VIEW : TYPE_FULL_VIEW;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        if (viewType == TYPE_PERSONAL_VIEW) {
            View view = LayoutInflater.from(context).inflate(R.layout.item_lesson, parent, false);
            return new PersonalViewHolder(view);
        } else {
            View view = LayoutInflater.from(context).inflate(R.layout.item_timetable_hour, parent, false);
            return new FullViewHolder(view);
        }
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        int hourNumber = position + 1;

        if (holder instanceof PersonalViewHolder) {
            PersonalViewHolder pInHolder = (PersonalViewHolder) holder;
            pInHolder.tvHourNumber.setText(String.valueOf(hourNumber));
            pInHolder.tvHourTime.setText(hourTimes[position][0] + " - " + hourTimes[position][1]);

            List<TimetableSlot> slotsForHour = new ArrayList<>();
            for (TimetableSlot slot : allSlotsForDay) {
                if (slot.getHour() == hourNumber) {
                    slotsForHour.add(slot);
                }
            }

            if (!slotsForHour.isEmpty()) {
                StringBuilder combinedText = new StringBuilder();
                for (int i = 0; i < slotsForHour.size(); i++) {
                    combinedText.append(slotsForHour.get(i).getDisplayName());
                    if (i < slotsForHour.size() - 1) {
                        combinedText.append("\n──────────────────\n");
                    }
                }

                pInHolder.tvSubjectName.setText(combinedText.toString());
                pInHolder.tvTeacherName.setText("");
                pInHolder.btnEditLesson.setVisibility((userType == 2 || userType == 3) ? View.VISIBLE : View.GONE);

                pInHolder.btnEditLesson.setOnClickListener(v -> {
                    if (listener != null) listener.onExistingSlotClick(slotsForHour.get(0));
                });
            } else {
                pInHolder.tvSubjectName.setText("Free Period");
                pInHolder.tvTeacherName.setText("");
                pInHolder.btnEditLesson.setVisibility(View.GONE);
            }

        } else if (holder instanceof FullViewHolder) {
            FullViewHolder fHolder = (FullViewHolder) holder;
            fHolder.tvHourNumber.setText(String.valueOf(hourNumber));
            fHolder.layoutLessonsContainer.removeAllViews();

            List<TimetableSlot> slotsForThisHour = new ArrayList<>();
            for (TimetableSlot slot : allSlotsForDay) {
                if (slot.getHour() == hourNumber) {
                    slotsForThisHour.add(slot);
                }
            }

            if (!slotsForThisHour.isEmpty()) {
                for (TimetableSlot slot : slotsForThisHour) {
                    TextView lessonView = createLessonTextView(slot.getDisplayName());
                    if (userType == 2 || userType == 3) {
                        lessonView.setOnClickListener(v -> {
                            if (listener != null) listener.onExistingSlotClick(slot);
                        });
                    }
                    fHolder.layoutLessonsContainer.addView(lessonView);
                }
            } else {
                if (userType == 2 || userType == 3) {
                    MaterialButton addButton = createAddButton();
                    addButton.setOnClickListener(v -> {
                        if (listener != null) listener.onAddSlotClick(hourNumber);
                    });
                    fHolder.layoutLessonsContainer.addView(addButton);
                } else {
                    TextView freeView = new TextView(context);
                    freeView.setText("- No Lessons -");
                    freeView.setTextColor(Color.parseColor("#BDBDBD"));
                    freeView.setPadding(16, 16, 16, 16);
                    fHolder.layoutLessonsContainer.addView(freeView);
                }
            }
        }
    }

    @Override
    public int getItemCount() {
        return totalHours;
    }

    private TextView createLessonTextView(String text) {
        TextView textView = new TextView(context);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
        params.setMargins(0, 4, 0, 4);
        textView.setLayoutParams(params);

        textView.setText(text);
        // טקסט כחול קריא ויפה
        textView.setTextColor(Color.parseColor("#1E88E5"));
        textView.setTextSize(14);

        // יצירת רקע תכלת בהיר ועדין עם פינות מעוגלות ישירות בקוד (בלי קבצים חיצוניים)
        android.graphics.drawable.GradientDrawable shape = new android.graphics.drawable.GradientDrawable();
        shape.setShape(android.graphics.drawable.GradientDrawable.RECTANGLE);
        shape.setCornerRadius(24f); // פינות עגולות ויפות
        shape.setColor(Color.parseColor("#E3F2FD")); // צבע תכלת פסטלי ורך
        textView.setBackground(shape);

        textView.setPadding(24, 20, 24, 20);
        return textView;
    }

    private MaterialButton createAddButton() {
        MaterialButton button = new MaterialButton(context, null, com.google.android.material.R.attr.materialButtonStyle);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
        params.setMargins(0, 4, 0, 4);
        button.setLayoutParams(params);
        button.setText("+ Add Lesson");
        button.setTextSize(14);
        button.setCornerRadius(20);
        button.setAllCaps(false);

        // התיקון העיצובי שמחזיר את האלגנטיות:
        button.setTextColor(Color.parseColor("#757575")); // טקסט אפור
        // רקע אפור בהיר ועדין במקום הסגול האטום
        button.setBackgroundTintList(android.content.res.ColorStateList.valueOf(Color.parseColor("#F5F5F5")));
        // ביטול הצל התלת-מימדי שהפך את זה למכוער!
        button.setStateListAnimator(null);

        button.setPadding(24, 12, 24, 12);
        return button;
    }

    static class FullViewHolder extends RecyclerView.ViewHolder {
        TextView tvHourNumber;
        LinearLayout layoutLessonsContainer;

        public FullViewHolder(@NonNull View itemView) {
            super(itemView);
            tvHourNumber = itemView.findViewById(R.id.tvHourNumber);
            layoutLessonsContainer = itemView.findViewById(R.id.layoutLessonsContainer);
        }
    }

    static class PersonalViewHolder extends RecyclerView.ViewHolder {
        TextView tvHourNumber, tvHourTime, tvSubjectName, tvTeacherName;
        ImageView btnEditLesson;

        public PersonalViewHolder(@NonNull View itemView) {
            super(itemView);
            tvHourNumber = itemView.findViewById(R.id.tvHourNumber);
            tvHourTime = itemView.findViewById(R.id.tvHourTime);
            tvSubjectName = itemView.findViewById(R.id.tvSubjectName);
            tvTeacherName = itemView.findViewById(R.id.tvTeacherName);
            btnEditLesson = itemView.findViewById(R.id.btnEditLesson);
        }
    }
}