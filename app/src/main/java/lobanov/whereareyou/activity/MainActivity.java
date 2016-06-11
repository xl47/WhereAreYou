package lobanov.whereareyou.activity;

import lobanov.whereareyou.R;
import lobanov.whereareyou.app.AppConfig;
import lobanov.whereareyou.app.AppController;
import lobanov.whereareyou.helper.SQLiteHandler;
import lobanov.whereareyou.helper.SessionManager;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import android.app.Activity;
import android.content.Intent;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;
import android.util.Log;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.TextView;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toast;

import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.mikepenz.iconics.typeface.FontAwesome;
import com.mikepenz.materialdrawer.Drawer;
import com.mikepenz.materialdrawer.model.DividerDrawerItem;
import com.mikepenz.materialdrawer.model.PrimaryDrawerItem;
import com.mikepenz.materialdrawer.model.SecondaryDrawerItem;
import com.mikepenz.materialdrawer.model.SectionDrawerItem;
import com.mikepenz.materialdrawer.model.interfaces.Badgeable;
import com.mikepenz.materialdrawer.model.interfaces.IDrawerItem;
import com.mikepenz.materialdrawer.model.interfaces.Nameable;

import org.json.JSONException;
import org.json.JSONObject;

import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = MainActivity.class.getSimpleName();
    private TextView txtName;
    private TextView txtEmail;
    private TextView txtLocation;
    private Button btnLogout;
    private Button btnGetLocation;
    private Button btnLoadLocations;

    private SQLiteHandler db;
    private SessionManager session;

    private Drawer.Result drawerResult = null;

    private LocationManager locationManager;
    StringBuilder sbGPS = new StringBuilder();
    StringBuilder sbNet = new StringBuilder();



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        txtName = (TextView) findViewById(R.id.name);
        txtEmail = (TextView) findViewById(R.id.email);
        txtLocation = (TextView) findViewById(R.id.location);
        btnLogout = (Button) findViewById(R.id.btnLogout);
        btnGetLocation = (Button) findViewById(R.id.btnGetLocation);
        btnLoadLocations = (Button) findViewById(R.id.btnLoadLocations);

        locationManager = (LocationManager) getSystemService(LOCATION_SERVICE);

        // SqLite database handler
        db = new SQLiteHandler(getApplicationContext());

        // session manager
        session = new SessionManager(getApplicationContext());

        if (!session.isLoggedIn()) {
            logoutUser();
        }

        // Fetching user details from sqlite
        HashMap<String, String> user = db.getUserDetails();

        String name = user.get("name");
        String email = user.get("email");


        // Displaying the user details on the screen
        txtName.setText(name);
        txtEmail.setText(email);


        new Drawer()
                .withActivity(this)
                .withToolbar(toolbar)
                .withActionBarDrawerToggle(true)
                .withHeader(R.layout.drawer_header)
                .addDrawerItems(
                        new PrimaryDrawerItem().withName(R.string.drawer_item_home).withIcon(FontAwesome.Icon.faw_home).withBadge("99").withIdentifier(1),
                        new PrimaryDrawerItem().withName(R.string.drawer_item_logout).withIcon(FontAwesome.Icon.faw_eye).withIdentifier(2)
                )
                .withOnDrawerListener(new Drawer.OnDrawerListener() {
                    @Override
                    public void onDrawerOpened(View drawerView) {
                    }

                    @Override
                    public void onDrawerClosed(View drawerView) {
                    }
                })
                .withOnDrawerItemClickListener(new Drawer.OnDrawerItemClickListener() {
                    @Override
                    // Обработка клика
                    public void onItemClick(AdapterView<?> parent, View view, int position, long id, IDrawerItem drawerItem) {
                        if (drawerItem instanceof Nameable) {
                            if (MainActivity.this.getString(((Nameable) drawerItem).getNameRes()).trim().equals(R.string.drawer_item_logout))
                            {
                                Toast.makeText(MainActivity.this, MainActivity.this.getString(((Nameable) drawerItem).getNameRes()), Toast.LENGTH_SHORT).show();
                                logoutUser();
                            }
                        }
                        if (drawerItem instanceof Badgeable) {
                            /*
                            Badgeable badgeable = (Badgeable) drawerItem;
                            if (badgeable.getBadge() != null) {
                                // учтите, не делайте так, если ваш бейдж содержит символ "+"
                                try {
                                    int badge = Integer.valueOf(badgeable.getBadge());
                                    if (badge > 0) {
                                        drawerResult.updateBadge(String.valueOf(badge - 1), position);
                                    }
                                } catch (Exception e) {
                                    Log.d("test", "Не нажимайте на бейдж, содержащий плюс! :)");
                                }
                            }
                            */
                        }
                    }
                })
                .withOnDrawerItemLongClickListener(new Drawer.OnDrawerItemLongClickListener() {
                    @Override
                    // Обработка длинного клика, например, только для SecondaryDrawerItem
                    public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id, IDrawerItem drawerItem) {
                        return false;
                    }
                })
                .build();


        // Logout button click event
        btnLogout.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                logoutUser();
            }
        });

        btnGetLocation.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                HashMap<String, String> user = db.getUserDetails();
                String uid = user.get("uid");
                showLocation(locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER));
                locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER,
                        1000 * 10, 10, locationListener);
                locationManager.requestLocationUpdates(
                        LocationManager.NETWORK_PROVIDER, 1000 * 10, 10,
                        locationListener);
                storeLocation(locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER), uid);
                getUsersLocations();
            }
        });

        btnLoadLocations.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View v) {
                getUsersLocations();
            }
        });
    }



    /**
     * Logging out the user. Will set isLoggedIn flag to false in shared
     * preferences Clears the user data from sqlite users table
     * */
    private void logoutUser() {
        session.setLogin(false);

        db.deleteUsers();

        // Launching the login activity
        Intent intent = new Intent(MainActivity.this, LoginActivity.class);
        startActivity(intent);
        finish();
    }

    private LocationListener locationListener = new LocationListener() {

        @Override
        public void onLocationChanged(Location location) {

        }

        @Override
        public void onProviderDisabled(String provider) {

        }

        @Override
        public void onProviderEnabled(String provider) {
        }

        @Override
        public void onStatusChanged(String provider, int status, Bundle extras) {

        }
    };

    private void showLocation(Location location) {
        if (location != null) {

            if (location.getProvider().equals(LocationManager.GPS_PROVIDER)) {
                txtLocation.setText(formatLocation(location));
            } else if (location.getProvider().equals(
                    LocationManager.NETWORK_PROVIDER)) {
                txtLocation.setText(formatLocation(location));
            }
        }
    }

    private String formatLocation(Location location) {
        if (location == null)
            return "";
        return String.format(
                "Coordinates: lat = %1$.4f, lon = %2$.4f, time = %3$tF %3$tT",
                location.getLatitude(), location.getLongitude(), new Date(
                        location.getTime()));
    }

    private void storeLocation(final Location location, final String uid) {
        if (location != null) {
            String tag_string_req = "req_storelocation";

            StringRequest strReq = new StringRequest(Request.Method.POST,
                    AppConfig.URL_STORELOCATION, new Response.Listener<String>() {

                @Override
                public void onResponse(String response) {
                    Log.d(TAG, "StoreLocation Response: " + response.toString());


                    try {
                        JSONObject jObj = new JSONObject(response);
                        boolean error = jObj.getBoolean("error");
                        if (!error) {

                        } else {

                            // Error occurred in registration. Get the error
                            // message
                            String errorMsg = jObj.getString("error_msg");
                            Toast.makeText(getApplicationContext(),
                                    errorMsg, Toast.LENGTH_LONG).show();
                        }
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }

                }
            }, new Response.ErrorListener() {

                @Override
                public void onErrorResponse(VolleyError error) {
                    Log.e(TAG, "StoreLocation Error: " + error.getMessage());
                    Toast.makeText(getApplicationContext(),
                            error.getMessage(), Toast.LENGTH_LONG).show();

                }
            }) {

                @Override
                protected Map<String, String> getParams() {
                    // Posting params to url
                    Map<String, String> params = new HashMap<String, String>();
                    params.put("uid", uid);
                    params.put("lat", String.valueOf(location.getLatitude()));
                    params.put("lon", String.valueOf(location.getLongitude()));

                    return params;
                }

            };

            AppController.getInstance().addToRequestQueue(strReq, tag_string_req);
        }
    }
    private void getUsersLocations() {

            String tag_string_req = "req_getuserslocation";

            StringRequest strReq = new StringRequest(Request.Method.GET,
                    AppConfig.URL_GETUSERSLOCATIONS, new Response.Listener<String>() {

                @Override
                public void onResponse(String response) {
                    Log.d(TAG, "GetUsersLocations Response: " + response.toString());


                    try {
                        JSONObject jObj = new JSONObject(response);
                        boolean error = jObj.getBoolean("error");
                        if (!error) {
                            int amount = jObj.getInt("amount");
                            for (int i =0; i<=amount; i++)
                            {
                                JSONObject temp = jObj.getJSONObject(Integer.toString(i));
                                String name = temp.getString("name");
                                Double lat = temp.getDouble("lat");
                                Double lon = temp.getDouble("lon");
                                String date = temp.getString("date");
                                db.addPoint(name,lat,lon,date);
                                Toast.makeText(getApplicationContext(), "Points successfully loaded.", Toast.LENGTH_LONG).show();
                            }
                        } else {

                            // Error occurred in registration. Get the error
                            // message
                            String errorMsg = jObj.getString("error_msg");
                            Toast.makeText(getApplicationContext(),
                                    errorMsg, Toast.LENGTH_LONG).show();
                        }
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }

                }
            }, new Response.ErrorListener() {

                @Override
                public void onErrorResponse(VolleyError error) {
                    Log.e(TAG, "GetUsersLocations Error: " + error.getMessage());
                    Toast.makeText(getApplicationContext(),
                            error.getMessage(), Toast.LENGTH_LONG).show();

                }
            }) {

                @Override
                protected Map<String, String> getParams() {
                    // Posting params to url
                    Map<String, String> params = new HashMap<String, String>();
                    return params;
                }

            };

            AppController.getInstance().addToRequestQueue(strReq, tag_string_req);

    }
}