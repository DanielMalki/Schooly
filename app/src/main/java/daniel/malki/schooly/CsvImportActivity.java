package daniel.malki.schooly;

import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.widget.Button;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AlertDialog;

import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

public class CsvImportActivity extends BaseMenuActivity {

    private Button btnImportStudentsCsv, btnImportTeachersCsv;
    private int currentImportType = -1;
    private static final int IMPORT_TYPE_STUDENT = 0;
    private static final int IMPORT_TYPE_TEACHER = 1;

    private FirebaseFirestore db;
    private DocumentReference currentSchoolRef;

    // File picker launcher mechanism
    private final ActivityResultLauncher<Intent> filePickerLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                    Uri fileUri = result.getData().getData();
                    if (fileUri != null) {
                        processCsvFile(fileUri, currentImportType);
                    }
                }
            }
    );

    @Override
    protected int getLayoutResourceId() {
        return R.layout.activity_csv_import;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        db = FirebaseFirestore.getInstance();
        loadCurrentSchoolReference();

        btnImportStudentsCsv = findViewById(R.id.btnImportStudentsCsv);
        btnImportTeachersCsv = findViewById(R.id.btnImportTeachersCsv);

        btnImportStudentsCsv.setOnClickListener(v -> openFilePicker(IMPORT_TYPE_STUDENT));
        btnImportTeachersCsv.setOnClickListener(v -> openFilePicker(IMPORT_TYPE_TEACHER));
    }

    private void loadCurrentSchoolReference() {
        // 1. קודם כל מנסים למשוך את בית הספר שהועבר מהמסך הקודם (יעבוד למנהל רמה 3)
        String schoolId = getIntent().getStringExtra("SCHOOL_ID");

        if (schoolId != null) {
            currentSchoolRef = db.collection("schools").document(schoolId);
            return;
        }

        // 2. גיבוי: אם לא הועבר כלום, ננסה למשוך מה-SharedPreferences (למנהלי בית ספר רמה 2)
        SharedPreferences prefs = getSharedPreferences("SchoolyPrefs", MODE_PRIVATE);
        String savedSchoolId = prefs.getString("currentSchoolId", null);

        if (savedSchoolId != null) {
            currentSchoolRef = db.collection("schools").document(savedSchoolId);
        } else {
            Toast.makeText(this, "Error: No school context found. Please select a school first.", Toast.LENGTH_LONG).show();
            finish();
        }
    }

    private void openFilePicker(int importType) {
        currentImportType = importType;
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("*/*");
        String[] mimeTypes = {"text/csv", "text/comma-separated-values", "application/csv"};
        intent.putExtra(Intent.EXTRA_MIME_TYPES, mimeTypes);
        filePickerLauncher.launch(intent);
    }

    private void processCsvFile(Uri uri, int importType) {
        List<String[]> parsedRows = new ArrayList<>();
        int invalidIdCount = 0; // מונה לשורות שנפסלו

        try {
            InputStream inputStream = getContentResolver().openInputStream(uri);
            if (inputStream == null) return;

            BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
            String line;
            boolean isHeader = true;

            while ((line = reader.readLine()) != null) {
                // Skip empty lines
                if (line.trim().isEmpty()) continue;

                // Simple split by comma, ignoring row headers
                String[] tokens = line.split(",");
                if (isHeader) {
                    isHeader = false;
                    continue;
                }

                // ולידציה של תעודת הזהות (הנחה שהיא בעמודה הראשונה, אינדקס 0)
                if (tokens.length > 0) {
                    String tz = tokens[0].trim();
                    if (!isValidIsraeliId(tz)) {
                        invalidIdCount++;
                        continue; // ת"ז לא חוקית -> מדלגים על השורה הזו לגמרי
                    }
                }

                parsedRows.add(tokens);
            }
            reader.close();
            inputStream.close();

            if (parsedRows.isEmpty()) {
                Toast.makeText(this, "The selected file contains no valid records.", Toast.LENGTH_LONG).show();
                return;
            }

            // מעבירים גם את כמות השגיאות לדיאלוג כדי ליידע את המשתמש
            showConfirmationDialog(parsedRows, importType, invalidIdCount);

        } catch (Exception e) {
            Toast.makeText(this, "Failed to parse CSV file: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    // הוספנו את המשתנה invalidIdCount לחתימה של הפונקציה
    // הוספנו את המשתנה invalidIdCount לחתימה של הפונקציה
    private void showConfirmationDialog(List<String[]> data, int importType, int invalidIdCount) {
        String roleStr = (importType == IMPORT_TYPE_STUDENT) ? "Students" : "Teachers";
        int totalValidRecords = data.size();

        StringBuilder message = new StringBuilder();
        message.append("Successfully read ").append(totalValidRecords).append(" valid ").append(roleStr.toLowerCase()).append(" records from the file.\n");

        // אם היו תעודות זהות שגויות, נוסיף אזהרה
        if (invalidIdCount > 0) {
            message.append("\n⚠️ Skipped ").append(invalidIdCount).append(" records due to invalid ID numbers (TZ checksum failed).\n");
        }

        message.append("\nDo you want to proceed and save them to the database?");

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Confirm Bulk Import");
        builder.setMessage(message.toString());

        builder.setPositiveButton("Import Now", (dialog, which) -> {
            if (importType == IMPORT_TYPE_STUDENT) {
                saveStudentsToFirestore(data);
            } else {
                saveTeachersToFirestore(data);
            }
        });

        builder.setNegativeButton("Cancel", null);
        builder.create().show();
    }

    private void saveStudentsToFirestore(List<String[]> studentData) {
        // Step 1: Query all current classes for mapping names to references efficiently
        // Step 2: Loop and insert students, placing unmatched classes in exceptions list
        Toast.makeText(this, "Starting students import... 🚀", Toast.LENGTH_SHORT).show();
    }

    private void saveTeachersToFirestore(List<String[]> teacherData) {
        // Loop and add teachers with their listed subjects array
        Toast.makeText(this, "Starting teachers import... 🚀", Toast.LENGTH_SHORT).show();
    }
    private boolean isValidIsraeliId(String tz) {
        if (tz == null || tz.trim().isEmpty()) return false;
        tz = tz.trim();

        // מוודא שיש רק ספרות
        if (!tz.matches("\\d+")) return false;

        // ריפוד באפסים משמאל עד ל-9 ספרות
        while (tz.length() < 9) {
            tz = "0" + tz;
        }

        // אם זה יותר מ-9 ספרות, זה לא תקין
        if (tz.length() > 9) return false;

        // חישוב ספרת הביקורת
        int sum = 0;
        for (int i = 0; i < 9; i++) {
            int digit = tz.charAt(i) - '0';
            int step = digit * ((i % 2) == 0 ? 1 : 2);
            sum += (step > 9) ? (step - 9) : step;
        }
        return sum % 10 == 0;
    }
}