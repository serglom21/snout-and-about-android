package com.example.snoutandabout;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Toast;

import io.sentry.ISpan;
import io.sentry.ITransaction;
import io.sentry.Sentry;
import io.sentry.SpanStatus;

public class HydrantPreferencesActivity extends AppCompatActivity {

    CheckBox cbRed, cbYellow, cbBlue, cbGreen;
    RadioGroup sizeRadioGroup;
    RadioButton radioSmall, radioMedium, radioLarge;
    EditText featuresInput;
    Button finishButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_hydrant_preferences);

        cbRed = findViewById(R.id.checkbox_red);
        cbYellow = findViewById(R.id.checkbox_yellow);
        cbBlue = findViewById(R.id.checkbox_blue);
        cbGreen = findViewById(R.id.checkbox_green);
        sizeRadioGroup = findViewById(R.id.size_radio_group);
        radioSmall = findViewById(R.id.radio_small);
        radioMedium = findViewById(R.id.radio_medium);
        radioLarge = findViewById(R.id.radio_large);
        featuresInput = findViewById(R.id.features_input);
        finishButton = findViewById(R.id.btn_finish_registration);

        finishButton.setOnClickListener(v -> {
            // Collect preferences (e.g., in a real app, save them to a database)
            String preferredColors = "";
            if (cbRed.isChecked()) preferredColors += "Red, ";
            if (cbYellow.isChecked()) preferredColors += "Yellow, ";
            if (cbBlue.isChecked()) preferredColors += "Blue, ";
            if (cbGreen.isChecked()) preferredColors += "Green, ";
            if (preferredColors.length() > 0) preferredColors = preferredColors.substring(0, preferredColors.length() - 2); // Remove trailing comma

            String preferredSize = "";
            int selectedSizeId = sizeRadioGroup.getCheckedRadioButtonId();
            if (selectedSizeId != -1) {
                RadioButton selectedRadioButton = findViewById(selectedSizeId);
                preferredSize = selectedRadioButton.getText().toString();
            }

            String specialFeatures = featuresInput.getText().toString().trim();

            if (preferredColors.isEmpty() && preferredSize.isEmpty() && specialFeatures.isEmpty()) {
                Toast.makeText(this, "Please select some preferences!", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, "Registration Complete! Woof!", Toast.LENGTH_LONG).show();

                ITransaction registrationTransaction = Sentry.getCurrentScopes().getScope().getTransaction();
                if (registrationTransaction != null) {
                    // Good practice: Add a span for this final step
                    ISpan step3Span = registrationTransaction.startChild("step3.preferences_submit");
                    try {
                        // Finish the main registration journey transaction
                        registrationTransaction.finish(SpanStatus.OK);
                    } finally {
                        step3Span.finish(SpanStatus.OK);
                    }
                }

                // Navigate to the main Swiping Activity
                Intent intent = new Intent(HydrantPreferencesActivity.this, SwipingActivity.class);
                // Add FLAG_ACTIVITY_CLEAR_TASK and FLAG_ACTIVITY_NEW_TASK to clear registration stack
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(intent);
                finish(); // Close this activity
            }
        });
    }
}