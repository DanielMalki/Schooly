package daniel.malki.schooly;

import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.PropertyName;
import java.util.List;

public class SchoolClass {
    private String classId; // מזהה המסמך (Document ID)
    private String displayName;
    private String type;
    private List<CourseAssignment> courseAssignments;

    // קונסטרקטור ריק חובה לפיירבייס
    public SchoolClass() {}

    public SchoolClass(String classId, String displayName, String type, List<CourseAssignment> courseAssignments) {
        this.classId = classId;
        this.displayName = displayName;
        this.type = type;
        this.courseAssignments = courseAssignments;
    }

    // Getters & Setters
    public String getClassId() { return classId; }
    public void setClassId(String classId) { this.classId = classId; }

    @PropertyName("displayName")
    public String getDisplayName() { return displayName; }
    @PropertyName("displayName")
    public void setDisplayName(String displayName) { this.displayName = displayName; }

    @PropertyName("type")
    public String getType() { return type; }
    @PropertyName("type")
    public void setType(String type) { this.type = type; }

    @PropertyName("course_assignments")
    public List<CourseAssignment> getCourseAssignments() { return courseAssignments; }
    @PropertyName("course_assignments")
    public void setCourseAssignments(List<CourseAssignment> courseAssignments) { this.courseAssignments = courseAssignments; }

    /**
     * מחלקה פנימית המייצגת איבר בודד בתוך מערך ה-course_assignments
     */
    public static class CourseAssignment {
        private DocumentReference subject;
        private DocumentReference teacher;

        public CourseAssignment() {}

        public CourseAssignment(DocumentReference subject, DocumentReference teacher) {
            this.subject = subject;
            this.teacher = teacher;
        }

        public DocumentReference getSubject() { return subject; }
        public void setSubject(DocumentReference subject) { this.subject = subject; }

        public DocumentReference getTeacher() { return teacher; }
        public void setTeacher(DocumentReference teacher) { this.teacher = teacher; }
    }
}