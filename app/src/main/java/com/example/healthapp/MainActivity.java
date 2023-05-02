package com.example.healthapp;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;

import com.example.healthapp.fragments.HeartRateFragment;
import com.example.healthapp.fragments.HomeFragment;
import com.example.healthapp.fragments.SleepFragment;
import com.example.healthapp.fragments.StepFragment;
import com.google.android.material.bottomnavigation.BottomNavigationView;

import org.checkerframework.checker.nullness.qual.NonNull;

public class MainActivity extends AppCompatActivity {

    private BottomNavigationView bottomNavigationView;
    private Fragment selectorFragment;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        if(ContextCompat.checkSelfPermission(this, android.Manifest.permission.ACTIVITY_RECOGNITION) == PackageManager.PERMISSION_DENIED){
            //ask for permission
            requestPermissions(new String[]{Manifest.permission.ACTIVITY_RECOGNITION}, 0);
        }
        bottomNavigationView = findViewById(R.id.bottom_navigation);
        selectorFragment = new SleepFragment();  // Set the HomeFragment as the initial fragment
        Log.d("MainActivity", "Starting service");
        Intent intent = new Intent(this, StepCounterService.class);
        startService(intent);
        Intent intent_1 = new Intent(this, SleepService.class);
        startService(intent_1);

        bottomNavigationView.setOnNavigationItemSelectedListener(new BottomNavigationView.OnNavigationItemSelectedListener() {
            @Override
            public boolean onNavigationItemSelected(@NonNull MenuItem menuItem) {

                switch (menuItem.getItemId()) {
                    case R.id.nav_home:
                        selectorFragment = new HomeFragment();
                        break;

                    case R.id.nav_sleep:
                        selectorFragment = new SleepFragment();
                        break;

                    case R.id.heart:
                        selectorFragment = null;
                        startActivity(new Intent(MainActivity.this, HeartActivity.class));
                        break;

                    //case R.id.heart:
                    //selectorFragment = new HeartFragment();
                    //break;

                    case R.id.nav_step:
                        selectorFragment = new StepFragment();
                        break;

                    case R.id.nav_heart:
                        selectorFragment = new HeartRateFragment();
                        break;
                }

                if (selectorFragment != null) {
                    getSupportFragmentManager().beginTransaction().replace(R.id.fragment_container, selectorFragment).commit();
                }

                return true;

            }
        });

        getSupportFragmentManager().beginTransaction().replace(R.id.fragment_container, selectorFragment).commit();
    }
}
