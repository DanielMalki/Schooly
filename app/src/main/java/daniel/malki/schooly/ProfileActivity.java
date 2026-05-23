package daniel.malki.schooly;

import android.app.AlertDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
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

        // לאנצ'ר גלריה
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

        // לאנצ'ר מצלמה
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

                // 🖼️ טעינת תמונת הפרופיל מה-Blob
                if (documentSnapshot.getBlob("profileImageBlob") != null) {
                    byte[] imageBytes = documentSnapshot.getBlob("profileImageBlob").toBytes();
                    com.bumptech.glide.Glide.with(this)
                            .load(imageBytes)
                            .circleCrop()
                            .into(imgAvatar);
                } else {
                    imgAvatar.setImageResource(android.R.drawable.sym_def_app_icon);
                }

                // 🏫 שליפת בית הספר והצגתו בצורה דינמית (עבור רמות 0, 1, 2)
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

            }).addOnFailureListener(e -> {
                android.util.Log.e("ProfileActivity", "Error loading image: " + e.getMessage());
            });
        }

        if (layoutDynamicInfoContainer != null) {
            layoutDynamicInfoContainer.removeAllViews();
        }

        if (userType == 0) {
            cardAcademicInfo.setVisibility(View.VISIBLE);
            tvAcademicHeader.setText("My Learning Groups & Teachers");
            fetchStudentCourses(id);
        } else if (userType == 1 || userType == 2) {
            cardAcademicInfo.setVisibility(View.VISIBLE);
            tvAcademicHeader.setText("My Teachable Subjects");
            fetchTeacherSubjects(id);
        } else if (userType == 3) {
            cardAcademicInfo.setVisibility(View.VISIBLE);
            tvAcademicHeader.setText("System Overseer Info");
            addNoDataTextView("👑 Global Application Administrator\nYou hold full access rights over all schools, global subjects, and system accounts.");
        } else {
            cardAcademicInfo.setVisibility(View.GONE);
        }
    }

    /* ---------------- לוגיקת תלמיד: שליפת קבוצות ומורים ---------------- */
    /* ---------------- לוגיקת תלמיד: שליפת קבוצות ומורים (עם מיון חכם) ---------------- */
    private void fetchStudentCourses(String studentId) {
        db.collection("users").document(studentId).get().addOnSuccessListener(documentSnapshot -> {
            if (!documentSnapshot.exists()) return;

            Map<String, Object> classesMap = (Map<String, Object>) documentSnapshot.get("classes");
            if (classesMap == null || classesMap.isEmpty()) {
                addNoDataTextView("Not assigned to any learning groups yet.");
                return;
            }

            // סינון מקדים של מצביעים ריקים (null) שנוצרו ממחיקות
            List<Map.Entry<String, Object>> validEntries = new ArrayList<>();
            for (Map.Entry<String, Object> entry : classesMap.entrySet()) {
                if (entry.getValue() != null) {
                    validEntries.add(entry);
                }
            }

            if (validEntries.isEmpty()) {
                addNoDataTextView("Not assigned to any learning groups yet.");
                return;
            }

            final int totalValidClasses = validEntries.size();
            List<StudentCourseItem> fetchedCourses = new ArrayList<>();

            for (Map.Entry<String, Object> entry : validEntries) {
                String classType = entry.getKey();
                DocumentReference classRef = (DocumentReference) entry.getValue();

                classRef.get().addOnSuccessListener(classDoc -> {
                    String className = classDoc.exists() ? classDoc.getString("displayName") : "Unknown Class";

                    if ("homeroom".equals(classType)) {
                        // כיתת אם - אין לה מורה רשום במערך הקורסים, נוסיף מיד
                        fetchedCourses.add(new StudentCourseItem(classType, className, ""));
                        checkAndRenderStudentCourses(fetchedCourses, totalValidClasses);
                    } else {
                        // קבוצת לימוד רגילה - נחפש את המורה שלה
                        List<Map<String, Object>> assignments = (List<Map<String, Object>>) classDoc.get("course_assignments");
                        DocumentReference teacherRef = null;

                        if (assignments != null && !assignments.isEmpty()) {
                            for (Map<String, Object> assignment : assignments) {
                                teacherRef = (DocumentReference) assignment.get("teacher");
                                if (teacherRef != null) break;
                            }
                        }

                        if (teacherRef != null) {
                            teacherRef.get().addOnSuccessListener(teacherDoc -> {
                                String teacherName = teacherDoc.exists() ? "Teacher: " + teacherDoc.getString("name") : "Teacher: Unknown";
                                fetchedCourses.add(new StudentCourseItem(classType, className, teacherName));
                                checkAndRenderStudentCourses(fetchedCourses, totalValidClasses);
                            }).addOnFailureListener(e -> {
                                fetchedCourses.add(new StudentCourseItem(classType, className, "Teacher: Unknown"));
                                checkAndRenderStudentCourses(fetchedCourses, totalValidClasses);
                            });
                        } else {
                            fetchedCourses.add(new StudentCourseItem(classType, className, "Teacher: Unknown"));
                            checkAndRenderStudentCourses(fetchedCourses, totalValidClasses);
                        }
                    }
                }).addOnFailureListener(e -> {
                    fetchedCourses.add(new StudentCourseItem(classType, "Error Loading", ""));
                    checkAndRenderStudentCourses(fetchedCourses, totalValidClasses);
                });
            }
        });
    }

    // פונקציית עזר שממתינה לסיום הטעינה של כל הקורסים, ממיינת ומציירת אותם
    private void checkAndRenderStudentCourses(List<StudentCourseItem> fetchedCourses, int totalExpected) {
        if (fetchedCourses.size() != totalExpected) return; // עדיין לא סיימנו לטעון את הכל

        // 👑 שלב המיון המתוחכם!
        Collections.sort(fetchedCourses, (item1, item2) -> {
            boolean isHome1 = "homeroom".equals(item1.classType);
            boolean isHome2 = "homeroom".equals(item2.classType);

            // חוק א': כיתת אם תמיד עוקפת ומגיעה לראש הרשימה
            if (isHome1 && !isHome2) return -1;
            if (!isHome1 && isHome2) return 1;

            // חוק ב': אם שתיהן כיתות אם או שתיהן קבוצות רגילות - נמיין אלפביתית (A-Z)
            String name1 = item1.className != null ? item1.className : "";
            String name2 = item2.className != null ? item2.className : "";
            return name1.compareToIgnoreCase(name2);
        });

        // 🛠️ עכשיו כשהכל מסודר פיקס, ננקה את המכלול ונציג למשתמש במסך
        if (layoutDynamicInfoContainer != null) {
            layoutDynamicInfoContainer.removeAllViews();
        }

        for (StudentCourseItem item : fetchedCourses) {
            if ("homeroom".equals(item.classType)) {
                addCourseRowView("🏠 Homeroom Class: " + item.className, "");
            } else {
                addCourseRowView("📚 " + item.className, item.teacherName);
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
                    tvAcademicHeader.setText("Administrative Controls");
                    addNoDataTextView("💼 School Staff Member\nUse the side menu to access administrative tools and manage users.");
                } else {
                    addNoDataTextView("No subjects assigned to you yet.");
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
                            layoutDynamicInfoContainer.addView(subjectBlock);

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
                List<Map<String, Object>> assignments = (List<Map<String, Object>>) classDoc.get("course_assignments");

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
                        List<DocumentReference> studentClasses = (List<DocumentReference>) userDoc.get("classes");
                        if (studentClasses != null) {
                            for (DocumentReference sClassRef : studentClasses) {
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

    // מחלקת עזר זמנית לצורך מיון כיתות התלמיד
    private static class StudentCourseItem {
        String classType;
        String className;
        String teacherName;

        StudentCourseItem(String classType, String className, String teacherName) {
            this.classType = classType;
            this.className = className;
            this.teacherName = teacherName;
        }
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

    private View createClassRowView(String mainText, String subText) {
        View rowView = getLayoutInflater().inflate(android.R.layout.simple_list_item_2, null);
        TextView text1 = rowView.findViewById(android.R.id.text1);
        TextView text2 = rowView.findViewById(android.R.id.text2);

        text1.setText(mainText);
        text1.setTextSize(15f);
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

    private View addCourseRowView(String mainText, String subText) {
        View rowView = getLayoutInflater().inflate(android.R.layout.simple_list_item_2, null);
        TextView text1 = rowView.findViewById(android.R.id.text1);
        TextView text2 = rowView.findViewById(android.R.id.text2);

        text1.setText(mainText);
        text1.setTextSize(16f);
        text1.setTextColor(android.graphics.Color.parseColor("#333333"));

        text2.setText(subText);
        text2.setTextSize(14f);
        text2.setTextColor(android.graphics.Color.parseColor("#666666"));

        rowView.setPadding(10, 15, 10, 15);
        layoutDynamicInfoContainer.addView(rowView);
        return rowView;
    }

    private void addNoDataTextView(String message) {
        TextView tv = new TextView(this);
        tv.setText(message);
        tv.setTextSize(15f);
        tv.setTextColor(android.graphics.Color.DKGRAY);
        tv.setLineSpacing(4f, 1.1f);
        tv.setPadding(15, 15, 15, 15);
        layoutDynamicInfoContainer.addView(tv);
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

    // ✨ הפונקציה שמוסיפה הרשאות גישה לכל סוגי המשתמשים באפליקציה!
    @Override
    protected int[] getAllowedUserTypes() {
        return new int[]{0, 1, 2, 3};
    }
}