package com.example.droplet;

import android.animation.ValueAnimator;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import androidx.cardview.widget.CardView;

import android.widget.Button;
import android.widget.TimePicker;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.color.DynamicColors;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

public class InputActivity extends AppCompatActivity {

    private TextInputEditText etName, etWeight, etGlassSize;
    private TextInputLayout tilName, tilWeight, tilGlassSize;
    private TimePicker tpWakeUp, tpBedTime;
    private Button btnSubmit;
    private CardView wakeUpCard, bedTimeCard;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_input);
        etName = findViewById(R.id.et_name);
        etWeight = findViewById(R.id.et_weight);
        etGlassSize = findViewById(R.id.et_glass_size);
        tilName = findViewById(R.id.til_name);
        tilWeight = findViewById(R.id.til_weight);
        tilGlassSize = findViewById(R.id.til_glass_size);
        tpWakeUp = findViewById(R.id.tp_wake_up);
        tpBedTime = findViewById(R.id.tp_bed_time);
        btnSubmit = findViewById(R.id.btn_submit);

        wakeUpCard = findViewById(R.id.time_picker_card_wake_up);
        bedTimeCard = findViewById(R.id.time_picker_card_go_to_bed);

        loadFromSharedPreference(etName,etWeight,etGlassSize,tpWakeUp,tpBedTime);
            //TODO Load from Shared Preferences(Bugs out on integer conversion)
        btnSubmit.setOnClickListener(v -> handleSubmit());
    }

    private void handleSubmit() {
        if (validateInputs()) {
            saveToSharedPreferences();

            Intent intent = new Intent(this, MainActivity.class);
            startActivity(intent);

            finish(); // Close the activity
        }
    }

    private boolean validateInputs() {
        boolean isValid = true;

        if (etName.getText().toString().trim().isEmpty()) {
            tilName.setError("Please enter your name");
            isValid = false;
        } else {
            tilName.setError(null);
        }

        if (etWeight.getText().toString().trim().isEmpty()) {
            tilWeight.setError("Please enter your weight");
            isValid = false;
        } else {
            tilWeight.setError(null);
        }

        if (etGlassSize.getText().toString().trim().isEmpty()) {
            tilGlassSize.setError("Please enter the glass size");
            isValid = false;
        } else {
            tilGlassSize.setError(null);
        }

        return isValid;
    }
    private void saveToSharedPreferences() {
        SharedPreferences sharedPreferences = getSharedPreferences("UserData", MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();

        editor.putString("name", etName.getText().toString().trim());
        editor.putInt("weight", Integer.parseInt(etWeight.getText().toString().trim()));
        editor.putInt("glassSize", Integer.parseInt(etGlassSize.getText().toString().trim()));

        // Get time from TimePickers
        int wakeUpHour = tpWakeUp.getHour();
        int wakeUpMinute = tpWakeUp.getMinute();
        int bedTimeHour = tpBedTime.getHour();
        int bedTimeMinute = tpBedTime.getMinute();
        editor.putString("wakeUpTime", wakeUpHour + ":" + wakeUpMinute);
        editor.putString("bedTime", bedTimeHour + ":" + bedTimeMinute);

        editor.apply();
    }
    private void loadFromSharedPreference( TextInputEditText etName, TextInputEditText etWeight,
                                           TextInputEditText etGlassSize,TimePicker tpWakeUp,
                                           TimePicker tpBedTime){
        SharedPreferences sharedPreferences = getSharedPreferences("UserData", MODE_PRIVATE);
        String name = sharedPreferences.getString("name","");
        int weight = sharedPreferences.getInt("weight",0);
        int glassSize = sharedPreferences.getInt("glassSize",0);
        String wakeUpTime = sharedPreferences.getString("wakeUpTime","6:00");
        String BedTime = sharedPreferences.getString("bedTime","22:00");

        etName.setText(name);
        etWeight.setText(weight == 0 ? "": (String.valueOf(weight)));
        etGlassSize.setText(glassSize == 0 ? "": (String.valueOf(glassSize)));

        String[] WakeUpTimeParts = wakeUpTime.split(":");
        tpWakeUp.setHour(Integer.parseInt((WakeUpTimeParts[0])));
        tpWakeUp.setMinute(Integer.parseInt((WakeUpTimeParts[1])));

        String[] BedTimeParts = BedTime.split(":");
        tpBedTime.setHour(Integer.parseInt((BedTimeParts[0])));
        tpBedTime.setMinute(Integer.parseInt((BedTimeParts[1])));


    }

}
