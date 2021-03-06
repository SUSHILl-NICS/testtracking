package com.example.nics.testtracking;

import android.Manifest;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.os.AsyncTask;
import android.os.Build;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;
import android.widget.TextView;
import android.widget.Toast;

import com.example.nics.testtracking.Database.MapUpdateDataBase;
import com.example.nics.testtracking.Util.DirectionsJSONParser;
import com.example.nics.testtracking.Util.LocationDto;
import com.example.nics.testtracking.Util.RideDto;
import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.PolylineOptions;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ResultActivity extends AppCompatActivity implements OnMapReadyCallback {
    private static final String TAG = ResultActivity.class.getSimpleName();
    private Context context = ResultActivity.this;
    private static final int REQUEST_COARSE_LOCATION = 1001;
    private GoogleMap googleMap;
    LocationDto locationDto = new LocationDto();
    private MapUpdateDataBase dbHelper;
    ArrayList<LocationDto> locationListDto;
    private Map<String, String> markerMap = new HashMap<>();
    private TextView tvTime;
    private TextView tvStartDate;
    private TextView tvEndDate;
    private TextView tvDistance;
    private ArrayList<RideDto> rideList;
    private double totalDistance=0.0;
    private static final String GOOGLE_API_KEY = "AIzaSyBFjK8UInAeNGfhx8attCH8UNY6xzNjuwU";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_result);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        dbHelper = new MapUpdateDataBase(this);
        initialiseUiElement();
        if (!checkPermission()) {
            requestPermission();
        }
        intializeMap();

    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()){
            case android.R.id.home:
                onBackPressed();
                break;
        }
        return super.onOptionsItemSelected(item);
    }

    private void initialiseUiElement() {
        tvTime = (TextView) findViewById(R.id.time_tv);
        tvStartDate = (TextView) findViewById(R.id.startDate_tv);
        tvEndDate = (TextView) findViewById(R.id.endDate_tv);
        tvDistance = (TextView) findViewById(R.id.distance_tv);
    }

    private void getAllRecordFromLocationDb() {
        locationListDto = dbHelper.getLocation(Constants.ADD_RECORD);
        if (locationListDto.size() > 0) {
            rideList = new ArrayList<>();
            for (int i = 0; i < locationListDto.size(); i++) {
                locationDto = locationListDto.get(i);
                RideDto rideDto = new RideDto();
                rideDto.setLatitude(locationDto.getLatitude());
                rideDto.setLongitude(locationDto.getLongitude());
                rideList.add(rideDto);
            }
            if (!rideList.isEmpty()) {
                for(int i=0;i<rideList.size()-1;i++){
                    // Getting URL to the Google Directions API
                    getDirectionsUrl(rideList.get(i), rideList.get(i+1));

                }
//                placeLatLongToMap();
                tvStartDate.setText(locationListDto.get(0).getStartDateTime());
                tvEndDate.setText(locationDto.getEndDateTime());
                int ll = Integer.parseInt(locationDto.getCumnTime());
                String l = getDurationTime(ll);
                tvTime.setText(l);
            }
        }
    }

    private String getDurationTime(int time) {

        int hrs = time / 3600;
        int mins = (time % 3600) / 60;
        time = time % 60;
        return twoDigitsString(hrs) + " : " + twoDigitsString(mins) + " : " + twoDigitsString(time);
    }

    private String twoDigitsString(int number) {
        if (number == 0) {

            return "00";
        }
        if (number / 10 == 0) {
            return "0" + number;
        }
        return String.valueOf(number);
    }

    private void placeLatLongToMap() {
       /* PolylineOptions pOptions = new PolylineOptions()
                .width(7)
                .color(Color.RED)
                .geodesic(true);

        for (RideDto rideDto : rideList) {
            pOptions.add(new LatLng(Double.parseDouble(rideDto.getLatitude()), Double.parseDouble(rideDto.getLongitude())));
        }
        RideDto rideDto = rideList.get(0);
        LatLng latLng = new LatLng(Double.parseDouble(rideDto.getLatitude()), Double.parseDouble(rideDto.getLongitude()));

        googleMap.moveCamera(CameraUpdateFactory.newLatLng(latLng));
        // Zoom in the Google Map
        googleMap.animateCamera(CameraUpdateFactory.zoomTo(15));
        googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(latLng, 13));
        googleMap.addPolyline(pOptions);*/



    }

   /* private void getDirectionsUrl(RideDto origin, RideDto dest) {
        // Origin of route
        String str_origin = "origin=" + origin.getLatitude() + "," + origin.getLongitude();
        // Destination of route
        String str_dest = "destination=" + dest.getLatitude() + "," + dest.getLongitude();
        // Sensor enabled
        String sensor = "mode=driving&sensor=false";
        String key = "key"+ GOOGLE_API_KEY;
        // Building the parameters to the web service
        String parameters = str_origin + "&" + str_dest + "&" + key;
        // Output format
        String output = "json";
        // Building the url to the web service
        String url = "https://maps.googleapis.com/maps/api/directions/json?" + parameters;
        DownloadTask downloadTask = new DownloadTask();
        // Start downloading json data from Google Directions API
        downloadTask.execute(url);
//        return url;
    }*/

    private void getDirectionsUrl(RideDto origin, RideDto dest) {
        // Origin of route
        String str_origin = "origin=" + origin.getLatitude() + "," + origin.getLongitude();
        // Destination of route
        String str_dest = "destination=" + dest.getLatitude() + "," + dest.getLongitude();
        // Sensor enabled
        String sensor = "mode=driving&sensor=true";
        // Building the parameters to the web service
        String parameters = str_origin + "&" + str_dest + "&" + sensor;
        // Output format
        String output = "json";
        // Building the url to the web service
        String url = "https://maps.googleapis.com/maps/api/directions/" + output + "?" + parameters;
        DownloadTask downloadTask = new DownloadTask();
        // Start downloading json data from Google Directions API
        downloadTask.execute(url);
//        return url;
    }
    // Fetches data from url passed
    private class DownloadTask extends AsyncTask<String, Void, String>{

        // Downloading data in non-ui thread
        @Override
        protected String doInBackground(String... url) {

            // For storing data from web service
            String data = "";

            try{
                // Fetching the data from web service
                data = downloadUrl(url[0]);
            }catch(Exception e){
                Log.d("Background Task",e.toString());
            }
            return data;
        }

        // Executes in UI thread, after the execution of
        // doInBackground()
        @Override
        protected void onPostExecute(String result) {
            super.onPostExecute(result);

            Log.i(TAG+"Map json result",result);
            ParserTask parserTask = new ParserTask();
            // Invokes the thread for parsing the JSON data
            parserTask.execute(result);

        }
    }
    /**
     * A method to download json data from url
     */
    private String downloadUrl(String strUrl) throws IOException {
        String data = "";
        InputStream iStream = null;

        HttpURLConnection urlConnection = null;
        try {
            URL url = new URL(strUrl);
            // Creating an http connection to communicate with url
            urlConnection = (HttpURLConnection) url.openConnection();
            // Connecting to url
            urlConnection.connect();
            // Reading data from url
            iStream = urlConnection.getInputStream();

            BufferedReader br = new BufferedReader(new InputStreamReader(iStream));

            StringBuffer sb = new StringBuffer();

            String line = "";
            while ((line = br.readLine()) != null) {
                sb.append(line);
            }

            data = sb.toString();

            br.close();

        } catch (Exception e) {
            //Log.d("Exception while downloading url", e.toString());
        } finally {
            iStream.close();
            urlConnection.disconnect();
        }
        return data;
    }

    /**
     * A class to parse the Google Places in JSON format
     */
    private class ParserTask extends AsyncTask<String, Integer, List<List<HashMap<String, String>>>> {
        Double dist = 0.0;
        Boolean isDistanceinKm = false;
        String distanceText ;
        String duration = "";
        // Parsing the data in non-ui thread
        @Override
        protected List<List<HashMap<String, String>>> doInBackground(String... jsonData) {

            JSONObject jObject;
            List<List<HashMap<String, String>>> routes = null;

            try {
                jObject = new JSONObject(jsonData[0]);
                DirectionsJSONParser parser = new DirectionsJSONParser();
                // Starts parsing data
                routes = parser.parse(jObject);
            } catch (Exception e) {
                e.printStackTrace();
            }
            Log.i(TAG+" Routs ",routes.toString());
            return routes;
        }

        // Executes in UI thread, after the parsing process
        @Override
        protected void onPostExecute(List<List<HashMap<String, String>>> result) {
            ArrayList<LatLng> points = null;
            PolylineOptions lineOptions = null;
            MarkerOptions markerOptions = new MarkerOptions();
            /// Traversing through all the routes
            for(int i=0;i<result.size();i++){
                points = new ArrayList<LatLng>();
                lineOptions = new PolylineOptions();
                // Fetching i-th route
                List<HashMap<String, String>> path = result.get(i);

                // Fetching all the points in i-th route
                for(int j=0;j<path.size();j++){
                    HashMap<String,String> point = path.get(j);

                    if(j==0){	// Get distance from the list
                        distanceText = (String)point.get("distance");
                        continue;
                    }else if(j==1){ // Get duration from the list
                        duration = (String)point.get("duration");
                        continue;
                    }

                    double lat = Double.parseDouble(point.get("lat"));
                    double lng = Double.parseDouble(point.get("lng"));
                    LatLng position = new LatLng(lat, lng);
                    points.add(position);
                }
                // Adding all the points in the route to LineOptions
                lineOptions.addAll(points);
                lineOptions.width(10);
                lineOptions.color(Color.BLUE);

            }
//            Log.i(TAG+" distance ",distanc eText);
            if (distanceText != null) {
                if (distanceText.contains("km")
                        || distanceText.contains("KM")
                        || distanceText.contains("Km")) {
                    isDistanceinKm = true;
                    Log.i(TAG, "isDistanceinKm " + isDistanceinKm);
                }
                dist = Double.parseDouble(distanceText.replaceAll("[^\\.0123456789]", ""));
                if (!isDistanceinKm) {
                    Double kilometers = 0.0;
                    kilometers = dist / 1000;
                    dist = kilometers;
                    Log.i(TAG+"isDistanceinKm",""+dist);
                }
            }

            // Drawing polyline in the Google Map for the i-th route
            try {
                googleMap.addPolyline(lineOptions);
                LatLngBounds.Builder builder = new LatLngBounds.Builder();
                for (LatLng point : points) {
                    builder.include(point);
                }
                LatLngBounds bounds = builder.build();
                int padding = 0; // offset from edges of the map in pixels
                CameraUpdate cu = CameraUpdateFactory.newLatLngBounds(bounds,padding);
                googleMap.moveCamera(cu);
                googleMap.animateCamera(cu,10,null);
            }catch (Exception e){
                e.printStackTrace();
            }

            LogFile.RecordLogFile("location Distance"+": "+distanceText+" time: "+new SimpleDateFormat("dd-MM-yyyy HH:mm:ss a").format(new Date()),Constants.LOG_DISTANCE_FILE);
            totalDistance+=dist;
            LogFile.RecordLogFile("location changed Distance"+": "+totalDistance+" time: "+new SimpleDateFormat("dd-MM-yyyy HH:mm:ss a").format(new Date()),Constants.LOG_DISTANCE_FILE);
            tvDistance.setText(""+totalDistance);

        }
    }

    @Override
    public void onBackPressed() {
        dbHelper.deleteRecord();
        int i = dbHelper.getItemCount();
        Log.i(TAG + " Database Count :", "size --" + i);
        rideList.clear();
        RideDto rideDto=new RideDto();
        rideDto.setLongitude("");
        rideDto.setLatitude("");
        locationDto.setStartDateTime("");
        locationDto.setEndDateTime("");
        locationDto.setLatitude("");
        locationDto.setLongitude("");
        locationDto.setCumnTime("0");

        Intent intent = new Intent(ResultActivity.this, MainActivity.class);
        startActivity(intent);
        this.finish();

    }

    private boolean checkPermission() {
        int fineLocation = ContextCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.ACCESS_FINE_LOCATION);
        int storage = ContextCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.WRITE_EXTERNAL_STORAGE);
        return fineLocation == PackageManager.PERMISSION_GRANTED && storage == PackageManager.PERMISSION_GRANTED;
    }

    private void requestPermission() {
        ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.WRITE_EXTERNAL_STORAGE}, REQUEST_COARSE_LOCATION);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults) {
        switch (requestCode) {
            case REQUEST_COARSE_LOCATION:
                if (grantResults.length > 0) {

                    boolean locationAccepted = grantResults[0] == PackageManager.PERMISSION_GRANTED;
                    boolean storage = grantResults[1] == PackageManager.PERMISSION_GRANTED;
                    if (locationAccepted && storage) {
                        Toast.makeText(context, "Permission Granted, Now you can access location data and storage", Toast.LENGTH_LONG).show();
                    } else {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                            if (shouldShowRequestPermissionRationale(Manifest.permission.ACCESS_FINE_LOCATION) ||
                                    shouldShowRequestPermissionRationale(Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
                                showMessageOKCancel("You need to allow access to both the permissions",
                                        new DialogInterface.OnClickListener() {
                                            @Override
                                            public void onClick(DialogInterface dialog, int which) {
                                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                                                    requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.WRITE_EXTERNAL_STORAGE}, REQUEST_COARSE_LOCATION);
                                                }
                                            }
                                        });
                                return;
                            }
                        }

                    }
                }


                break;
        }
    }

    private void showMessageOKCancel(String message, DialogInterface.OnClickListener okListener) {
        new AlertDialog.Builder(ResultActivity.this)
                .setMessage(message)
                .setPositiveButton("OK", okListener)
                .setNegativeButton("Cancel", null)
                .create()
                .show();
    }

    private void intializeMap() {
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map_resultActivity);
        mapFragment.getMapAsync(this);
    }

    private void implementation() {

        googleMap.getUiSettings().setZoomControlsEnabled(true);
        googleMap.getUiSettings().setMyLocationButtonEnabled(true);
        googleMap.getUiSettings().setCompassEnabled(true);
        googleMap.setBuildingsEnabled(true);
        googleMap.setTrafficEnabled(false);
        googleMap.getUiSettings().setRotateGesturesEnabled(true);
        getAllRecordFromLocationDb();
    }
    @Override
    public void onMapReady(GoogleMap googleMap) {
        this.googleMap = googleMap;
        //show point on ur location
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            return;
        }
        this.googleMap.setMyLocationEnabled(true);
        implementation();
    }



}
