/***
 Copyright 2013-2015 Tushar Dudani

 Licensed under the Apache License, Version 2.0 (the "License");
 you may not use this file except in compliance with the License.
 You may obtain a copy of the License at

 http://www.apache.org/licenses/LICENSE-2.0

 Unless required by applicable law or agreed to in writing, software
 distributed under the License is distributed on an "AS IS" BASIS,
 WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 See the License for the specific language governing permissions and
 limitations under the License.
 */

package com.tushar.cmspen2;

import com.tushar.cmspen2.libsuperuser.Shell;
import com.tushar.cmspen2.util.IabHelper;
import com.tushar.cmspen2.util.IabResult;
import com.tushar.cmspen2.util.Inventory;
import com.tushar.cmspen2.util.Purchase;

import com.tushar.cmspen2.libsuperuser.Events;
import com.tushar.cmspen2.libsuperuser.Events.InputDevice;

import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.preference.PreferenceManager;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;
import android.util.Log;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

public class MainActivity extends Activity {
    //int id = -1;
    String path = "";
    boolean compatible = false;
    IabHelper mHelper;
    boolean success = true;
    boolean setup = false;
    static final String BASE64_KEY = "baqadiW3It7bF8JSGKlw8EzP0ET2SqlN8gT7cNyMMWd91YnEzwVv1GAu3Zi4Gx0SXKMp7JnK2s8v5MBavlM/y9eXxC050kYWwX+zU9mo+tQzorcX0axyP9Jgv/vnM37mHpPmmhgPbTVWh+XULCrnsCiM3O/x+GnRWDNMppxM2Mz0vwAO15LgHsucmenQjf5sZU2Xz96oK/fQ+LGC1PxI+pISg1AxLxg6fMpVjHToRwL8/MKYfLQSRWW7WTOVKt8+58ZJINw9tQwhR2b07b4AASPo3ei9BIGkZxbU0aydxGy4lSZaEEfTDFMibSLGs1MPdgQgezWdSAvOHnZsaFCrTL9Ee+FHaeqackGcbiima8qacoaafeqab0W9gIKHQKGbnaJibiim";
    static final int KB_CODE = 100010;
    boolean done = false;
    boolean done_check = false;
    static LegacyHandler h;
    private int displayMode = 0;
    RelativeLayout buy_frame, edit_buy_frame, keyb, block, soft_block, ss, main_button_container, 
            legacy_frame, edit_hover, button_adjustments, orientation_fix;
    LinearLayout button_frame;
    Button buy, edit_buy, activate, keyboard_main_button, touchscreen_main_button,
            screenshot_main_button, bfeat_main_button, check_legacy;
    ToggleButton block_enable, soft_block_enable, ss_enable, kb_enable, 
            startStop, edit_hover_enable, orientation_fix_enable;
    CheckBox logging, intensive_mode, test_features;
    SeekBar minSeekBar, maxSeekBar;
    TextView minSeekDisp, maxSeekDisp;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
        final SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());

        buy_frame = (RelativeLayout) findViewById(R.id.buy_frame);
        edit_buy_frame = (RelativeLayout) findViewById(R.id.edit_buy_frame);
        button_frame = (LinearLayout) findViewById(R.id.button_frame);
        legacy_frame = (RelativeLayout) findViewById(R.id.legacy_frame);
        block = (RelativeLayout) findViewById(R.id.block);
        soft_block = (RelativeLayout) findViewById(R.id.soft_block);
        ss = (RelativeLayout) findViewById(R.id.ss);
        main_button_container = (RelativeLayout) findViewById(R.id.main_button_container);
        edit_hover = (RelativeLayout) findViewById(R.id.edit_hover);
        button_adjustments= (RelativeLayout) findViewById(R.id.button_adjustments);
        orientation_fix = (RelativeLayout) findViewById(R.id.default_landscape);

        minSeekBar = (SeekBar) findViewById(R.id.minSeekBar);
        maxSeekBar = (SeekBar) findViewById(R.id.maxSeekBar);

        minSeekDisp = (TextView) findViewById(R.id.minSeekDisp);
        maxSeekDisp = (TextView) findViewById(R.id.maxSeekDisp);

        keyboard_main_button = (Button) findViewById(R.id.keyboard_main_button);
        keyboard_main_button.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                KeyboardMode();
            }
        });
        screenshot_main_button = (Button) findViewById(R.id.screenshot_main_button);
        screenshot_main_button.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                ScreenshotMode();
            }
        });
        touchscreen_main_button = (Button) findViewById(R.id.touchscreen_main_button);
        touchscreen_main_button.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                TouchscreenMode();
            }
        });
        bfeat_main_button = (Button) findViewById(R.id.bfeat_main_button);
        bfeat_main_button.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                ButtonMode();
            }
        });
        check_legacy = (Button) findViewById(R.id.check_legacy);
        check_legacy.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                if(legacyCheck(MainActivity.this))
                {
                    SharedPreferences.Editor edit = pref.edit();
                    edit.putBoolean("button_features", true);
                    edit.putBoolean("test_features", false);
                    edit.putInt("legacy_purchase", 1);
                    edit.apply();
                    legacy_frame.setVisibility(View.GONE);
                    buy_frame.setVisibility(View.GONE);
                    button_frame.setVisibility(View.VISIBLE);
                }
                else
                    Toast.makeText(MainActivity.this, "Previous purchase was not detected!", Toast.LENGTH_LONG).show();
            }
        });
        //enableBlockCompat(this);

        mHelper = new IabHelper(this, GetString(BASE64_KEY));
        try
        {
            mHelper.startSetup(new IabHelper.OnIabSetupFinishedListener() {
                public void onIabSetupFinished(IabResult result) {
                    setup = true;
                    if (!result.isSuccess()) {
                        Log.d("CMSPen", "Problem setting up In-app Billing: " + result);
                        success = false;
                        SharedPreferences.Editor edit = pref.edit();
                        edit.putBoolean("button_features", false);
                        edit.putBoolean("edit_features", false);
                        edit.apply();
                        Log.d("CMSPen", "Button Features not purchased.");
                        Log.d("CMSPen", "Edit Features purchased.");
                        //edit_buy_frame.setVisibility(View.VISIBLE);
                        //buy_frame.setVisibility(View.VISIBLE);
                        //button_frame.setVisibility(View.GONE);
                        done = true;
                    }
                    else
                    {
                        Log.d("CMSPen", "In-app Billing: " + result);

                        mHelper.queryInventoryAsync(new IabHelper.QueryInventoryFinishedListener() {

                            @Override
                            public void onQueryInventoryFinished(IabResult result, Inventory inv) {

                                if(result.isSuccess())
                                {
                                    SharedPreferences.Editor edit = pref.edit();
                                    if(inv.hasPurchase("button_features"))
                                    {
                                        edit.putBoolean("button_features", true);
                                        edit.putBoolean("test_features", false);
                                        edit.apply();
                                        Log.d("CMSPen", "Button Features purchased.");
                                        //buy_frame.setVisibility(View.GONE);
                                        //button_frame.setVisibility(View.VISIBLE);
                                    }
                                    else
                                    {
                                        edit.putBoolean("button_features", false);
                                        edit.apply();
                                        Log.d("CMSPen", "Button Features not purchased.");
                                        //buy_frame.setVisibility(View.VISIBLE);
                                        //button_frame.setVisibility(View.GONE);
                                    }

                                    if(inv.hasPurchase("edit_features"))
                                    {
                                        edit.putBoolean("edit_features", true);
                                        edit.apply();
                                        Log.d("CMSPen", "Edit Features purchased.");
                                        //edit_buy_frame.setVisibility(View.GONE);
                                    }
                                    else
                                    {
                                        edit.putBoolean("edit_features", false);
                                        edit.apply();
                                        Log.d("CMSPen", "Edit Features not purchased.");
                                        //edit_buy_frame.setVisibility(View.VISIBLE);
                                    }
                                }
                                done = true;
                            }
                        });
                    }
                }
            });
        }
        catch(Exception e)
        {
            success = false;
            SharedPreferences.Editor edit = pref.edit();
            edit.putBoolean("button_features", false);
            edit.putBoolean("edit_features", false);
            edit.apply();
            Log.d("CMSPen", "Button Features not purchased.");
            Log.d("CMSPen", "Edit Features not purchased.");
            //buy_frame.setVisibility(View.VISIBLE);
            //edit_buy_frame.setVisibility(View.VISIBLE);
            //button_frame.setVisibility(View.GONE);
            done = true;
        }

        if(pref.getInt("legacy_purchase", -1) == -1)
        {
            SharedPreferences.Editor edit = pref.edit();
            if(legacyCheck(this))
                edit.putInt("legacy_purchase", 1);
            else
                edit.putInt("legacy_purchase", 0);
            edit.apply();
            done_check = true;
        }
        else
            done_check = true;

        h = new LegacyHandler(pref/*, buy_frame, button_frame*/);

        new Thread()
        {
            public void run() {
                while(!done || !done_check)
                {
                    try
                    {
                        Thread.sleep(100);
                    }
                    catch (Exception e)
                    {
                        e.printStackTrace();
                    }
                }
                /*if(!pref.getBoolean("orientation_set", false))
                {
                    WindowManager wm = (WindowManager) MainActivity.this.getSystemService(Context.WINDOW_SERVICE);
                    Display display = wm.getDefaultDisplay();
                    Point size = new Point();
                    display.getSize(size);
                    SharedPreferences.Editor edit = pref.edit();
                    if(size.x > size.y)
                        edit.putBoolean("landscape_enable", true);
                    edit.putBoolean("orientation_set", true);
                    edit.apply();
                }*/
                h.sendEmptyMessage(0);
            }
        }.start();


        buy = (Button) findViewById(R.id.buy);
        final IabHelper.OnIabPurchaseFinishedListener PurchaseListener = new IabHelper.OnIabPurchaseFinishedListener() {

            @Override
            public void onIabPurchaseFinished(IabResult result, Purchase info) {

                if(result.isFailure())
                {
                    return;
                }
                if(info.getSku().equals("button_features"))
                {
                    SharedPreferences.Editor edit = pref.edit();
                    edit.putBoolean("button_features", true);
                    edit.putBoolean("test_features", false);
                    edit.apply();
                    Log.d("CMSPen", "Button Features purchased.");
                    buy_frame.setVisibility(View.GONE);
                    button_frame.setVisibility(View.VISIBLE);
                    legacy_frame.setVisibility(View.GONE);
                }
                else if(info.getSku().equals("edit_features"))
                {
                    SharedPreferences.Editor edit = pref.edit();
                    edit.putBoolean("edit_features", true);
                    edit.apply();
                    Log.d("CMSPen", "Edit Features purchased.");
                    edit_buy_frame.setVisibility(View.GONE);
                }
            }
        };
        buy.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View v) {

                if (success && !mHelper.mAsyncInProgress && setup)
                    mHelper.launchPurchaseFlow(MainActivity.this, "button_features", 10001, PurchaseListener);

            }

        });

        edit_buy = (Button) findViewById(R.id.edit_buy);
        edit_buy.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View v) {

                if(success && !mHelper.mAsyncInProgress && setup)
                    mHelper.launchPurchaseFlow(MainActivity.this, "edit_features", 10001, PurchaseListener);

            }

        });

        activate = (Button) findViewById(R.id.activate);
        activate.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View v) {
                Intent act_int = new Intent("com.tushar.cm_spen.BUTTON_ACTIVATE");
                act_int.setPackage("com.tushar.spen_helper");
                if (!appInstalledOrNot("com.tushar.spen_helper", MainActivity.this)) {
                    Uri uri = Uri.parse("https://play.google.com/store/apps/details?id=com.tushar.spen_helper");
                    act_int = new Intent(Intent.ACTION_VIEW, uri);
                    startActivity(act_int);
                } else {
                    MainActivity.this.sendBroadcast(act_int);
                }
            }

        });


        block_enable = (ToggleButton) findViewById(R.id.block_enable);
        if(pref.getBoolean("block_enable", false))
            block_enable.setChecked(true);
        else
            block_enable.setChecked(false);

        block_enable.setOnCheckedChangeListener(new OnCheckedChangeListener() {

            @Override
            public void onCheckedChanged(CompoundButton buttonView,
                                         boolean isChecked) {

                SharedPreferences.Editor edit = pref.edit();
                if (isChecked)
                    edit.putBoolean("block_enable", true);
                else
                    edit.putBoolean("block_enable", false);
                edit.apply();
            }

        });

        soft_block_enable = (ToggleButton) findViewById(R.id.soft_block_enable);
        if(pref.getBoolean("soft_block_enable", false))
            soft_block_enable.setChecked(true);
        else
            soft_block_enable.setChecked(false);

        soft_block_enable.setOnCheckedChangeListener(new OnCheckedChangeListener () {

            @Override
            public void onCheckedChanged(CompoundButton buttonView,
                                         boolean isChecked) {

                SharedPreferences.Editor edit = pref.edit();
                if(isChecked)
                    edit.putBoolean("soft_block_enable", true);
                else
                    edit.putBoolean("soft_block_enable", false);
                edit.apply();
            }

        });

        orientation_fix_enable = (ToggleButton) findViewById(R.id.default_landscape_enable);
        if(pref.getBoolean("orientation_fix_enable", false))
            orientation_fix_enable.setChecked(true);
        else
            orientation_fix_enable.setChecked(false);

        orientation_fix_enable.setOnCheckedChangeListener(new OnCheckedChangeListener () {

            @Override
            public void onCheckedChanged(CompoundButton buttonView,
                                         boolean isChecked) {

                SharedPreferences.Editor edit = pref.edit();
                if(isChecked)
                    edit.putBoolean("orientation_fix_enable", true);
                else
                    edit.putBoolean("orientation_fix_enable", false);
                edit.apply();
            }

        });

        ss_enable = (ToggleButton) findViewById(R.id.ss_enable);
        if(pref.getBoolean("ss_enable", false))
            ss_enable.setChecked(true);
        else
            ss_enable.setChecked(false);

        ss_enable.setOnCheckedChangeListener(new OnCheckedChangeListener () {

            @Override
            public void onCheckedChanged(CompoundButton buttonView,
                                         boolean isChecked) {

                SharedPreferences.Editor edit = pref.edit();
                if(isChecked)
                    edit.putBoolean("ss_enable", true);
                else
                    edit.putBoolean("ss_enable", false);
                edit.apply();
            }

        });


        kb_enable = (ToggleButton) findViewById(R.id.kb_enable);
        if(pref.getBoolean("kb_enable", false))
            kb_enable.setChecked(true);
        else
            kb_enable.setChecked(false);

        kb_enable.setOnCheckedChangeListener(new OnCheckedChangeListener () {

            @Override
            public void onCheckedChanged(CompoundButton buttonView,
                                         boolean isChecked) {

                SharedPreferences.Editor edit = pref.edit();
                if(isChecked)
                {
                    edit.putBoolean("kb_enable", true);
                    Intent i = new Intent(MainActivity.this, KeyboardActivity.class);
                    i.putExtra("c_kb", pref.getString("s_kb", ""));
                    startActivityForResult(i, KB_CODE);
                }
                else
                    edit.putBoolean("kb_enable", false);

                edit.apply();
            }

        });

        edit_hover_enable = (ToggleButton) findViewById(R.id.edit_hover_enable);
        if(pref.getBoolean("edit_hover_enable", true))
            edit_hover_enable.setChecked(true);
        else
            edit_hover_enable.setChecked(false);

        edit_hover_enable.setOnCheckedChangeListener(new OnCheckedChangeListener () {
            @Override
            public void onCheckedChanged(CompoundButton buttonView,
                                         boolean isChecked) {
                SharedPreferences.Editor edit = pref.edit();
                if(isChecked)
                    edit.putBoolean("edit_hover_enable", true);
                else
                    edit.putBoolean("edit_hover_enable", false);
                edit.apply();
            }
        });

        keyb = (RelativeLayout) findViewById(R.id.keyb);
        keyb.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View arg0) {

                if(kb_enable.isChecked())
                {
                    Intent i = new Intent(MainActivity.this, KeyboardActivity.class);
                    i.putExtra("c_kb", pref.getString("s_kb", ""));
                    startActivityForResult(i, KB_CODE);
                }

            }

        });


        logging = (CheckBox) findViewById(R.id.logging);
        intensive_mode = (CheckBox) findViewById(R.id.intensive_mode);
        test_features = (CheckBox) findViewById(R.id.test_features);
        if(/*pref.getInt("id", id) == -1*/pref.getString("path", path).equals(""))
        {
            try {
                compatible = new CheckComp().execute().get();
            } catch (Exception e) {
                e.printStackTrace();
            }
            if(compatible)
            {
                SharedPreferences.Editor editor = pref.edit();
                //editor.putInt("id", id);
                editor.putString("path", path);
                editor.apply();
            }
            else
            {
                AlertDialog.Builder builderdonate = new AlertDialog.Builder(this);
                builderdonate.setTitle("Access Denied!");
                builderdonate.setMessage("Probable causes are:\n\n" +
                        "-Lack of Root Access.\n" +
                        "To fix this please go to the Developer Settings and make sure that Root Access is enabled.\n\n" +
                        "-SELinux is blocking access.\n" +
                        "To fix this you can install SuperSU.\n\n" +
                        "-You have an incompatible device.");
                builderdonate.setNeutralButton("Ok", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        finish();
                    }
                });
                builderdonate.setCancelable(false);
                builderdonate.show();
            }
        }


        //Button Adjustments

        minSeekBar.setProgress(pref.getInt("long_min_offset", 4));
        minSeekDisp.setText(SPenDetection.Long_Min_Map[minSeekBar.getProgress()] + " ms");

        minSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                minSeekDisp.setText(SPenDetection.Long_Min_Map[progress] + " ms");
                SharedPreferences.Editor edit = pref.edit();
                edit.putInt("long_min_offset", progress);
                int maxSeekProgress = maxSeekBar.getProgress();
                while(SPenDetection.Long_Max_Map[maxSeekProgress] <= SPenDetection.Long_Min_Map[progress])
                    maxSeekProgress++;
                edit.putInt("long_max_offset", maxSeekProgress);
                edit.apply();
                maxSeekBar.setProgress(maxSeekProgress);
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });

        int max_offset = pref.getInt("long_max_offset", 5);
        maxSeekBar.setProgress(max_offset);
        if(max_offset != 16)
            maxSeekDisp.setText(SPenDetection.Long_Max_Map[maxSeekBar.getProgress()] + " ms");
        else
            maxSeekDisp.setText("\u221E");

        maxSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                int minSeekProgress = minSeekBar.getProgress();
                if(SPenDetection.Long_Max_Map[progress] > SPenDetection.Long_Min_Map[minSeekProgress]) {
                    if (progress != 16)
                        maxSeekDisp.setText(SPenDetection.Long_Max_Map[progress] + " ms");
                    else
                        maxSeekDisp.setText("\u221E");
                    pref.edit().putInt("long_max_offset", progress).apply();
                }
                else
                    seekBar.setProgress(progress + 1);
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });

        //Debug
        
        /*final Events events = new Events();
        events.Init();
        for (InputDevice idev:events.m_Devs) 
        {
        		try
        		{
        			idev.Open(true);
        		}
        		catch(Exception e)
        		{
        			e.printStackTrace();
        		}
        }
        
        new Thread() {
        	
        	public void run()
        	{
        		while(true)
        		{
        			for (InputDevice idev:events.m_Devs) {
        				// Open more devices to see their messages
        				if (0 == idev.getPollingEvent()) {
        					final String line = idev.getName()+
        							":" + idev.getSuccessfulPollingType()+
        							" " + idev.getSuccessfulPollingCode() + 
        							" " + idev.getSuccessfulPollingValue();
        					Log.d("CMSPen", "Event: "+line);
        				}
        				
        			}
        		}
        	}
        	
        }.start();*/


        //End Debug

        if(pref.getBoolean("logging", false))
        {
            logging.setChecked(true);
        }
        logging.setOnCheckedChangeListener(new OnCheckedChangeListener() {

            public void onCheckedChanged(CompoundButton buttonView,
                                         boolean isChecked) {
                SharedPreferences.Editor edit = pref.edit();
                if(isChecked)
                {
                    edit.putBoolean("logging", true);
                }
                else
                {
                    edit.putBoolean("logging", false);
                }
                edit.apply();
            }

        });

        if(pref.getBoolean("intensive_mode", false))
        {
            intensive_mode.setChecked(true);
        }
        intensive_mode.setOnCheckedChangeListener(new OnCheckedChangeListener() {

            public void onCheckedChanged(CompoundButton buttonView,
                                         boolean isChecked) {
                SharedPreferences.Editor edit = pref.edit();
                if(isChecked)
                {
                    edit.putBoolean("intensive_mode", true);
                }
                else
                {
                    edit.putBoolean("intensive_mode", false);
                }
                edit.apply();
            }

        });

        if(pref.getBoolean("test_features", false))
        {
            test_features.setChecked(true);
        }
        test_features.setOnCheckedChangeListener(new OnCheckedChangeListener() {

            public void onCheckedChanged(CompoundButton buttonView,
                                         boolean isChecked) {
                SharedPreferences.Editor edit = pref.edit();
                if(isChecked)
                {
                    edit.putBoolean("test_features", true);
                }
                else
                {
                    edit.putBoolean("test_features", false);
                }
                edit.apply();
            }

        });

        startStop = (ToggleButton) findViewById(R.id.onOfftoggle);
        if(pref.getBoolean("enabled", false))
        {
            startStop.setChecked(true);
                //StopEventMonitor(MainActivity.this);
            StartEventMonitor(MainActivity.this);
        }
        startStop.setOnCheckedChangeListener(new OnCheckedChangeListener() {
            public void onCheckedChanged(CompoundButton toggleButton, boolean isChecked) {
                if(isChecked)
                {
                    StartEventMonitor(MainActivity.this);
                    Toast.makeText(MainActivity.this, "Detection Enabled", Toast.LENGTH_SHORT).show();
                    SharedPreferences.Editor editor = pref.edit();
                    editor.putBoolean("enabled", true);
                    editor.apply();
                }
                else
                {
                    Toast.makeText(MainActivity.this, "Detection Disabled", Toast.LENGTH_SHORT).show();
                    StopEventMonitor(MainActivity.this);
                    SharedPreferences.Editor editor = pref.edit();
                    editor.putBoolean("enabled", false);
                    editor.apply();
                }
            }
        });
    }

    private void KeyboardMode()
    {
        displayMode = 1;
        main_button_container.setVisibility(View.GONE);
        keyb.setVisibility(View.VISIBLE);
    }

    private void TouchscreenMode()
    {
        displayMode = 2;
        main_button_container.setVisibility(View.GONE);
        block.setVisibility(View.VISIBLE);
        soft_block.setVisibility(View.VISIBLE);
    }

    private void ScreenshotMode()
    {
        displayMode = 3;
        main_button_container.setVisibility(View.GONE);
        if(!PreferenceManager.getDefaultSharedPreferences(this).getBoolean("edit_features", false))
            edit_buy_frame.setVisibility(View.VISIBLE);
        else
            edit_hover.setVisibility(View.VISIBLE);
        ss.setVisibility(View.VISIBLE);
        button_adjustments.setVisibility(View.VISIBLE);
        orientation_fix.setVisibility(View.VISIBLE);
    }

    private void ButtonMode()
    {
        displayMode = 4;
        main_button_container.setVisibility(View.GONE);
        if(PreferenceManager.getDefaultSharedPreferences(this).getBoolean("button_features", false)) {
            button_frame.setVisibility(View.VISIBLE);
            button_adjustments.setVisibility(View.VISIBLE);
        }
        else {
            buy_frame.setVisibility(View.VISIBLE);
            legacy_frame.setVisibility(View.VISIBLE);
        }
        intensive_mode.setVisibility(View.VISIBLE);
    }

    private void DefaultMode()
    {
        switch(displayMode)
        {
            case 1:
                keyb.setVisibility(View.GONE);
                break;
            case 2:
                block.setVisibility(View.GONE);
                soft_block.setVisibility(View.GONE);
                break;
            case 3:
                edit_buy_frame.setVisibility(View.GONE);
                ss.setVisibility(View.GONE);
                edit_hover.setVisibility(View.GONE);
                button_adjustments.setVisibility(View.GONE);
                orientation_fix.setVisibility(View.GONE);
                break;
            case 4:
                button_frame.setVisibility(View.GONE);
                buy_frame.setVisibility(View.GONE);
                intensive_mode.setVisibility(View.GONE);
                legacy_frame.setVisibility(View.GONE);
                button_adjustments.setVisibility(View.GONE);
                break;
        }
        main_button_container.setVisibility(View.VISIBLE);
        displayMode = 0;
    }

    @Override
    public void onBackPressed() {
        if(displayMode == 0)
            super.onBackPressed();
        else
            DefaultMode();
    }

    static class LegacyHandler extends Handler
    {
        SharedPreferences pref;
        //RelativeLayout buy_frame;
        //LinearLayout button_frame;

        LegacyHandler(SharedPreferences pref/*, RelativeLayout buy_frame, LinearLayout button_frame*/)
        {
            this.pref = pref;
            //this.buy_frame = buy_frame;
            //this.button_frame = button_frame;
        }

        public void handleMessage(Message msg) {

            if(!pref.getBoolean("button_features", false))
            {
                if(pref.getInt("legacy_purchase", -1) == 1)
                {
                    SharedPreferences.Editor edit = pref.edit();
                    edit.putBoolean("button_features", true);
                    edit.putBoolean("test_features", false);
                    edit.apply();
                    //buy_frame.setVisibility(View.GONE);
                    //button_frame.setVisibility(View.VISIBLE);
                }
                else
                {
                    SharedPreferences.Editor edit = pref.edit();
                    edit.putBoolean("button_features", false);
                    edit.apply();
                    //buy_frame.setVisibility(View.VISIBLE);
                    //button_frame.setVisibility(View.GONE);
                }

				/*SharedPreferences.Editor editt = pref.edit();
				editt.putBoolean("button_features", true);
				editt.putBoolean("test_features", false);
				editt.apply();
				Log.d("CMSPen", "Button Features purchased.");
				buy_frame.setVisibility(View.GONE);
				button_frame.setVisibility(View.VISIBLE);*/
            }
        }
    }

    static boolean appInstalledOrNot(String uri, Context ctx)
    {
        PackageManager pm = ctx.getPackageManager();
        boolean app_installed;
        try
        {
            pm.getPackageInfo(uri, PackageManager.GET_ACTIVITIES);
            app_installed = true;
        }
        catch (PackageManager.NameNotFoundException e)
        {
            app_installed = false;
        }
        return app_installed ;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (mHelper != null && success) mHelper.dispose();
        mHelper = null;
    }
    public static void StopEventMonitor(Context ctx) {
        ctx.stopService(new Intent(ctx, SPenDetection.class));
    }

    private boolean legacyCheck(Context ctx)
    {
        if(appInstalledOrNot("com.tushar.cmspen", ctx))
        {
            ExecutorService executorService = Executors.newSingleThreadExecutor();

            Future<Boolean> result = executorService.submit(new Callable<Boolean>() {
                @Override
                public Boolean call() throws Exception {
                    if(Shell.SU.available())
                    {
                        Shell.SU.run("cat /data/data/com.tushar.cmspen/shared_prefs/com.tushar.cmspen_preferences.xml | grep button_features > /sdcard/cmspen.temp");
                        File ip = new File(Environment.getExternalStorageDirectory().getPath() + "/cmspen.temp");
                        if(ip.exists())
                        {
                            try
                            {
                                BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(ip)));
                                String check = br.readLine();
                                return check.contains("true");
                            }
                            catch (Exception e) {
                                e.printStackTrace();
                            }
                            ip.delete();
                        }
                    }
                    return false;
                }
            });

            try {
                return result.get();
            } catch (Exception e) {
                return false;
            }
        }
        return false;
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data)
    {
        super.onActivityResult(requestCode, resultCode, data);
        if(!mHelper.handleActivityResult(requestCode, resultCode, data))
            if(requestCode == KB_CODE)
                if(resultCode == Activity.RESULT_OK)
                {
                    SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(MainActivity.this);
                    SharedPreferences.Editor edit = pref.edit();
                    edit.putString("s_kb", data.getStringExtra("s_kb"));
                    edit.apply();
                }
    }

    public static void StartEventMonitor(Context ctx) {
        ctx.startService(new Intent(ctx,SPenDetection.class));
    }

    private static String GetString(String str)
    {
        char[] result = str.toCharArray();
        for (int i = 0; i < str.length(); i++)
        {
            char temp = str.charAt(i);
            if(temp >= 'a' && temp <= 'z')
                temp = (char) (temp - ('a' - 'A'));
            else if(temp >= 'A' && temp <= 'Z')
                temp = (char) (temp + ('a' - 'A'));
            result[str.length() - i - 1] = temp;
        }
        return String.valueOf(result);
    }

    static void simKey(final int kc)
    {
        new Thread() {
            @Override
            public void run() {
                if(Shell.SU.available())
                {
                    Shell.SU.run("input keyevent " + kc);
                }
            }
        }.start();
    }

    class CheckComp extends AsyncTask<Void, Void, Boolean> {
        ProgressDialog mDialog = new ProgressDialog(MainActivity.this);

        @Override
        protected void onPreExecute()
        {
            mDialog.setMessage("             Checking Compatibility");
            mDialog.setProgressStyle(ProgressDialog.THEME_HOLO_DARK);
            mDialog.setIndeterminate(true);
            mDialog.setCancelable(false);
            if(!MainActivity.this.isFinishing())
                mDialog.show();
        }

        @Override
        protected Boolean doInBackground(Void... arg0) {
            Events events = new Events();
            events.Init();
            Boolean temp = false;
            for (InputDevice idev:events.m_Devs) {
                {
                    try
                    {
                        if(idev.Open(true))
                        {
                            String currentname = idev.getName();
                            if(currentname.contains("sec_e-pen"))
                            {
                                temp = true;
                                path = idev.getPath();
                            }
                            if(temp)
                                break;
                        }
                    }
                    catch(Exception e)
                    {
                        e.printStackTrace();
                    }
                }
            }
            return temp;
        }

        @Override
        protected void onPostExecute(Boolean v) {
            try
            {
                mDialog.dismiss();
            }
            catch(Exception e)
            {
                e.printStackTrace();
            }
        }
    }
}
