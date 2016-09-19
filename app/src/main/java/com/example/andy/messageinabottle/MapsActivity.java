package com.example.andy.messageinabottle;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.StrictMode;
import android.location.Location;
import android.support.annotation.NonNull;
import android.support.v4.app.FragmentActivity;
import android.support.v4.content.ContextCompat;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;

import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

import java.util.ArrayList;
import java.util.List;

public class MapsActivity extends FragmentActivity implements OnMapReadyCallback, GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener {

    public static final MediaType JSONType = MediaType.parse("application/json; charset=utf-8");
    private static final String TAG = MapsActivity.class.getName();
    private static final int ZOOM = 18;

    private GoogleMap mMap;
    private EditText mMessageEditText;
    private Button mMessageSubmitButton;

    private String serverUrl = "http://65976bdc.ngrok.io";
    private OkHttpClient mClient;

    /**
     * Provides the entry point to Google Play services.
     */
    protected GoogleApiClient mGoogleApiClient;

    /**
     * Represents a geographical location.
     */
    protected Location mLastLocation;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mClient = new OkHttpClient();
        setContentView(R.layout.activity_maps);
        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

        mMessageEditText = (EditText) findViewById(R.id.message_edit_text);
        mMessageSubmitButton = (Button) findViewById(R.id.message_submit_button);

        mMessageSubmitButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View mMessageSubmitButton) {
                String stringJson = createJson();
                post(serverUrl, stringJson);
                mMessageEditText.setText("");
            }
        });
    }

    /**
     * Manipulates the map once available.
     * This callback is triggered when the map is ready to be used.
     * This is where we can add markers or lines, add listeners or move the camera. In this case,
     * we just add a marker near Sydney, Australia.
     * If Google Play services is not installed on the device, the user will be prompted to install
     * it inside the SupportMapFragment. This method will only be triggered once the user has
     * installed Google Play services and returned to the app.
     */
    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;

        try {
            StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder()
                    .permitAll().build();
            StrictMode.setThreadPolicy(policy);
//            System.out.println("Messages: " + getMessages(serverUrl+"/open/"));
            List<Message> msgs = getMessages(serverUrl + "/open/");
            for (Message m : msgs){
                System.out.println("message:" + m.text);
                System.out.println("lat" + m.lat);
                System.out.println("long" + m.lng);
                mMap.addMarker(new MarkerOptions()
                        .position(new LatLng(m.lat, m.lng))
                        .title(m.text));
            }
        } catch (IOException e) {
            System.out.println(e);
        }

        enableMyLocation();
        buildGoogleApiClient();
    }

    /**
     * Enables the My Location layer if the fine location permission has been granted.
     */
    private void enableMyLocation() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            mMap.setMyLocationEnabled(true);
            mMap.getUiSettings().setMyLocationButtonEnabled(true);
        } else {
            Log.d(TAG, "Need location access");
        }
    }

    private List<Message> getMessages(String server) throws IOException {
        String json = "{\"lat\":43.473010,\"long\":-80.540047}";
        RequestBody body = RequestBody.create(JSONType, json);
        Request request = new Request.Builder()
                .url(server)
                .post(body)
                .build();
        System.out.println("Url: " + request.toString());
        Response response = mClient.newCall(request).execute();
        List<Message> messages = new ArrayList<Message>();
        try {
            JSONArray jsonMessages = new JSONArray(response.body().string());
            for (int i = 0; i < jsonMessages.length(); i++) {
                JSONObject jsonMsg = jsonMessages.getJSONObject(i);
                Message msg = new Message(jsonMsg.getString("text"), jsonMsg.getString("long"), jsonMsg.getString("lat"));
                messages.add(msg);
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
//        return response.body().string();
        return messages;
    }

    protected synchronized void buildGoogleApiClient() {
        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .addApi(LocationServices.API)
                .build();
        mGoogleApiClient.connect();
    }

    @Override
    protected void onStart() {
        super.onStart();
        if (mGoogleApiClient != null && !mGoogleApiClient.isConnected())
            mGoogleApiClient.connect();
    }

    @Override
    public void onConnected(Bundle connectionHint) {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            mLastLocation = LocationServices.FusedLocationApi.getLastLocation(mGoogleApiClient);
            if (mLastLocation != null) {
                double latitude = mLastLocation.getLatitude();
                double longitude = mLastLocation.getLongitude();
                mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(latitude, longitude), ZOOM));
            } else {
                Log.e(TAG, "Failed to find last location");
            }
        }
    }

    @Override
    public void onConnectionSuspended(int i) {

    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {

    }

    private String createJson() {

        try {
            JSONObject obj = new JSONObject();

            obj.put("long", mLastLocation.getLongitude());
            obj.put("lat", mLastLocation.getLatitude());
            obj.put("EditText", mMessageEditText.getText());

            return obj.toString();
        } catch (JSONException e) {
            return null;
        }
    }

    String post(String url, String json) {
        try {
            RequestBody body = RequestBody.create(JSONType, json);
            Request request = new Request.Builder()
                    .url(url)
                    .post(body)
                    .build();
            Response response = mClient.newCall(request).execute();
            return response.body().string();
        } catch (IOException e) {
            return null;
        }
    }
}