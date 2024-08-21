package com.example.testdoorbellapi28;

import android.Manifest;
import android.app.ProgressDialog;
import android.content.ContentValues;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Rect;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.provider.MediaStore;
import android.speech.tts.TextToSpeech;
import android.util.Log;
import android.widget.Toast;

import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.text.Text;
import com.google.mlkit.vision.text.TextRecognition;
import com.google.mlkit.vision.text.TextRecognizer;
import com.google.mlkit.vision.text.latin.TextRecognizerOptions;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class ReadActivity extends AppCompatActivity {

    private static final int CAMERA_REQUEST_CODE = 100;
    private static final String TAG = "ReadActivity";

    private Uri imageUri = null;
    private ProgressDialog progressDialog;
    private TextRecognizer textRecognizer;
    private TextToSpeech textToSpeech;
    private String[] cameraPermissions;

    private ActivityResultLauncher<Intent> cameraActivityResultLauncher;

    private List<String> textQueue = new ArrayList<>();
    private int textQueueIndex = 0;
    private Handler handler = new Handler();
    private Runnable speakRunnable;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_read);

        // Initialize TextToSpeech
        textToSpeech = new TextToSpeech(this, status -> {
            if (status == TextToSpeech.SUCCESS) {
                textToSpeech.setLanguage(Locale.UK);
            } else {
                Log.e(TAG, "TextToSpeech initialization failed");
            }
        });

        // Initialize TextRecognizer
        textRecognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS);

        // Initialize ProgressDialog
        progressDialog = new ProgressDialog(this);
        progressDialog.setTitle("Please wait");
        progressDialog.setCanceledOnTouchOutside(false);

        // Initialize camera permissions
        cameraPermissions = new String[]{Manifest.permission.CAMERA, Manifest.permission.WRITE_EXTERNAL_STORAGE};

        // Initialize camera result launcher
        cameraActivityResultLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                this::handleCameraResult
        );

        // Check and request camera permissions, then open camera
        if (checkCameraPermissions()) {
            pickImageCamera();
        } else {
            requestCameraPermission();
        }
    }

    private void handleCameraResult(ActivityResult result) {
        if (result.getResultCode() == RESULT_OK) {
            recognizeTextFromImage();
        } else {
            Toast.makeText(this, "Camera operation canceled", Toast.LENGTH_SHORT).show();
            finish();
        }
    }

    private void pickImageCamera() {
        ContentValues values = new ContentValues();
        values.put(MediaStore.Images.Media.TITLE, "Sample Title");
        values.put(MediaStore.Images.Media.DESCRIPTION, "Sample Image Description");

        imageUri = getContentResolver().insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values);

        Intent cameraIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        cameraIntent.putExtra(MediaStore.EXTRA_OUTPUT, imageUri);
        cameraActivityResultLauncher.launch(cameraIntent);
    }

    private boolean checkCameraPermissions() {
        List<String> listPermissionsNeeded = new ArrayList<>();
        for (String permission : cameraPermissions) {
            if (ContextCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED) {
                listPermissionsNeeded.add(permission);
            }
        }
        return listPermissionsNeeded.isEmpty();
    }

    private void requestCameraPermission() {
        ActivityCompat.requestPermissions(this, cameraPermissions, CAMERA_REQUEST_CODE);
    }

    private List<Rect> getExpectedFieldPositions() {
        List<Rect> expectedPositions = new ArrayList<>();
        expectedPositions.add(new Rect(340, 370, 1280, 760));  // Field 1
        expectedPositions.add(new Rect(340, 884, 1280, 1260));  // Field 2
        expectedPositions.add(new Rect(340, 1385, 1280, 1782));  // Field 3
        expectedPositions.add(new Rect(340, 1907, 1280, 2300));  // Field 4
        expectedPositions.add(new Rect(3366, 370, 4320, 760));  // Field 5
        expectedPositions.add(new Rect(3366, 884, 4320, 1260));  // Field 6
        expectedPositions.add(new Rect(3366, 1385, 4320, 1782));  // Field 7
        expectedPositions.add(new Rect(3366, 1907, 4320, 2300));  // Field 8
        return expectedPositions;
    }

    private void recognizeTextFromImage() {
        progressDialog.setMessage("Preparing image....");
        progressDialog.show();

        try {
            InputImage inputImage = InputImage.fromFilePath(this, imageUri);
            textRecognizer.process(inputImage)
                    .addOnSuccessListener(visionText -> {
                        progressDialog.dismiss();
                        processTextRecognitionResult(visionText);
                    })
                    .addOnFailureListener(e -> {
                        progressDialog.dismiss();
                        Toast.makeText(ReadActivity.this, "Text recognition failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    });
        } catch (IOException e) {
            progressDialog.dismiss();
            Toast.makeText(this, "Failed to process the image: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private void processTextRecognitionResult(Text text) {
        List<Rect> fieldPositions = getExpectedFieldPositions();
        textQueue.clear();

        for (int i = 0; i < fieldPositions.size(); i++) {
            Rect fieldPosition = fieldPositions.get(i);
            boolean isFieldEmpty = true;
            StringBuilder fieldText = new StringBuilder();

            for (Text.TextBlock block : text.getTextBlocks()) {
                if (Rect.intersects(fieldPosition, block.getBoundingBox())) {
                    isFieldEmpty = false;
                    fieldText.append(block.getText()).append(" ");
                }
            }

            if (isFieldEmpty) {
                textQueue.add(Integer.toString(i + 1) + "empty");
            } else {
                textQueue.add(Integer.toString(i + 1) + ": " + fieldText.toString().trim());
            }
        }

        textQueueIndex = 0;
        speakNextText();
    }

    private void speakNextText() {
        if (textQueueIndex < textQueue.size()) {
            String textToSpeak = textQueue.get(textQueueIndex);
            textToSpeech.speak(textToSpeak, TextToSpeech.QUEUE_FLUSH, null, Integer.toString(textQueueIndex));
            textQueueIndex++;
            // Schedule the next item to be spoken after a delay
            handler.postDelayed(this::speakNextText, 2000); // Adjust the delay as needed
        } else {
            navigateToMainScreen();
        }
    }

    private void navigateToMainScreen() {
        new Handler().postDelayed(() -> {
            Intent intent = new Intent(ReadActivity.this, MainActivity.class);
            startActivity(intent);
            finish();
        }, 500); // Delay to ensure all text has been spoken
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == CAMERA_REQUEST_CODE) {
            if (grantResults.length > 1 && grantResults[0] == PackageManager.PERMISSION_GRANTED && grantResults[1] == PackageManager.PERMISSION_GRANTED) {
                pickImageCamera();
            } else {
                Toast.makeText(this, "Camera & Storage permissions are required", Toast.LENGTH_SHORT).show();
            }
        }
    }

    @Override
    protected void onDestroy() {
        if (textToSpeech != null) {
            textToSpeech.stop();
            textToSpeech.shutdown();
        }
        super.onDestroy();
    }
}
