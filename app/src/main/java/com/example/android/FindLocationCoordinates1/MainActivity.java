package com.example.android.FindLocationCoordinates1;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.app.Dialog;
import android.content.IntentSender;
import android.content.pm.PackageManager;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.PopupWindow;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.common.api.ResolvableApiException;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.LocationSettingsRequest;
import com.google.android.gms.location.LocationSettingsResponse;
import com.google.android.gms.location.SettingsClient;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.android.libraries.places.api.Places;
import com.google.android.libraries.places.api.model.AutocompletePrediction;
import com.google.android.libraries.places.api.model.AutocompleteSessionToken;
import com.google.android.libraries.places.api.model.Place;
import com.google.android.libraries.places.api.model.TypeFilter;
import com.google.android.libraries.places.api.net.FetchPlaceRequest;
import com.google.android.libraries.places.api.net.FetchPlaceResponse;
import com.google.android.libraries.places.api.net.FindAutocompletePredictionsRequest;
import com.google.android.libraries.places.api.net.FindAutocompletePredictionsResponse;
import com.google.android.libraries.places.api.net.PlacesClient;
import com.mancj.materialsearchbar.MaterialSearchBar;
import com.mancj.materialsearchbar.adapter.SuggestionsAdapter;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class MainActivity extends AppCompatActivity implements OnMapReadyCallback {

    //INSERT API KEY
    private String API_KEY = "";
    private static final String TAG = "MainActivity";
    private static final int ERROR_RESULT = 9001;

    private Boolean mLocationPermission = false;
    private final int LOCATION_PERMISSION_REQUEST_CODE = 1234;

    //widgets
    private MaterialSearchBar searchText;
    private ImageView mGps;
    private PlacesClient placesClient;

    private GoogleMap mGoogleMap;

    private Button mButton;
    private String placeName;

    private List<AutocompletePrediction> mAutocompletePredictionList;
    private TextView popUpText;
    private String message;
    private PopupWindow popupWindow;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        if (isGPServiceWorking()) {
            Toast.makeText(getApplicationContext(), "google play services is active", Toast.LENGTH_SHORT).show();
        }

        getLocationPermission();

        mButton = findViewById(R.id.btn_find);
        mGps = findViewById(R.id.gps);
        searchText = findViewById(R.id.searchText);
        popUpText = findViewById(R.id.popUpText);

        Places.initialize(MainActivity.this, API_KEY);
        placesClient = Places.createClient(this);
        final AutocompleteSessionToken token = AutocompleteSessionToken.newInstance();

        mButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                geoLocate();

                // inflate the layout of the popup window
                LayoutInflater inflater = (LayoutInflater) getSystemService(LAYOUT_INFLATER_SERVICE);
                View popupView = inflater.inflate(R.layout.popup_window, null);

                // create the popup window
                int width = LinearLayout.LayoutParams.WRAP_CONTENT;
                int height = LinearLayout.LayoutParams.WRAP_CONTENT;
                boolean focusable = true; // lets taps outside the popup also dismiss it

                popupWindow = new PopupWindow(popupView, width, height, focusable);

                ((TextView) popupWindow.getContentView().findViewById(R.id.popUpText)).setText(message);

                // show the popup window
                // which view you pass in doesn't matter, it is only used for the window token
                if (((TextView) popupWindow.getContentView().findViewById(R.id.popUpText)).getText() == "") {
                    Toast.makeText(getApplication(), "Enter location", Toast.LENGTH_SHORT).show();
                } else {
                    popupWindow.showAtLocation(view, Gravity.CENTER, 0, 0);
                }
                // dismiss the popup window when touched
                popupView.setOnTouchListener(new View.OnTouchListener() {
                    @Override
                    public boolean onTouch(View v, MotionEvent event) {
                        popupWindow.dismiss();
                        return true;
                    }
                });
            }
        });

        searchText.setOnSearchActionListener(new MaterialSearchBar.OnSearchActionListener() {
            @Override
            public void onSearchStateChanged(boolean enabled) {

            }

            @Override
            public void onSearchConfirmed(CharSequence text) {
                startSearch(text.toString(), true, null, true);
            }

            @Override
            public void onButtonClicked(int buttonCode) {
                if (buttonCode == MaterialSearchBar.BUTTON_NAVIGATION) {
                    Toast.makeText(getApplicationContext(), "doesn't work", Toast.LENGTH_SHORT).show();
                } else if (buttonCode == MaterialSearchBar.BUTTON_BACK) {
                    searchText.disableSearch();
                }
            }
        });

        searchText.addTextChangeListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
                FindAutocompletePredictionsRequest predictionsRequests = FindAutocompletePredictionsRequest.builder().setTypeFilter(TypeFilter.ESTABLISHMENT).setSessionToken(token).setQuery(charSequence.toString()).setCountry("in").build();

                placesClient.findAutocompletePredictions(predictionsRequests).addOnCompleteListener(new OnCompleteListener<FindAutocompletePredictionsResponse>() {
                    @Override
                    public void onComplete(@NonNull Task<FindAutocompletePredictionsResponse> task) {
                        if (task.isSuccessful()) {
                            FindAutocompletePredictionsResponse predictionsResponse = task.getResult();
                            if (predictionsResponse != null) {
                                mAutocompletePredictionList = predictionsResponse.getAutocompletePredictions();

                                List<String> suggestionList = new ArrayList<>();
                                for (int i = 0; i < mAutocompletePredictionList.size(); i++) {
                                    AutocompletePrediction predictionList = mAutocompletePredictionList.get(i);
                                    suggestionList.add(predictionList.getFullText(null).toString());
                                }
                                searchText.updateLastSuggestions(suggestionList);
                                if (!searchText.isSuggestionsVisible()) {
                                    searchText.showSuggestionsList();
                                }
                            }
                        }
                    }
                });

            }

            @Override
            public void afterTextChanged(Editable editable) {

            }
        });

//after we click or choose one suggestion
        searchText.setSuggestionsClickListener(new SuggestionsAdapter.OnItemViewClickListener() {
            @Override
            public void OnItemClickListener(int position, View v) {
                if (position >= mAutocompletePredictionList.size()) {
                    return;
                }
                AutocompletePrediction selectionList = mAutocompletePredictionList.get(position);
                String suggestion = searchText.getLastSuggestions().get(position).toString();
                searchText.setText(suggestion);
                placeName = suggestion;

                new Handler().postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        searchText.clearSuggestions();
                    }
                }, 1000);


                InputMethodManager imm = (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
                if (imm != null) {
                    imm.hideSoftInputFromWindow(searchText.getWindowToken(), InputMethodManager.HIDE_IMPLICIT_ONLY);
                }
                final String placeid = selectionList.getPlaceId();
                List<Place.Field> placeFields = Arrays.asList(Place.Field.LAT_LNG);

                FetchPlaceRequest fetchPlaceRequest = FetchPlaceRequest.builder(placeid, placeFields).build();
                placesClient.fetchPlace(fetchPlaceRequest).addOnSuccessListener(new OnSuccessListener<FetchPlaceResponse>() {
                    @Override
                    public void onSuccess(FetchPlaceResponse fetchPlaceResponse) {
                        Place place = fetchPlaceResponse.getPlace();

                        Log.d(TAG, "found place After choosing one " + place);
                        //now we have to fetch the lat long of the place
                        LatLng latLngOfPlace = place.getLatLng();
                        Log.d(TAG, " " + latLngOfPlace);
                        if (latLngOfPlace != null) {
                            mGoogleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(latLngOfPlace, 15f));
                        }
                        moveCamera(new LatLng(latLngOfPlace.latitude, latLngOfPlace.longitude), 15f, null);

                    }
                }).addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        if (e instanceof ApiException) {
                            ApiException apiException = (ApiException) e;
                            apiException.printStackTrace();

                            int statusCode = apiException.getStatusCode();
                            Log.d(TAG, "places not found After choosing one " + e.getMessage());
                        }
                    }
                });
            }

            @Override
            public void OnItemDeleteListener(int position, View v) {

            }
        });
    }

    private void geoLocate() {
        String searchString = searchText.getText();
        Geocoder geocoder = new Geocoder(MainActivity.this);

        List<Address> addressList = new ArrayList<>();
        try {
            addressList = geocoder.getFromLocationName(searchString, 1);

        } catch (IOException e) {

        }
        if (addressList.size() > 0) {
            Address address = addressList.get(0);
            //Toast.makeText(getApplicationContext(), address.toString(), Toast.LENGTH_SHORT).show();
            Log.d(TAG, "Location: " + address.toString());
            //Location: Address[addressLines=[0:"Toli Chowki, Hyderabad, Telangana 500008, India"],
            // feature=Toli Chowki,admin=Telangana,sub-admin=Ranga Reddy,locality=Hyderabad,thoroughfare=null,
            // postalCode=500008,countryCode=IN,countryName=India,hasLatitude=true,latitude=17.3990023,
            // hasLongitude=true,longitude=78.4156933,phone=null,url=null,extras=null]
            moveCamera(new LatLng(address.getLatitude(), address.getLongitude()), 15f, address.getAddressLine(0));
            message = ("Address: " + placeName + "\n\nLat: " + address.getLatitude() + "\n Long: " + address.getLongitude());
        }
    }

    //check location permission
    //fine location gps, cell, wifi.... coarse cell, wifi
    //send permission requests
    private void getLocationPermission() {
        String[] permission = {Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION};

        if (ContextCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            if (ContextCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                //if permission is there
                mLocationPermission = true;
                initializeMap();
            } else {
                //ask permission
                ActivityCompat.requestPermissions(this, permission, LOCATION_PERMISSION_REQUEST_CODE);
            }
        } else {
            //ask permission
            ActivityCompat.requestPermissions(this, permission, LOCATION_PERMISSION_REQUEST_CODE);
        }
    }

    //get permission request results
    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        mLocationPermission = false;

        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0) {
                for (int i = 0; i < grantResults.length; i++) {
                    if (grantResults[0] != PackageManager.PERMISSION_GRANTED) {
                        mLocationPermission = false;
                        return;
                    }
                }
                mLocationPermission = true;
                initializeMap();

            }
        }
    }

    //prepares/initializes the map
    private void initializeMap() {
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map);
        mapFragment.getMapAsync(MainActivity.this);
    }


    @RequiresApi(api = Build.VERSION_CODES.M)
    @Override
    public void onMapReady(GoogleMap googleMap) {
        Toast.makeText(getApplicationContext(), "map is ready", Toast.LENGTH_SHORT).show();
        mGoogleMap = googleMap;

        checkIfGPSEnabled();

        if (checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {

            return;
        }
        //exact location will be seen as blue dot and the gps icon
        mGoogleMap.setMyLocationEnabled(true);
        //the search bar will block the gps icon so we have to disable it
        mGoogleMap.getUiSettings().setMyLocationButtonEnabled(false);

        init();
    }

    public boolean checkIfGPSEnabled() {
        //check if gps is enabled or not and then request user to enable it
        LocationRequest locationRequest = LocationRequest.create();
        locationRequest.setInterval(10000);
        locationRequest.setFastestInterval(5000);
        locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);

        LocationSettingsRequest.Builder builder = new LocationSettingsRequest.Builder().addLocationRequest(locationRequest);

        SettingsClient settingsClient = LocationServices.getSettingsClient(MainActivity.this);
        Task<LocationSettingsResponse> task = settingsClient.checkLocationSettings(builder.build());
//if gps is on
        task.addOnSuccessListener(MainActivity.this, new OnSuccessListener<LocationSettingsResponse>() {
            @Override
            public void onSuccess(LocationSettingsResponse locationSettingsResponse) {
                getDeviceLocation();
            }
        });
        //if gps is NOT ON
        task.addOnFailureListener(MainActivity.this, new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {
                if (e instanceof ResolvableApiException) {
                    ResolvableApiException resolvable = (ResolvableApiException) e;
                    try {
                        resolvable.startResolutionForResult(MainActivity.this, LOCATION_PERMISSION_REQUEST_CODE);
                        getDeviceLocation();
                    } catch (IntentSender.SendIntentException e1) {
                        e1.printStackTrace();
                    }
                } else {
                    Toast.makeText(getApplicationContext(), "THS ONE", Toast.LENGTH_SHORT).show();
                }
            }
        });
        return true;
    }


    //get device location
    private FusedLocationProviderClient mFusedLocationProviderClient;
    private Location currentLocation;
    private LocationCallback locationCallback;

    private void getDeviceLocation(){
        mFusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(MainActivity.this);
        try{
            Task location = mFusedLocationProviderClient.getLastLocation();
            location.addOnCompleteListener(new OnCompleteListener() {
                @Override
                public void onComplete(@NonNull Task task) {
                    if(task.isSuccessful()){
                        Log.d(TAG, "found location: found");
                        currentLocation = (Location)task.getResult();
                        if(currentLocation != null){
                            moveCamera(new LatLng(currentLocation.getLatitude(), currentLocation.getLongitude()),
                                    15f, "My location");
                        }else {
                            //Toast.makeText(getApplicationContext(), "we r here", Toast.LENGTH_SHORT).show();
                            final LocationRequest locationRequest = LocationRequest.create();
                            locationRequest.setInterval(10000);
                            locationRequest.setFastestInterval(5000);
                            locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
                            locationCallback = new LocationCallback() {
                                @Override
                                public void onLocationResult(LocationResult locationResult) {
                                    super.onLocationResult(locationResult);
                                    if (locationResult == null) {
                                        return;
                                    }
                                    currentLocation = locationResult.getLastLocation();
                                    mGoogleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(currentLocation.getLatitude(), currentLocation.getLongitude()), 15f));
                                    mFusedLocationProviderClient.removeLocationUpdates(locationCallback);
                                }
                            };
                            mFusedLocationProviderClient.requestLocationUpdates(locationRequest, locationCallback, null);
                        }

                    }
                    else{
                        Log.d(TAG, "found location: null");
                        Toast.makeText(MainActivity.this, "unable to get last location", Toast.LENGTH_SHORT).show();
                    }
                }
            });

        }catch (SecurityException e){
            Toast.makeText(MainActivity.this, "security error", Toast.LENGTH_SHORT).show();
        }
    }

    private void moveCamera(LatLng latLng, float zoom, String title){
        Log.d(TAG, "moving camera to, lat: " + latLng.latitude + ", long: " + latLng.longitude);
        mGoogleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(latLng, zoom));

        //to drop pin
        MarkerOptions options =new MarkerOptions().position(latLng).title(title);
        mGoogleMap.addMarker(options);
    }

    //widgets
    private void init(){

        mGps.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(checkIfGPSEnabled()) {
                    getDeviceLocation();
                }else {
                    Toast.makeText(getApplication(), "TURN ON GPS", Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    public boolean isGPServiceWorking() {
        int available = GoogleApiAvailability.getInstance().isGooglePlayServicesAvailable(MainActivity.this);
        if (available == ConnectionResult.SUCCESS) {
            //user can do map requests, everything works fine
            Log.d(TAG, "google play services is working");
            return true;
        } else if (GoogleApiAvailability.getInstance().isUserResolvableError(available)) {
            //error but can be resolved
            Dialog dialog = GoogleApiAvailability.getInstance().getErrorDialog(MainActivity.this,available, ERROR_RESULT);
            dialog.show();
        }
        else {
            Toast.makeText(getApplicationContext(), "you can't make map request", Toast.LENGTH_SHORT).show();
        }

        return false;
    }

}
