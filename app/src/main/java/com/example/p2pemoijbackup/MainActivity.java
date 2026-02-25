package com.example.p2pemoijbackup;

import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.security.MessageDigest;

public class MainActivity extends AppCompatActivity {

    TextView statusText;
    Button emojiBtn1, emojiBtn2, emojiBtn3, emojiBtn4;
    Button resetBtn, selectFileBtn, restoreBtn;

    StringBuilder input = new StringBuilder();
    String savedPasswordHash = "";

    int emojiCount = 0;
    int attempts = 0;

    SharedPreferences prefs;

    private static final int PASSWORD_LENGTH = 4;
    private static final int FILE_PICK_CODE = 101;

    private Uri selectedFileUri;
    private byte[] selectedFileBytes;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        statusText = findViewById(R.id.statusText);
        emojiBtn1 = findViewById(R.id.btnEmoji1);
        emojiBtn2 = findViewById(R.id.btnEmoji2);
        emojiBtn3 = findViewById(R.id.btnEmoji3);
        emojiBtn4 = findViewById(R.id.btnEmoji4);
        resetBtn = findViewById(R.id.resetBtn);
        selectFileBtn = findViewById(R.id.btnSelectFile);
        restoreBtn = findViewById(R.id.btnRestore);

        prefs = getSharedPreferences("secure_backup", MODE_PRIVATE);
        savedPasswordHash = prefs.getString("pwd_hash", "");

        selectFileBtn.setEnabled(false);
        restoreBtn.setEnabled(false);

        updateDots();

        View.OnClickListener emojiClick = v -> {
            if (emojiCount >= PASSWORD_LENGTH) return;

            Button b = (Button) v;
            input.append(b.getText().toString());
            emojiCount++;

            updateDots();

            if (emojiCount == PASSWORD_LENGTH) {
                checkLogin();
            }
        };

        emojiBtn1.setOnClickListener(emojiClick);
        emojiBtn2.setOnClickListener(emojiClick);
        emojiBtn3.setOnClickListener(emojiClick);
        emojiBtn4.setOnClickListener(emojiClick);

        selectFileBtn.setOnClickListener(v -> openFilePicker());
        restoreBtn.setOnClickListener(v -> restoreOfflineBackup());

        resetBtn.setOnClickListener(v -> {
            prefs.edit().clear().apply();
            savedPasswordHash = "";
            attempts = 0;
            clearInput();
            setAuthenticatedUI(false);
            selectFileBtn.setEnabled(false);
            restoreBtn.setEnabled(false);
            Toast.makeText(this, "Reset Done", Toast.LENGTH_SHORT).show();
        });
    }

    // ================= DOT UI =================
    private void updateDots() {
        StringBuilder dots = new StringBuilder();
        for (int i = 0; i < PASSWORD_LENGTH; i++) {
            if (i < emojiCount) dots.append("● ");
            else dots.append("○ ");
        }
        statusText.setText(dots.toString().trim());
    }

    // ================= AUTH UI =================
    private void setAuthenticatedUI(boolean isAuth) {
        emojiBtn1.setEnabled(!isAuth);
        emojiBtn2.setEnabled(!isAuth);
        emojiBtn3.setEnabled(!isAuth);
        emojiBtn4.setEnabled(!isAuth);

        if (isAuth) {
            statusText.setText("Authenticated ✅");
        } else {
            updateDots();
        }
    }

    // ================= LOGIN =================
    private void checkLogin() {
        String enteredHash = sha256(input.toString());

        // First time password set
        if (savedPasswordHash.isEmpty()) {
            savedPasswordHash = enteredHash;
            prefs.edit().putString("pwd_hash", savedPasswordHash).apply();

            selectFileBtn.setEnabled(true);
            restoreBtn.setEnabled(true);
            setAuthenticatedUI(true);

            Toast.makeText(this, "Password Set 🔐", Toast.LENGTH_SHORT).show();
            clearInput();
            return;
        }

        // Login check
        if (enteredHash.equals(savedPasswordHash)) {
            selectFileBtn.setEnabled(true);
            restoreBtn.setEnabled(true);
            setAuthenticatedUI(true);
            Toast.makeText(this, "Login Success 🔓", Toast.LENGTH_SHORT).show();
        } else {
            attempts++;
            Toast.makeText(this,
                    "Wrong Password (" + attempts + "/3)",
                    Toast.LENGTH_SHORT).show();

            if (attempts >= 3) {
                prefs.edit().clear().apply();
                savedPasswordHash = "";
                attempts = 0;
                setAuthenticatedUI(false);
                selectFileBtn.setEnabled(false);
                restoreBtn.setEnabled(false);
            }
        }
        clearInput();
    }

    private void clearInput() {
        input.setLength(0);
        emojiCount = 0;
        updateDots();
    }

    // ================= FILE PICK =================
    private void openFilePicker() {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.setType("*/*");
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        startActivityForResult(intent, FILE_PICK_CODE);
    }

    @Override
    protected void onActivityResult(int req, int res, @Nullable Intent data) {
        super.onActivityResult(req, res, data);

        if (req == FILE_PICK_CODE && res == RESULT_OK && data != null) {
            selectedFileUri = data.getData();
            selectedFileBytes = readFileFromUri(selectedFileUri);
            saveOfflineBackup(selectedFileBytes);
        }
    }

    private byte[] readFileFromUri(Uri uri) {
        try {
            InputStream is = getContentResolver().openInputStream(uri);
            ByteArrayOutputStream buffer = new ByteArrayOutputStream();
            byte[] data = new byte[1024];
            int n;
            while ((n = is.read(data)) != -1) buffer.write(data, 0, n);
            is.close();
            return buffer.toByteArray();
        } catch (Exception e) {
            return null;
        }
    }

    // ================= BACKUP =================
    private void saveOfflineBackup(byte[] data) {
        try {
            byte[] encrypted = xorEncrypt(data, savedPasswordHash);
            File dir = new File(getFilesDir(), "secure_backup");
            if (!dir.exists()) dir.mkdirs();

            FileOutputStream fos =
                    new FileOutputStream(new File(dir, "offline_backup.dat"));
            fos.write(encrypted);
            fos.close();

            Toast.makeText(this, "Backup Saved 🔐", Toast.LENGTH_SHORT).show();
        } catch (Exception e) {
            Toast.makeText(this, "Backup Failed ❌", Toast.LENGTH_SHORT).show();
        }
    }

    // ================= RESTORE =================
    private void restoreOfflineBackup() {
        try {
            File file = new File(getFilesDir(),
                    "secure_backup/offline_backup.dat");

            if (!file.exists()) {
                Toast.makeText(this, "No Backup Found ❌", Toast.LENGTH_SHORT).show();
                return;
            }

            FileInputStream fis = new FileInputStream(file);
            byte[] encrypted = new byte[fis.available()];
            fis.read(encrypted);
            fis.close();

            byte[] decrypted = xorEncrypt(encrypted, savedPasswordHash);

            File restoreDir = new File(getFilesDir(), "restored");
            if (!restoreDir.exists()) restoreDir.mkdirs();

            FileOutputStream fos =
                    new FileOutputStream(new File(restoreDir, "restored_file.dat"));
            fos.write(decrypted);
            fos.close();

            Toast.makeText(this,
                    "Restore Successful ✅",
                    Toast.LENGTH_LONG).show();

        } catch (Exception e) {
            Toast.makeText(this, "Restore Failed ❌", Toast.LENGTH_SHORT).show();
        }
    }

    // ================= SECURITY =================
    private String sha256(String input) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] hash = md.digest(input.getBytes("UTF-8"));
            StringBuilder sb = new StringBuilder();
            for (byte b : hash)
                sb.append(String.format("%02x", b));
            return sb.toString();
        } catch (Exception e) {
            return "";
        }
    }

    private byte[] xorEncrypt(byte[] data, String key) {
        byte[] keyBytes = key.getBytes();
        byte[] out = new byte[data.length];
        for (int i = 0; i < data.length; i++)
            out[i] = (byte) (data[i] ^ keyBytes[i % keyBytes.length]);
        return out;
    }
}