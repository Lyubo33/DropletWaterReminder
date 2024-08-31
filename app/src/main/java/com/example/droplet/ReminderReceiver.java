package com.example.droplet;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;

import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

public class ReminderReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        String reminder_text = intent.getStringExtra("reminder_text");
        String reminder_title = intent.getStringExtra("reminder_title");
        Uri soundUri = Uri.parse("android.resource://" + context.getPackageName() + "/" + R.raw.droplet_notification_sound);
        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, NotificationHelper.CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_stat_name)
                .setContentTitle(reminder_title)
                .setContentText(reminder_text)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setSound(soundUri)
                .setAutoCancel(true);
        if(NotificationManagerCompat.from(context).areNotificationsEnabled()) {
            NotificationManagerCompat compat_manager = NotificationManagerCompat.from(context);
            compat_manager.notify(0, builder.build());
        }
    }
}
