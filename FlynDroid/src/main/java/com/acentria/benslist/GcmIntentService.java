package com.acentria.benslist;

/**
 * Created by FED9I on 13.03.2015.
 */
import android.annotation.SuppressLint;
import android.app.ActivityManager;
import android.app.IntentService;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.media.RingtoneManager;
import android.os.Bundle;
import android.support.v4.app.NotificationCompat;

import com.acentria.benslist.controllers.MyMessages;
import com.acentria.benslist.controllers.SavedSearch;
import com.google.android.gms.gcm.GoogleCloudMessaging;

import java.util.List;
import java.util.Random;


public class GcmIntentService extends IntentService
{
    public static final int NOTIFICATION_ID = 1;
    private NotificationManager notificationManager;
    private NotificationCompat.Builder builder;

    public GcmIntentService()
    {
        super(GcmIntentService.class.getSimpleName());
    }

    @Override
    protected void onHandleIntent(Intent intent)
    {
        Bundle extras = intent.getExtras();
        GoogleCloudMessaging gcm = GoogleCloudMessaging.getInstance(this);
        String messageType = gcm.getMessageType(intent);

        if (!extras.isEmpty())
        {  // has effect of unparcelling Bundle
            sendNotification(extras);
        }
        GcmReceiver.completeWakefulIntent(intent);
    }

    @SuppressLint("InlinedApi")
    private void sendNotification(Bundle data)
    {
        notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        boolean push = true;
        Intent intent = null;
        Context context = null;

        if (Config.activeInstances.contains("Home") && isApplicationRunningBackground(Config.context)) {
            context = Config.context;

            ActivityManager am = (ActivityManager)Config.context.getSystemService(Config.context.ACTIVITY_SERVICE);
            List<ActivityManager.RunningTaskInfo> taskInfo = am.getRunningTasks(1);
            ComponentName componentInfo = taskInfo.get(0).topActivity;
            String activityName = componentInfo.getShortClassName();

            if(data.getString("key").equals("message")) {
                MyMessages.parseNewMessage(data);
                if (activityName.equals(".MessagesActivity") && MyMessages.contactID.equals(MyMessages.lastMessage.get("from")) && Account.accountData.get("id").equals(MyMessages.lastMessage.get("to"))) {
                    push = false;
                    MyMessages.sendNewMessage(true);
                } else if (Config.tabletMode && Config.currentView.equals("MyMessages")) {
                    push = false;

                    if (Config.currentView.equals("MyMessages") && MyMessages.contactID.equals(MyMessages.lastMessage.get("from")) && Account.accountData.get("id").equals(MyMessages.lastMessage.get("to"))) {
                        MyMessages.sendNewMessage(true);
                    } else {
                        MyMessages.updateContacts(false);
                    }
                } else {
                    intent = new Intent(this, FlynDroid.class);
                    intent.putExtra("key", data.getString("key"));
                    // updated counter messages and contact
                    MyMessages.updateContacts(false);
                    intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
                }
            }
            else if(data.getString("key").equals("save_search")) {
                intent = new Intent(this, FlynDroid.class);
                intent.putExtra("key", data.getString("key"));
                intent.putExtra("id", data.getString("id"));
                intent.putExtra("type", data.getString("type"));
                intent.putExtra("data", data.getString("data"));
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
                SavedSearch.saveSSPreference(Config.context, 1, data.getString("id"), data.getString("matches"));
                SavedSearch.updateSSCounter();
            }
        }
        else {
            context = this;
            intent = new Intent(this, FlynDroid.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
            intent.putExtra("key", data.getString("key"));
            if(data.getString("key").equals("save_search")) {
                intent.putExtra("id", data.getString("id"));
                intent.putExtra("type", data.getString("type"));
                intent.putExtra("data", data.getString("data"));
                SavedSearch.saveSSPreference(context, 1, data.getString("id"), data.getString("matches"));
                SavedSearch.updateSSCounter();
            }
        }

        if (push) {
            Random generator = new Random();
            PendingIntent contentIntent = PendingIntent.getActivity(this, generator.nextInt(), intent, PendingIntent.FLAG_UPDATE_CURRENT, data);

            NotificationCompat.Builder builder = new NotificationCompat.Builder(context)
                    .setSmallIcon(R.mipmap.ic_notification)
                    .setSound(RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION))
                    .setContentTitle(data.getString("title"))
                    .setStyle(new NotificationCompat.BigTextStyle()
                            .bigText(data.getString("message")))
                    .setContentText(data.getString("message"))
                    .setPriority(NotificationCompat.PRIORITY_HIGH)
                    .setAutoCancel(true);

            builder.setContentIntent(contentIntent);
            notificationManager.notify(NOTIFICATION_ID, builder.build());
        }
    }

    public static boolean isApplicationRunningBackground(final Context context) {
        ActivityManager am = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
        List<ActivityManager.RunningTaskInfo> tasks = am.getRunningTasks(am.getRunningAppProcesses().size());
        for (ActivityManager.RunningTaskInfo runningTaskInfo : tasks) {
            if (runningTaskInfo.topActivity.getPackageName().equals(context.getPackageName())) {
//                Log.d("FD", "packageName:" + runningTaskInfo.topActivity.getPackageName());
//                Log.d("FD", "className" + runningTaskInfo.topActivity.getClassName());
                return true;
            }
        }
        return false;
    }
}