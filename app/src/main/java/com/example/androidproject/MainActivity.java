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

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
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
import com.google.zxing.integration.android.IntentIntegrator;
import com.google.zxing.integration.android.IntentResult;

import java.io.IOException;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MainActivity extends AppCompatActivity implements OnMapReadyCallback {

    private static final int LOCATION_PERMISSION_REQUEST_CODE = 1;
    private FirebaseAuth mAuth;
    private MapView mapView;
    private GoogleMap googleMap;
    private FusedLocationProviderClient fusedLocationClient;

    // UI Components
    private SearchView searchView;
    private Button logoutButton;
    private FloatingActionButton layersFab, myLocationFab, directionsFab, googleEarthFab, placesFab, qrScanFab, geminiFab;
    private ImageButton zoomInButton, zoomOutButton;

    private final ActivityResultLauncher<Intent> placesLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                    String cityName = result.getData().getStringExtra("CITY_NAME");
                    if (cityName != null && !cityName.isEmpty()) {
                        searchView.setQuery(cityName, true);
                    }
                }
            });

    private final ActivityResultLauncher<Intent> qrCodeScanner = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                IntentResult scanResult = IntentIntegrator.parseActivityResult(result.getResultCode(), result.getData());
                if (scanResult != null) {
                    if (scanResult.getContents() == null) {
                        Toast.makeText(this, "Cancelled", Toast.LENGTH_LONG).show();
                    } else {
                        handleQrCodeResult(scanResult.getContents());
                    }
                }
            });

    private void handleQrCodeResult(String contents) {
        // Try to parse as a Google Maps URL
        LatLng latLng = parseGoogleMapsUrl(contents);

        if (latLng != null) {
            handleScannedLocation(latLng);
            return;
        }

        // Try to parse as "latitude,longitude"
        String[] location = contents.split(",");
        if (location.length == 2) {
            try {
                double latitude = Double.parseDouble(location[0].trim());
                double longitude = Double.parseDouble(location[1].trim());
                latLng = new LatLng(latitude, longitude);
                handleScannedLocation(latLng);
                return;
            } catch (NumberFormatException e) {
                // Not a valid coordinate pair, proceed to treat as a location name
            }
        }

        // Treat as a location name and search
        performSearch(contents, true);
    }

    private LatLng parseGoogleMapsUrl(String url) {
        // Regex for standard Google Maps URLs and geo URIs
        Pattern pattern = Pattern.compile("(?:@|q=)(-?[0-9.]+),(-?[0-9.]+)");
        Matcher matcher = pattern.matcher(url);
        if (matcher.find()) {
            try {
                double latitude = Double.parseDouble(matcher.group(1));
                double longitude = Double.parseDouble(matcher.group(2));
                return new LatLng(latitude, longitude);
            } catch (NumberFormatException e) {
                return null;
            }
        }
        return null;
    }


    private void handleScannedLocation(LatLng latLng) {
        addMarkerAtLocation(latLng, "Scanned Location");
        googleMap.animateCamera(CameraUpdateFactory.newLatLngZoom(latLng, 15));
        showNavigationDialog(latLng);
    }

    private void showNavigationDialog(LatLng latLng) {
        new AlertDialog.Builder(this)
                .setTitle("Navigate to Location")
                .setMessage("Do you want to start navigation to the scanned location?")
                .setPositiveButton("Navigate", (dialog, which) -> {
                    Uri gmmIntentUri = Uri.parse("google.navigation:q=" + latLng.latitude + "," + latLng.longitude);
                    Intent mapIntent = new Intent(Intent.ACTION_VIEW, gmmIntentUri);
                    mapIntent.setPackage("com.google.android.apps.maps");
                    if (mapIntent.resolveActivity(getPackageManager()) != null) {
                        startActivity(mapIntent);
                    } else {
                        Toast.makeText(MainActivity.this, "Google Maps is not installed.", Toast.LENGTH_SHORT).show();
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
    }


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
        qrScanFab = findViewById(R.id.qrScanFab);
        geminiFab = findViewById(R.id.geminiFab);
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
                performSearch(query, false);
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
        placesFab.setOnClickListener(v -> placesLauncher.launch(new Intent(MainActivity.this, PlacesActivity.class)));

        qrScanFab.setOnClickListener(v -> {
            IntentIntegrator integrator = new IntentIntegrator(this);
            integrator.setDesiredBarcodeFormats(IntentIntegrator.QR_CODE);
            integrator.setPrompt("Scan a QR code for location");
            integrator.setCameraId(0);
            integrator.setBeepEnabled(true);
            qrCodeScanner.launch(integrator.createScanIntent());
        });

        geminiFab.setOnClickListener(v -> {
            startActivity(new Intent(MainActivity.this, GeminiActivity.class));
        });

        if (googleMap != null) {
            googleMap.setOnMapClickListener(latLng -> addMarkerAtLocation(latLng, "Tapped Location"));
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
        googleMap.setOnMapClickListener(latLng -> addMarkerAtLocation(latLng, "Tapped Location"));
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
                            addMarkerAtLocation(currentLatLng, "My Location");
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

    private void performSearch(String locationName, boolean fromQr) {
        if (googleMap == null) {
            mapView.getMapAsync(googleMap -> {
                this.googleMap = googleMap;
                performSearch(locationName, fromQr);
            });
            return;
        }

        Geocoder geocoder;
        String searchString = locationName;

        if (fromQr) {
            geocoder = new Geocoder(this, Locale.getDefault());
        } else {
            geocoder = new Geocoder(this, new Locale("en", "PK"));
            if (!locationName.toLowerCase().contains("pakistan")) {
                searchString = locationName + ", Pakistan";
            }
        }

        try {
            List<Address> addressList = geocoder.getFromLocationName(searchString, 1);
            if (addressList != null && !addressList.isEmpty()) {
                Address address = addressList.get(0);
                LatLng latLng = new LatLng(address.getLatitude(), address.getLongitude());
                googleMap.clear(); // Clear existing markers
                if (fromQr) {
                    handleScannedLocation(latLng);
                } else {
                    addMarkerAtLocation(latLng, address.getAddressLine(0));
                    googleMap.animateCamera(CameraUpdateFactory.newLatLngZoom(latLng, 12));
                }
            } else {
                Toast.makeText(this, "Location not found: " + locationName, Toast.LENGTH_SHORT).show();
            }
        } catch (IOException e) {
            e.printStackTrace();
            Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private void addMarkerAtLocation(LatLng latLng, String title) {
        Geocoder geocoder = new Geocoder(this, Locale.getDefault());
        try {
            List<Address> addresses = geocoder.getFromLocation(latLng.latitude, latLng.longitude, 1);
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
