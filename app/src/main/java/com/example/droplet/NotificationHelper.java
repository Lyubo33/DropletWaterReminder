package com.example.droplet;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.media.AudioAttributes;
import android.net.Uri;
import android.os.Build;

public class NotificationHelper {

    public static final String CHANNEL_ID = "reminder_channel";

    public static void createNotificationChannel(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            Uri soundUri = Uri.parse("android.resource://" + context.getPackageName() + "/" + R.raw.droplet_notification_sound);
            CharSequence name = "Reminder Channel";
            String description = "Channel for water reminder notifications";
            int importance = NotificationManager.IMPORTANCE_HIGH;
            NotificationChannel channel = new NotificationChannel(CHANNEL_ID, name, importance);
            channel.setDescription(description);
            AudioAttributes.Builder audioAttributes = new AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_NOTIFICATION);
            channel.setSound(soundUri, audioAttributes.build());

            NotificationManager notificationManager = context.getSystemService(NotificationManager.class);
            notificationManager.createNotificationChannel(channel);
        }
    }
}
