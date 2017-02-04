package com.tushar.cmspen2;

import com.tushar.cmspen2.libsuperuser.Events;
import com.tushar.cmspen2.libsuperuser.Events.InputDevice;
import com.tushar.cmspen2.libsuperuser.Shell;

import android.app.KeyguardManager;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.PowerManager;
import android.os.PowerManager.WakeLock;
import android.os.Vibrator;
import android.preference.PreferenceManager;
import android.provider.Settings;
import android.util.Log;
import android.widget.Toast;

import java.util.concurrent.Callable;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

public class SPenDetection extends Service {
    static Vibrator v;
    String path = "";
    int spenFD = -1;
    public static int polling = 3;
    static Intent i = new Intent("com.samsung.pen.INSERT");
    static Intent SPen_Event = new Intent("com.tushar.cm_spen.SPEN_EVENT");
    static WakeLock screenLock;
    static SharedPreferences pref;
    static EventHandler h;
    static Handler pCheck;
    static Handler timeout;
    static String touchs_path = "", touchk_path = "";
    static int touchk_fd = -1;
    static int touchs_fd = -1;
    private static final Object lock = new Object();
    static int Long_Min_Map[] = { 300, 350, 400, 450, 500, 550, 600, 650, 700, 750, 800,
            1000, 1200, 1400, 1600, 1800, 2000, 2200, 2400, 2600, 2800, 3000 };
    static int Long_Max_Map[] = { 1400, 1600, 1800, 2000, 2200, 2400, 2600, 2800, 3000, 3200,
            3400, 3600, 3800, 4000, 4200, 4400, 60000 };

    //Tasker
    private static int Tasker_Last_Event = -1;
    protected static final Intent INTENT_REQUEST_REQUERY =
            new Intent("com.twofortyfouram.locale.intent.action.REQUEST_QUERY")
                    .putExtra("com.twofortyfouram.locale.intent.extra.ACTIVITY",
                            TaskerEditActivity.class.getName());

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    static void waitForEvent()
    {
        Log.d("CMSPen", "waitForEvent() called");
        h.sendEmptyMessage(0);
    }

    //@SuppressWarnings("deprecation")
    @Override
    public void onCreate()
    {
        pref = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        enableBlockCompat(this);
        touchk_fd = touchs_fd = -1;
        touchs_path = pref.getString("touchs_path", "");
        touchk_path = pref.getString("touchk_path", "");
        SPen_Event.setPackage("com.tushar.spen_helper");

        path = pref.getString("path", "");
        if(path.equals(""))
        {
            stopSelf();
            Log.d("CMSPen", "Service stopped due to invalid Path");
        }

        v = (Vibrator) getApplicationContext().getSystemService(VIBRATOR_SERVICE);
        try
        {
            spenFD = openFromPath(path);
            if(spenFD == -1)
            {
                ExecutorService executorService = Executors.newSingleThreadExecutor();

                Future<Integer> sPenFdFuture = executorService.submit(new Callable<Integer>() {
                    @Override
                    public Integer call() throws Exception {
                        int fd = -1;

                        if(Shell.SU.available())
                        {
                            Shell.SU.run("chmod 666 "+ path);
                            fd = openFromPath(path);

                            if(fd == -1)
                            {
                                Shell.SU.run("supolicy --live " +
                                        "\"permissive appdomain\"" +
                                        "\"permissive untrusted_app\"");
                                fd = openFromPath(path);
                            }
                        }

                        return fd;
                    }
                });

                spenFD = sPenFdFuture.get();
            }

            if (spenFD == -1)
            {
                stopSelf();
                Log.d("CMSPen", "SPen open failed");
            }
        }
        catch(Exception e)
        {
            stopSelf();
            Log.d("CMSPen",e.getMessage());
        }

        screenLock = ((PowerManager)getSystemService(POWER_SERVICE)).newWakeLock(PowerManager.FULL_WAKE_LOCK | PowerManager.ACQUIRE_CAUSES_WAKEUP, "CMSPen");
        h = new EventHandler();
        pCheck = new Handler();
        timeout = new Handler();
        new Thread() {
            @Override
            public void run()
            {
                AddListener(path);
            }
        }.start();
    }

    class EventHandler extends Handler
    {
        @Override
        public void handleMessage(Message msg) {
            if(msg.what == 0 && PreferenceManager.getDefaultSharedPreferences(SPenDetection.this).getBoolean("enabled", false))
                new EventAsync().execute();
        }
    }

    class EventAsync extends AsyncTask<Void, Void, Void>
    {
        int hPressCount = 0;
        boolean running = true;
        long lastEventTime = System.currentTimeMillis();
        int min_offset = pref.getInt("long_min_offset", 7);
        int max_offset = pref.getInt("long_max_offset", 3);

        @Override
        protected Void doInBackground(Void... params) {
            synchronized(lock)
            {
                boolean inserted = false;
                long pressTime = 0;
                while(running)
                {
                    //Log.d("CMSPen","running");
                    if((System.currentTimeMillis() - lastEventTime) > 1000)
                    {
                        Log.d("CMSPen", "Timed Out!");
                        Tasker_Last_Event = -1;
                        break;
                    }
                    if(PollDevFD(spenFD) == 0)
                    {
                        //Log.d("CMSPen","got polling event");
                        lastEventTime = System.currentTimeMillis();
                        final Boolean button_features = pref.getBoolean("button_features", false);
                        final Boolean test_features = pref.getBoolean("test_features", false);
                        if(pref.getBoolean("logging", false))
                            Log.d("CMSPen",(String.valueOf(Events.getSuccessfulPollingType()) + " " + String.valueOf(Events.getSuccessfulPollingCode()) + " " + String.valueOf(Events.getSuccessfulPollingValue())));
                        if(Events.getSuccessfulPollingType() == 5 && (Events.getSuccessfulPollingCode() == 14 || Events.getSuccessfulPollingCode() == 19))
                        {
                            if(Events.getSuccessfulPollingValue() == 1)
                            {
                                i.putExtra("penInsert", false);

                                v.vibrate(75);

                                //Direct Unlock


                                //kl.disableKeyguard();

                                //End

                                screenLock.acquire();
                                screenLock.release();

                                try
                                {
                                    sendStickyBroadcast(i);
                                }
                                catch(Exception e)
                                {
                                    e.printStackTrace();
                                }

                                KbHandle(0);
                                if(pref.getBoolean("block_enable", false))
                                    touchBlockStart();
                                if(pref.getBoolean("soft_block_enable", false))
                                    softBlockStart();
                            }
                            if(Events.getSuccessfulPollingValue() == 0)
                            {
                                i.putExtra("penInsert", true);

                                //kl.reenableKeyguard();

                                v.vibrate(75);
                                try
                                {
                                    sendStickyBroadcast(i);
                                }
                                catch(Exception e)
                                {
                                    e.printStackTrace();
                                }
                                KbHandle(1);
                                if(pref.getBoolean("block_enable", false))
                                    touchBlockStop();
                                if(pref.getBoolean("soft_block_enable", false))
                                    softBlockStop();
                                inserted = true;
                            }
                        }
                        if(Events.getSuccessfulPollingType() == 0 && Events.getSuccessfulPollingCode() == 0 && Events.getSuccessfulPollingValue() == 0 && inserted)
                        {
                            running = false;
                            break;
                        }
                        if(Events.getSuccessfulPollingType() == 1 && Events.getSuccessfulPollingCode() == 331)
                        {
                            if(Events.getSuccessfulPollingValue() == 1)
                            {
                                pressTime = System.currentTimeMillis();
                            }
                            if(Events.getSuccessfulPollingValue() == 0)
                            {
                                final long pressedFor = System.currentTimeMillis() - pressTime;
                                if(pressedFor >= Long_Min_Map[min_offset])
                                {
                                    if(pressedFor <= Long_Max_Map[max_offset] || max_offset == 16) {
                                        SPen_Event.putExtra("EVENT_CODE", 2);
                                        if (button_features)
                                            sendBroadcast(SPen_Event);
                                        Log.d("CMSPen", "Button Long Press");

                                        //Tasker

                                        if (button_features) {
                                            Tasker_Last_Event = 2;
                                            sendBroadcast(INTENT_REQUEST_REQUERY);
                                        }

                                        //End Tasker

                                        pCheck.post(new Runnable() {

                                            @Override
                                            public void run() {
                                                if (test_features)
                                                    Toast.makeText(SPenDetection.this, "Button Long Press", Toast.LENGTH_SHORT).show();
                                                new Thread() {
                                                    public void run() {
                                                        ScreenshotActivity.takeScreenshot(SPenDetection.this, false);
                                                    }
                                                }.start();
                                            }

                                        });
                                    }
                                }
                                else
                                {
                                    hPressCount++;
                                    if(hPressCount == 3)
                                    {
                                        SPen_Event.putExtra("EVENT_CODE", 4);
                                        if(button_features)
                                            sendBroadcast(SPen_Event);
                                        Log.d("CMSPen","Button Triple Press");

                                        //Tasker

                                        if(button_features) {
                                            Tasker_Last_Event = 4;
                                            sendBroadcast(INTENT_REQUEST_REQUERY);
                                        }

                                        //End Tasker

                                        if(test_features)
                                            pCheck.post(new Runnable () {

                                                @Override
                                                public void run() {
                                                    Toast.makeText(SPenDetection.this, "Button Triple Press", Toast.LENGTH_SHORT).show();
                                                }

                                            });
                                        hPressCount = 0;
                                    }
                                    pCheck.postDelayed(new Runnable() {

                                        public void run() {
                                            if(hPressCount == 1)
                                            {
                                                if((pressedFor <= Long_Max_Map[max_offset] || max_offset == 10)
                                                        && !DrawView.handlingButton) {
                                                    SPen_Event.putExtra("EVENT_CODE", 1);
                                                    if (button_features)
                                                        sendBroadcast(SPen_Event);
                                                    Log.d("CMSPen", "Button Press");

                                                    //Tasker

                                                    if(button_features) {
                                                        Tasker_Last_Event = 1;
                                                        sendBroadcast(INTENT_REQUEST_REQUERY);
                                                    }

                                                    //End Tasker

                                                    if (test_features)
                                                        Toast.makeText(SPenDetection.this, "Button Press", Toast.LENGTH_SHORT).show();
                                                }
                                                hPressCount = 0;
                                            }
                                            else if(hPressCount == 2)
                                            {
                                                SPen_Event.putExtra("EVENT_CODE", 3);
                                                if(button_features)
                                                    sendBroadcast(SPen_Event);
                                                Log.d("CMSPen","Button Double Press");

                                                //Tasker

                                                if(button_features) {
                                                    Tasker_Last_Event = 3;
                                                    sendBroadcast(INTENT_REQUEST_REQUERY);
                                                }

                                                //End Tasker

                                                if(test_features)
                                                    pCheck.post(new Runnable () {

                                                        @Override
                                                        public void run() {
                                                            Toast.makeText(SPenDetection.this, "Button Double Press", Toast.LENGTH_SHORT).show();
                                                        }

                                                    });
                                                hPressCount = 0;
                                            }
                                        }

                                    }, 500);
                                }
                            }
                        }
                    }
                    if(!pref.getBoolean("intensive_mode", false))
                    {
                        try
                        {
                            Thread.sleep(polling);
                        }
                        catch(Exception e)
                        {
                            e.printStackTrace();
                        }
                    }
                }
            }
            return null;
        }

        @Override
        protected void onPostExecute(Void v) {
            new Thread() {
                @Override
                public void run()
                {
                    AddListener(path);
                }
            }.start();
        }

    }

    static int getTasker_Last_Event()
    {
        return Tasker_Last_Event;
    }

    static void clearTaskerEvent()
    {
        Tasker_Last_Event = -1;
    }

    synchronized static void touchBlockStart()
    {
        if(!touchs_path.equals(""))
        {
            touchs_fd = BlockStart(touchs_path);
        }
    }

    synchronized static void softBlockStart()
    {
        if(!touchk_path.equals(""))
            touchk_fd = BlockStart(touchk_path);
    }

    synchronized static void touchBlockStop()
    {
        if(touchs_fd != -1)
            BlockStop(touchs_fd);
        touchs_fd = -1;
    }

    synchronized static void softBlockStop()
    {
        if(touchk_fd != -1)
            BlockStop(touchk_fd);
        touchk_fd = -1;
    }

    synchronized static void blockStop()
    {
        if(touchk_fd != -1)
            BlockStop(touchk_fd);
        if(touchs_fd != -1)
            BlockStop(touchs_fd);
        touchk_fd = touchs_fd = -1;
    }

    synchronized public void KbHandle(final int mode)
    {
        new Thread()
        {
            public void run()
            {
                SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(SPenDetection.this);
                String s_kb;

                if(mode == 0)
                {
                    SharedPreferences.Editor edit = pref.edit();
                    String c_kb = Settings.Secure.getString(getContentResolver(), Settings.Secure.DEFAULT_INPUT_METHOD);
                    edit.putString("c_kb", c_kb);
                    edit.apply();
                    s_kb = pref.getString("s_kb", "");
                }
                else
                {
                    s_kb = pref.getString("c_kb", "");
                }

                if(pref.getBoolean("kb_enable", false))
                    if(!s_kb.equals("") && Shell.SU.available())
                        Shell.SU.run("ime set " + s_kb);
            }
        }.start();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.d("CMSPen", "Service Destroyed");
        blockStop();
        if(screenLock.isHeld())
            screenLock.release();
        v.cancel();
    }

    synchronized static void enableBlockCompat(Context ctx)
    {
        if(pref.getString("touchs_path", "").equals("") || pref.getString("touchk_path", "").equals("")) {
            Events events = new Events();
            events.Init();
            Boolean temp = false;
            Boolean temp2 = false;
            SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(ctx);
            SharedPreferences.Editor edit = pref.edit();
            for (InputDevice idev : events.m_Devs) {
                {
                    try {
                        if (idev.Open(true)) {
                            String currentname = idev.getName();
                            if (currentname.contains("sec_touchscreen")) {
                                edit.putString("touchs_path", idev.getPath());
                                temp = true;
                            }
                            if (currentname.contains("sec_touchkey")) {
                                edit.putString("touchk_path", idev.getPath());
                                temp2 = true;
                            }
                            if (temp && temp2)
                                break;
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }
            edit.apply();
        }
    }

    synchronized static public int AddListener(String devPath)
    {
        return AddFileChangeListener(devPath);
    }

    private static native int BlockStart(String path);
    private static native int BlockStop(int fd);
    private static native int AddFileChangeListener(String devpath);
    private static native int PollDevFD(int fd);
    private static native int openFromPath(String path);

    static {
        try
        {
            System.loadLibrary("EventInjector");
        }
        catch(Exception e)
        {
            e.printStackTrace();
            System.exit(0);
        }
    }
}
