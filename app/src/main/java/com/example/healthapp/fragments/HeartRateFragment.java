package com.example.healthapp.fragments;

import static android.content.ContentValues.TAG;

import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.fragment.app.Fragment;

import com.example.healthapp.R;
import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.AxisBase;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.formatter.ValueFormatter;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.Locale;
import java.util.Map;
import java.util.TimeZone;


public class HeartRateFragment extends Fragment {

    private FirebaseFirestore db;
    private CollectionReference userHeartDataRef;
    private FirebaseUser currentUser;
    private String userId;
    private TextView HeartTextView;

    private LineChart HeartLineChart;
    private SimpleDateFormat dayFormat;
    private Date xyzw;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_heartrate, container, false);
        // Initialize Firebase
        db = FirebaseFirestore.getInstance();
        dayFormat = new SimpleDateFormat("HH:mm", Locale.getDefault());
        dayFormat.setTimeZone(TimeZone.getDefault());

        // Get current user's ID
        FirebaseAuth mAuth = FirebaseAuth.getInstance();
        currentUser = mAuth.getCurrentUser();
        userId = currentUser.getUid();
        userHeartDataRef = db.collection("Data").document(userId).collection("HeartData");
        userHeartDataRef.orderBy("timestamp", Query.Direction.DESCENDING).limit(1).get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    ArrayList<Entry> heartDataEntries = new ArrayList<>();
                    ArrayList<String> xValues = new ArrayList<>();
                    int index = 0;
                    if (queryDocumentSnapshots.isEmpty()) {
                        // No documents found
                        Toast.makeText(getActivity(), "No heart rate data found", Toast.LENGTH_SHORT).show();
                    } else {
                        QueryDocumentSnapshot document = (QueryDocumentSnapshot) queryDocumentSnapshots.getDocuments().get(0); // Get the first document
                        Map<String, Object> data = document.getData();
                        for (Map.Entry<String, Object> entry : data.entrySet()) {
                            if (!entry.getKey().equals("timestamp")) {
                                int bpm = Integer.parseInt(entry.getValue().toString());
                                Log.d(TAG, "onCreateView: " + bpm);
                                heartDataEntries.add(new Entry(index++, bpm));
                                String xyz= entry.getKey();
                                xValues.add(String.valueOf(xyz));
//                        for (QueryDocumentSnapshot document : queryDocumentSnapshots){
//                            int bpm = 0;
//                            Map<String, Object> data = document.getData();
//                            for (Map.Entry<String, Object> entry : data.entrySet()) {
//                                if (!entry.getKey().equals("timestamp")) {
//                                    bpm = Integer.parseInt(entry.getValue().toString());
//                                }
//                                Log.d(TAG, "onCreateView: " +bpm);
//                            }
//                            heartDataEntries.add(new Entry(index++, bpm));
//                            Log.d(TAG, "onCreateView: "+ heartDataEntries.size());
//                        }
                            }
                        }
                    }
                    // Create a dataset and add the sleep data entries to it
                    LineDataSet heartDataSet = new LineDataSet(heartDataEntries, "Heart Rate");

                    // Set the line color
                    heartDataSet.setColor(Color.WHITE);
                    // Set the circle color
                    heartDataSet.setCircleColor(Color.WHITE);

                    // Set the style of the line chart
                    LineChart lineChart = view.findViewById(R.id.lineChart);
                    lineChart.setDrawGridBackground(false);
                    lineChart.setDrawBorders(false);
                    lineChart.getDescription().setEnabled(false);
                    lineChart.getLegend().setEnabled(false);
                    lineChart.getXAxis().setEnabled(true); // Enable X-axis
                    lineChart.getAxisRight().setEnabled(false);
                    lineChart.setTouchEnabled(false);

                    // Reverse the order of the xValues and heartDataEntries arrays
                    Collections.reverse(xValues);
                    Collections.reverse(heartDataEntries);

                    int totalEntries = heartDataEntries.size();
                    for (int i = 0; i < totalEntries; i++) {
                        int reversedIndex = totalEntries - i - 1;
                        heartDataEntries.get(reversedIndex).setX(reversedIndex);
                    }
                    // Set up the axis labels
                    YAxis yAxis = lineChart.getAxisLeft();
                    yAxis.setTextColor(Color.WHITE);
                    yAxis.setDrawGridLines(false);

                    XAxis xAxis = lineChart.getXAxis();
                    xAxis.setTextColor(Color.WHITE);
                    xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
                    xAxis.setDrawGridLines(false);
                    xAxis.setAxisLineColor(Color.WHITE);
                    xAxis.setValueFormatter(new ValueFormatter() {

                        @Override
                        public String getAxisLabel(float value, AxisBase axis) {
                            int index = (int) value;
                            if ((int) value >= 0 && (int) value < xValues.size()) {
                                String fieldValue = xValues.get(index);
                                String formattedTime = fieldValue.substring(0, 2) + ":" + fieldValue.substring(2);
                                return formattedTime;

                            } else {
                                return "";
                            }

                        }
                    });
                    Log.d(TAG, "onCreateV: " + xValues.size());
                    LineData data = new LineData(heartDataSet);

                    // Set the data to the chart
                    lineChart.setData(data);

                    // Refresh the chart
                    lineChart.invalidate();
                });




        return view;
    }
}