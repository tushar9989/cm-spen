package com.tushar.cmspen2;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.widget.Toast;

public class TaskerQueryReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {

        int selected = intent.getBundleExtra("com.twofortyfouram.locale.intent.extra.BUNDLE")
                .getInt("com.tushar.cmspen.BUTTON_EVENT_SELECTION");
        //Toast.makeText(context, "I SHALL SURVIVE! " + selected, Toast.LENGTH_SHORT).show();
        if(selected == SPenDetection.getTasker_Last_Event())
        {
            SPenDetection.clearTaskerEvent();
            setResultCode(16);
        }
        else
            setResultCode(17);
    }
}
