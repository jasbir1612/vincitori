package com.example.jasbirsingh.vincitori;

import android.Manifest;
import android.content.pm.PackageManager;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.HashMap;

import org.json.JSONObject;

import android.app.Dialog;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Menu;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Spinner;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.GoogleMap.OnMapClickListener;
import com.google.android.gms.maps.GoogleMap.OnMarkerClickListener;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;

public class MainActivity2 extends FragmentActivity {

    // GoogleMap
    GoogleMap mGoogleMap;

    // Spinner in which the location types are stored
    Spinner mSprPlaceType;

    // A button to find the near by places
    Button mBtnFind = null;

    // Stores near by places
    Place[] mPlaces = null;

    // A String array containing place types sent to Google Place service
    String[] mPlaceType = null;

    // A String array containing place types displayed to user
    String[] mPlaceTypeName = null;

    // The location at which user touches the Google Map
    LatLng mLocation = null;

    // Links marker id and place object
    HashMap<String, Place> mHMReference = new HashMap<String, Place>();

    // Specifies the drawMarker() to draw the marker with default color
    private static final float UNDEFINED_COLOR = -1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main2);
        mPlaceType = getResources().getStringArray(R.array.place_type);
        mPlaceTypeName = getResources().getStringArray(R.array.place_type_name);
        ArrayAdapter<String> adapter = new ArrayAdapter<String>(this,
                android.R.layout.simple_spinner_dropdown_item,
                mPlaceTypeName);
        mSprPlaceType = (Spinner) findViewById(R.id.spr_place_type);
        mSprPlaceType.setAdapter(adapter);
        mBtnFind = (Button) findViewById(R.id.btn_find);
        int status = GooglePlayServicesUtil.isGooglePlayServicesAvailable(getBaseContext());

        if (status != ConnectionResult.SUCCESS) {
            Toast.makeText(this, "not Available", Toast.LENGTH_SHORT).show();

            int requestCode = 10;
            Dialog dialog = GooglePlayServicesUtil.getErrorDialog(status, this, requestCode);
            dialog.show();

        } else {
            SupportMapFragment fragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map);
            mGoogleMap = fragment.getMap();
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
            mGoogleMap.setMyLocationEnabled(true);
            if(savedInstanceState !=null) {

                // Remove links from marker id
                mHMReference.clear();

                if(savedInstanceState.containsKey("places")){
                    mPlaces = (Place[]) savedInstanceState.getParcelableArray("places");

                    for(int i=0;i<mPlaces.length;i++){

                        // Getting latitude and longitude of the i-th place
                        LatLng point = new LatLng(Double.parseDouble(mPlaces[i].mLat),
                                Double.parseDouble(mPlaces[i].mLng));

                        // Drawing the marker corresponding to the i-th place
                        Marker m = drawMarker(point,UNDEFINED_COLOR);

                        // Linkng i-th place and its marker id
                        mHMReference.put(m.getId(), mPlaces[i]);
                    }
                }

                if(savedInstanceState.containsKey("location")){

                    // Retrieving the touched location and setting in member variable
                    mLocation = (LatLng) savedInstanceState.getParcelable("location");

                    // Drawing a marker at the touched location
                    drawMarker(mLocation, BitmapDescriptorFactory.HUE_GREEN);
                }
            }

            // Setting click event lister for the find button
            mBtnFind.setOnClickListener(new OnClickListener() {

                @Override
                public void onClick(View v) {
//                    Toast.makeText(MainActivity2.this, "Clicked", Toast.LENGTH_SHORT).show();

                    Log.d("find button", "Clicked");
                    int selectedPosition = mSprPlaceType.getSelectedItemPosition();
                    String type = mPlaceType[selectedPosition];

                    mGoogleMap.clear();

                    if(mLocation==null){
                        Toast.makeText(getBaseContext(), "Please mark a location", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    drawMarker(mLocation, BitmapDescriptorFactory.HUE_GREEN);

                    StringBuilder sb = new StringBuilder("https://maps.googleapis.com/maps/api/place/nearbysearch/json?");
                    sb.append("location="+mLocation.latitude+","+mLocation.longitude);
                    sb.append("&radius=5000");
                    sb.append("&types="+type);
                    sb.append("&sensor=true");
                    sb.append("&key=AIzaSyC6eFunlAN73H7fKPeoypqh_CRcecda_PU");
                    Toast.makeText(MainActivity2.this, "" +sb, Toast.LENGTH_SHORT).show();
                    Log.d("api", "api is: " +sb);

                    // Creating a new non-ui thread task to download Google place json data
                    PlacesTask placesTask = new PlacesTask();

                    // Invokes the "doInBackground()" method of the class PlaceTask
                    placesTask.execute(sb.toString());
                }
            });

            // Map Click listener
            mGoogleMap.setOnMapClickListener(new OnMapClickListener() {

                @Override
                public void onMapClick(LatLng point) {

                    // Clears all the existing markers
                    mGoogleMap.clear();

                    // Setting the touched location in member variable
                    mLocation = point;

                    // Drawing a marker at the touched location
                    drawMarker(mLocation,BitmapDescriptorFactory.HUE_GREEN);
                }
            });

            // Marker click listener
            mGoogleMap.setOnMarkerClickListener(new OnMarkerClickListener() {

                @Override
                public boolean onMarkerClick(Marker marker) {

                    // If touched at User input location
                    if(!mHMReference.containsKey(marker.getId()))
                        return false;

                    // Getting place object corresponding to the currently clicked Marker
                    Place place = mHMReference.get(marker.getId());

                    // Creating an instance of DisplayMetrics
                    DisplayMetrics dm = new DisplayMetrics();

                    // Getting the screen display metrics
                    getWindowManager().getDefaultDisplay().getMetrics(dm);

                    // Creating a dialog fragment to display the photo
                    PlaceDialogFragment dialogFragment = new PlaceDialogFragment(place,dm);

                    // Getting a reference to Fragment Manager
                    FragmentManager fm = getSupportFragmentManager();

                    // Starting Fragment Transaction
                    FragmentTransaction ft = fm.beginTransaction();

                    // Adding the dialog fragment to the transaction
                    ft.add(dialogFragment, "TAG");

                    // Committing the fragment transaction
                    ft.commit();

                    return false;
                }
            });
        }
    }

    /**
     * A callback function, executed on screen rotation
     */
    @Override
    protected void onSaveInstanceState(Bundle outState) {

        // Saving all the near by places objects
        if(mPlaces!=null)
            outState.putParcelableArray("places", mPlaces);

        // Saving the touched location
        if(mLocation!=null)
            outState.putParcelable("location", mLocation);

        super.onSaveInstanceState(outState);
    }

    /** A method to download json data from argument url */
    private String downloadUrl(String strUrl) throws IOException{
        String data = "";
        InputStream iStream = null;
        HttpURLConnection urlConnection = null;
        try{
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
            while( ( line = br.readLine()) != null){
                sb.append(line);
            }

            data = sb.toString();

            br.close();

        }catch(Exception e){
//            Log.d("Exception while downloading url", e.toString());
        }finally{
            iStream.close();
            urlConnection.disconnect();
        }
        return data;
    }
    /** A class, to download Google Places */
    private class PlacesTask extends AsyncTask<String, Integer, String>{

        String data = null;

        // Invoked by execute() method of this object
        @Override
        protected String doInBackground(String... url) {
            try{
                data = downloadUrl(url[0]);
            }catch(Exception e){
                Log.d("Background Task",e.toString());
            }
            return data;
        }

        // Executed after the complete execution of doInBackground() method
        @Override
        protected void onPostExecute(String result){
            ParserTask parserTask = new ParserTask();

            // Start parsing the Google places in JSON format
            // Invokes the "doInBackground()" method of ParserTask
            parserTask.execute(result);
        }
    }

    /** A class to parse the Google Places in JSON format */
    private class ParserTask extends AsyncTask<String, Integer, Place[]>{

        JSONObject jObject;

        // Invoked by execute() method of this object
        @Override
        protected Place[] doInBackground(String... jsonData) {

            Place[] places = null;
            PlaceJSONParser placeJsonParser = new PlaceJSONParser();

            try{
                jObject = new JSONObject(jsonData[0]);
                /** Getting the parsed data as a List construct */
                places = placeJsonParser.parse(jObject);

            }catch(Exception e){
                Log.d("Exception",e.toString());
            }
            return places;
        }

        // Executed after the complete execution of doInBackground() method
        @Override
        protected void onPostExecute(Place[] places){

            mPlaces = places;

            for(int i=0;i< places.length ;i++){
                Place place = places[i];

                // Getting latitude of the place
                double lat = Double.parseDouble(place.mLat);

                // Getting longitude of the place
                double lng = Double.parseDouble(place.mLng);

                LatLng latLng = new LatLng(lat, lng);

                Marker m = drawMarker(latLng,UNDEFINED_COLOR);

                // Adding place reference to HashMap with marker id as HashMap key
                // to get its reference in infowindow click event listener
                mHMReference.put(m.getId(), place);
            }
        }
    }

    /**
     * Drawing marker at latLng with color
     */
    private Marker drawMarker(LatLng latLng,float color){
        // Creating a marker
        MarkerOptions markerOptions = new MarkerOptions();

        // Setting the position for the marker
        markerOptions.position(latLng);

        if(color != UNDEFINED_COLOR)
            markerOptions.icon(BitmapDescriptorFactory.defaultMarker(color));

        // Placing a marker on the touched position
        Marker m = mGoogleMap.addMarker(markerOptions);

        return m;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
//        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }
}