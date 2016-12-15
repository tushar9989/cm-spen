package com.tushar.cmspen2;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

import com.tushar.cmspen2.libsuperuser.Shell;

public class BootReceiver extends BroadcastReceiver {

	@Override
	public void onReceive(final Context ctx,final Intent i) {
		Intent spen_det = new Intent(ctx,SPenDetection.class);
		final SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(ctx);

		if(i.getAction().equals("com.tushar.cmspen.PKE"))
		{
            new Thread()
            {
                public void run()
                {
                    int code = i.getIntExtra("code", -1);
                    if(code != -1 && Shell.SU.available())
                        MainActivity.simKey(code);
                }
            }.start();

		}
		else if(i.getAction().equals("com.tushar.cmspen.Screenshot"))
        {
            new Thread()
            {
                public void run()
                {
                    ScreenshotActivity.takeScreenshot(ctx, true);
                }
            }.start();
        }
        else if(i.getAction().equals("com.tushar.cmspen.KBSWITCH"))
        {
            new Thread()
            {
                public void run()
                {
                    String id = i.getStringExtra("id");
                    String switchTo = i.getStringExtra("switchTo");
                    KeyboardActivity.Switch(id, switchTo, ctx);
                }
            }.start();
        }
        else if(i.getAction().equals("com.tushar.cmspen.Touch_Block"))
        {
            new Thread()
            {
                @Override
                public void run() {
                    super.run();
                    SPenDetection.touchs_path = pref.getString("touchs_path", "");
                    SPenDetection.touchk_path = pref.getString("touchk_path", "");
                    if(SPenDetection.touchs_path.equals("") || SPenDetection.touchk_path.equals("")) {
                        SPenDetection.enableBlockCompat(ctx);
                        SPenDetection.touchs_path = pref.getString("touchs_path", "");
                        SPenDetection.touchk_path = pref.getString("touchk_path", "");
                    }
                    String blockWhat = i.getStringExtra("blockWhat");
                    if(blockWhat != null)
                    {
                        if(blockWhat.equals("Screen"))
                        {
                            if(SPenDetection.touchs_fd == -1) {
                                SPenDetection.touchBlockStart();
                            }
                            else {
                                SPenDetection.touchBlockStop();
                            }
                        }
                        else if(blockWhat.equals("Keys"))
                        {
                            if(SPenDetection.touchk_fd == -1) {
                                SPenDetection.softBlockStart();
                            }
                            else {
                                SPenDetection.softBlockStop();
                            }
                        }
                        else if(blockWhat.equals("Both"))
                        {
                            if(SPenDetection.touchs_fd == -1)
                                SPenDetection.touchBlockStart();
                            if(SPenDetection.touchk_fd == -1) {
                                SPenDetection.softBlockStart();
                                return;
                            }
                            if(SPenDetection.touchk_fd != -1 || SPenDetection.touchs_fd != -1)
                                SPenDetection.blockStop();
                        }
                        else if(blockWhat.equals("Stop"))
                        {
                            SPenDetection.blockStop();
                        }
                    }
                }
            }.start();
        }
        else
		{
			if(pref.getBoolean("enabled", false))
			{
				//if(Shell.isSuAvailable())
					//Shell.runCommand("setenforce 0");
				ctx.startService(spen_det);
			}
		}
	}

}
