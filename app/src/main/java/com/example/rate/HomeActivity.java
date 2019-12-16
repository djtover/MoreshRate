package com.example.rate;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import com.google.firebase.auth.FirebaseAuth;

public class HomeActivity extends AppCompatActivity {
    FirebaseAuth firebaseAuth;
    Button btnRate, btnSearch, btnLogout;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //hides the action bar.
        try
        {
            this.getSupportActionBar().hide();
        }
        catch (NullPointerException e){}
        setContentView(R.layout.activity_home);
        firebaseAuth = firebaseAuth.getInstance();
        btnSearch = findViewById(R.id.button5);
        btnRate = findViewById(R.id.button3);
        btnLogout = findViewById(R.id.button8);

        /**
         * this method takes the user to the rate activity after clicking the rate button.
         */
        btnRate.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent i = new Intent(HomeActivity.this, RateActivity.class);
                startActivity(i);
            }
        });

        /**
         * this method takes the user to the search activity after clicking the search button.
         */
        btnSearch.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent i = new Intent(HomeActivity.this, SearchActivity.class);
                startActivity(i);
            }
        });

        /**
         * this method takes the user to the main activity after clicking the logout button.
         */
        btnLogout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                try {
                    firebaseAuth.getInstance().signOut();
                    Intent i = new Intent(HomeActivity.this, MainActivity.class);
                    startActivity(i);
                    finish();
                } catch (Exception e) {
                    e.printStackTrace();
                }


            }
        });
    }


}
