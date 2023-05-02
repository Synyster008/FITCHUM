package com.example.healthapp;

import static android.content.ContentValues.TAG;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.SetOptions;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.LocalTime;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.TimeZone;
import java.util.concurrent.TimeUnit;

public class SleepService extends Service implements SensorEventListener {
    private SensorManager sensorManager;
    private Sensor accelerometer;
    private FirebaseFirestore db = FirebaseFirestore.getInstance();
    private FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
    String userId = currentUser.getUid();
    private CollectionReference userSleepDataRef = db.collection("Data").document(userId).collection("SleepData");
    private Handler handler;
    private SimpleDateFormat dateFormat, timeFormat;
    private String start_time, end_time;
    private Date current_time, startTime, endTime;

    float[] last_acceleration = new float[0];
    private int sleepTime = 0;
    private long lastCheckTime = System.currentTimeMillis();
    private long time_diff;

    public void onCreate() {
        super.onCreate();
        // Initialize the SensorManager and Sensor objects
        sensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
        accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        handler = new Handler();
        handler.postDelayed(runnable, TimeUnit.MINUTES.toMillis(1));
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d("SleepService", "onStartCommand() called");
        sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_STEP_COUNTER);
        sensorManager.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_NORMAL);

        dateFormat = new SimpleDateFormat("yyMMdd", Locale.getDefault());
        dateFormat.setTimeZone(TimeZone.getDefault());
        timeFormat = new SimpleDateFormat("HH:mm", Locale.getDefault());
        timeFormat.setTimeZone(TimeZone.getDefault());
        db.collection("Users").document(userId).get()
                .addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
                    @Override
                    public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                        if (task.isSuccessful()) {
                            DocumentSnapshot document = task.getResult();
                            if (document.exists()) {
                                start_time = document.getString("start_time");
                                end_time = document.getString("end_time");
                                time_diff = document.getLong("time_diff");
                                sleepTime = (int) time_diff;
                                try {
                                    startTime = timeFormat.parse(start_time);
                                    endTime = timeFormat.parse(end_time);
                                } catch (ParseException e) {
                                    throw new RuntimeException(e);
                                }
                                Log.d(TAG, "onStarte: " + startTime);
                                Log.d(TAG, "onStarte: " + endTime);
                                Log.d(TAG, "onStarte: " + time_diff);
                            }
                        }
                    }
                });

        return START_STICKY;
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
            float x = event.values[0];
            float y = event.values[1];
            float z = event.values[2];
            LocalTime time = null;
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                time = LocalTime.now();
            }
            try {
                current_time = timeFormat.parse(String.valueOf(time));
            } catch (ParseException e) {
                throw new RuntimeException(e);
            }
            Log.d(TAG, "onSensorChanged: Detected!!!");

            if ((current_time.after(startTime) && current_time.before(endTime)) || current_time.equals(startTime)) {
                Log.d(TAG, "onSensorChanged: Operation Started");
                float acceleration = Math.abs(x - last_acceleration[0])
                        + Math.abs(y - last_acceleration[1])
                        + Math.abs(z - last_acceleration[2] - Sensor.TYPE_GRAVITY);
                Log.d(TAG, "onSensorChanged: " + acceleration);
                if (acceleration > 2) {
                    sleepTime--;
                    handler.postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            // introduce a delay of a minute
                        }
                    },60000);
                    Log.d(TAG, "onSensorChanged: " + sleepTime);
                }
            }
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int i) {

    }

    private Runnable runnable = new Runnable() {
        @RequiresApi(api = Build.VERSION_CODES.O)
        @Override
        public void run() {
            LocalTime time = LocalTime.now();
            try {
                current_time = timeFormat.parse(String.valueOf(time));
            } catch (ParseException e) {
                throw new RuntimeException(e);
            }
            Log.d(TAG, "run: " + startTime);
            Log.d(TAG, "run: " + endTime);
            Log.d(TAG, "run: " + current_time);
            if (current_time.after(endTime)) {
                Log.d(TAG, "onComplete: Starting Store");
                String date = dateFormat.format(new Date());
                Map<String, Long> data = new HashMap<>();
                Map<String, FieldValue> tdata = new HashMap<>();
                tdata.put("timestamp", FieldValue.serverTimestamp());
                data.put("Actual Sleep Hours", (long) sleepTime);
                db.collection("Data").document(userId).collection("SleepData").document(date).set(data).addOnCompleteListener(new OnCompleteListener<Void>() {
                    public void onComplete(@NonNull Task<Void> task) {
                        if (task.isSuccessful()) {
                            Log.d(TAG, "Sleep data saved successfully.");
                        } else {
                            Log.e(TAG, "Error saving sleep data: ", task.getException());
                        }
                    }
                });
                db.collection("Data").document(userId).collection("SleepData").document(date).set(tdata, SetOptions.merge());
                Log.d(TAG, "onComplete: Stored" + sleepTime);



            }

            handler.postDelayed(this, 60000);
        }
    };
}
