package com.example.food_saver;

import android.content.Intent;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;

import com.example.food_saver.ngo.NgoDashboardActivity;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Login/role-check ka logic abhi nahi hai, isliye seedha NGO
        // Dashboard khol rahe hain.
        startActivity(new Intent(this, NgoDashboardActivity.class));
        finish();
    }
}
