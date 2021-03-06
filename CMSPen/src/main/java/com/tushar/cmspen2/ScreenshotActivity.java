package com.tushar.cmspen2;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Point;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.ContextThemeWrapper;
import android.view.Display;
import android.view.LayoutInflater;
import android.view.Surface;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.WindowManager;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

public class ScreenshotActivity extends Activity {
    File imgFile;
    CropView cropView;
    DrawView drawView;
    ColorPickerView pickerView = null;
    EraserView eraserView = null;
    RelativeLayout default_controls, crop_controls, edit_controls;
    Bitmap myBitmap;
    ImageView myImage;
    int CURRENT_MODE = 0;
    Point size = new Point();
    float scale = 1.0f;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_screenshot);

        WindowManager wm = (WindowManager) ScreenshotActivity.this.getSystemService(Context.WINDOW_SERVICE);
        Display display = wm.getDefaultDisplay();
        display.getSize(size);
        scale = 1.0f;

        default_controls = (RelativeLayout) findViewById(R.id.default_controls);
        crop_controls = (RelativeLayout) findViewById(R.id.crop_controls);
        edit_controls = (RelativeLayout) findViewById(R.id.edit_controls);

        String ss_name = getIntent().getStringExtra("ss_name");
        String ss_title = getIntent().getStringExtra("ss_title");
        int rotation = getIntent().getIntExtra("rotation", Surface.ROTATION_0);
        boolean failed = getIntent().getBooleanExtra("failed", false);
        if(failed)
        {
            AlertDialog.Builder builder = new AlertDialog.Builder(new ContextThemeWrapper(this,android.R.style.Theme_DeviceDefault));
            builder.setTitle("Screenshot failed");
            builder.setMessage("Please install SuperSU from the Play Store and try again.");
            builder.setNeutralButton("Ok", new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int id) {
                    finish();
                }
            });
            builder.setCancelable(false);
            builder.show();
        }

        setTitle(ss_title);

        imgFile = new  File(ss_name);

        if(imgFile.exists()){

            sendBroadcast(new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE, Uri.fromFile(imgFile)));

            BitmapFactory.Options opt = new BitmapFactory.Options();
            opt.inMutable = true;
            myBitmap = BitmapFactory.decodeFile(imgFile.getAbsolutePath(), opt);

            if(myBitmap != null)
            {
                //Handle Rotation

                if(PreferenceManager.getDefaultSharedPreferences(this).getBoolean("toRotate", false)) {

                    int angle;

                    switch (rotation) {
                        case Surface.ROTATION_90:
                            angle = 90;
                            break;
                        case Surface.ROTATION_180:
                            angle = 180;
                            break;
                        case Surface.ROTATION_270:
                            angle = 270;
                            break;
                        default:
                            angle = 0;
                    }

                    angle = 360 - angle;

                    if(PreferenceManager.getDefaultSharedPreferences(this).getBoolean("orientation_fix_enable", false))
                        angle += 90;

                    if(angle == 360)
                        angle = 0;

                    //Toast.makeText(this, String.valueOf(angle) + " " + (360 - angle), Toast.LENGTH_LONG).show();

                    if (angle != 0/*defaultRotation*/) {
                        Matrix matrix = new Matrix();
                        matrix.setRotate(angle/* - defaultRotation*/);

                        myBitmap = Bitmap.createBitmap(myBitmap, 0, 0, myBitmap.getWidth(), myBitmap.getHeight(), matrix, true);
                        myBitmap = myBitmap.copy(Bitmap.Config.ARGB_8888, true);
                        saveChanges(true);
                    }

                    PreferenceManager.getDefaultSharedPreferences(this).edit().putBoolean("toRotate", false).apply();
                }
                //End Rotation

                myImage = (ImageView) findViewById(R.id.ss_img_view);

                myImage.setImageBitmap(myBitmap);

                myImage.invalidate();
            }
        }

        //Crop
        ImageView crop = (ImageView) findViewById(R.id.crop);
        crop.setOnClickListener(new OnClickListener () {

            @Override
            public void onClick(View arg0) {
                cropMode();
            }

        });

        ImageView crop_save = (ImageView) findViewById(R.id.crop_save);
        crop_save.setOnClickListener(new OnClickListener () {

            @Override
            public void onClick(View arg0) {
                try
                {
                    myBitmap = Bitmap.createBitmap(myBitmap,
                            (int)(cropView.corners[0].x / scale),
                            (int)(cropView.corners[0].y / scale),
                            (int)((cropView.corners[3].x - cropView.corners[0].x) / scale),
                            (int)((cropView.corners[3].y - cropView.corners[0].y) / scale));
                    final SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(ScreenshotActivity.this);
                    boolean overwrite = pref.getBoolean("crop_overwrite", true);
                    boolean ask_again = pref.getBoolean("crop_ask", true);
                    if(ask_again)
                    {
                        AlertDialog.Builder builder = new AlertDialog.Builder(
                                new ContextThemeWrapper(ScreenshotActivity.this, android.R.style.Theme_DeviceDefault));
                        builder.setTitle("Overwrite?");
                        LayoutInflater inflater = (LayoutInflater)new ContextThemeWrapper(ScreenshotActivity.this,
                                android.R.style.Theme_DeviceDefault)
                                .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
                        View content = inflater.inflate(R.layout.crop_ask, null);
                        final CheckBox ask = (CheckBox) content.findViewById(R.id.crop_overwrite_pref);
                        builder.setView(content);
                        builder.setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                saveChanges(true);
                                if (ask.isChecked()) {
                                    SharedPreferences.Editor edit = pref.edit();
                                    edit.putBoolean("crop_ask", false);
                                    edit.putBoolean("crop_overwrite", true);
                                    edit.apply();
                                }
                                defaultMode();
                            }
                        });
                        builder.setNegativeButton("No", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                saveChanges(false);
                                if (ask.isChecked()) {
                                    SharedPreferences.Editor edit = pref.edit();
                                    edit.putBoolean("crop_ask", false);
                                    edit.putBoolean("crop_overwrite", false);
                                    edit.apply();
                                }
                                defaultMode();
                            }
                        });
                        builder.show();
                    }
                    else {
                        saveChanges(overwrite);
                        defaultMode();
                    }
                }
                catch(IllegalArgumentException e)
                {
                    Toast.makeText(ScreenshotActivity.this, "Invalid Selection!", Toast.LENGTH_LONG).show();
                }
            }

        });

        ImageView crop_discard = (ImageView) findViewById(R.id.crop_discard);
        crop_discard.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View arg0) {
                defaultMode();
            }

        });

        //Edit
        ImageView edit = (ImageView) findViewById(R.id.edit);
        edit.setOnClickListener(new OnClickListener () {

            @Override
            public void onClick(View arg0) {
                final SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(ScreenshotActivity.this);
                boolean show_instructions = pref.getBoolean("show_instructions", true);
                if(show_instructions)
                {
                    AlertDialog.Builder builder = new AlertDialog.Builder(
                            new ContextThemeWrapper(ScreenshotActivity.this, android.R.style.Theme_DeviceDefault));
                    builder.setTitle("Instructions");
                    LayoutInflater inflater = (LayoutInflater)new ContextThemeWrapper(ScreenshotActivity.this,
                            android.R.style.Theme_DeviceDefault)
                            .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
                    View content = inflater.inflate(R.layout.crop_ask, null);
                    final CheckBox ask = (CheckBox) content.findViewById(R.id.crop_overwrite_pref);
                    TextView ask_body = (TextView) content.findViewById(R.id.crop_ask_body);
                    ask_body.setText("Press the S Pen button to toggle between Erasing and Drawing mode.\n\n" +
                            "Long press the Eraser icon on the bottom-left corner to clear the screen.");
                    ask.setText("Do not show again");
                    builder.setView(content);
                    builder.setNeutralButton("Ok", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            saveChanges(true);
                            if (ask.isChecked()) {
                                SharedPreferences.Editor edit = pref.edit();
                                edit.putBoolean("show_instructions", false);
                                edit.apply();
                            }
                        }
                    });
                    builder.show();
                }
                editMode();
            }

        });

        if(!PreferenceManager.getDefaultSharedPreferences(ScreenshotActivity.this).getBoolean("edit_features", false))
            edit.setVisibility(View.GONE);
        else
            edit.setVisibility(View.VISIBLE);

        ImageView undo = (ImageView) findViewById(R.id.edit_undo);
        undo.setOnClickListener(new OnClickListener () {

            @Override
            public void onClick(View arg0) {
                drawView.onClickUndo();
            }

        });

        ImageView redo = (ImageView) findViewById(R.id.edit_redo);
        redo.setOnClickListener(new OnClickListener () {

            @Override
            public void onClick(View arg0) {
                drawView.onClickRedo();
            }

        });

        ImageView edit_discard = (ImageView) findViewById(R.id.edit_discard);
        edit_discard.setOnClickListener(new OnClickListener () {

            @Override
            public void onClick(View arg0) {
                defaultMode();
            }

        });

        ImageView edit_save = (ImageView) findViewById(R.id.edit_save);
        edit_save.setOnClickListener(new OnClickListener () {

            @Override
            public void onClick(View arg0) {
                if(myBitmap != null)
                {
                    Canvas canvas = new Canvas(myBitmap);
                    drawView.setDrawingCacheQuality(View.DRAWING_CACHE_QUALITY_HIGH);
                    drawView.setDrawingCacheEnabled(true);
                    Bitmap b = drawView.getDrawingCache();
                    if(scale != 1.0f)
                        b = Bitmap.createScaledBitmap(b, myBitmap.getWidth(), myBitmap.getHeight(), true);

                    Paint paint = new Paint();
                    paint.setAntiAlias(true);
                    paint.setFilterBitmap(true);
                    canvas.drawBitmap(b, 0, 0, paint);

                    final SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(ScreenshotActivity.this);
                    boolean overwrite = pref.getBoolean("edit_overwrite", true);
                    boolean ask_again = pref.getBoolean("edit_ask", true);
                    if(ask_again)
                    {
                        AlertDialog.Builder builder = new AlertDialog.Builder(
                                new ContextThemeWrapper(ScreenshotActivity.this, android.R.style.Theme_DeviceDefault));
                        builder.setTitle("Overwrite?");
                        LayoutInflater inflater = (LayoutInflater)new ContextThemeWrapper(ScreenshotActivity.this,
                                android.R.style.Theme_DeviceDefault)
                                .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
                        View content = inflater.inflate(R.layout.crop_ask, null);
                        final CheckBox ask = (CheckBox) content.findViewById(R.id.crop_overwrite_pref);
                        builder.setView(content);
                        builder.setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                saveChanges(true);
                                if (ask.isChecked()) {
                                    SharedPreferences.Editor edit = pref.edit();
                                    edit.putBoolean("edit_ask", false);
                                    edit.putBoolean("edit_overwrite", true);
                                    edit.apply();
                                }
                                defaultMode();
                            }
                        });
                        builder.setNegativeButton("No", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                saveChanges(false);
                                if (ask.isChecked()) {
                                    SharedPreferences.Editor edit = pref.edit();
                                    edit.putBoolean("edit_ask", false);
                                    edit.putBoolean("edit_overwrite", false);
                                    edit.apply();
                                }
                                defaultMode();
                            }
                        });
                        builder.show();
                    }
                    else {
                        saveChanges(overwrite);
                        defaultMode();
                    }
                }
            }

        });

        ImageView erase = (ImageView) findViewById(R.id.edit_erase);
        erase.setOnClickListener(new OnClickListener () {

            @Override
            public void onClick(View arg0) {
                drawView.eraserMode();

                int margin = (int)(0.025f * size.x);
                int mWidth = (size.x * 4) / 5 + 2 * margin;
                int mHeight = size.y / 9 + 2 * margin;

                RelativeLayout.LayoutParams params = new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.MATCH_PARENT,
                        RelativeLayout.LayoutParams.MATCH_PARENT);
                final ImageView outOfBounds = new ImageView(ScreenshotActivity.this);
                edit_controls.addView(outOfBounds, params);

                eraserView = new EraserView(ScreenshotActivity.this, margin, drawView.getEraseWidthOffset(),
                        new EraserView.OnWidthChangedListener() {
                            @Override
                            public void widthChanged(int width) {
                                drawView.setEraserWidth(width);
                                if(eraserView != null)
                                {
                                    edit_controls.removeView(eraserView);
                                    edit_controls.removeView(outOfBounds);
                                }
                            }
                        });

                outOfBounds.setOnClickListener(new OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        if(eraserView != null) {
                            edit_controls.removeView(eraserView);
                            edit_controls.removeView(outOfBounds);
                        }
                    }
                });

                params = new RelativeLayout.LayoutParams(mWidth, mHeight);
                //params.addRule(RelativeLayout.ALIGN_PARENT_LEFT);
                params.addRule(RelativeLayout.ALIGN_LEFT, R.id.edit_erase);
                params.addRule(RelativeLayout.ABOVE, R.id.edit_erase);
                params.bottomMargin = 30;
                edit_controls.addView(eraserView, params);
                Toast.makeText(ScreenshotActivity.this,
                        "Click on the right side to select", Toast.LENGTH_LONG).show();
            }

        });

        erase.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                if (drawView != null)
                    drawView.clearScreen();
                return true;
            }
        });

        final ImageView color = (ImageView) findViewById(R.id.edit_color);
        color.setColorFilter(PreferenceManager.getDefaultSharedPreferences(ScreenshotActivity.this).getInt("draw_color", Color.RED));
        color.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View arg0) {

                int margin = (int) (0.025f * size.x);
                int mWidth = (size.x * 3) / 4 + 2 * margin;
                int mHeight = size.y / 2 + 2 * margin;

                RelativeLayout.LayoutParams params = new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.MATCH_PARENT,
                        RelativeLayout.LayoutParams.MATCH_PARENT);
                final ImageView outOfBounds = new ImageView(ScreenshotActivity.this);
                edit_controls.addView(outOfBounds, params);

                pickerView = new ColorPickerView(ScreenshotActivity.this, new ColorPickerView.OnColorChangedListener() {
                    @Override
                    public void colorChanged(int newColor, int sliderOffset) {
                        color.setColorFilter(newColor);
                        drawView.setColor(newColor);
                        drawView.setWidth(sliderOffset);
                        if (pickerView != null) {
                            edit_controls.removeView(pickerView);
                            edit_controls.removeView(outOfBounds);
                        }
                    }
                }, drawView.getChosenColor(), drawView.getDrawWidthOffset(), margin);

                outOfBounds.setOnClickListener(new OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        if (pickerView != null) {
                            edit_controls.removeView(pickerView);
                            edit_controls.removeView(outOfBounds);
                        }
                    }
                });

                params = new RelativeLayout.LayoutParams(mWidth, mHeight);
                params.addRule(RelativeLayout.CENTER_IN_PARENT);
                edit_controls.addView(pickerView, params);
                Toast.makeText(ScreenshotActivity.this,
                        "Click the bottom-right corner to select", Toast.LENGTH_LONG).show();
            }

        });

        //Share
        ImageView share = (ImageView) findViewById(R.id.share);
        share.setOnClickListener(new OnClickListener () {

            @Override
            public void onClick(View arg0) {
                Intent sharingIntent = new Intent(Intent.ACTION_SEND);
                Uri screenshotUri = Uri.fromFile(imgFile);
                sharingIntent.setType("image/png");
                sharingIntent.putExtra(Intent.EXTRA_STREAM, screenshotUri);
                startActivity(Intent.createChooser(sharingIntent, "Share using"));
            }

        });

        //Delete
        ImageView delete = (ImageView) findViewById(R.id.delete);
        delete.setOnClickListener(new OnClickListener () {

            @Override
            public void onClick(View arg0) {
                AlertDialog.Builder builder = new AlertDialog.Builder(
                        new ContextThemeWrapper(ScreenshotActivity.this, android.R.style.Theme_DeviceDefault));
                builder.setTitle("Delete Screenshot?");
                builder.setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        boolean result = imgFile.delete();
                        sendBroadcast(new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE, Uri.fromFile(imgFile)));
                        finish();
                    }
                });
                builder.setNegativeButton("No", null);
                builder.show();
            }

        });
    }

    private void saveChanges(boolean overwrite)
    {
        if(myBitmap != null) {
            FileOutputStream out = null;
            try {
                String path = "";
                if (overwrite)
                    out = new FileOutputStream(imgFile);
                else {
                    path = imgFile.getAbsolutePath();
                    int l = path.length();
                    path = path.substring(0, l - 4) + "_edited.png";
                    out = new FileOutputStream(path);
                }
                myBitmap.compress(Bitmap.CompressFormat.PNG, 100, out);
                if (!overwrite) {
                    imgFile = new File(path);
                    sendBroadcast(new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE, Uri.fromFile(imgFile)));
                }
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                try {
                    if (out != null) {
                        out.close();
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    private void cropMode()
    {
        default_controls.setVisibility(View.GONE);
        edit_controls.setVisibility(View.GONE);
        crop_controls.setVisibility(View.VISIBLE);
        CURRENT_MODE = 1;
        if(cropView != null)
            crop_controls.removeView(cropView);
        cropView = new CropView(ScreenshotActivity.this);
        setScaledView(crop_controls, cropView);
    }

    private void editMode()
    {
        default_controls.setVisibility(View.GONE);
        crop_controls.setVisibility(View.GONE);
        edit_controls.setVisibility(View.VISIBLE);
        CURRENT_MODE = 2;
        if(drawView != null)
            edit_controls.removeView(drawView);
        drawView = new DrawView(ScreenshotActivity.this);
        setScaledView(edit_controls, drawView);
    }

    private void setScaledView(RelativeLayout parent, View child)
    {
        int localWidth = 0, localHeight = 0;
        if(myBitmap != null) {
            localWidth = myBitmap.getWidth();
            localHeight = myBitmap.getHeight();

            if (localHeight > size.y) {
                scale = (float) size.y / localHeight;
                localHeight = size.y;
                localWidth *= scale;
            } else if (localWidth > size.x) {
                scale = (float) size.x / localWidth;
                localWidth = size.x;
                localHeight *= scale;
            } else
                scale = 1.0f;
        }

        if(drawView != null)
            drawView.setScale(scale);

        if(child != null) {
            RelativeLayout.LayoutParams params = new RelativeLayout.LayoutParams(localWidth,
                    localHeight);
            params.addRule(RelativeLayout.CENTER_IN_PARENT);
            parent.addView(child, 0, params);
        }
    }

    private void defaultMode()
    {
        default_controls.setVisibility(View.VISIBLE);
        crop_controls.setVisibility(View.GONE);
        edit_controls.setVisibility(View.GONE);
        CURRENT_MODE = 0;
        BitmapFactory.Options opt = new BitmapFactory.Options();
        opt.inMutable = true;
        myBitmap = BitmapFactory.decodeFile(imgFile.getAbsolutePath(), opt);
        if(myBitmap != null) {
            myImage.setImageBitmap(myBitmap);
            myImage.invalidate();
        }
    }
}
