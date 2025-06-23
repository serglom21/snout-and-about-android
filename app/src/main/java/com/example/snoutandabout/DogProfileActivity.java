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

public class DogProfileActivity extends AppCompatActivity {

    EditText dogNameInput, dogAgeInput;
    Spinner breedSpinner;
    RadioGroup genderRadioGroup;
    RadioButton radioMale, radioFemale;
    ImageView dogPhotoPreview; // Just a placeholder ImageView for now
    Button nextButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_dog_profile);

        dogNameInput = findViewById(R.id.dog_name_input);
        dogAgeInput = findViewById(R.id.dog_age_input);
        breedSpinner = findViewById(R.id.breed_spinner);
        genderRadioGroup = findViewById(R.id.gender_radio_group);
        radioMale = findViewById(R.id.radio_male);
        radioFemale = findViewById(R.id.radio_female);
        dogPhotoPreview = findViewById(R.id.dog_photo_preview);
        nextButton = findViewById(R.id.btn_next_step2);

        // Populate Breed Spinner
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(
                this,
                R.array.dog_breeds, // Define this array in res/values/strings.xml
                android.R.layout.simple_spinner_item
        );
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        breedSpinner.setAdapter(adapter);

        // Handle dog photo upload (VERY basic, no actual file picker)
        dogPhotoPreview.setOnClickListener(v -> {
            Toast.makeText(this, "Imagine a photo picker here!", Toast.LENGTH_SHORT).show();
            // In a real app, you'd use an Intent to open a gallery or camera
            // Example: Intent pickPhoto = new Intent(Intent.ACTION_PICK, android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
            // startActivityForResult(pickPhoto , 1);
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
                // Pass dog profile data to the next activity
                Intent intent = new Intent(DogProfileActivity.this, HydrantPreferencesActivity.class);
                // You would bundle all this data if you wanted to save it later
                // intent.putExtra("dog_name", dogName);
                startActivity(intent);
            }
        });
    }
}