package com.example.snoutandabout;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;

public class SwipingActivity extends AppCompatActivity {

    ImageView hydrantImageView;
    TextView hydrantInfoTextView;
    Button btnLike, btnNope;

    List<Hydrant> hydrants;
    int currentIndex = 0;

    // For basic swipe detection
    private GestureDetector gestureDetector;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_swiping);

        hydrantImageView = findViewById(R.id.hydrant_image_view);
        hydrantInfoTextView = findViewById(R.id.hydrant_info_text_view);
        btnLike = findViewById(R.id.btn_like);
        btnNope = findViewById(R.id.btn_nope);

        // 1. Prepare Dummy Hydrant Data
        hydrants = new ArrayList<>();
        hydrants.add(new Hydrant("Rusty Rex", R.drawable.hydrant_red, "Red", "Old school charm, looking for a good sniff!"));
        hydrants.add(new Hydrant("Blue Belle", R.drawable.hydrant_blue, "Blue", "Freshly painted, loves park walks."));
        hydrants.add(new Hydrant("Green Giant", R.drawable.hydrant_green, "Green", "Big and sturdy, good for tall dogs."));
        hydrants.add(new Hydrant("Yellow Fellow", R.drawable.hydrant_yellow, "Yellow", "Sunny disposition, always accessible."));
        // Add more hydrants and their images as needed

        // 2. Display the first hydrant
        displayCurrentHydrant();

        // 3. Implement button click listeners
        btnLike.setOnClickListener(v -> {
            handleSwipeRight();
        });

        btnNope.setOnClickListener(v -> {
            handleSwipeLeft();
        });

        // 4. Implement simple swipe gestures using GestureDetector
        // This allows for more robust swipe detection than a simple OnTouchListener
        gestureDetector = new GestureDetector(this, new GestureDetector.SimpleOnGestureListener() {
            private static final int SWIPE_THRESHOLD = 100;
            private static final int SWIPE_VELOCITY_THRESHOLD = 100;

            @Override
            public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
                float diffX = e2.getX() - e1.getX();
                float diffY = e2.getY() - e1.getY();

                if (Math.abs(diffX) > Math.abs(diffY) && Math.abs(diffX) > SWIPE_THRESHOLD && Math.abs(velocityX) > SWIPE_VELOCITY_THRESHOLD) {
                    if (diffX > 0) { // Swiped right
                        handleSwipeRight();
                    } else { // Swiped left
                        handleSwipeLeft();
                    }
                    return true;
                }
                return false;
            }
        });

        // Attach the GestureDetector to the ImageView
        hydrantImageView.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                return gestureDetector.onTouchEvent(event);
            }
        });
    }

    private void displayCurrentHydrant() {
        if (currentIndex < hydrants.size()) {
            Hydrant currentHydrant = hydrants.get(currentIndex);
            hydrantImageView.setImageResource(currentHydrant.getImageResId());
            hydrantInfoTextView.setText(currentHydrant.getName() + " - " + currentHydrant.getColor() + ", " + currentHydrant.getDescription());
        } else {
            // No more hydrants to show
            hydrantImageView.setVisibility(View.GONE); // Hide image
            hydrantInfoTextView.setText("No more hydrants for now! Check back later.");
            btnLike.setVisibility(View.GONE);
            btnNope.setVisibility(View.GONE);
            Toast.makeText(this, "You've seen all the hydrants!", Toast.LENGTH_LONG).show();
        }
    }

    private void handleSwipeRight() {
        if (currentIndex < hydrants.size()) {
            Hydrant likedHydrant = hydrants.get(currentIndex);
            Toast.makeText(this, "Woof! You liked " + likedHydrant.getName() + "!", Toast.LENGTH_SHORT).show();
            // In a real app, you'd save this 'like' to a database
            // Simulate a match randomly for fun:
            if (Math.random() < 0.3) { // 30% chance of a match
                Toast.makeText(this, "IT'S A MATCH with " + likedHydrant.getName() + "!", Toast.LENGTH_LONG).show();
                // You could open a "Match" dialog here
            }
            currentIndex++;
            displayCurrentHydrant();
        }
    }

    private void handleSwipeLeft() {
        if (currentIndex < hydrants.size()) {
            Hydrant dislikedHydrant = hydrants.get(currentIndex);
            Toast.makeText(this, "Grrr... " + dislikedHydrant.getName() + " is a no-go.", Toast.LENGTH_SHORT).show();
            currentIndex++;
            displayCurrentHydrant();
        }
    }
}

// At the bottom of SwipingActivity.java or in a new file Hydrant.java
class Hydrant {
    String name;
    int imageResId; // Resource ID for the drawable
    String color;
    String description; // A short bio

    public Hydrant(String name, int imageResId, String color, String description) {
        this.name = name;
        this.imageResId = imageResId;
        this.color = color;
        this.description = description;
    }

    public String getName() { return name; }
    public int getImageResId() { return imageResId; }
    public String getColor() { return color; }
    public String getDescription() { return description; }
}