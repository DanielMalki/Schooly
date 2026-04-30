package daniel.malki.schooly; // ודא שזה תואם לשם החבילה שלך

public class Lesson {
    private String id; // מזהה ייחודי בפיירסטור
    private int hourNumber; // מספר שיעור (1, 2, 3...)
    private String startTime; // "08:00"
    private String endTime; // "08:45"
    private String subjectName; // "מתמטיקה" או "שיעור חופשי"
    private String teacherName; // "ישראל ישראלי"
    private String type; // "class" (כיתתי) או "layer" (שכבתי - מתמטיקה/אנגלית/מגמה)

    // קונסטרקטור ריק בשביל פיירבייס (חובה!)
    public Lesson() {}

    public Lesson(int hourNumber, String startTime, String endTime, String subjectName, String teacherName, String type) {
        this.hourNumber = hourNumber;
        this.startTime = startTime;
        this.endTime = endTime;
        this.subjectName = subjectName;
        this.teacherName = teacherName;
        this.type = type;
    }

    // Getters and Setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public int getHourNumber() { return hourNumber; }
    public void setHourNumber(int hourNumber) { this.hourNumber = hourNumber; }
    public String getStartTime() { return startTime; }
    public void setStartTime(String startTime) { this.startTime = startTime; }
    public String getEndTime() { return endTime; }
    public void setEndTime(String endTime) { this.endTime = endTime; }
    public String getSubjectName() { return subjectName; }
    public void setSubjectName(String subjectName) { this.subjectName = subjectName; }
    public String getTeacherName() { return teacherName; }
    public void setTeacherName(String teacherName) { this.teacherName = teacherName; }
    public String getType() { return type; }
    public void setType(String type) { this.type = type; }
}