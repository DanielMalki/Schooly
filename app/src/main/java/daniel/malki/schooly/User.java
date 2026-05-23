package daniel.malki.schooly;

import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.PropertyName;
import java.util.Map;

public class User {
    private String userId;
    private String name;
    private String email;
    private int type;        // 0=תלמיד, 1=מורה, 2=מנהל בי"ס, 3=מנהל מערכת
    private DocumentReference schoolRef; // הרפרנס החדש לבית הספר!
    private Map<String, DocumentReference> classes;

    public User() {}

    public User(String userId, String name, String email, int type, DocumentReference schoolRef) {
        this.userId = userId;
        this.name = name;
        this.email = email;
        this.type = type;
        this.schoolRef = schoolRef;
    }

    // Getters ו-Setters הקיימים...
    public String getUserId() { return userId; }
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

    // הגטר והסאטר החדשים לשדה בית הספר
    @PropertyName("schoolRef")
    public DocumentReference getSchoolRef() { return schoolRef; }
    @PropertyName("schoolRef")
    public void setSchoolRef(DocumentReference schoolRef) { this.schoolRef = schoolRef; }

    @PropertyName("classes")
    public Map<String, DocumentReference> getClasses() { return classes; }
    @PropertyName("classes")
    public void setClasses(Map<String, DocumentReference> classes) { this.classes = classes; }

    // ✨ הפונקציה שמגדירה מיהו תלמיד חריג (מתעלמת מהשמירה לדאטהבייס)
    @com.google.firebase.firestore.Exclude
    public boolean isExceptionStudent() {
        if (this.type != 0) return false; // מורים ומנהלים לא יכולים להיות תלמידים חריגים
        if (this.classes == null) return true; // אם אין לו כיתות בכלל, הוא חריג

        // בודק אם חסרה אחת מכיתות החובה
        return !classes.containsKey("homeroom") ||
                !classes.containsKey("math") ||
                !classes.containsKey("english") ||
                !classes.containsKey("sports");
    }
}