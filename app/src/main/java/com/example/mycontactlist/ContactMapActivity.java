package com.example.mycontactlist;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;

import android.location.LocationManager;
import android.os.Build;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import android.location.Location;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.location.LocationListener;




public class ContactMapActivity extends AppCompatActivity  implements OnMapReadyCallback, GoogleApiClient.OnConnectionFailedListener, GoogleApiClient.ConnectionCallbacks, LocationListener  {

    final int PERMISSION_REQUEST_LOCATION = 101;
    GoogleMap gMap;
    GoogleApiClient mGoogleApiClient;
    LocationRequest mLocationRequest;
    ArrayList<Contact> contacts = new ArrayList<>();
    Contact currentContact = null;

    SensorManager sensorManager;
    Sensor accelerometer;
    Sensor magnetometer;
    TextView textDirection;



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_contact_map);

        Bundle extras = getIntent().getExtras();
        try {
            ContactDataSource ds = new ContactDataSource(ContactMapActivity.this);
            ds.open();
            if (extras != null) {
                currentContact = ds.getSpecificContact(extras.getInt("contactid"));
            } else {
                contacts = ds.getContacts("contactname", "ASC");
            }
            ds.close();
        }
        catch (Exception e) {
            Toast.makeText(this, "Contact(s) could not be retrieved.", Toast.LENGTH_LONG).show();
        }

        initListButton();
        initMapButton();
        initSettingsButton();
        initMapTypeButton();

        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

        createLocationRequest();

        if (mGoogleApiClient == null) {
            mGoogleApiClient = new GoogleApiClient.Builder(this)
                    .addConnectionCallbacks(this)
                    .addOnConnectionFailedListener(this)
                    .addApi(LocationServices.API)
                    .build();
        }

        sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        magnetometer = sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);

        if (accelerometer != null && magnetometer != null) {
            sensorManager.registerListener(mySensorEventListener, accelerometer, SensorManager.SENSOR_DELAY_FASTEST);
            sensorManager.registerListener(mySensorEventListener, magnetometer, SensorManager. SENSOR_DELAY_FASTEST);
        } else {
            Toast.makeText(this, "Sensors not found",Toast.LENGTH_LONG).show();
        }
        textDirection = (TextView) findViewById(R.id.textHeading);

    }

    protected void onStart() {
        mGoogleApiClient.connect();
        super.onStart();
    }

    protected void onStop() {
        mGoogleApiClient.disconnect();
        super.onStop();
    }

    protected void createLocationRequest() {
        mLocationRequest = new LocationRequest();
        mLocationRequest.setInterval(10000);
        mLocationRequest.setFastestInterval(5000);
        mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
    }



    @Override
    public void onConnected(@Nullable Bundle bundle) {
        if ( Build.VERSION.SDK_INT >= 23 && ContextCompat.checkSelfPermission(getBaseContext(),
                android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
                ContextCompat.checkSelfPermission( getBaseContext(),
                        android.Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return  ;
        }
        LocationServices.FusedLocationApi.requestLocationUpdates(mGoogleApiClient, mLocationRequest, this);
    }

    @Override
    public void onConnectionSuspended(int i) {

    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {

    }

    @Override
    public void onLocationChanged(Location location) {
        Toast.makeText(getBaseContext(), "Lat: "+location.getLatitude()+ " Long: "+location.getLongitude()+" Accuracy:  "+ location.getAccuracy(), Toast.LENGTH_LONG).show();
    }

    @Override
    public void onStatusChanged(String provider, int status, Bundle extras) {

    }

    @Override
    public void onProviderEnabled(String provider) {

    }

    @Override
    public void onProviderDisabled(String provider) {

    }

    private SensorEventListener mySensorEventListener = new SensorEventListener() {

        public void onAccuracyChanged(Sensor sensor, int accuracy) {  }

        float[] accelerometerValues;
        float[] magneticValues;

        public void onSensorChanged(SensorEvent event) {
            if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER)
                accelerometerValues = event.values;
            if (event.sensor.getType() == Sensor.TYPE_MAGNETIC_FIELD)
                magneticValues = event.values;
            if (accelerometerValues!= null && magneticValues!= null) {
                float R[] = new float[9];
                float I[] = new float[9];
                boolean success = SensorManager.getRotationMatrix(R, I, accelerometerValues, magneticValues);
                if (success) {
                    float orientation[] = new float[3];
                    SensorManager.getOrientation(R, orientation);

                    float azimut = (float) Math.toDegrees(orientation[0]);
                    if (azimut < 0.0f) { azimut+=360.0f;}
                    String direction;
                    if (azimut >= 315 || azimut < 45) { direction = "N"; }
                    else if (azimut >= 225 && azimut < 315) { direction = "W"; }
                    else if (azimut >= 135 && azimut < 225) { direction = "S"; }
                    else { direction = "E"; }
                    textDirection.setText(direction);
                }
            }
        }
    };

    @Override
    public void onPause() {
        super.onPause();
        if ( Build.VERSION.SDK_INT >= 23 && ContextCompat.checkSelfPermission(getBaseContext(),
                android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ContextCompat.checkSelfPermission( getBaseContext(),
                android.Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return  ;
        }

        try{
            locationManager.removeUpdates(gpsListener);
            locationManager.removeUpdates(networkListener);

        }
        catch(Exception e){
            e.printStackTrace();
        }
    }

    private void startLocationUpdates() {

        if ( Build.VERSION.SDK_INT >= 23 && ContextCompat.checkSelfPermission(getBaseContext(),
                android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ContextCompat.checkSelfPermission( getBaseContext(),
                android.Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return  ;
        }

        try {
            locationManager = (LocationManager) getBaseContext().getSystemService(Context.LOCATION_SERVICE);

            gpsListener = new LocationListener(){
                public void onLocationChanged(Location location) {
                    TextView txtLatitude = (TextView) findViewById(R.id.textLatitude);
                    TextView txtLongitude = (TextView) findViewById(R.id.textLongitude);
                    //5
                    TextView txtAccuracy = (TextView) findViewById(R.id.textAccuracy);


                    txtLatitude.setText(String.valueOf(location.getLatitude()));
                    txtLongitude.setText(String.valueOf(location.getLongitude()));
                    txtAccuracy.setText(String.valueOf(location.getAccuracy()));
                    if(isBetterLocation(location)){
                        currentBestLocation = location;
                    }

                }
                public void onStatusChanged(String provider, int status, Bundle extras){}
                public void onProviderEnabled(String provider){}
                public void onProviderDisabled(String provider){}


            };
            //addresses = geo.getFromLocationName(address, 1);

            networkListener = new LocationListener(){
                public void onLocationChanged(Location location) {
                    TextView txtLatitude = (TextView) findViewById(R.id.textLatitude);
                    TextView txtLongitude = (TextView) findViewById(R.id.textLongitude);
                    //5
                    TextView txtAccuracy = (TextView) findViewById(R.id.textAccuracy);


                    txtLatitude.setText(String.valueOf(location.getLatitude()));
                    txtLongitude.setText(String.valueOf(location.getLongitude()));
                    txtAccuracy.setText(String.valueOf(location.getAccuracy()));
                    if(isBetterLocation(location)){
                        currentBestLocation = location;
                    }

                }
                public void onStatusChanged(String provider, int status, Bundle extras){}
                public void onProviderEnabled(String provider){}
                public void onProviderDisabled(String provider){}

            };
            locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 0, 0, networkListener);
            locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER,0,0, gpsListener);

        }
        catch (Exception e) {
            Toast.makeText(getBaseContext(),"Error, location not available",
                    Toast.LENGTH_LONG).show();
        }



    }



    @Override
    public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults) {

        switch (requestCode) {
            case PERMISSION_REQUEST_LOCATION: {
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {

                    startLocationUpdates();

                } else {
                    Toast.makeText(ContactMapActivity.this, "MyContactList will not locate  your contacts.", Toast.LENGTH_LONG).show();
                }
            }
        }
    }


    private void initListButton() {
        ImageButton ibList = (ImageButton) findViewById(R.id.imageButtonList);
        ibList.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                Intent intent = new Intent(ContactMapActivity.this, ContactListActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                startActivity(intent);
            }
        });
    }

    private void initMapButton() {
        ImageButton ibMap = (ImageButton) findViewById(R.id.imageButtonMap);
        ibMap.setEnabled(false);
    }
    private void initSettingsButton() {
        ImageButton ibSett = (ImageButton) findViewById(R.id.imageButtonSettings);
        ibSett.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                Intent intent = new Intent(ContactMapActivity.this, ContactSettingsActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                startActivity(intent);
            }
        });
    }

    private void initGetLocationButton() {
        Button locationButton = (Button) findViewById(R.id.buttonGetLocation);
        locationButton.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                try{
                    if(Build.VERSION.SDK_INT>=23){
                        if(ContextCompat.checkSelfPermission(
                                ContactMapActivity.this,
                                android.Manifest.permission.ACCESS_FINE_LOCATION)!=
                                PackageManager.PERMISSION_GRANTED){
                            if(ActivityCompat.shouldShowRequestPermissionRationale(
                                    ContactMapActivity.this,
                                    android.Manifest.permission.ACCESS_FINE_LOCATION)){
                                Snackbar.make(findViewById(R.id.activity_contact_map),
                                        "MyContactList requires permissions to locate your contact", Snackbar.LENGTH_INDEFINITE).setAction("OK", new View.OnClickListener() {
                                    @Override
                                    public void onClick(View v) {
                                        ActivityCompat.requestPermissions(
                                                ContactMapActivity.this, new String[]{
                                                        android.Manifest.permission.ACCESS_FINE_LOCATION},
                                                PERMISSION_REQUEST_LOCATION);
                                                }

                                    })
                                .show();
                            }else{
                                ActivityCompat.requestPermissions(ContactMapActivity.this,
                                        new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                                        PERMISSION_REQUEST_LOCATION);
                            }
                        }else{
                            startLocationUpdates();
                        }
                    }else {
                        startLocationUpdates();
                    }
                }

            catch(Exception e){
                Toast.makeText(getBaseContext(), "Error requesting permission", Toast.LENGTH_LONG).show();
            }

            }

        });


    }

    private boolean isBetterLocation (Location location){
        boolean isBetter = false;
        if(currentBestLocation == null){
            isBetter = true;
        } else if(location.getAccuracy() <= currentBestLocation.getAccuracy()){
            isBetter = true;
        }else if(location.getTime() - currentBestLocation.getAccuracy() > 5*60*1000){
            isBetter =true;
        }
        return isBetter;
    }


    private SensorEventListener mySensorEventListener = new SensorEventListener() {

        public void onAccuracyChanged(Sensor sensor, int accuracy) {  }

        float[] accelerometerValues;
        float[] magneticValues;

        public void onSensorChanged(SensorEvent event) {
            if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER)
                accelerometerValues = event.values;
            if (event.sensor.getType() == Sensor.TYPE_MAGNETIC_FIELD)
                magneticValues = event.values;
            if (accelerometerValues!= null && magneticValues!= null) {
                float R[] = new float[9];
                float I[] = new float[9];
                boolean success = SensorManager.getRotationMatrix(R, I, accelerometerValues, magneticValues);
                if (success) {
                    float orientation[] = new float[3];
                    SensorManager.getOrientation(R, orientation);

                    float azimut = (float) Math.toDegrees(orientation[0]);
                    if (azimut < 0.0f) { azimut+=360.0f;}
                    String direction;
                    if (azimut >= 315 || azimut < 45) { direction = "N"; }
                    else if (azimut >= 225 && azimut < 315) { direction = "W"; }
                    else if (azimut >= 135 && azimut < 225) { direction = "S"; }
                    else { direction = "E"; }
                    textDirection.setText(direction);
                }
            }
        }
    };


    @Override
    public void onMapReady(GoogleMap googleMap) {

    }
}

