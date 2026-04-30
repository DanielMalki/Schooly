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
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;

import com.bumptech.glide.Glide;
import com.google.firebase.firestore.Blob;
import com.google.firebase.firestore.FirebaseFirestore;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

import com.google.firebase.firestore.FieldValue;

public class ProfileActivity extends BaseMenuActivity {

    private ImageView imgAvatar;
    private TextView tvFullName, tvRole;
    private TextView valueFirstName, valueLastName, valueId;
    private View cardAcademicInfo;
    private TextView valueClassGrade, valueMath, valueEnglish, valueMajors;

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
        valueFirstName = findViewById(R.id.valueFirstName);
        valueLastName = findViewById(R.id.valueLastName);
        valueId = findViewById(R.id.valueId);

        cardAcademicInfo = findViewById(R.id.cardAcademicInfo);
        valueClassGrade = findViewById(R.id.valueClassGrade);
        valueMath = findViewById(R.id.valueMath);
        valueEnglish = findViewById(R.id.valueEnglish);
        valueMajors = findViewById(R.id.valueMajors);

        // 1. לאנצ'ר גלריה
        galleryLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                        Uri selectedImageUri = result.getData().getData();
                        imgAvatar.setImageURI(selectedImageUri);
                        imgAvatar.setImageTintList(null);

                        // קריאת התמונה מהגלריה והעלאתה כ-Blob
                        try {
                            Bitmap bitmap = MediaStore.Images.Media.getBitmap(this.getContentResolver(), selectedImageUri);
                            saveImageAsBlob(bitmap);
                        } catch (IOException e) {
                            Toast.makeText(this, "Failed to read image", Toast.LENGTH_SHORT).show();
                        }
                    }
                }
        );

        // 2. לאנצ'ר מצלמה
        cameraLauncher = registerForActivityResult(
                new ActivityResultContracts.TakePicturePreview(),
                bitmap -> {
                    if (bitmap != null) {
                        imgAvatar.setImageBitmap(bitmap);
                        imgAvatar.setImageTintList(null);

                        // העלאת התמונה מהמצלמה כ-Blob
                        saveImageAsBlob(bitmap);
                    }
                }
        );

        imgAvatar.setOnClickListener(v -> showImageSourceDialog());

        loadUserData();
    }

    private void showImageSourceDialog() {
        String[] options = {"Open Camera 📷", "Choose from Gallery 🖼️", "Remove Picture 🗑️", "Cancel ❌"};
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Update Profile Picture");
        builder.setItems(options, (dialog, which) -> {
            if (which == 0) {
                cameraLauncher.launch(null);
            } else if (which == 1) {
                Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
                galleryLauncher.launch(intent);
            } else if (which == 2) {
                removeProfilePicture(); // הפונקציה החדשה שלנו שניצור עכשיו
            } else {
                dialog.dismiss();
            }
        });
        builder.show();
    }

    /* ---------------- שמירת התמונה כ-Blob ב-Firestore ---------------- */

    private void saveImageAsBlob(Bitmap originalBitmap) {
        String userId = getSharedPreferences("SchoolyPrefs", MODE_PRIVATE).getString("userId", "");
        if (userId.isEmpty()) return;

        Toast.makeText(this, "Saving profile picture...", Toast.LENGTH_SHORT).show();

        // 1. כיווץ התמונה כדי שלא נעבור את המגבלה של 1MB של פיירסטור
        int maxWidth = 400; // גודל מקסימלי לתמונת פרופיל
        int maxHeight = 400;
        float scale = Math.min(((float)maxWidth / originalBitmap.getWidth()), ((float)maxHeight / originalBitmap.getHeight()));
        Bitmap resizedBitmap = Bitmap.createScaledBitmap(originalBitmap,
                (int)(originalBitmap.getWidth() * scale),
                (int)(originalBitmap.getHeight() * scale), true);

        // 2. המרה של ה-Bitmap למערך בייטים (Byte Array) באיכות נמוכה יותר
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        resizedBitmap.compress(Bitmap.CompressFormat.JPEG, 70, baos); // איכות 70%
        byte[] data = baos.toByteArray();

        // 3. יצירת ה-Blob ושמירה במסמך של המשתמש
        Blob blob = Blob.fromBytes(data);
        db.collection("users").document(userId).update("profileImageBlob", blob)
                .addOnSuccessListener(aVoid -> Toast.makeText(this, "Picture saved perfectly! 🚀", Toast.LENGTH_SHORT).show())
                .addOnFailureListener(e -> Toast.makeText(this, "Error saving picture", Toast.LENGTH_SHORT).show());
    }

    private void removeProfilePicture() {
        String userId = getSharedPreferences("SchoolyPrefs", MODE_PRIVATE).getString("userId", "");
        if (userId.isEmpty()) return;

        Toast.makeText(this, "Removing picture...", Toast.LENGTH_SHORT).show();

        // מוחק את שדה התמונה מהמסמך של המשתמש ב-Firestore
        db.collection("users").document(userId).update("profileImageBlob", FieldValue.delete())
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(this, "Picture removed successfully", Toast.LENGTH_SHORT).show();

                    // מנקה את התמונה הקיימת מ-Glide
                    Glide.with(this).clear(imgAvatar);

                    // מחזיר את האייקון הריק (שים לב: אם יש לך אייקון ברירת מחדל אחר, שנה את השם פה)
                    imgAvatar.setImageResource(R.drawable.ic_launcher_foreground);

                })
                .addOnFailureListener(e -> Toast.makeText(this, "Error removing picture", Toast.LENGTH_SHORT).show());
    }

    /* ---------------- טעינת נתונים (ותמונת פרופיל) ---------------- */

    private void loadUserData() {
        SharedPreferences prefs = getSharedPreferences("SchoolyPrefs", MODE_PRIVATE);
        String fullName = prefs.getString("userName", "Guest");
        String id = prefs.getString("userId", "");
        int type = prefs.getInt("userType", 0);

        tvFullName.setText(fullName);
        tvRole.setText(getRoleName(type));
        valueId.setText(id);

        if (fullName != null && !fullName.isEmpty()) {
            String[] parts = fullName.split(" ");
            if (parts.length > 0) valueFirstName.setText(parts[0]);
            if (parts.length > 1) {
                valueLastName.setText(fullName.substring(parts[0].length()).trim());
            } else valueLastName.setText("");
        }

        // טעינת תמונת הפרופיל (Blob) מה-Firestore
        db.collection("users").document(id).get().addOnSuccessListener(doc -> {
            if (doc.exists()) {
                Blob blob = doc.getBlob("profileImageBlob");
                if (blob != null) {
                    // הופכים את ה-Blob בחזרה לבייטים, ו-Glide יודע לצייר אותם!
                    byte[] imageBytes = blob.toBytes();
                    Glide.with(this)
                            .load(imageBytes)
                            .circleCrop() // תמונה עגולה
                            .into(imgAvatar);
                    imgAvatar.setImageTintList(null);
                }
            }
        });

        if (type == 0) {
            cardAcademicInfo.setVisibility(View.VISIBLE);
            fetchStudentAcademicInfo(id);
        } else {
            cardAcademicInfo.setVisibility(View.GONE);
        }
    }

    private void fetchStudentAcademicInfo(String userId) {
        db.collection("users").document(userId).get().addOnSuccessListener(documentSnapshot -> {
            if (documentSnapshot.exists()) {
                String grade = documentSnapshot.getString("grade");
                String classNum = documentSnapshot.getString("classNum");
                String mathClass = documentSnapshot.getString("mathClass");
                String englishClass = documentSnapshot.getString("englishClass");
                String major1 = documentSnapshot.getString("major1");
                String major2 = documentSnapshot.getString("major2");

                valueClassGrade.setText(grade + " - " + classNum);
                valueMath.setText(mathClass != null ? mathClass : "N/A");
                valueEnglish.setText(englishClass != null ? englishClass : "N/A");

                String majorsText = "";
                if (major1 != null && !major1.isEmpty()) majorsText += major1;
                if (major2 != null && !major2.isEmpty()) {
                    if (!majorsText.isEmpty()) majorsText += ", ";
                    majorsText += major2;
                }
                if (majorsText.isEmpty()) majorsText = "No Majors";
                valueMajors.setText(majorsText);
            }
        });
    }

    private String getRoleName(int type) {
        switch (type) {
            case 0: return "Student";
            case 1: return "Teacher";
            case 2: return "System Administrator";
            default: return "Unknown";
        }
    }

    @Override
    protected int getLayoutResourceId() {
        return R.layout.activity_profile;
    }
}