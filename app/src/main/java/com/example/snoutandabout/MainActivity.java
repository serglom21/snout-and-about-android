package com.example.snoutandabout;

import androidx.appcompat.app.AppCompatActivity;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.activity.result.ActivityResult;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import io.sentry.HubAdapter;
import io.sentry.ITransaction;
import io.sentry.ISpan;
import io.sentry.Sentry;
import io.sentry.SpanStatus;
import io.sentry.protocol.SentryId;

public class MainActivity extends AppCompatActivity {

    EditText emailInput, passwordInput;
    Button nextButton;

    private ITransaction registrationTransaction;
    private ISpan registrationStepsSpan;
    private ISpan waitingForUserInputSpan; // Make this class-level as well to finish it.

    // 1. Declare the ActivityResultLauncher
    private ActivityResultLauncher<Intent> dogProfileActivityLauncher;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        emailInput = findViewById(R.id.email_input);
        passwordInput = findViewById(R.id.password_input);
        nextButton = findViewById(R.id.btn_next_step1);

        registrationTransaction = Sentry.startTransaction("Registration Journey", "user_journey.registration");
        Sentry.getCurrentScopes().getScope().setTransaction(registrationTransaction);
        
        Log.d("MainActivity", "Created Registration Journey transaction: " + registrationTransaction.getSpanContext().getTraceId());

        registrationStepsSpan = registrationTransaction.startChild("registration_steps_flow", "registration.steps");
        waitingForUserInputSpan = registrationStepsSpan.startChild("step1.waiting_for_input", "ui.wait");


        // 2. Initialize the ActivityResultLauncher
        dogProfileActivityLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    // This callback is executed when DogProfileActivity finishes and sends a result
                    if (result.getResultCode() == RESULT_OK) { // Check if DogProfileActivity succeeded
                        // DogProfileActivity completed successfully, and user proceeded.
                        // You might check result.getData() here if DogProfileActivity passed back data.
                        Toast.makeText(MainActivity.this, "Dog Profile completed!", Toast.LENGTH_SHORT).show();

                        // Sentry: If DogProfileActivity finished successfully, its span should have been finished there.
                        // The `registrationStepsSpan` is still ongoing and will be finished in HydrantPreferencesActivity.
                    } else if (result.getResultCode() == RESULT_CANCELED) {
                        // DogProfileActivity was cancelled (e.g., user pressed back button).
                        Toast.makeText(MainActivity.this, "Dog Profile cancelled.", Toast.LENGTH_SHORT).show();

                        // Sentry: The registration journey was interrupted.
                        // Finish the entire registration transaction as cancelled.
                        if (registrationTransaction != null && !registrationTransaction.isFinished()) {
                            registrationTransaction.finish(SpanStatus.CANCELLED);
                        }
                        if (registrationStepsSpan != null && !registrationStepsSpan.isFinished()) {
                            registrationStepsSpan.finish(SpanStatus.CANCELLED);
                        }
                        // Consider whether to finish MainActivity here or let user try again.
                        // finish();
                    }
                }
        );

        nextButton.setOnClickListener(v -> {
            if (waitingForUserInputSpan != null && !waitingForUserInputSpan.isFinished()) {
                waitingForUserInputSpan.finish(SpanStatus.OK);
            }

            String email = emailInput.getText().toString().trim();
            String password = passwordInput.getText().toString().trim();

            if (email.isEmpty() || password.isEmpty()) {
                Toast.makeText(MainActivity.this, "Please fill in all fields", Toast.LENGTH_SHORT).show();
            } else if (password.length() < 6) {
                Toast.makeText(MainActivity.this, "Password must be at least 6 characters", Toast.LENGTH_SHORT).show();
            } else {
                ISpan step1ProcessSpan = registrationStepsSpan.startChild("step1.process_input", "registration.process");
                try {
                    Intent intent = new Intent(MainActivity.this, DogProfileActivity.class);

                    intent.putExtra("sentry_trace_id", registrationStepsSpan.getSpanContext().getTraceId().toString());
                    intent.putExtra("sentry_span_id", registrationStepsSpan.getSpanContext().getSpanId().toString());
                    intent.putExtra("sentry_parent_span_id", registrationStepsSpan.getSpanContext().getParentSpanId().toString());
                    intent.putExtra("sentry_transaction_name", "Registration Journey");
                    intent.putExtra("sentry_transaction_operation", "user_journey.registration");

                    // 3. Launch the activity for result
                    dogProfileActivityLauncher.launch(intent);

                } finally {
                    step1ProcessSpan.finish(SpanStatus.OK);
                }
            }
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // The transaction and span should only be finished if the whole flow is cancelled
        // This is handled in the dogProfileActivityLauncher callback or if user presses back from DogProfileActivity itself.
        // If the activity is truly finishing (e.g., user exits app from this screen)
        if (isFinishing()) {
            if (registrationTransaction != null && !registrationTransaction.isFinished()) {
                registrationTransaction.finish(SpanStatus.CANCELLED);
            }
            if (registrationStepsSpan != null && !registrationStepsSpan.isFinished()) {
                registrationStepsSpan.finish(SpanStatus.CANCELLED);
            }
        }
    }
}