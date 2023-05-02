package com.example.healthapp;

import static android.content.ContentValues.TAG;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Handler;
import android.os.IBinder;
import android.util.Log;

import com.github.mikephil.charting.data.Entry;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.firestore.SetOptions;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TimeZone;
import java.util.concurrent.TimeUnit;

public class StepCounterService extends Service implements SensorEventListener {

    private SensorManager sensorManager;
    private Sensor stepSensor;

    private Calendar calendar;
    private SimpleDateFormat hourFormat;
    private SimpleDateFormat dateFormat;
    private SimpleDateFormat dayFormat;
    private FirebaseFirestore db = FirebaseFirestore.getInstance();
    private FirebaseUser currentUser;
    String userId;
    private CollectionReference userStepsDataRef;
    private CollectionReference userStepsActualDataRef;

    private Handler handler;

    private int currentSteps = 0;

    @Override
    public void onCreate() {
        super.onCreate();
        // Initialize the SensorManager and Sensor objects
        sensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
        stepSensor = sensorManager.getDefaultSensor(Sensor.TYPE_STEP_COUNTER);
        handler = new Handler();
        handler.postDelayed(runnable, TimeUnit.MINUTES.toMillis(1));

    }


    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        //Log.d("StepCounterService", "onStartCommand() called");
        sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        stepSensor = sensorManager.getDefaultSensor(Sensor.TYPE_STEP_COUNTER);
        currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser!= null){
            userId = currentUser.getUid();
            userStepsDataRef = db.collection("Data").document(userId).collection("StepsData_Temp");
            userStepsActualDataRef = db.collection("Data").document(userId).collection("StepsData");
        }

        sensorManager.registerListener(this, stepSensor, SensorManager.SENSOR_DELAY_NORMAL);
        calendar = Calendar.getInstance();
        hourFormat = new SimpleDateFormat("HH", Locale.getDefault());
        hourFormat.setTimeZone(TimeZone.getDefault());
        dateFormat = new SimpleDateFormat("yyMMdd", Locale.getDefault());
        dateFormat.setTimeZone(TimeZone.getDefault());
        dayFormat = new SimpleDateFormat("yyMMddHH", Locale.getDefault());
        dayFormat.setTimeZone(TimeZone.getDefault());
        return START_STICKY;
    }



    @Override
    public void onSensorChanged(SensorEvent event) {
        if (event.sensor.getType() == Sensor.TYPE_STEP_COUNTER) {
            currentSteps = (int) event.values[0];
            //Log.d("StepCounterService", "Step event detected!");
        }
    }

    private Runnable runnable = new Runnable() {
        @Override
        public void run() {
            // Store the step count data to the Firebase database
            String hour = hourFormat.format(new Date());
            String date = dateFormat.format(new Date());
            String day = dayFormat.format(new Date());
            Map<String, Integer> data = new HashMap<>();
            data.put("Steps", currentSteps);
            userStepsDataRef.document(day).set(data, SetOptions.merge());
            Map<String, FieldValue> tdata = new HashMap<>();
            tdata.put("timestamp", FieldValue.serverTimestamp());
            userStepsDataRef.document(day).set(tdata, SetOptions.merge());
            //Log.d("StepCounterService", "Step count data stored to Firebase database! Current steps: " + currentSteps);
            userStepsDataRef.orderBy("timestamp", Query.Direction.DESCENDING).limit(2).get().addOnSuccessListener(new OnSuccessListener<QuerySnapshot>() {
                @Override
                public void onSuccess(QuerySnapshot queryDocumentSnapshots) {
                    //Log.d("1", "onSuccess: Accessed");
                    List<DocumentSnapshot> documents= queryDocumentSnapshots.getDocuments();
                    if (documents.size() == 2) {
                        long steps1 = documents.get(0).getLong("Steps");
                        long steps2 = documents.get(1).getLong("Steps");

                        // Calculate the difference between the step counts
                        long diff = steps1 - steps2;
                        if (diff < 0){
                            diff = steps1;
                        }

                        // Create a new document in userStepsActualDataRef with the step count difference
                        Map<String, Object> actual_data = new HashMap<>();
                        actual_data.put(hour, diff);
                        actual_data.put("timestamp", FieldValue.serverTimestamp());
                        userStepsActualDataRef.document(date).set(actual_data, SetOptions.merge());
                        //Log.d("StepCounterService", "Hourly step difference recorded: " + diff);
                    }
                }

            });
            handler.postDelayed(this, 60000);
        }
    };

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}


