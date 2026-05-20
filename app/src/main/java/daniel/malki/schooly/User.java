package daniel.malki.schooly;

import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.PropertyName;

public class User {
    private String userId;
    private String name;
    private String email;
    private int type;        // 0=תלמיד, 1=מורה, 2=מנהל בי"ס, 3=מנהל מערכת
    private DocumentReference schoolRef; // הרפרנס החדש לבית הספר!

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
}