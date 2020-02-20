package com.example.locate;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.Looper;
import android.provider.Settings;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toast;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.navigation.NavigationView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.navigation.NavArgument;
import androidx.navigation.NavController;
import androidx.navigation.NavDestination;
import androidx.navigation.Navigation;
import androidx.navigation.ui.AppBarConfiguration;
import androidx.navigation.ui.NavigationUI;

public class MainActivity extends AppCompatActivity {

    int PERMISSION_ID = 44;
    FusedLocationProviderClient mFusedLocationClient;
    private AppBarConfiguration mAppBarConfiguration, mBottomAppBarConfiguration;
    private double lat, lng;
    NavArgument navArgumentLat, navArgumentLng;
    private NavigationView navigationView;
    private NavController navController;
    private Bundle positionBundle;
    Activity home;
    private BottomNavigationView bottomNavigationView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);


        mFusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
        /*
        FloatingActionButton fab = findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
                        .setAction("Action", null).show();
            }
        });
        */
        home = this;
        positionBundle = new Bundle();
        DrawerLayout drawer = findViewById(R.id.drawer_layout);
        navigationView = findViewById(R.id.nav_view);
        bottomNavigationView = findViewById(R.id.bottom_nav_view);

        // Passing each menu ID as a set of Ids because each
        // menu should be considered as top level destinations.
        mAppBarConfiguration = new AppBarConfiguration.Builder(
                R.id.nav_home, R.id.nav_send_position, R.id.nav_emergency,
                R.id.nav_map, R.id.nav_account)
                .setDrawerLayout(drawer)
                .build();

        getLastLocation();
        /*
        mBottomAppBarConfiguration = new AppBarConfiguration.Builder(
                R.id.bottom_nav_home, R.id.bottom_nav_gallery, R.id.bottom_nav_slideshow,
                R.id.bottom_nav_tools, R.id.bottom_nav_account)
                .setDrawerLayout(drawer)
                .build();
        NavController bottomNavController = Navigation.findNavController(this, R.id.bottom_nav_host_fragment);
        NavigationUI.setupActionBarWithNavController(this, bottomNavController, mBottomAppBarConfiguration);
        NavigationUI.setupWithNavController(bottomNavigationView,bottomNavController);
        */
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        return super.onOptionsItemSelected(item);
    }

    @Override
    public boolean onSupportNavigateUp() {
        NavController navController = Navigation.findNavController(this, R.id.nav_host_fragment);
        return NavigationUI.navigateUp(navController, mAppBarConfiguration)
                || super.onSupportNavigateUp();
    }

    private void getLastLocation() {
        if (checkPermissions()) {
            if (isLocationEnabled()) {
                mFusedLocationClient.getLastLocation().addOnCompleteListener(
                        new OnCompleteListener<Location>() {
                            @Override
                            public void onComplete(@NonNull Task<Location> task) {
                                final Location location = task.getResult();
                                if (location == null) {
                                    requestNewLocationData();
                                } else {
                                    lat = location.getLatitude();
                                    lng = location.getLongitude();
                                    System.out.println("Latitude: " + lat);

                                    positionBundle.putDouble("position_latitude", lat);
                                    positionBundle.putDouble("position_longitude", lng);

                                    navController = Navigation.findNavController(home, R.id.nav_host_fragment);
                                    NavigationUI.setupActionBarWithNavController((AppCompatActivity) home, navController, mAppBarConfiguration);
                                    navController.setGraph(R.navigation.mobile_navigation, positionBundle);
                                    NavigationUI.setupWithNavController(navigationView, navController);
                                    NavigationUI.setupWithNavController(bottomNavigationView, navController);
                                    navController.addOnDestinationChangedListener(new NavController.OnDestinationChangedListener() {
                                        @Override
                                        public void onDestinationChanged(@NonNull NavController controller, @NonNull NavDestination destination, @Nullable Bundle arguments) {
                                            System.out.println("destination: " + destination.getLabel());
                                            switch (destination.getId()) {
                                                case R.id.nav_send_position:
                                                    navArgumentLat = new NavArgument.Builder().setDefaultValue(lat).build();
                                                    navArgumentLng = new NavArgument.Builder().setDefaultValue(lng).build();
                                                    destination.addArgument("position_latitude", navArgumentLat);
                                                    destination.addArgument("position_longitude", navArgumentLng);

                                                case R.id.nav_map:
                                                    navArgumentLat = new NavArgument.Builder().setDefaultValue(lat).build();
                                                    navArgumentLng = new NavArgument.Builder().setDefaultValue(lng).build();
                                                    destination.addArgument("position_latitude", navArgumentLat);
                                                    destination.addArgument("position_longitude", navArgumentLng);
                                                case R.id.nav_home:
                                                    navArgumentLat = new NavArgument.Builder().setDefaultValue(lat).build();
                                                    navArgumentLng = new NavArgument.Builder().setDefaultValue(lng).build();
                                                    destination.addArgument("position_latitude", navArgumentLat);
                                                    destination.addArgument("position_longitude", navArgumentLng);

                                            }
                                        }
                                    });

                                }
                            }
                        }
                );

            } else {
                Toast.makeText(this, "Turn on location", Toast.LENGTH_LONG).show();
                Intent intent = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
                startActivity(intent);
            }
        } else {

            requestPermissions();
        }

    }


    @SuppressLint("MissingPermission")
    private void requestNewLocationData() {

        LocationRequest mLocationRequest = new LocationRequest();
        mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
        mLocationRequest.setInterval(0);
        mLocationRequest.setFastestInterval(0);
        mLocationRequest.setNumUpdates(1);

        mFusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
        mFusedLocationClient.requestLocationUpdates(
                mLocationRequest, mLocationCallback,
                Looper.myLooper()
        );

    }

    private LocationCallback mLocationCallback = new LocationCallback() {
        @Override
        public void onLocationResult(LocationResult locationResult) {
            Location mLastLocation = locationResult.getLastLocation();
            lat = mLastLocation.getLatitude();
            lng = mLastLocation.getLongitude();
        }
    };

    private boolean checkPermissions() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED &&
                ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            return true;
        }
        return false;
    }

    private void requestPermissions() {
        ActivityCompat.requestPermissions(
                this,
                new String[]{Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.ACCESS_FINE_LOCATION},
                PERMISSION_ID
        );
    }

    private boolean isLocationEnabled() {
        LocationManager locationManager = (LocationManager) this.getSystemService(Context.LOCATION_SERVICE);
        return locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) || locationManager.isProviderEnabled(
                LocationManager.NETWORK_PROVIDER
        );
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PERMISSION_ID) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                getLastLocation();
            }
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        if (checkPermissions()) {
            getLastLocation();
        }

    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

    }
}
