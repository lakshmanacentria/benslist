package com.acentria.benslist;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.support.v4.content.WakefulBroadcastReceiver;

public class GcmReceiver extends WakefulBroadcastReceiver
{

    @Override
    public void onReceive(Context context, Intent intent) {

        ComponentName comp = new ComponentName(context.getPackageName(), GcmIntentService.class.getName());
        intent.setComponent(comp);
        startWakefulService(context, intent);
        setResultCode(Activity.RESULT_OK);
    }
}