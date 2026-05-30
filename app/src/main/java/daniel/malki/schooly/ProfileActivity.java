package daniel.malki.schooly;

import android.app.AlertDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;

import com.bumptech.glide.Glide;
import com.google.firebase.firestore.Blob;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

public class ProfileActivity extends BaseMenuActivity {

    private ImageView imgAvatar;
    private TextView tvFullName, tvRole, tvSchoolName;

    private int userType = 0;

    private com.google.android.material.card.MaterialCardView cardAcademicInfo;
    private TextView tvAcademicHeader;
    private LinearLayout layoutDynamicInfoContainer;

    private LinearLayout personalContainer;
    private LinearLayout academicContainer;

    private FirebaseFirestore db;
    private ActivityResultLauncher<Intent> galleryLauncher;
    private ActivityResultLauncher<Void> cameraLauncher;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setTitle("Profile");

        db = FirebaseFirestore.getInstance();

        imgAvatar = findViewById(R.id.imgAvatar);
        tvFullName = findViewById(R.id.tvFullName);
        tvRole = findViewById(R.id.tvRole);
        tvSchoolName = findViewById(R.id.tvSchoolName);

        cardAcademicInfo = findViewById(R.id.cardAcademicInfo);
        tvAcademicHeader = findViewById(R.id.tvAcademicHeader);
        layoutDynamicInfoContainer = findViewById(R.id.layoutDynamicInfoContainer);

        galleryLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                        Uri selectedImageUri = result.getData().getData();
                        imgAvatar.setImageURI(selectedImageUri);
                        try {
                            Bitmap bitmap = MediaStore.Images.Media.getBitmap(this.getContentResolver(), selectedImageUri);
                            saveImageAsBlob(bitmap);
                        } catch (IOException e) {
                            Toast.makeText(this, "Failed to read image", Toast.LENGTH_SHORT).show();
                        }
                    }
                }
        );

        cameraLauncher = registerForActivityResult(
                new ActivityResultContracts.TakePicturePreview(),
                bitmap -> {
                    if (bitmap != null) {
                        imgAvatar.setImageBitmap(bitmap);
                        saveImageAsBlob(bitmap);
                    }
                }
        );

        imgAvatar.setOnClickListener(v -> showImageSourceDialog());
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadUserData();
    }

    private void showImageSourceDialog() {
        String[] options = {"Open Camera 📷", "Choose from Gallery 🖼️", "Remove Picture 🗑️", "Cancel ❌"};
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Update Profile Picture");
        builder.setItems(options, (dialog, which) -> {
            if (which == 0) cameraLauncher.launch(null);
            else if (which == 1) {
                Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
                galleryLauncher.launch(intent);
            } else if (which == 2) removeProfilePicture();
            else dialog.dismiss();
        });
        builder.show();
    }

    private void saveImageAsBlob(Bitmap originalBitmap) {
        String userId = getSharedPreferences("SchoolyPrefs", MODE_PRIVATE).getString("userId", "");
        if (userId.isEmpty()) return;

        int maxWidth = 400;
        int maxHeight = 400;
        float scale = Math.min(((float)maxWidth / originalBitmap.getWidth()), ((float)maxHeight / originalBitmap.getHeight()));
        Bitmap resizedBitmap = Bitmap.createScaledBitmap(originalBitmap, (int)(originalBitmap.getWidth() * scale), (int)(originalBitmap.getHeight() * scale), true);

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        resizedBitmap.compress(Bitmap.CompressFormat.JPEG, 70, baos);
        byte[] data = baos.toByteArray();

        Blob blob = Blob.fromBytes(data);

        db.collection("users").document(userId).update("profileImageBlob", blob)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(this, "Picture saved perfectly! 🚀", Toast.LENGTH_SHORT).show();
                    loadUserData();
                });
    }

    private void removeProfilePicture() {
        String userId = getSharedPreferences("SchoolyPrefs", MODE_PRIVATE).getString("userId", "");
        if (userId.isEmpty()) return;

        db.collection("users").document(userId).update("profileImageBlob", FieldValue.delete())
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(this, "Picture removed successfully", Toast.LENGTH_SHORT).show();
                    Glide.with(this).clear(imgAvatar);
                    imgAvatar.setImageResource(android.R.drawable.sym_def_app_icon);
                });
    }

    private void loadUserData() {
        SharedPreferences prefs = getSharedPreferences("SchoolyPrefs", MODE_PRIVATE);
        String fullName = prefs.getString("userName", "Guest");
        String id = prefs.getString("userId", "");

        userType = prefs.getInt("userType", 0);

        tvFullName.setText(fullName);
        tvRole.setText(getRoleName(userType));

        if (!id.isEmpty()) {
            db.collection("users").document(id).get().addOnSuccessListener(documentSnapshot -> {
                if (!documentSnapshot.exists()) return;

                if (documentSnapshot.getBlob("profileImageBlob") != null) {
                    byte[] imageBytes = documentSnapshot.getBlob("profileImageBlob").toBytes();
                    com.bumptech.glide.Glide.with(this)
                            .load(imageBytes)
                            .circleCrop()
                            .into(imgAvatar);
                } else {
                    imgAvatar.setImageResource(android.R.drawable.sym_def_app_icon);
                }

                if (userType != 3) {
                    DocumentReference schoolRef = documentSnapshot.getDocumentReference("school");
                    if (schoolRef != null) {
                        schoolRef.get().addOnSuccessListener(schoolDoc -> {
                            if (schoolDoc.exists()) {
                                tvSchoolName.setVisibility(View.VISIBLE);
                                tvSchoolName.setText("🏫 " + schoolDoc.getString("displayName"));
                            }
                        });
                    } else {
                        tvSchoolName.setText("No School Assigned");
                    }
                } else {
                    tvSchoolName.setVisibility(View.GONE);
                }

                if (layoutDynamicInfoContainer != null) {
                    layoutDynamicInfoContainer.removeAllViews();

                    personalContainer = new LinearLayout(ProfileActivity.this);
                    personalContainer.setOrientation(LinearLayout.VERTICAL);
                    layoutDynamicInfoContainer.addView(personalContainer);

                    View divider = new View(ProfileActivity.this);
                    LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 2);
                    params.setMargins(0, 30, 0, 30);
                    divider.setLayoutParams(params);
                    divider.setBackgroundColor(android.graphics.Color.parseColor("#E0E0E0"));
                    layoutDynamicInfoContainer.addView(divider);

                    academicContainer = new LinearLayout(ProfileActivity.this);
                    academicContainer.setOrientation(LinearLayout.VERTICAL);
                    layoutDynamicInfoContainer.addView(academicContainer);
                }

                // 👤 הצגת הפרטים האישיים מופרדים לאנגלית בלבד
                addHeaderToContainer(personalContainer, "👤 Personal Information");
                String fName = documentSnapshot.getString("firstName");
                String mName = documentSnapshot.getString("middleName");
                String lName = documentSnapshot.getString("lastName");
                String email = documentSnapshot.getString("email");

                addRowToContainer(personalContainer, "ID Number", id);
                addRowToContainer(personalContainer, "First Name", fName != null ? fName : "Not provided");

                // הוספת שם שני רק אם קיים נתון אמיתי
                if (mName != null && !mName.trim().isEmpty()) {
                    addRowToContainer(personalContainer, "Middle Name", mName);
                }

                addRowToContainer(personalContainer, "Last Name", lName != null ? lName : "Not provided");
                addRowToContainer(personalContainer, "Email Address", email != null ? email : "Not provided");

                cardAcademicInfo.setVisibility(View.VISIBLE);
                tvAcademicHeader.setText("Profile Details");

                if (userType == 0) {
                    addHeaderToContainer(academicContainer, "🎓 My Learning Groups");
                    fetchStudentCourses(id, documentSnapshot);
                } else if (userType == 1 || userType == 2) {
                    addHeaderToContainer(academicContainer, "🎓 My Teachable Subjects");
                    fetchTeacherSubjects(id);
                } else if (userType == 3) {
                    addHeaderToContainer(academicContainer, "👑 System Overseer Info");
                    addNoDataTextView(academicContainer, "Global Application Administrator\nYou hold full access rights over all schools, global subjects, and system accounts.");
                }

            }).addOnFailureListener(e -> {
                android.util.Log.e("ProfileActivity", "Error loading data: " + e.getMessage());
            });
        }
    }

    /* ---------------- לוגיקת תלמיד ---------------- */
    private void fetchStudentCourses(String studentId, com.google.firebase.firestore.DocumentSnapshot documentSnapshot) {

        // שליפת השכבה
        DocumentReference gradeRef = documentSnapshot.getDocumentReference("grade");
        if (gradeRef != null) {
            gradeRef.get().addOnCompleteListener(task -> {
                String gradeName = "Unknown";
                if (task.isSuccessful() && task.getResult() != null && task.getResult().exists()) {
                    gradeName = task.getResult().getString("displayName");
                    if (gradeName == null) gradeName = task.getResult().getString("name");
                }
                addRowToContainer(layoutDynamicInfoContainer, "📅 Grade", gradeName != null ? gradeName : "Unknown");
            });
        } else {
            addRowToContainer(layoutDynamicInfoContainer, "📅 Grade", "Not assigned");
        }

        Map<String, Object> classesMap = null;
        try {
            classesMap = (Map<String, Object>) documentSnapshot.get("classes");
        } catch (Exception e) { }

        if (classesMap == null || classesMap.isEmpty()) {
            addNoDataTextView(layoutDynamicInfoContainer, "Not assigned to any learning groups yet.");
            return;
        }

        List<Map.Entry<String, Object>> validEntries = new ArrayList<>();
        for (Map.Entry<String, Object> entry : classesMap.entrySet()) {
            if (entry.getValue() instanceof DocumentReference) {
                validEntries.add(entry);
            }
        }

        final int totalValidClasses = validEntries.size();
        if (totalValidClasses == 0) {
            addNoDataTextView(layoutDynamicInfoContainer, "No valid learning groups found.");
            return;
        }

        List<StudentCourseItem> fetchedCourses = new ArrayList<>();

        for (Map.Entry<String, Object> entry : validEntries) {
            String classType = entry.getKey();
            DocumentReference classRef = (DocumentReference) entry.getValue();

            classRef.get().addOnCompleteListener(classTask -> {
                if (!classTask.isSuccessful() || classTask.getResult() == null || !classTask.getResult().exists()) {
                    fetchedCourses.add(new StudentCourseItem(classType, "Unknown Class", new ArrayList<>()));
                    checkAndRenderStudentCourses(fetchedCourses, totalValidClasses);
                    return;
                }

                com.google.firebase.firestore.DocumentSnapshot classDoc = classTask.getResult();

                String tempClassName = classDoc.getString("displayName");
                if (tempClassName == null) {
                    tempClassName = classDoc.getString("name");
                }
                final String className = (tempClassName != null) ? tempClassName : "Unknown Class";

                List<Map<String, Object>> assignments = null;
                try {
                    assignments = (List<Map<String, Object>>) classDoc.get("courseAssignments");
                } catch (Exception e) { }

                if (assignments == null || assignments.isEmpty()) {
                    fetchedCourses.add(new StudentCourseItem(classType, className, new ArrayList<>()));
                    checkAndRenderStudentCourses(fetchedCourses, totalValidClasses);
                    return;
                }

                List<String> resolvedAssignments = new ArrayList<>();
                final int[] pendingRequests = {assignments.size()};

                for (Map<String, Object> assignment : assignments) {
                    if (assignment == null) {
                        pendingRequests[0]--;
                        if (pendingRequests[0] == 0) {
                            fetchedCourses.add(new StudentCourseItem(classType, className, resolvedAssignments));
                            checkAndRenderStudentCourses(fetchedCourses, totalValidClasses);
                        }
                        continue;
                    }

                    // הגנה: חיפוש המפתח גם אם כתבו אותו בטעות עם אות גדולה
                    Object subObj = assignment.get("subject");
                    if (subObj == null) subObj = assignment.get("Subject");

                    Object tObj = assignment.get("teacher");
                    if (tObj == null) tObj = assignment.get("Teacher");

                    DocumentReference subRef = (subObj instanceof DocumentReference) ? (DocumentReference) subObj : null;
                    DocumentReference teacherRef = (tObj instanceof DocumentReference) ? (DocumentReference) tObj : null;

                    if (subRef != null && teacherRef != null) {
                        final DocumentReference finalTeacherRef = teacherRef;

                        subRef.get().addOnCompleteListener(subTask -> {
                            String subName = "Unknown Subject";
                            if (subTask.isSuccessful() && subTask.getResult() != null && subTask.getResult().exists()) {
                                subName = subTask.getResult().getString("displayName");
                                if (subName == null) subName = subTask.getResult().getString("name");
                            }
                            final String finalSubName = subName != null ? subName : "Unknown Subject";

                            finalTeacherRef.get().addOnCompleteListener(teacherTask -> {
                                String tFullName = "Unknown Teacher";
                                if (teacherTask.isSuccessful() && teacherTask.getResult() != null && teacherTask.getResult().exists()) {

                                    // ✨ התיקון שלך: קודם כל ולפני הכל משתמשים בשם המלא
                                    String tName = teacherTask.getResult().getString("name");

                                    if (tName != null && !tName.trim().isEmpty()) {
                                        tFullName = tName.trim();
                                    } else {
                                        // שימוש בשם משפחה ופרטי רק אם שם מלא לא נמצא
                                        String tFirstName = teacherTask.getResult().getString("firstName");
                                        String tLastName = teacherTask.getResult().getString("lastName");
                                        tFullName = ((tFirstName != null ? tFirstName : "") + " " + (tLastName != null ? tLastName : "")).trim();
                                    }

                                    if (tFullName.isEmpty()) tFullName = "Unknown Teacher";
                                }

                                resolvedAssignments.add(finalSubName + ": " + tFullName);

                                pendingRequests[0]--;
                                if (pendingRequests[0] == 0) {
                                    fetchedCourses.add(new StudentCourseItem(classType, className, resolvedAssignments));
                                    checkAndRenderStudentCourses(fetchedCourses, totalValidClasses);
                                }
                            });
                        });
                    } else {
                        pendingRequests[0]--;
                        if (pendingRequests[0] == 0) {
                            fetchedCourses.add(new StudentCourseItem(classType, className, resolvedAssignments));
                            checkAndRenderStudentCourses(fetchedCourses, totalValidClasses);
                        }
                    }
                }
            });
        }
    }

    private void checkAndRenderStudentCourses(List<StudentCourseItem> fetchedCourses, int totalExpected) {
        if (fetchedCourses.size() != totalExpected) return;

        Collections.sort(fetchedCourses, (item1, item2) -> {
            boolean isHome1 = "homeroom".equals(item1.classType);
            boolean isHome2 = "homeroom".equals(item2.classType);

            if (isHome1 && !isHome2) return -1;
            if (!isHome1 && isHome2) return 1;

            String name1 = item1.className != null ? item1.className : "";
            String name2 = item2.className != null ? item2.className : "";
            return name1.compareToIgnoreCase(name2);
        });

        for (StudentCourseItem item : fetchedCourses) {
            // בונים את הטקסט של השורות (שם הכיתה ואז המקצועות)
            StringBuilder valueBuilder = new StringBuilder(item.className != null ? item.className : "");

            if (item.assignments != null && !item.assignments.isEmpty()) {
                for (String assignmentInfo : item.assignments) {
                    valueBuilder.append("\n• ").append(assignmentInfo);
                }
            } else {
                valueBuilder.append("\n• (No valid subject/teacher pairs found)");
            }

            // בודקים איזו כותרת לשים בהתאם לסוג הכיתה
            if ("homeroom".equals(item.classType)) {
                addRowToContainer(layoutDynamicInfoContainer, "🏠 Homeroom", valueBuilder.toString());
            } else {
                String formattedType = item.classType;
                if (formattedType != null && formattedType.length() > 0) {
                    formattedType = formattedType.substring(0, 1).toUpperCase() + formattedType.substring(1);
                }
                addRowToContainer(layoutDynamicInfoContainer, "📚 " + formattedType, valueBuilder.toString());
            }
        }
    }

    /* ---------------- לוגיקת מורה ---------------- */
    private void fetchTeacherSubjects(String teacherId) {
        DocumentReference teacherRef = db.collection("users").document(teacherId);

        teacherRef.get().addOnSuccessListener(documentSnapshot -> {
            if (!documentSnapshot.exists()) return;

            List<DocumentReference> subjectRefs = (List<DocumentReference>) documentSnapshot.get("teachableSubjects");

            if (subjectRefs == null || subjectRefs.isEmpty()) {
                if (userType == 2) {
                    addNoDataTextView(academicContainer, "💼 School Staff Member\nUse the side menu to access administrative tools and manage users.");
                } else {
                    addNoDataTextView(academicContainer, "No subjects assigned to you yet.");
                }
                return;
            }

            List<com.google.firebase.firestore.DocumentSnapshot> fetchedSubjects = new ArrayList<>();
            final int totalSubjects = subjectRefs.size();

            for (DocumentReference subRef : subjectRefs) {
                subRef.get().addOnSuccessListener(subDoc -> {
                    if (subDoc.exists()) {
                        fetchedSubjects.add(subDoc);
                    }

                    if (fetchedSubjects.size() == totalSubjects) {
                        Collections.sort(fetchedSubjects, (doc1, doc2) -> {
                            String name1 = doc1.getString("displayName");
                            String name2 = doc2.getString("displayName");
                            if (name1 == null) name1 = "";
                            if (name2 == null) name2 = "";
                            return name1.compareToIgnoreCase(name2);
                        });

                        for (com.google.firebase.firestore.DocumentSnapshot sortedSubDoc : fetchedSubjects) {
                            String subjectName = sortedSubDoc.getString("displayName");

                            LinearLayout subjectBlock = new LinearLayout(this);
                            subjectBlock.setOrientation(LinearLayout.VERTICAL);

                            addSubjectHeaderToBlock(subjectBlock, "📖 " + subjectName);
                            academicContainer.addView(subjectBlock);

                            findClassesForTeacherAndSubject(teacherRef, sortedSubDoc.getReference(), subjectBlock);
                        }
                    }
                });
            }
        });
    }

    private void findClassesForTeacherAndSubject(DocumentReference teacherRef, DocumentReference subRef, LinearLayout subjectBlock) {
        db.collection("classes").get().addOnSuccessListener(queryDocumentSnapshots -> {
            boolean foundAnyClass = false;

            for (QueryDocumentSnapshot classDoc : queryDocumentSnapshots) {
                List<Map<String, Object>> assignments = (List<Map<String, Object>>) classDoc.get("courseAssignments");

                if (assignments != null) {
                    for (Map<String, Object> assignment : assignments) {
                        DocumentReference assignedTeacherRef = (DocumentReference) assignment.get("teacher");
                        DocumentReference assignedSubRef = (DocumentReference) assignment.get("subject");

                        if (assignedTeacherRef != null && assignedTeacherRef.getPath().equals(teacherRef.getPath()) &&
                                assignedSubRef != null && assignedSubRef.getPath().equals(subRef.getPath())) {

                            foundAnyClass = true;
                            String className = classDoc.getString("displayName");
                            DocumentReference classRef = classDoc.getReference();

                            View classRow = createClassRowView("    Class: " + className, "Tap to view student roster 👥");
                            classRow.setOnClickListener(v -> showStudentsForClassDialog(className, classRef));

                            subjectBlock.addView(classRow);
                        }
                    }
                }
            }

            if (!foundAnyClass) {
                TextView tvNoData = new TextView(this);
                tvNoData.setText("    No active classes assigned for this subject yet.");
                tvNoData.setTextSize(13f);
                tvNoData.setTextColor(android.graphics.Color.GRAY);
                tvNoData.setPadding(10, 5, 10, 5);
                subjectBlock.addView(tvNoData);
            }
        });
    }

    private void showStudentsForClassDialog(String className, DocumentReference classRef) {
        db.collection("users")
                .whereEqualTo("type", 0)
                .get()
                .addOnSuccessListener(userDocs -> {
                    List<StudentHolder> studentsList = new ArrayList<>();

                    for (QueryDocumentSnapshot userDoc : userDocs) {
                        Map<String, Object> classesMap = (Map<String, Object>) userDoc.get("classes");
                        if (classesMap != null) {
                            for (Object value : classesMap.values()) {
                                if (value instanceof DocumentReference) {
                                    DocumentReference sClassRef = (DocumentReference) value;
                                    if (sClassRef.getPath().equals(classRef.getPath())) {
                                        String name = userDoc.getString("name");
                                        String lastName = userDoc.getString("lastName");
                                        if (name != null) {
                                            studentsList.add(new StudentHolder(name, lastName != null ? lastName : ""));
                                        }
                                    }
                                }
                            }
                        }
                    }

                    if (studentsList.isEmpty()) {
                        new AlertDialog.Builder(this)
                                .setTitle(className + " - Student Roster")
                                .setMessage("No students enrolled in this class yet.")
                                .setPositiveButton("Close", null).show();
                        return;
                    }

                    Collections.sort(studentsList, (s1, s2) -> s1.lastName.compareToIgnoreCase(s2.lastName));

                    StringBuilder builder = new StringBuilder();
                    for (int i = 0; i < studentsList.size(); i++) {
                        builder.append((i + 1)).append(". ").append(studentsList.get(i).fullName).append("\n");
                    }

                    new AlertDialog.Builder(this)
                            .setTitle(className + " - Student Roster (" + studentsList.size() + " students)")
                            .setMessage(builder.toString())
                            .setPositiveButton("Close", null)
                            .show();
                });
    }

    private static class StudentHolder {
        String fullName;
        String lastName;
        StudentHolder(String fullName, String lastName) {
            this.fullName = fullName;
            this.lastName = lastName;
        }
    }

    private static class StudentCourseItem {
        String classType;
        String className;
        List<String> assignments;

        StudentCourseItem(String classType, String className, List<String> assignments) {
            this.classType = classType;
            this.className = className;
            this.assignments = assignments;
        }
    }

    // --- מתודות עזר לעיצוב הדינמי ---

    private void addHeaderToContainer(LinearLayout container, String text) {
        TextView tv = new TextView(this);
        tv.setText(text);
        tv.setTextSize(17f);
        tv.setTypeface(null, android.graphics.Typeface.BOLD);
        tv.setTextColor(android.graphics.Color.parseColor("#1A237E"));
        tv.setPadding(10, 25, 10, 10);
        container.addView(tv);
    }

    private void addSubjectHeaderToBlock(LinearLayout subjectBlock, String subjectText) {
        TextView tv = new TextView(this);
        tv.setText(subjectText);
        tv.setTextSize(17f);
        tv.setTypeface(null, android.graphics.Typeface.BOLD);
        tv.setTextColor(android.graphics.Color.parseColor("#1A237E"));
        tv.setPadding(10, 25, 10, 5);
        subjectBlock.addView(tv);
    }

    // ⭐ העיצוב החדש וההפוך - תווית קטנה למעלה, ערך גדול למטה! ⭐
    private void addRowToContainer(LinearLayout container, String labelText, String valueText) {
        View rowView = getLayoutInflater().inflate(android.R.layout.simple_list_item_2, null);
        TextView text1 = rowView.findViewById(android.R.id.text1);
        TextView text2 = rowView.findViewById(android.R.id.text2);

        // תווית (לדוגמה: "First Name") - טקסט קטן ואפור
        text1.setText(labelText);
        text1.setTextSize(13f);
        text1.setTextColor(android.graphics.Color.parseColor("#777777"));

        // ערך (לדוגמה: "Daniel") - טקסט גדול ובולט יותר
        text2.setText(valueText);
        text2.setTextSize(16f);
        text2.setTextColor(android.graphics.Color.parseColor("#222222"));

        rowView.setPadding(20, 15, 10, 15);
        container.addView(rowView);
    }

    // הפונקציה הזו נשארה לטובת כיתות מורה בלבד, שם ההיגיון דורש שכותרת הכיתה תהיה גדולה יותר
    private View createClassRowView(String mainText, String subText) {
        View rowView = getLayoutInflater().inflate(android.R.layout.simple_list_item_2, null);
        TextView text1 = rowView.findViewById(android.R.id.text1);
        TextView text2 = rowView.findViewById(android.R.id.text2);

        text1.setText(mainText);
        text1.setTextSize(16f);
        text1.setTextColor(android.graphics.Color.parseColor("#333333"));

        text2.setText(subText);
        text2.setTextSize(13f);
        text2.setTextColor(android.graphics.Color.parseColor("#777777"));

        rowView.setPadding(30, 10, 10, 10);

        android.util.TypedValue outValue = new android.util.TypedValue();
        getTheme().resolveAttribute(android.R.layout.simple_list_item_2, outValue, true);
        rowView.setBackgroundResource(android.R.drawable.list_selector_background);

        return rowView;
    }

    private void addNoDataTextView(LinearLayout container, String message) {
        TextView tv = new TextView(this);
        tv.setText(message);
        tv.setTextSize(15f);
        tv.setTextColor(android.graphics.Color.DKGRAY);
        tv.setLineSpacing(4f, 1.1f);
        tv.setPadding(15, 15, 15, 15);
        container.addView(tv);
    }

    private String getRoleName(int type) {
        switch (type) {
            case 0: return "Student";
            case 1: return "Teacher";
            case 2: return "School Administrator";
            case 3: return "Global Schooly Admin";
            default: return "Unknown";
        }
    }

    @Override
    protected int getLayoutResourceId() { return R.layout.activity_profile; }

    @Override
    protected int[] getAllowedUserTypes() {
        return new int[]{0, 1, 2, 3};
    }
}