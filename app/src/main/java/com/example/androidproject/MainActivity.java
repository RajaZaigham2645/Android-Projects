package com.example.androidproject;

import android.Manifest;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.net.Uri;
import android.os.Bundle;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SearchView;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapView;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.auth.FirebaseAuth;

import java.io.IOException;
import java.util.List;
import java.util.Locale;

public class MainActivity extends AppCompatActivity implements OnMapReadyCallback {

    private static final int LOCATION_PERMISSION_REQUEST_CODE = 1;
    private FirebaseAuth mAuth;
    private MapView mapView;
    private GoogleMap googleMap;
    private FusedLocationProviderClient fusedLocationClient;

    // UI Components
    private SearchView searchView;
    private Button logoutButton;
    private FloatingActionButton layersFab, myLocationFab, directionsFab, googleEarthFab, placesFab;
    private ImageButton zoomInButton, zoomOutButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mAuth = FirebaseAuth.getInstance();
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        logoutButton = findViewById(R.id.logoutButton);
        mapView = findViewById(R.id.mapView);
        searchView = findViewById(R.id.searchView);
        layersFab = findViewById(R.id.layersFab);
        myLocationFab = findViewById(R.id.myLocationFab);
        directionsFab = findViewById(R.id.directionsFab);
        googleEarthFab = findViewById(R.id.googleEarthFab);
        placesFab = findViewById(R.id.placesFab);
        zoomInButton = findViewById(R.id.zoomInButton);
        zoomOutButton = findViewById(R.id.zoomOutButton);

        mapView.onCreate(savedInstanceState);
        mapView.getMapAsync(this);

        setupClickListeners();
        handleIntent(getIntent()); // Handle intent on initial creation
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent); // Update the activity's intent
        handleIntent(intent); // Handle the new intent
    }

    private void handleIntent(Intent intent) {
        if (intent != null && intent.hasExtra("CITY_NAME")) {
            String cityName = intent.getStringExtra("CITY_NAME");
            if (cityName != null && !cityName.isEmpty()) {
                // Set the search view text and perform the search
                searchView.setQuery(cityName, true);
            }
        }
    }

    private void setupClickListeners() {
        logoutButton.setOnClickListener(v -> {
            mAuth.signOut();
            startActivity(new Intent(MainActivity.this, LoginActivity.class));
            finish();
        });

        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                performSearch(query);
                return true;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                return false;
            }
        });

        layersFab.setOnClickListener(v -> showMapTypeDialog());

        myLocationFab.setOnClickListener(v -> centerMapOnMyLocation());
        directionsFab.setOnClickListener(v -> startGoogleMapsDirections());
        googleEarthFab.setOnClickListener(v -> startGoogleEarth());
        placesFab.setOnClickListener(v -> startActivity(new Intent(MainActivity.this, PlacesActivity.class)));

        if (googleMap != null) {
            googleMap.setOnMapClickListener(this::addMarkerAtLocation);
        }

        zoomInButton.setOnClickListener(v -> {
            if (googleMap != null) {
                googleMap.animateCamera(CameraUpdateFactory.zoomIn());
            }
        });

        zoomOutButton.setOnClickListener(v -> {
            if (googleMap != null) {
                googleMap.animateCamera(CameraUpdateFactory.zoomOut());
            }
        });
    }

    @Override
    public void onMapReady(@NonNull GoogleMap map) {
        googleMap = map;
        enableMyLocation();
        googleMap.setOnMapClickListener(this::addMarkerAtLocation);
        // If the activity was started with a city name, the search will be triggered
        // from handleIntent -> setQuery -> onQueryTextSubmit -> performSearch
    }

    private void enableMyLocation() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED) {
            if (googleMap != null) {
                googleMap.setMyLocationEnabled(true);
            }
        } else {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                    LOCATION_PERMISSION_REQUEST_CODE);
        }
    }

    private void centerMapOnMyLocation() {
        if (googleMap != null && googleMap.isMyLocationEnabled()) {
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                return;
            }
            fusedLocationClient.getLastLocation()
                    .addOnSuccessListener(this, location -> {
                        if (location != null) {
                            LatLng currentLatLng = new LatLng(location.getLatitude(), location.getLongitude());
                            googleMap.animateCamera(CameraUpdateFactory.newLatLngZoom(currentLatLng, 15));
                            addMarkerAtLocation(currentLatLng);
                        } else {
                            Toast.makeText(MainActivity.this, "Waiting for location...", Toast.LENGTH_SHORT).show();
                        }
                    });
        } else {
            enableMyLocation();
        }
    }

    private void startGoogleMapsDirections() {
        if (googleMap != null && googleMap.isMyLocationEnabled()) {
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                return;
            }
            fusedLocationClient.getLastLocation()
                    .addOnSuccessListener(this, location -> {
                        if (location != null) {
                            Uri gmmIntentUri = Uri.parse("google.navigation:q=" + location.getLatitude() + "," + location.getLongitude());
                            Intent mapIntent = new Intent(Intent.ACTION_VIEW, gmmIntentUri);
                            mapIntent.setPackage("com.google.android.apps.maps");
                            if (mapIntent.resolveActivity(getPackageManager()) != null) {
                                startActivity(mapIntent);
                            } else {
                                Toast.makeText(MainActivity.this, "Google Maps is not installed", Toast.LENGTH_SHORT).show();
                            }
                        } else {
                            Toast.makeText(MainActivity.this, "Waiting for location...", Toast.LENGTH_SHORT).show();
                        }
                    });
        } else {
            enableMyLocation();
        }
    }

    private void startGoogleEarth() {
        Intent earthIntent = getPackageManager().getLaunchIntentForPackage("com.google.earth");
        if (earthIntent != null) {
            startActivity(earthIntent);
        } else {
            Toast.makeText(this, "Google Earth is not installed", Toast.LENGTH_SHORT).show();
        }
    }

    private void performSearch(String locationName) {
        if (googleMap == null) {
            mapView.getMapAsync(googleMap -> {
                this.googleMap = googleMap;
                performSearch(locationName);
            });
            return;
        }


        Geocoder geocoder = new Geocoder(this, Locale.getDefault());
        try {
            List<Address> addressList = geocoder.getFromLocationName(locationName, 1);
            if (addressList != null && !addressList.isEmpty()) {
                Address address = addressList.get(0);
                LatLng latLng = new LatLng(address.getLatitude(), address.getLongitude());
                googleMap.clear(); // Clear existing markers
                addMarkerAtLocation(latLng);
                googleMap.animateCamera(CameraUpdateFactory.newLatLngZoom(latLng, 12));
            } else {
                Toast.makeText(this, "Location not found: " + locationName, Toast.LENGTH_SHORT).show();
            }
        } catch (IOException e) {
            e.printStackTrace();
            Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private void addMarkerAtLocation(LatLng latLng) {
        Geocoder geocoder = new Geocoder(this, Locale.getDefault());
        try {
            List<Address> addresses = geocoder.getFromLocation(latLng.latitude, latLng.longitude, 1);
            String title = "Tapped Location";
            if (addresses != null && !addresses.isEmpty()) {
                title = addresses.get(0).getAddressLine(0);
            }
            googleMap.addMarker(new MarkerOptions().position(latLng).title(title));
        } catch (IOException e) {
            e.printStackTrace();
            Toast.makeText(this, "Error getting address", Toast.LENGTH_SHORT).show();
        }
    }

    private void showMapTypeDialog() {
        if (googleMap == null) return;

        final String[] mapTypes = {"Normal", "Satellite", "Terrain"};
        int currentMapTypeIndex = googleMap.getMapType() - 1;
        if (currentMapTypeIndex > 2 || currentMapTypeIndex < 0) currentMapTypeIndex = 0; // Default to normal
        if (googleMap.getMapType() == GoogleMap.MAP_TYPE_HYBRID) currentMapTypeIndex = 1;


        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Select Map Type");
        builder.setSingleChoiceItems(mapTypes, currentMapTypeIndex, (dialog, which) -> {
            int mapType = GoogleMap.MAP_TYPE_NORMAL;
            switch(which) {
                case 0:
                    mapType = GoogleMap.MAP_TYPE_NORMAL;
                    break;
                case 1:
                    mapType = GoogleMap.MAP_TYPE_HYBRID;
                    break;
                case 2:
                    mapType = GoogleMap.MAP_TYPE_TERRAIN;
                    break;
            }
            googleMap.setMapType(mapType);
            dialog.dismiss();
        });
        builder.create().show();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                enableMyLocation();
            } else {
                Toast.makeText(this, "Location permission denied", Toast.LENGTH_SHORT).show();
            }
        }
    }

    // MapView Lifecycle Methods
    @Override
    protected void onResume() {
        super.onResume();
        mapView.onResume();
    }

    @Override
    protected void onPause() {
        super.onPause();
        mapView.onPause();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mapView.onDestroy();
    }

    @Override
    public void onLowMemory() {
        super.onLowMemory();
        mapView.onLowMemory();
    }
}
