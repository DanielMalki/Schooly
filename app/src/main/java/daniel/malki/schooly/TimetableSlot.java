package daniel.malki.schooly;

import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.Exclude;
import com.google.firebase.firestore.PropertyName;

public class TimetableSlot {
    private String slotId; // מזהה המסמך ב-Firestore (Document ID)
    private DocumentReference school;
    private DocumentReference gradeRef;
    private DocumentReference classRef;
    private DocumentReference teacherRef;
    private DocumentReference subjectRef;
    private String day; // "Sunday", "Monday" ... "Saturday"
    private int hour; // מספר השיעור (1, 2, 3...)
    private String displayName; // מקצוע | שכבה + כיתה | מורה | סוג כיתה

    // קונסטרקטור ריק חובה עבור Firebase Architecture
    public TimetableSlot() {}

    public TimetableSlot(String slotId, DocumentReference school, DocumentReference gradeRef,
                         DocumentReference classRef, DocumentReference teacherRef,
                         DocumentReference subjectRef, String day, int hour, String displayName) {
        this.slotId = slotId;
        this.school = school;
        this.gradeRef = gradeRef;
        this.classRef = classRef;
        this.teacherRef = teacherRef;
        this.subjectRef = subjectRef;
        this.day = day;
        this.hour = hour;
        this.displayName = displayName;
    }

    // נחריג את ה-ID מהשמירה בתוך ה-Body של המסמך
    @Exclude
    public String getSlotId() { return slotId; }
    @Exclude
    public void setSlotId(String slotId) { this.slotId = slotId; }

    @PropertyName("school")
    public DocumentReference getSchool() { return school; }
    @PropertyName("school")
    public void setSchool(DocumentReference school) { this.school = school; }

    @PropertyName("gradeRef")
    public DocumentReference getGradeRef() { return gradeRef; }
    @PropertyName("gradeRef")
    public void setGradeRef(DocumentReference gradeRef) { this.gradeRef = gradeRef; }

    @PropertyName("classRef")
    public DocumentReference getClassRef() { return classRef; }
    @PropertyName("classRef")
    public void setClassRef(DocumentReference classRef) { this.classRef = classRef; }

    @PropertyName("teacherRef")
    public DocumentReference getTeacherRef() { return teacherRef; }
    @PropertyName("teacherRef")
    public void setTeacherRef(DocumentReference teacherRef) { this.teacherRef = teacherRef; }

    @PropertyName("subjectRef")
    public DocumentReference getSubjectRef() { return subjectRef; }
    @PropertyName("subjectRef")
    public void setSubjectRef(DocumentReference subjectRef) { this.subjectRef = subjectRef; }

    @PropertyName("day")
    public String getDay() { return day; }
    @PropertyName("day")
    public void setDay(String day) { this.day = day; }

    @PropertyName("hour")
    public int getHour() { return hour; }
    @PropertyName("hour")
    public void setHour(int hour) { this.hour = hour; }

    @PropertyName("displayName")
    public String getDisplayName() { return displayName; }
    @PropertyName("displayName")
    public void setDisplayName(String displayName) { this.displayName = displayName; }
}