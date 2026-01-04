package com.example.studybuddy.focus;

import android.Manifest;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Bundle;
import android.util.Log;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import com.example.studybuddy.BaseBottomNavActivity;
import com.example.studybuddy.R;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.material.appbar.MaterialToolbar;

import org.json.JSONArray;
import org.json.JSONObject;

public class StudySpotActivity extends BaseBottomNavActivity implements OnMapReadyCallback {

    private static final String TAG = "StudySpotActivity";

    private GoogleMap map;
    private FusedLocationProviderClient fusedLocation;
    private RequestQueue queue;

    // Adjust this to show more/less places
    private static final int RADIUS_METERS = 2000;

    private final ActivityResultLauncher<String> locationPermissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), granted -> {
                if (granted) {
                    loadMapData();
                } else {
                    // Permission denied: show fallback area
                    LatLng fallback = new LatLng(53.3498, -6.2603); // Dublin
                    if (map != null) map.moveCamera(CameraUpdateFactory.newLatLngZoom(fallback, 12.5f));
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_study_spot);
        setupBottomNav(R.id.nav_focus);

        // Back button (returns to Focus page)
        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        if (toolbar != null) {
            toolbar.setNavigationOnClickListener(v -> finish());
        }

        queue = Volley.newRequestQueue(this);
        fusedLocation = LocationServices.getFusedLocationProviderClient(this);

        SupportMapFragment mapFragment =
                (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.mapFragment);
        if (mapFragment != null) mapFragment.getMapAsync(this);
    }

    @Override
    public void onMapReady(@NonNull GoogleMap googleMap) {
        map = googleMap;
        requestLocationThenLoad();
    }

    private void requestLocationThenLoad() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED) {
            loadMapData();
        } else {
            locationPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION);
        }
    }

    private void loadMapData() {
        if (map == null) return;

        // show blue dot if allowed
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED) {
            try {
                map.setMyLocationEnabled(true);
            } catch (SecurityException ignored) {}
        }

        fusedLocation.getLastLocation()
                .addOnSuccessListener(this::handleLocation)
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Location failed: " + e.getMessage());
                    LatLng fallback = new LatLng(53.3498, -6.2603); // Dublin
                    map.moveCamera(CameraUpdateFactory.newLatLngZoom(fallback, 12.5f));
                });
    }

    private void handleLocation(Location location) {
        LatLng center;

        if (location == null) {
            // fallback if device hasn't got a location yet
            center = new LatLng(53.3498, -6.2603);
            map.moveCamera(CameraUpdateFactory.newLatLngZoom(center, 12.5f));
        } else {
            center = new LatLng(location.getLatitude(), location.getLongitude());
            map.moveCamera(CameraUpdateFactory.newLatLngZoom(center, 14.5f));
        }

        // Clear old markers, then load new ones
        map.clear();

        // Fetch nearby cafes + libraries
        searchNearbyType("cafe", center, RADIUS_METERS);
        searchNearbyType("library", center, RADIUS_METERS);
    }

    /**
     * Uses Google Places Nearby Search Web API to get many places in a radius.
     * Note: this returns up to ~20 results per request (per type).
     */
    private void searchNearbyType(String type, LatLng center, int radiusMeters) {
        String apiKey = getString(R.string.google_maps_key);

        String url = "https://maps.googleapis.com/maps/api/place/nearbysearch/json"
                + "?location=" + center.latitude + "," + center.longitude
                + "&radius=" + radiusMeters
                + "&type=" + type
                + "&key=" + apiKey;

        JsonObjectRequest req = new JsonObjectRequest(
                Request.Method.GET,
                url,
                null,
                response -> addMarkersFromNearbyResponse(response, type),
                error -> Log.e(TAG, "Nearby search failed (" + type + "): " + error)
        );

        queue.add(req);
    }

    private void addMarkersFromNearbyResponse(JSONObject response, String type) {
        try {
            String status = response.optString("status", "");
            if (!"OK".equals(status) && !"ZERO_RESULTS".equals(status)) {
                Log.e(TAG, "Places status (" + type + "): " + status + " | " + response.toString());
                return;
            }

            JSONArray results = response.optJSONArray("results");
            if (results == null) return;

            for (int i = 0; i < results.length(); i++) {
                JSONObject place = results.getJSONObject(i);

                String name = place.optString("name", type);
                JSONObject geometry = place.optJSONObject("geometry");
                if (geometry == null) continue;

                JSONObject loc = geometry.optJSONObject("location");
                if (loc == null) continue;

                double lat = loc.optDouble("lat", 0);
                double lng = loc.optDouble("lng", 0);
                if (lat == 0 && lng == 0) continue;

                map.addMarker(new MarkerOptions()
                        .position(new LatLng(lat, lng))
                        .title(name));
            }
        } catch (Exception e) {
            Log.e(TAG, "Parse markers error: " + e.getMessage());
        }
    }
}