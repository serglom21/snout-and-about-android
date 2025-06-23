package com.example.snoutandabout;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Spinner;
import android.widget.Toast;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import io.sentry.CompositePerformanceCollector;
import io.sentry.HubAdapter;
import io.sentry.IScopes;
import io.sentry.ISpanFactory;
import io.sentry.ITransaction;
import io.sentry.ISpan;
import io.sentry.PropagationContext;
import io.sentry.Sentry;
import io.sentry.SpanId;
import io.sentry.SpanOptions;
import io.sentry.SpanStatus;
import io.sentry.SpanContext;
import io.sentry.TraceContext;
import io.sentry.TransactionContext;
import io.sentry.TransactionOptions;
import io.sentry.protocol.SentryId;

public class DogProfileActivity extends AppCompatActivity {

    EditText dogNameInput, dogAgeInput;
    Spinner breedSpinner;
    RadioGroup genderRadioGroup;
    RadioButton radioMale, radioFemale;
    ImageView dogPhotoPreview;
    Button nextButton;

    private ISpan dogProfileActivitySpan;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_dog_profile);

        String traceId = getIntent().getStringExtra("sentry_trace_id");
        String parentSpanId = getIntent().getStringExtra("sentry_span_id");

        if (traceId != null && parentSpanId != null) {

            PropagationContext propagationContext = new PropagationContext(
                    new SentryId(traceId),
                    new SpanId(parentSpanId),
                    new SpanId(),
                    null,
                    null
            );
            TransactionContext transactionContext = new TransactionContext(
                    propagationContext.getTraceId(),
                    propagationContext.getSpanId(),
                    propagationContext.getParentSpanId(),
                    null,
                    null
            );

            transactionContext.setName("Dog Profile Screen");
            transactionContext.setOperation("registration.step2");

            dogProfileActivitySpan = Sentry.startTransaction(transactionContext);
            Sentry.getCurrentScopes().getScope().setActiveSpan(dogProfileActivitySpan);
        } else {
            dogProfileActivitySpan = Sentry.startTransaction("Dog Profile Screen", "registration.step2.standalone");
        }

        dogNameInput = findViewById(R.id.dog_name_input);
        dogAgeInput = findViewById(R.id.dog_age_input);
        breedSpinner = findViewById(R.id.breed_spinner);
        genderRadioGroup = findViewById(R.id.gender_radio_group);
        radioMale = findViewById(R.id.radio_male);
        radioFemale = findViewById(R.id.radio_female);
        dogPhotoPreview = findViewById(R.id.dog_photo_preview);
        nextButton = findViewById(R.id.btn_next_step2);

        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(
                this,
                R.array.dog_breeds,
                android.R.layout.simple_spinner_item
        );
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        breedSpinner.setAdapter(adapter);

        dogPhotoPreview.setOnClickListener(v -> {
            Toast.makeText(this, "Imagine a photo picker here!", Toast.LENGTH_SHORT).show();
        });

        nextButton.setOnClickListener(v -> {
            String dogName = dogNameInput.getText().toString().trim();
            String dogAge = dogAgeInput.getText().toString().trim();
            String selectedBreed = breedSpinner.getSelectedItem().toString();

            int selectedGenderId = genderRadioGroup.getCheckedRadioButtonId();
            String gender = "";
            if (selectedGenderId != -1) {
                RadioButton selectedRadioButton = findViewById(selectedGenderId);
                gender = selectedRadioButton.getText().toString();
            }

            if (dogName.isEmpty() || dogAge.isEmpty() || selectedBreed.equals("Select Breed") || gender.isEmpty()) {
                Toast.makeText(DogProfileActivity.this, "Please fill in all fields", Toast.LENGTH_SHORT).show();
            } else {
                ISpan step2ProcessSpan = dogProfileActivitySpan.startChild("step2.process_dog_profile", "registration.process");
                try {
                    Intent intent = new Intent(DogProfileActivity.this, HydrantPreferencesActivity.class);

                    intent.putExtra("sentry_trace_id", dogProfileActivitySpan.getSpanContext().getTraceId().toString());
                    intent.putExtra("sentry_span_id", dogProfileActivitySpan.getSpanContext().getSpanId().toString());
                    intent.putExtra("sentry_parent_span_id", dogProfileActivitySpan.getSpanContext().getParentSpanId().toString());

                    // 1. Set the result for MainActivity: OK means successful
                    setResult(RESULT_OK, intent); // Pass the intent (can contain data if needed)

                    // 2. Start the next activity normally (no need for result from here unless next step needs callback)
                    startActivity(intent);

                    // 3. Finish this activity
                    finish();

                } finally {
                    step2ProcessSpan.finish(SpanStatus.OK);
                    // Sentry: Finish the activity's span here as it's completed its task for the next step.
                    // This is ideal if this span represents the work *within* this activity.
                    if (dogProfileActivitySpan != null && !dogProfileActivitySpan.isFinished()) {
                        dogProfileActivitySpan.finish(SpanStatus.OK);
                    }
                }
            }
        });
    }

    @Override
    public void onBackPressed() {
        // Handle back press: signal cancellation to the calling activity
        setResult(RESULT_CANCELED);
        super.onBackPressed();
        // Sentry: If user cancels by pressing back
        if (dogProfileActivitySpan != null && !dogProfileActivitySpan.isFinished()) {
            dogProfileActivitySpan.finish(SpanStatus.CANCELLED);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // Ensure span is finished if not already. This catches cases where finish()
        // might not have been called due to an unexpected scenario.
        if (dogProfileActivitySpan != null && !dogProfileActivitySpan.isFinished()) {
            // If the activity is finishing, but not due to a successful "next" button click,
            // it likely means cancellation or an unexpected exit.
            // However, if we finished it in nextButton.setOnClickListener, this won't be called.
            if (isFinishing()) {
                dogProfileActivitySpan.finish(SpanStatus.CANCELLED);
            }
        }
    }
}