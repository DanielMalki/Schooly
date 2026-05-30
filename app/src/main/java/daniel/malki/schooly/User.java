package daniel.malki.schooly;

import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.PropertyName;

import java.util.List;
import java.util.Map;

public class User {
    private String userId; // למקרה של משתמשים ישנים שנוצרו בלי תעודת זהות
    private String tz;     // ✨ השדה החדש שמופה ישירות מהדאטהבייס!
    private String name;
    private String email;
    private int type;        // 0=תלמיד, 1=מורה, 2=מנהל בי"ס, 3=מנהל מערכת
    private DocumentReference schoolRef;
    private Map<String, DocumentReference> classes;
    private DocumentReference grade;
    private List<DocumentReference> teachableSubjects;

    public User() {}

    public User(String userId, String tz, String name, String email, int type, DocumentReference schoolRef) {
        this.userId = userId;
        this.tz = tz;
        this.name = name;
        this.email = email;
        this.type = type;
        this.schoolRef = schoolRef;
    }

    // ✨ חילוץ והגדרת תעודת הזהות
    @PropertyName("tz")
    public String getTz() { return tz; }
    @PropertyName("tz")
    public void setTz(String tz) { this.tz = tz; }

    // ✨ טריק חכם: אם מנסים למשוך את ה-ID עבור המסך הבא, נחזיר את תעודת הזהות (tz)
    public String getUserId() {
        if (userId != null && !userId.isEmpty()) return userId;
        if (tz != null && !tz.isEmpty()) return tz; // מחזיר תעודת זהות כגיבוי!
        return null;
    }
    public void setUserId(String userId) { this.userId = userId; }

    @PropertyName("name")
    public String getName() { return name; }
    @PropertyName("name")
    public void setName(String name) { this.name = name; }

    @PropertyName("email")
    public String getEmail() { return email; }
    @PropertyName("email")
    public void setEmail(String email) { this.email = email; }

    @PropertyName("type")
    public int getType() { return type; }
    @PropertyName("type")
    public void setType(int type) { this.type = type; }

    @PropertyName("school")
    public DocumentReference getSchoolRef() { return schoolRef; }
    @PropertyName("school")
    public void setSchoolRef(DocumentReference schoolRef) { this.schoolRef = schoolRef; }

    @PropertyName("classes")
    public Map<String, DocumentReference> getClasses() { return classes; }
    @PropertyName("classes")
    public void setClasses(Map<String, DocumentReference> classes) { this.classes = classes; }

    @PropertyName("grade")
    public DocumentReference getGrade() { return grade; }
    @PropertyName("grade")
    public void setGrade(DocumentReference grade) { this.grade = grade; }

    @PropertyName("teachableSubjects")
    public List<DocumentReference> getTeachableSubjects() { return teachableSubjects; }
    @PropertyName("teachableSubjects")
    public void setTeachableSubjects(List<DocumentReference> teachableSubjects) { this.teachableSubjects = teachableSubjects; }

    // הפונקציה שמגדירה מיהו תלמיד חריג
    @com.google.firebase.firestore.Exclude
    public boolean isExceptionStudent() {
        if (this.type != 0) return false;
        if (this.classes == null) return true;

        // בודק אם חסרה אחת מכיתות החובה
        return !classes.containsKey("homeroom") ||
                !classes.containsKey("math") ||
                !classes.containsKey("english") ||
                !classes.containsKey("sports");
    }
}