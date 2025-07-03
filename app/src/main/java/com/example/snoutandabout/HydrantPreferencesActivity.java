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
import android.util.Log;

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

    private ISpan hydrantPreferencesSpan;
    private ITransaction registrationTransaction;

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

        // Get transaction information from intent
        String transactionName = getIntent().getStringExtra("sentry_transaction_name");
        String transactionOperation = getIntent().getStringExtra("sentry_transaction_operation");

        Log.d("HydrantPreferencesActivity", "Received transaction info - Name: " + transactionName + ", Operation: " + transactionOperation);

        // Get the existing Registration Journey transaction from the current scope
        registrationTransaction = Sentry.getCurrentScopes().getScope().getTransaction();
        
        Log.d("HydrantPreferencesActivity", "Current scope transaction: " + (registrationTransaction != null ? registrationTransaction.getName() : "null"));
        
        if (registrationTransaction != null && transactionName != null && registrationTransaction.getName().equals(transactionName)) {
            // Create a span under the existing Registration Journey transaction
            hydrantPreferencesSpan = registrationTransaction.startChild("step3.hydrant_preferences_screen", "registration.step3");
            Sentry.getCurrentScopes().getScope().setActiveSpan(hydrantPreferencesSpan);
            Log.d("HydrantPreferencesActivity", "Continued existing Registration Journey transaction");
        } else if (transactionName != null && transactionOperation != null) {
            // Create a new transaction with the same name and operation to continue the journey
            registrationTransaction = Sentry.startTransaction(transactionName, transactionOperation);
            Sentry.getCurrentScopes().getScope().setTransaction(registrationTransaction);
            hydrantPreferencesSpan = registrationTransaction.startChild("step3.hydrant_preferences_screen", "registration.step3");
            Sentry.getCurrentScopes().getScope().setActiveSpan(hydrantPreferencesSpan);
            Log.d("HydrantPreferencesActivity", "Created new Registration Journey transaction: " + registrationTransaction.getSpanContext().getTraceId());
        } else {
            // Fallback: create a standalone transaction if the parent transaction is not found
            hydrantPreferencesSpan = Sentry.startTransaction("Hydrant Preferences Screen", "registration.step3.standalone");
            Log.d("HydrantPreferencesActivity", "Created standalone transaction");
        }

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

                if (registrationTransaction != null) {
                    // Good practice: Add a span for this final step
                    ISpan step3Span = registrationTransaction.startChild("step3.preferences_submit", "registration.process");
                    try {
                        // Finish the main registration journey transaction
                        registrationTransaction.finish(SpanStatus.OK);
                        Log.d("HydrantPreferencesActivity", "Finished Registration Journey transaction with status: OK");
                        
                        // Also finish the App Session transaction since registration is complete
                        MyApplication.finishAppSession();
                        Log.d("HydrantPreferencesActivity", "Finished App Session transaction");
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

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // Ensure span is finished if not already
        if (hydrantPreferencesSpan != null && !hydrantPreferencesSpan.isFinished()) {
            if (isFinishing()) {
                hydrantPreferencesSpan.finish(SpanStatus.CANCELLED);
            }
        }
    }
}