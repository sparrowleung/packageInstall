package com.example.lyybbc.installapk;

import android.Manifest;
import android.app.PendingIntent;
import android.content.Intent;
import android.content.pm.PackageInstaller;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Environment;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.FileProvider;
import android.support.v4.content.PermissionChecker;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.OutputStream;

public class MainActivity extends AppCompatActivity {

    private final static String TAG = MainActivity.class.getSimpleName();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Button intentInstall = findViewById(R.id.intent_install);
        intentInstall.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (PermissionChecker.checkSelfPermission(getBaseContext(), Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED
                        && PermissionChecker.checkSelfPermission(getBaseContext(), Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED) {
                    intentInstallMethod();
                } else {
                    ActivityCompat.requestPermissions(MainActivity.this, new String[]{Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE}, 0);
                }
            }
        });

        Button silentInstall = findViewById(R.id.silent_install);
        silentInstall.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (PermissionChecker.checkSelfPermission(getBaseContext(), Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED
                        && PermissionChecker.checkSelfPermission(getBaseContext(), Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED) {
                    silentInstallMethod();
                } else {
                    ActivityCompat.requestPermissions(MainActivity.this, new String[]{Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE}, 0);
                }
            }
        });
    }

    public void intentInstallMethod() {
        String apkName = "appInstall.apk";
        String apkFilePath = Environment.getExternalStorageDirectory().getAbsolutePath() + "/" + apkName;
        File apkFile = new File(apkFilePath);
        Uri uri = FileProvider.getUriForFile(getBaseContext(), "om.example.lyybbc.provider", apkFile);

        Intent intent = new Intent(Intent.ACTION_VIEW);
        intent.setDataAndType(uri, "application/vnd.android.package-archive");
        intent.addCategory(Intent.CATEGORY_DEFAULT);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        intent.addFlags(Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION);
        startActivity(intent);
    }

    public void silentInstallMethod() {
        String apkName = "appInstall.apk";
        String apkFilePath = Environment.getExternalStorageDirectory().getAbsolutePath() + "/" + apkName;

        File apkFile = new File(apkFilePath);
        long apkFileLength = apkFile.length();

        PackageManager pm = getPackageManager();
        PackageInstaller packageInstaller = pm.getPackageInstaller();
        packageInstaller.registerSessionCallback(new PackageInstaller.SessionCallback() {
            @Override
            public void onCreated(int sessionId) {
                Log.e(TAG, "Install Start sessionId-> " + sessionId);
            }

            @Override
            public void onBadgingChanged(int sessionId) {}

            @Override
            public void onActiveChanged(int sessionId, boolean active) {}

            @Override
            public void onProgressChanged(int sessionId, float progress) {}

            @Override
            public void onFinished(int sessionId, boolean success) {
                if (success) {
                    Log.e(TAG, "Silent Install Success");
                } else {
                    Log.e(TAG, "Silent Install Fail");
                }
            }
        });

        int count;
        int sessionId;
        byte[] buffer = new byte[65536];

        InputStream inputStream;
        OutputStream outputStream;
        PackageInstaller.Session session = null;
        PackageInstaller.SessionParams sessionParams;

        try {
            sessionParams = new PackageInstaller.SessionParams(PackageInstaller.SessionParams.MODE_FULL_INSTALL);
            sessionId =  packageInstaller.createSession(sessionParams);
            session = packageInstaller.openSession(sessionId);

            inputStream = new FileInputStream(apkFile);
            outputStream = session.openWrite(apkName, 0, apkFileLength);

            while((count = inputStream.read(buffer)) != -1) {
                outputStream.write(buffer, 0, count);
                float progress = ((float)count / (float)apkFileLength);
                session.setStagingProgress(progress);
            }
            session.fsync(outputStream);

            inputStream.close();
            outputStream.flush();
            outputStream.close();

            Intent intent = new Intent();
            PendingIntent pendingIntent = PendingIntent.getBroadcast(getBaseContext(), 0, intent, 0);
            session.commit(pendingIntent.getIntentSender());
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (session != null) {
                session.abandon();
            }
        }
    }
}
