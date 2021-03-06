package com.tushar.cmspen2;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Environment;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.os.Bundle;
import android.util.Log;
import android.view.ContextThemeWrapper;
import android.view.WindowManager;

import com.tushar.cmspen2.libsuperuser.Shell;

import java.io.File;
import java.io.FileDescriptor;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

public class ScreenshotMiddleMan extends Activity implements ActivityCompat.OnRequestPermissionsResultCallback {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_bhadva);

        takeScreenshot(getIntent().getBooleanExtra("external", false));
    }

    public void takeScreenshot(boolean external)
    {
        if(PreferenceManager.getDefaultSharedPreferences(this).getBoolean("ss_enable", false) || external)
        {
            int permissionCheck = ContextCompat.checkSelfPermission(this,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE);

            if(permissionCheck == PackageManager.PERMISSION_GRANTED)
            {
                screenShotLogic();
            }
            else
            {
                if (ActivityCompat.shouldShowRequestPermissionRationale(this,
                        Manifest.permission.WRITE_EXTERNAL_STORAGE)) {

                    AlertDialog.Builder builder = new AlertDialog.Builder(
                            new ContextThemeWrapper(ScreenshotMiddleMan.this, android.R.style.Theme_DeviceDefault));
                    builder.setTitle("Write Storage permission needed to be able to take Screenshots and store them. Grant Permission?");
                    builder.setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            ActivityCompat.requestPermissions(ScreenshotMiddleMan.this,
                                    new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
                                    1337);
                        }
                    });
                    builder.setNegativeButton("No", null);
                    builder.show();

                } else {

                    // Camera permission has not been granted yet. Request it directly.
                    ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
                            1337);
                }
            }

        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {

        if (requestCode == 1337) {

            if (grantResults.length == 1 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {

                android.os.Process.killProcess(android.os.Process.myPid());

                screenShotLogic();
            }
        } else {
            super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        }
    }

    private void screenShotLogic() {
        new Thread() {
            @Override
            public void run() {
                Intent launch = new Intent(ScreenshotMiddleMan.this, ScreenshotActivity.class);
                String ss_path = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES) + "/Screenshots";

                DateFormat df = new SimpleDateFormat("yyyy-MM-dd-HH-mm-ss", Locale.getDefault());
                String ss_title = df.format(Calendar.getInstance().getTime());
                //boolean failed = false;
                String ss_name = ss_path + "/" + ss_title + ".png";
                File ss_check = new File(ss_path);
                boolean directorySuccessful = ss_check.mkdirs();

                Log.d("CMSPen", "screencap -p " + ss_name);
                if(Shell.SU.available())
                {
                    Shell.SU.run("screencap -p " + ss_name);
                }

                int rotation = ((WindowManager) ScreenshotMiddleMan.this.getSystemService(Context.WINDOW_SERVICE)).getDefaultDisplay().getRotation();

                launch.putExtra("rotation", rotation);
                PreferenceManager.getDefaultSharedPreferences(ScreenshotMiddleMan.this).edit().putBoolean("toRotate", true).apply();
                launch.putExtra("ss_name", ss_name);
                launch.putExtra("ss_title", ss_title);
                //launch.putExtra("failed", failed);

                launch.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                launch.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK);
                launch.addFlags(Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS);
                launch.addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY);
                ScreenshotMiddleMan.this.startActivity(launch);
            }
        }.start();
    }
}
