package com.android2ee.formation.librairies.google.map;

import android.Manifest;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.IntentSender.SendIntentException;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.location.Location;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.DialogFragment;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.Window;
import android.widget.Toast;

import com.android2ee.formation.librairies.google.map.utils.direction.DCACallBack;
import com.android2ee.formation.librairies.google.map.utils.direction.GDirectionsApiUtils;
import com.android2ee.formation.librairies.google.map.utils.direction.model.GDirection;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.GoogleMap.OnMapClickListener;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.Circle;
import com.google.android.gms.maps.model.CircleOptions;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.Polygon;
import com.google.android.gms.maps.model.PolygonOptions;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Mathias Seguy (Android2EE)
 * @goals
 *        This class aims to:
 *        <ul>
 *        <li></li>
 *        </ul>
 */
public class MainActivity extends AppCompatActivity implements LocationListener, GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener, DCACallBack,OnMapReadyCallback {
    /******************************************************************************************/
    /** Attributes **************************************************************************/
    /******************************************************************************************/

    private SupportMapFragment mMapFragment;
    /**
     * The GoogleMap Object
     */
    private GoogleMap mMap=null;
    /**
     * The owner marker
     */
    private Marker mDeviceMarker;
    /**
     * The owner marker
     */
    private Marker mDestinationMarker = null;
    /**
     * The LatLong used to display the mDeviceMarker
     */
    LatLng mDeviceLatlong;
    /**
     * To know when map is initialized
     */
    private boolean permissionGranted =false,mapInitialized=false,mapElementsInitialized=false;

    /******************************************************************************************/
    /** Managing LifeCycle **************************************************************************/
    /******************************************************************************************/

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        supportRequestWindowFeature(Window.FEATURE_ACTION_BAR_OVERLAY);
        setContentView(R.layout.activity_main);
        Log.e("MapActivity",
                "FindFragById : " + ((SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map)));
        mMapFragment = ((SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map));
        mMapFragment.getMapAsync(this);
        // Build to the location service
        buildLBS();
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
        // Sets the map type to be "hybrid"/"Normal"/"Satelite"/"Terrain"
        mMap.setMapType(GoogleMap.MAP_TYPE_NORMAL);
        // Set the indoor enable
        mMap.setIndoorEnabled(true);
        // Add TouchListener
        mMap.setOnMapClickListener(new OnMapClickListener() {
            @Override
            public void onMapClick(LatLng point) {
                Log.e("MainActivity", "onMapClick called");
                // showDirection(point);
                getDirections(point);
            }
        });

        mapInitialized=true;
        if(permissionGranted){
            initializeMapElements();
        }
    }
    /*
	 * (non-Javadoc)
	 * 
	 * @see android.support.v4.app.FragmentActivity#onStart()
	 */
    @Override
    protected void onStart() {
        //then connect to location service
        if (mGoogleLocationApiClient != null) {
            mGoogleLocationApiClient.connect();
        }
        super.onStart();
    }

    /*
	 * (non-Javadoc)
	 * 
	 * @see android.support.v4.app.FragmentActivity#onResume()
	 */
    @Override
    protected void onResume() {
        super.onResume();
        if (ActivityCompat.checkSelfPermission(this,Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED
                && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            askForPermission(MY_PERMISSIONS_REQUEST_ID_4_GET_LAST_KNOW_LOC);
        }else{
            permissionGranted =true;
            if(mapInitialized){
                initializeMapElements();
            }
        }
    }

    /**
     * Initialize Maps elements
     * If we didn't had to check for permissions
     * this code will be in the onResumeMethod
     */
    private void initializeMapElements() {
        if(!mapElementsInitialized) {
            // initialize the device location
            LocationManager locMan = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
            Location deviceLocation = locMan.getLastKnownLocation(LocationManager.PASSIVE_PROVIDER);
            if (deviceLocation != null) {
                mDeviceLatlong = new LatLng(deviceLocation.getLatitude(), deviceLocation.getLongitude());
            }
            // Center Map and initialize it
            onInitializeMapCamera(deviceLocation);
            // Create Markers & shapes
            onCreateMarkers();
            onCreateShapes();
            // Animate shape
            animateShapes();
        }
        mapElementsInitialized=true;
    }
    /*
	 * (non-Javadoc)
	 * 
	 * @see android.support.v7.app.AppCompatActivity#onStop()
	 */
    @Override
    protected void onStop() {
        if (mGoogleLocationApiClient != null) {
            mGoogleLocationApiClient.disconnect();
        }
        super.onStop();
    }

    /**
     * Ask for all the permissions needed by the app
     */
    private void askForPermission(int requestCode) {
        // Here, thisActivity is the current activity
        if (ContextCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {

            // Should we show an explanation?
            if (ActivityCompat.shouldShowRequestPermissionRationale(this,
                    Manifest.permission.ACCESS_FINE_LOCATION)) {
                // Show an expanation to the user *asynchronously* -- don't block
                // this thread waiting for the user's response! After the user
                // sees the explanation, try again to request the permission.

            } else {
                // No explanation needed, we can request the permission.
                ActivityCompat.requestPermissions(this,
                        new String[]{
                                Manifest.permission.ACCESS_COARSE_LOCATION,
                                Manifest.permission.ACCESS_FINE_LOCATION},
                        requestCode);
            }
        }
    }

    @Override
    public boolean shouldShowRequestPermissionRationale(String permission) {
        //because it's a tutorial on GoogleMap not on permission
        //in real life you have a switch depending on the permission name
        //and you say true if this permission is not natural for a lambda user
        return false;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,String permissions[], int[] grantResults) {
        {
            // If request is cancelled, the result arrays are empty.
            if (grantResults.length > 0
                    && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                //then do your stuff
                switch (requestCode) {
                    case MY_PERMISSIONS_REQUEST_ID_4_GET_LAST_KNOW_LOC:
                        permissionGranted=true;
                        if(mapInitialized){
                            initializeMapElements();
                        }
                        break;
                    case MY_PERMISSIONS_REQUEST_ID_4_FUSE_LOC:
                        requestUpdates();
                        break;
                }
            } else {
                //you always run this code at the first launch because permission not granted at first launched
                Toast.makeText(this, "We need permission", Toast.LENGTH_LONG).show();
                // permission denied, boo! Disable the
                // functionality that depends on this permission.
            }
            return;
        }
    }


    /******************************************************************************************/
    /** Managing Menu **************************************************************************/
    /******************************************************************************************/

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    /*
	 * (non-Javadoc)
	 * 
	 * @see android.app.Activity#onOptionsItemSelected(android.view.MenuItem)
	 */
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_google_attribution:
                // display the googleInformation using Dialog
                // new way
                new GoogleAttributionDialogFragment().show(getSupportFragmentManager(),
                        GOOGLE_ATTRIBUTION_DIALOG_FRAGMENT_TAG);
                return true;
            case R.id.ic_action_centermap_ondevice:
                centerMap();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    /******************************************************************************************/
    /** Mananging Camera **************************************************************************/
    /******************************************************************************************/

    /**
     * Initialize the Camera
     *
     * @param location
     */
    private void onInitializeMapCamera(Location location) {
        CameraPosition cameraPosition = new CameraPosition.Builder().target(mDeviceLatlong).zoom(16f).tilt(55f)
                .bearing(0).build();
        CameraUpdate cameraUpdate = CameraUpdateFactory.newCameraPosition(cameraPosition);
        mMap.moveCamera(cameraUpdate);
    }

    /**
     * Center the map on the device
     */
    private void centerMap() {
        // update camera position
        CameraPosition cameraPositionBefore = mMap.getCameraPosition();
        CameraPosition cameraPosition = new CameraPosition.Builder(cameraPositionBefore).target(mDeviceLatlong).build();
        CameraUpdate cameraUpdate = CameraUpdateFactory.newCameraPosition(cameraPosition);
        mMap.animateCamera(cameraUpdate);
    }

    /**
     * Camera Animation: The Runnable that does the work
     */
    Runnable mCameraAnimator;
    /**
     * Camera Animation: The handler that manages the runnable
     */
    Handler mHandler = new Handler();
    /**
     * Camera Animation: The number of iteration to do
     */
    int counter = 0;

    /**
     * Turn around the marker
     */
    private void animateCamera() {
        final float duration = 360;
        mCameraAnimator = new Runnable() {
            @Override
            public void run() {
                CameraPosition cameraPosition = new CameraPosition.Builder().target(mDeviceLatlong).zoom(14f).tilt(55f)
                        .bearing((float) (counter % 360 - 360)).build();
                CameraUpdate cameraUpdate = CameraUpdateFactory.newCameraPosition(cameraPosition);
                mMap.animateCamera(cameraUpdate, 500, new GoogleMap.CancelableCallback() {
                    @Override
                    public void onFinish() {
                        // Do the following stuff
                    }

                    @Override
                    public void onCancel() {
                        // Do the following stuff
                    }
                });
                counter++;
                if (counter < duration) {
                    // Launch again the Runnable in 160 ms
                    mHandler.postDelayed(this, 160);
                }
            }
        };
        // launch the Runnable in 5 seconds
        mHandler.postDelayed(mCameraAnimator, 5000);
    }

    /******************************************************************************************/
    /** Showing/Animating Markers **************************************************************************/
    /******************************************************************************************/
    /**
     * The owner marker with anchor 1, 0
     */
    private Marker mDeviceMarker1;
    /**
     * The owner marker with anchor 0, 0
     */
    private Marker mDeviceMarker2;
    /**
     * The owner marker with anchor 0,1
     */
    private Marker mDeviceMarker3;
    /**
     * The owner marker with anchor 1, 1
     */
    private Marker mDeviceMarker4;

    /**
     * Create
     * the DeviceMarker
     */
    private void onCreateMarkers() {
        float u = 3, v = 3;
        mDeviceMarker = mMap
                .addMarker(new MarkerOptions().position(mDeviceLatlong).title("You're here")
                        .snippet("Marked with a heart")
                        .icon(BitmapDescriptorFactory.fromResource(R.drawable.ic_device_marker)));
        mDeviceMarker1 = mMap.addMarker(new MarkerOptions().position(mDeviceLatlong).anchor(u, -1 * v)
                .title("You're here").snippet("anchor: " + u + ", -" + v)
                .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_AZURE)));
        mDeviceMarker2 = mMap.addMarker(new MarkerOptions().position(mDeviceLatlong).anchor(-1 * u, v)
                .title("You're here").snippet("anchor: -" + u + ", " + v)
                .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_GREEN)));
        mDeviceMarker3 = mMap.addMarker(new MarkerOptions().position(mDeviceLatlong).anchor(u, v).title("You're here")
                .snippet("anchor: " + u + ", " + v)
                .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED)));
        mDeviceMarker4 = mMap.addMarker(new MarkerOptions().position(mDeviceLatlong).anchor(-1 * u, -1 * v)
                .title("You're here").snippet("anchor: -" + u + ", -" + v)
                .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_ORANGE)));
    }

    /**
     * Map shape: The mCircle
     */
    Circle mCircle1;
    /**
     * Map shape: The mCircle
     */
    Circle mCircle2;
    /**
     * Map shape: The mCircle
     */
    Circle mCircle3;
    /**
     * The radius of the mCircle
     */
    double mCircleRadius = 300;
    /**
     * Map shape: The polygone
     */
    Polygon mPolygon;
    double mPolygonLength = 0.0005;
    double mPolygonHoleLenght = 0.0002;
    /**
     * Map shape: The polyLine
     */
    Polyline mPolyline;
    double mPolylineLenght = 0.001;

    /**
     * Create
     * the DeviceMarker
     */
    private void onCreateShapes() {
        double lat = mDeviceLatlong.latitude;
        double lon = mDeviceLatlong.longitude;
        // PloyLine
        // 7.5km=0.1
        // Instantiates a new Polyline object and adds points to define a rectangle
        // Define a square of 750m around the device
        PolylineOptions rectOptions = new PolylineOptions().addAll(buildLatLngShape(lat, lon, mPolylineLenght));

        // Get back the mutable Polyline
        mPolyline = mMap.addPolyline(rectOptions);
        mPolyline.setWidth(3);
        mPolyline.setColor(Color.RED);

        // Polygone
        // Instantiates a new Polygon object and adds points to define a rectangle
        PolygonOptions rectOptionsPolygon = new PolygonOptions().addAll(buildLatLngShape(lat, lon, mPolygonLength));
        // add a hole
        rectOptionsPolygon.addHole(buildLatLngShape(lat, lon, mPolygonHoleLenght));
        // Get back the mutable Polygon
        mPolygon = mMap.addPolygon(rectOptionsPolygon);
        mPolygon.setFillColor(getResources().getColor(R.color.bluetranslucent));
        mPolygon.setStrokeWidth(1);
        mPolygon.setStrokeColor(Color.GREEN);

        // Circles
        // Instantiates a new CircleOptions object and defines the center and radius
        CircleOptions circleOptions = new CircleOptions().center(new LatLng(lat, lon)).radius(300); // In
        // meters
        // Get back the mutable Circle
        mCircle1 = mMap.addCircle(circleOptions);
        setCircleAttribute(mCircle1);
        circleOptions.radius(200);
        mCircle2 = mMap.addCircle(circleOptions);
        setCircleAttribute(mCircle2);
        circleOptions.radius(100);
        mCircle3 = mMap.addCircle(circleOptions);
        setCircleAttribute(mCircle3);
    }

    /**
     * Set the fillColor, the stroke colr and width of the circle
     *
     * @param circle
     *            The circle on to set the attributes
     */
    private void setCircleAttribute(Circle circle) {
        // circle.setFillColor(getResources().getColor(R.color.greentranslucent));
        circle.setStrokeColor(getResources().getColor(R.color.greentranslucent));
        circle.setStrokeWidth(5);
    }

    /**
     * Return the LatLon list to make a square of 2*delta length around the (lat,lon) center point
     *
     * @param latitude
     *            Latitude of the center
     * @param longitude
     *            Longitude of the center
     * @param delta
     *            1/2 the length of the square
     * @return The list of LatLng points to build the shape
     */
    private List<LatLng> buildLatLngShape(double latitude, double longitude, double delta) {
        List<LatLng> latLon = new ArrayList<LatLng>();
        latLon.add(new LatLng(latitude + delta, longitude - delta));
        latLon.add(new LatLng(latitude - delta, longitude - delta));
        latLon.add(new LatLng(latitude - delta, longitude + delta));
        latLon.add(new LatLng(latitude + delta, longitude + delta));
        // and close the polyline
        latLon.add(new LatLng(latitude + delta, longitude - delta));
        return latLon;
    }

    /**
     * Take a polygon and return it as a list
     * Use to setHoles
     *
     * @param latLngList
     * @return
     */
    private List<List<LatLng>> asListForHole(List<LatLng> latLngList) {
        List<List<LatLng>> ret = new ArrayList<List<LatLng>>();
        ret.add(latLngList);
        return ret;
    }

    /**
     * Shapes Animation: The Runnable that does the work
     */
    Runnable mShapeAnimator;
    /**
     * Shapes Animation: The handler that manages the runnable
     */
    Handler mShapeAnimHandler = new Handler();
    /**
     * Shapes Animation: The number of iteration to do
     */
    int mCounterForAnimShape = 0;

    /**
     * Turn around the marker
     */
    private void animateShapes() {
        final int duration = (int) (mCircleRadius * 5);
        mShapeAnimator = new Runnable() {
            @Override
            public void run() {
                // Do your animation here
                mCircle1.setRadius(mCounterForAnimShape % mCircleRadius);
                mCircle3.setRadius((mCounterForAnimShape + 100) % mCircleRadius);
                mCircle2.setRadius((mCounterForAnimShape + 200) % mCircleRadius);
                mCounterForAnimShape++;
                if (mCounterForAnimShape < duration) {
                    // Launch again the Runnable in 32 ms
                    mShapeAnimHandler.postDelayed(this, 32);
                }
            }
        };
        // launch the Runnable in 5 seconds
        mShapeAnimHandler.postDelayed(mShapeAnimator, 5000);
    }

    /******************************************************************************************/
    /** Using the Google Direction API **************************************************************************/
    /******************************************************************************************/
    // public class MainActivity extends AppCompatActivity implements DCACallBack {

    /**
     * Get the Google Direction between mDevice location and the touched location using the Walk
     *
     * @param point
     */
    private void getDirections(LatLng point) {
        GDirectionsApiUtils.getDirection(this, mDeviceLatlong, point, GDirectionsApiUtils.MODE_WALKING);
    }

    /*
	 * (non-Javadoc)
	 * 
	 * @see
	 * com.android2ee.formation.librairies.google.map.direction.DCACallBack#onDirectionLoaded(com
	 * .android2ee.formation.librairies.google.map.direction.GDirection)
	 */
    @Override
    public void onDirectionLoaded(List<GDirection> directions) {
        // Display the direction or use the DirectionsApiUtils
        for (GDirection direction : directions) {
            Log.e("MainActivity", "onDirectionLoaded : Draw GDirections Called with path " + directions);
            GDirectionsApiUtils.drawGDirection(direction, mMap);
        }
    }

    /******************************************************************************************/
    /** Managing Service Location **************************************************************************/
    /******************************************************************************************/
    /**
     * The location Client
     */
    private GoogleApiClient mGoogleLocationApiClient = null;
    /**
     * The location Request to be updated by location event
     */
    private LocationRequest mLocationRequest;
    /**
     * The permission request Id to ask for permissions
     * Should be lower than 8 bits (WTF)
     */
    private final int MY_PERMISSIONS_REQUEST_ID_4_GET_LAST_KNOW_LOC = 11;
    private final int MY_PERMISSIONS_REQUEST_ID_4_FUSE_LOC = 1;
    /**
     * Unique Id to ask for a resolution
     * Used when the connect failed to find a resolution
     */
    private final static int requestCodeGoogleServiceConnection = -13041974;

    /**
     * Entry point to Location Service Connection
     */
    private void buildLBS() {
        int gpsExists = GooglePlayServicesUtil.isGooglePlayServicesAvailable(this);
        if (gpsExists == ConnectionResult.SUCCESS) {
            mGoogleLocationApiClient = new GoogleApiClient.Builder(this)
                    .addApi(LocationServices.API)
                    .addConnectionCallbacks(this)
                    .addOnConnectionFailedListener(this)
                    .build();
        }
    }

    // Managing the mLocationClient.connect
    // with ConnectionCallbacks
    // and OnConnectionFailedListener
    @Override
    public void onConnected(Bundle connectionHint) {
        requestUpdates();
    }

    @Override
    public void onConnectionSuspended(int i) {

    }

    @Override
    public void onConnectionFailed(ConnectionResult result) {
        try {
            if (result.hasResolution()) {
                result.startResolutionForResult(this, requestCodeGoogleServiceConnection);
            }
        } catch (SendIntentException siExc) {
            Log.e("MainActivity", "Resolution failed ", siExc);
        }
    }

    /**
     * Set the type of request update for the location you want
     */
    private void requestUpdates() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED
                && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            askForPermission(MY_PERMISSIONS_REQUEST_ID_4_FUSE_LOC);
        }else{
            //initialize Fuse Location requests
            mLocationRequest = LocationRequest.create();
            mLocationRequest.setPriority(LocationRequest.PRIORITY_BALANCED_POWER_ACCURACY);
            mLocationRequest.setInterval(1000); // Update location every second
            LocationServices.FusedLocationApi.requestLocationUpdates(
                    mGoogleLocationApiClient, mLocationRequest, this);
        }

    }
	/**
	 * To center the map when we first receive an location
	 */
	boolean centerOnce = false;

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * com.google.android.gms.location.LocationListener#onLocationChanged(android.location.Location)
	 */
	@Override
	public void onLocationChanged(Location location) {
		LatLng locationLatLng = new LatLng(location.getLatitude(), location.getLongitude());
		// center map only once, when we receive an accurate location
		if (!centerOnce && locationLatLng != mDeviceLatlong && location.getAccuracy() < 100.0f) {
			// update LatLong
			mDeviceLatlong = locationLatLng;
			// update Marker
			mDeviceMarker.setPosition(mDeviceLatlong);
			mDeviceMarker1.setPosition(mDeviceLatlong);
			mDeviceMarker2.setPosition(mDeviceLatlong);
			mDeviceMarker3.setPosition(mDeviceLatlong);
			mDeviceMarker4.setPosition(mDeviceLatlong);
			// update shape
			mCircle1.setCenter(mDeviceLatlong);
			mCircle2.setCenter(mDeviceLatlong);
			mCircle3.setCenter(mDeviceLatlong);
			mPolygon.setPoints(buildLatLngShape(mDeviceLatlong.latitude, mDeviceLatlong.longitude, mPolygonLength));
			// List<List<LatLng>> getHoles();
			mPolygon.setHoles(asListForHole(buildLatLngShape(mDeviceLatlong.latitude, mDeviceLatlong.longitude,
					mPolygonHoleLenght)));
			mPolyline.setPoints(buildLatLngShape(mDeviceLatlong.latitude, mDeviceLatlong.longitude, mPolylineLenght));
			centerMap();
			centerOnce=true;
		}
	}



	/******************************************************************************************/
	/** GoogleAttributionRequirement Management *****************************************************/
	/******************************************************************************************/

	/**
	 * The Tag for the GOOGLE_ATTRIBUTION_DIALOG_FRAGMENT
	 */
	private static final String GOOGLE_ATTRIBUTION_DIALOG_FRAGMENT_TAG = "GoogleAttributionDialogFragmentTag";

	/**
	 * @author Mathias Seguy (Android2EE)
	 * @goals
	 *        This class aims to define an AlertDialog using fragments
	 */
	public class GoogleAttributionDialogFragment extends DialogFragment{
		@Override
		public Dialog onCreateDialog(Bundle savedInstanceState) {
			// Creation of the AlertDialog Builder
			AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
			// Message and title
			builder.setMessage(GooglePlayServicesUtil.getOpenSourceSoftwareLicenseInfo(getActivity()));
			builder.setTitle(R.string.dialog_google_att_title);
			// No cancel button
			builder.setCancelable(false);
			// Define the OK button, it's message and its listener
			builder.setPositiveButton(getString(R.string.dialog_google_att_btn), null);
			// Then create the Dialog
			return builder.create();
		}
	}

}
