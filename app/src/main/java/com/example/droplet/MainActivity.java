package com.example.droplet;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.appcompat.content.res.AppCompatResources;
import androidx.core.app.NotificationManagerCompat;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.util.Log;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.PopupWindow;
import android.widget.Toast;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.color.DynamicColors;
import com.google.android.material.color.DynamicColorsOptions;
import com.google.android.material.color.MaterialColors;
import com.google.android.material.materialswitch.MaterialSwitch;
import com.google.android.material.radiobutton.MaterialRadioButton;
import com.google.android.material.textview.MaterialTextView;

import java.util.Calendar;

public class MainActivity extends AppCompatActivity {
    private MaterialTextView next_reminder, greeting, glass_size, next_reminder_title, weight,
            total_water_intake, wake_up, bed_time;
    private MaterialButton btn_edit, btn_theme;
    private SharedPreferences sharedPreferences;
    private boolean areNotificationsEnabled;
    private Handler uiHandler;
    private Runnable uiUpdater;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        sharedPreferences = getSharedPreferences("UserData", MODE_PRIVATE);
        setInitialTheme();
        setInitialMode();
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        areNotificationsEnabled = NotificationManagerCompat.from(this).areNotificationsEnabled();
        if (!sharedPreferences.contains("name")) {
            Intent intent = new Intent(MainActivity.this, InputActivity.class);
            startActivity(intent);
            finish();
            return;
        }
        if (!areNotificationsEnabled) {
            requestNotificationPermission();
        }
        if (!hasExactAlarmPermission()) {
            requestExactAlarmPermission();
        }

        next_reminder = findViewById(R.id.sched_timer);
        greeting = findViewById(R.id.tv_greeting);
        glass_size = findViewById(R.id.tv_glass_size);
        btn_edit = findViewById(R.id.btn_edit);
        btn_theme = findViewById(R.id.btn_theme);
        next_reminder_title = findViewById(R.id.next_timer_title);
        weight = findViewById(R.id.tv_user_stats_weight);
        total_water_intake = findViewById(R.id.tv_user_stats_total_water);
        wake_up = findViewById(R.id.tv_user_stats_wake_time);
        bed_time = findViewById(R.id.tv_user_stats_bed_time);

        btn_edit.setOnClickListener(v -> {
            animateButtonClick(btn_edit);
            Intent intent = new Intent(MainActivity.this, InputActivity.class);
            startActivity(intent);
        });
        btn_theme.setOnClickListener(v -> {
            animateButtonClick(btn_theme);
            showThemePopupWindow(btn_theme);
         });

        setUpReminders();  // Initial setup for today's reminders
        scheduleBedTimeReminder();
        loadUserData(greeting,glass_size,weight,total_water_intake,wake_up,bed_time);
        setupPeriodicUIUpdate(60 * 1000);
        NotificationHelper.createNotificationChannel(this);
    }

    @Override
    protected void onDestroy(){
        super.onDestroy();
        if (uiHandler != null && uiUpdater != null) {
            uiHandler.removeCallbacks(uiUpdater);
        }
    }
    @Override
    protected void onPause(){
        super.onPause();
        if (uiHandler != null && uiUpdater != null) {
            uiHandler.removeCallbacks(uiUpdater);
        }
    }
    @Override
    protected void onResume(){
        super.onResume();
        if (uiHandler != null && uiUpdater != null) {
            uiHandler.post(uiUpdater);
        }
    }

    private void animateTransition(float from, float to) {
        View rootView = findViewById(android.R.id.content);

        // Apply custom animation to root view
        rootView.animate()
                .alpha(0.8f)
                .setDuration(100)
                .withEndAction(() -> rootView.animate()
                        .rotationYBy(from)
                        .setDuration(300)
                        .withEndAction(() -> {
                            rootView.animate()
                                    .rotationYBy(to)
                                    .setDuration(300)
                                    .withEndAction(() -> {
                                        // Change theme and recreate activity
                                        recreate();
                                    });
                        }))
                .start();
    }

    private void animateButtonClick(MaterialButton btn){
        btn.animate()
                .scaleX(1.0f)
                .scaleY(1.1f)
                .setDuration(100) // Duration to scale up
                .withEndAction(() -> btn.animate()
                        .scaleX(0.7f)
                        .scaleY(0.9f)
                        .setDuration(250) // Duration to scale down
                        .withEndAction(() -> btn.animate()
                                .scaleX(1.0f) // Scale back to original size
                                .scaleY(1.0f)
                                .setDuration(100) // Duration to settle back
                                .start())
                        .start())
                .start();
    }

    private void loadUserData(MaterialTextView greeting, MaterialTextView glass_size, MaterialTextView weight,
                              MaterialTextView total_water_intake, MaterialTextView wake_up, MaterialTextView bed_time) {
        String user_name = sharedPreferences.getString("name", "");
        int user_glass_size = sharedPreferences.getInt("glassSize", 0);

        greeting.setText("Hi, " + user_name);
        glass_size.setText(user_glass_size + " ml");
        weight.setText("Your weight:"+" " + sharedPreferences.getInt("weight", 0) + " kg");
        total_water_intake.setText("Recommended daily intake: " + " " + sharedPreferences.getInt(
                "totalWaterIntake", 0) + " ml");
        wake_up.setText("You Wake up at: " + " " + sharedPreferences.getString("wakeUpTime", "6:00") + "am");
        bed_time.setText("You go to bed at: " + " " + sharedPreferences.getString("bedTime", "22:00") + "pm");
    }

    private void showThemePopupWindow(View anchorView) {
        LayoutInflater inflater = LayoutInflater.from(this);
        View popupView = inflater.inflate(R.layout.theme_popup, null);
        int currentTheme = sharedPreferences.getInt("currentTheme", 1);
        boolean isNightMode = sharedPreferences.getBoolean("currentMode",false);

        PopupWindow popupWindow = new PopupWindow(popupView, LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);

        popupWindow.setFocusable(true);
        popupWindow.setOutsideTouchable(true);
        popupWindow.setAnimationStyle(2131427317);
        popupWindow.setBackgroundDrawable(getDrawable(R.drawable.shiny_gradient_background));

        MaterialSwitch switchNightMode = popupView.findViewById(R.id.switchNightMode);
        MaterialRadioButton radioButtonTheme1 = popupView.findViewById(R.id.radioButtonTheme1);
        MaterialRadioButton radioButtonTheme2 = popupView.findViewById(R.id.radioButtonTheme2);
        if(!DynamicColors.isDynamicColorAvailable()){
            radioButtonTheme2.setVisibility(View.GONE);
        }
        if(currentTheme == 1){
            radioButtonTheme1.setChecked(true);
            radioButtonTheme2.setChecked(false);
            popupWindow.setBackgroundDrawable(AppCompatResources.getDrawable(this, R.drawable.shiny_gradient_background));
        }else{
            radioButtonTheme2.setChecked(true);
            radioButtonTheme1.setChecked(false);
            int dynamicThemeColor = MaterialColors.getColor(this, com.google.android.material.R.attr.colorTertiaryContainer, Color.TRANSPARENT);
            GradientDrawable bgr_drawable = new GradientDrawable();
            bgr_drawable.setColor(dynamicThemeColor);
            bgr_drawable.setCornerRadius(50f);
            popupWindow.setBackgroundDrawable(bgr_drawable);
        }
        if(isNightMode){
            switchNightMode.setChecked(true);
        }else{
            switchNightMode.setChecked(false);
        }

        radioButtonTheme1.setOnClickListener(v -> applyBasicTheme());
        radioButtonTheme2.setOnClickListener(v -> {
                applyDynamicTheme();
                animateTransition(90.0f,-90.0f);
        });

        switchNightMode.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) {
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
                saveMode(true);
            } else {
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
                saveMode(false);
            }
        });

        popupWindow.showAsDropDown(anchorView,-400,-600);
}
    private void applyBasicTheme() {
        saveTheme(1);
        setTheme(R.style.Base_Theme_WaterReminderApp);
        animateTransition(-90.0f,90.0f);
    }
    private void applyDynamicTheme(){
        if(DynamicColors.isDynamicColorAvailable()) {
            DynamicColorsOptions dynamicColorsOptions = new DynamicColorsOptions.Builder()
                    .setThemeOverlay(R.style.MaterialYou)
                    .build();
            DynamicColors.applyToActivitiesIfAvailable(this.getApplication(), dynamicColorsOptions);
            saveTheme(2);
        }else{
            applyBasicTheme();
        }
    }
    private void setInitialTheme(){
        int currentTheme = sharedPreferences.getInt("currentTheme", 1);
        if (currentTheme == 1) {
            setTheme(R.style.Base_Theme_WaterReminderApp);
        } else {
            applyDynamicTheme();
            setTheme(R.style.MaterialYou);
        }
    }
    private void saveTheme(int themeId) {
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putInt("currentTheme", themeId);
        editor.apply();
    }
    private void setInitialMode(){
        boolean currentMode = sharedPreferences.getBoolean("currentMode", false);
        if(currentMode){
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
        }else{
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);

        }
    }
    private void saveMode(boolean isNight){
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putBoolean("currentMode", isNight);
        editor.apply();
    }

    void setUpReminders() {
        int glass_size = sharedPreferences.getInt("glassSize", 0);
        int weight = sharedPreferences.getInt("weight", 0);
        String wakeUpTime = sharedPreferences.getString("wakeUpTime", "6:00");
        String bedTime = sharedPreferences.getString("bedTime", "22:00");

        // Splitting time into hours and minutes
        int wakeUpHour = Integer.parseInt(wakeUpTime.split(":")[0]);
        int wakeUpMinute = Integer.parseInt(wakeUpTime.split(":")[1]);
        int bedTimeHour = Integer.parseInt(bedTime.split(":")[0]);
        int bedTimeMinute = Integer.parseInt(bedTime.split(":")[1]);
        long bedTimeInMillis = ((long) bedTimeHour * 60 * 60 * 1000) + ((long) bedTimeMinute * 60 * 1000);

        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putInt("wake_up_hour", wakeUpHour);
        editor.putInt("wake_up_minute", wakeUpMinute);
        editor.putLong("bed_time_in_millis", bedTimeInMillis);

        // Total intake in ml
        int total_water_intake = (int) (0.035 * weight * 1000); // Calculate water intake in ml
        int total_reminders = (total_water_intake + glass_size - 1) / glass_size; // Total number of reminders

        // Total minutes between waking up and bedtime
        int total_minutes = ((bedTimeHour * 60 + bedTimeMinute) - (wakeUpHour * 60 + wakeUpMinute));
        int interval = total_minutes / total_reminders; // Interval of time in minutes between each reminder

        if (total_minutes <= 0) {
            // Handle the case where the time range is invalid
            Toast.makeText(this, "Invalid time range for reminders", Toast.LENGTH_SHORT).show();
            return;
        }

        editor.putInt("intervalMinutes", interval);
        editor.putInt("totalReminders", total_reminders);
        editor.putInt("totalWaterIntake", total_water_intake);
        editor.apply();

        scheduleReminders(wakeUpHour, wakeUpMinute);  // Schedule today's reminders
    }

    private void scheduleReminders(int wakeupHour, int wakeupMinute) {
        String user_name = sharedPreferences.getString("name","drinker");
        AlarmManager alarmManager = (AlarmManager) getSystemService(ALARM_SERVICE);
        Calendar nextReminder = Calendar.getInstance();
        nextReminder.set(Calendar.HOUR_OF_DAY, wakeupHour);
        nextReminder.set(Calendar.MINUTE, wakeupMinute);
        nextReminder.set(Calendar.SECOND, 0);

        long intervalInMillis = sharedPreferences.getInt("intervalMinutes", 30) * 60 * 1000;
        int totalReminders = sharedPreferences.getInt("totalReminders", 4);
        SharedPreferences.Editor editor = sharedPreferences.edit();


        for (int i = 0; i < totalReminders; i++) {
            long nextReminderTimeInMillis = nextReminder.getTimeInMillis();
            editor.putLong("reminder_" + i, nextReminderTimeInMillis);

            if (nextReminderTimeInMillis < System.currentTimeMillis()) {
                nextReminder.add(Calendar.MILLISECOND, (int) intervalInMillis);
                continue;
            }

            Intent intent = new Intent(this, ReminderReceiver.class);
            intent.putExtra("reminder_text", user_name + ", it's time to drink water");
            intent.putExtra("reminder_title", "Hey");

            PendingIntent pendingIntent = PendingIntent.getBroadcast(this, i, intent,
                    PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                if (alarmManager.canScheduleExactAlarms()) {
                    alarmManager.setRepeating(AlarmManager.RTC_WAKEUP, nextReminderTimeInMillis,
                            AlarmManager.INTERVAL_DAY,pendingIntent);
                }
            } else {
                alarmManager.setRepeating(AlarmManager.RTC_WAKEUP, nextReminderTimeInMillis,
                        AlarmManager.INTERVAL_DAY, pendingIntent);
            }
            Log.d("Scheduled reminder:", "Reminder " + i + " Time: " + nextReminder.getTime());

            nextReminder.add(Calendar.MILLISECOND, (int) intervalInMillis); // Move to next reminder time
        }
        editor.apply();
    }
    private void scheduleBedTimeReminder(){
        String user_name = sharedPreferences.getString("name","");
        String bedTime = sharedPreferences.getString("bedTime","22:00");
        String bedTimeParts[] = bedTime.split(":");
        if (bedTimeParts.length != 2) {
            Toast.makeText(this, "Invalid bed time format", Toast.LENGTH_SHORT).show();
            return;
        }
        int bedTimeHour = Integer.parseInt((bedTimeParts[0]));
        int bedTimeMinute = Integer.parseInt((bedTimeParts[1]));

        AlarmManager alarmManager = (AlarmManager) getSystemService(ALARM_SERVICE);
        Calendar bedTimeReminder = Calendar.getInstance();

        bedTimeReminder.set(Calendar.HOUR_OF_DAY, bedTimeHour);
        bedTimeReminder.set(Calendar.MINUTE, bedTimeMinute);
        bedTimeReminder.set(Calendar.SECOND, 0);

        long bedTimeInMillis = bedTimeReminder.getTimeInMillis();
        if(bedTimeInMillis < System.currentTimeMillis()){
            bedTimeReminder.add(Calendar.DAY_OF_MONTH,1);
        }
        bedTimeInMillis = bedTimeReminder.getTimeInMillis();
        Intent intent = new Intent(this, ReminderReceiver.class);
        intent.putExtra("reminder_title", user_name);
        intent.putExtra("reminder_text", "It's time for bed ");

        PendingIntent pendingIntent = PendingIntent.getBroadcast(this, 1,intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (alarmManager.canScheduleExactAlarms()) {
                alarmManager.setRepeating(AlarmManager.RTC_WAKEUP, bedTimeInMillis,
                        AlarmManager.INTERVAL_DAY,pendingIntent);
            }
        } else {
            alarmManager.setRepeating(AlarmManager.RTC_WAKEUP, bedTimeInMillis,
                    AlarmManager.INTERVAL_DAY, pendingIntent);
        }

    }

    // Update next reminder, display it on the screen
    private void updateNextReminder() {
        long currentTimeMillis = System.currentTimeMillis();
        long nextReminderTimeInMillis = 0;
        boolean foundNextReminder = false;

        int totalReminders = sharedPreferences.getInt("totalReminders", 4);

        for (int i = 0; i < totalReminders; i++) {
            long reminderTime = sharedPreferences.getLong("reminder_" + i, 0);
            if (reminderTime > currentTimeMillis) {
                nextReminderTimeInMillis = reminderTime;
                foundNextReminder = true;
                break;
            }
        }

        if (foundNextReminder) {
            Calendar nextReminderCalendar = Calendar.getInstance();
            nextReminderCalendar.setTimeInMillis(nextReminderTimeInMillis);

            int nextHour = nextReminderCalendar.get(Calendar.HOUR_OF_DAY);
            int nextMinute = nextReminderCalendar.get(Calendar.MINUTE);

            String nextReminderText = String.format("%02d:%02d", nextHour, nextMinute);
            next_reminder.setText(nextReminderText);
            Log.d("Next Reminder at:", nextReminderText);
        }else{
            next_reminder_title.setText("");
            next_reminder.setTextSize(TypedValue.COMPLEX_UNIT_SP, 24);
            next_reminder.setText("No reminders left for today");
        }
    }

    private void setupPeriodicUIUpdate(long intervalInMillis) {
        uiHandler = new Handler(Looper.getMainLooper());
        uiUpdater = new Runnable() {
            @Override
            public void run() {
                updateNextReminder();
                uiHandler.postDelayed(this, intervalInMillis);
            }
        };

        uiHandler.post(uiUpdater);
    }

    // Remove the `cancelAllReminders` and `resetReminders` functions as they are no longer needed

    private void requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            Intent intent = new Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS);
            intent.putExtra(Settings.EXTRA_APP_PACKAGE, getPackageName());
            startActivity(intent);
        } else {
            Toast.makeText(this, "Notification permission enabled by default for this version", Toast.LENGTH_SHORT).show();
        }
    }

    private boolean hasExactAlarmPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            // Get the AlarmManager service
            AlarmManager alarmManager = (AlarmManager) getSystemService(getApplicationContext().ALARM_SERVICE);
            // Check if exact alarms can be scheduled
            return alarmManager != null && alarmManager.canScheduleExactAlarms();
        }
        return true;  // On versions below Android 12, exact alarms are allowed by default
    }

    private void requestExactAlarmPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            Intent intent = new Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM);
            startActivity(intent);
            Toast.makeText(this, "Please enable exact alarm permission for reminders.", Toast.LENGTH_SHORT).show();
        }
    }

}
