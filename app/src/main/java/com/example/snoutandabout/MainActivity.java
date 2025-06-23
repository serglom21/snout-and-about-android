package com.example.snoutandabout;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import android.content.Intent;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import io.sentry.ISpan;
import io.sentry.ITransaction;
import io.sentry.Sentry;
import io.sentry.SpanStatus;
import io.sentry.android.core.SentryAndroid;

public class MainActivity extends AppCompatActivity { // This is our Registration Step 1

    EditText emailInput, passwordInput;
    Button nextButton;
    private ITransaction registrationTransaction;
    private ISpan registrationStepsTransaction;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main); // Our Step 1 layout

        emailInput = findViewById(R.id.email_input);
        passwordInput = findViewById(R.id.password_input);
        nextButton = findViewById(R.id.btn_next_step1);

        registrationTransaction = Sentry.startTransaction("Registration Journey", "user_journey.registration");
        Sentry.getCurrentScopes().configureScope(scope -> scope.setTransaction(registrationTransaction));
        ISpan waitingForUserInputSpan = registrationTransaction.startChild("step1.waiting_for_input");

        nextButton.setOnClickListener(v -> {
            waitingForUserInputSpan.finish();
            String email = emailInput.getText().toString().trim();
            String password = passwordInput.getText().toString().trim();

            if (email.isEmpty() || password.isEmpty()) {
                Toast.makeText(MainActivity.this, "Please fill in all fields", Toast.LENGTH_SHORT).show();
            } else if (password.length() < 6) {
                Toast.makeText(MainActivity.this, "Password must be at least 6 characters", Toast.LENGTH_SHORT).show();
            } else {
                ISpan step1Span = registrationTransaction.startChild("step1.email_password_input");
                try {
                    registrationStepsTransaction = registrationTransaction.startChild("registration_steps");
                    Intent intent = new Intent(MainActivity.this, DogProfileActivity.class);
                    startActivity(intent);
                } finally {
                    step1Span.finish(SpanStatus.OK); // Finish the span regardless of success
                }
            }
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        registrationStepsTransaction.finish();
        registrationTransaction.finish(SpanStatus.OK);
        // If the user backs out of the first screen, consider the journey cancelled/aborted
        //if (registrationTransaction != null && !registrationTransaction.isFinished()) {
            //registrationTransaction.finish(SpanStatus.CANCELLED);
        //}
    }
}