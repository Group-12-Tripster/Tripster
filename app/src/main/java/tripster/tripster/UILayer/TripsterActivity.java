package tripster.tripster.UILayer;

import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.NavigationView;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.Fragment;
import android.support.v4.content.ContextCompat;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.couchbase.lite.Document;
import com.github.clans.fab.FloatingActionButton;

import net.grandcentrix.tray.AppPreferences;
import net.grandcentrix.tray.core.OnTrayPreferenceChangeListener;
import net.grandcentrix.tray.core.TrayItem;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import tripster.tripster.Image;
import tripster.tripster.R;
import tripster.tripster.UILayer.newsfeed.NewsfeedFragment;
import tripster.tripster.UILayer.notifications.NotificationsFragment;
import tripster.tripster.UILayer.users.SearchForUsersFragment;
import tripster.tripster.UILayer.users.UserProfileFragment;
import tripster.tripster.account.LogoutProvider;
import tripster.tripster.dataLayer.TripsterDb;
import tripster.tripster.services.LocationService;

import static android.Manifest.permission.ACCESS_FINE_LOCATION;
import static android.Manifest.permission.READ_EXTERNAL_STORAGE;
import static android.content.pm.PackageManager.PERMISSION_DENIED;
import static android.content.pm.PackageManager.PERMISSION_GRANTED;
import static tripster.tripster.Constants.CURR_TRIP;
import static tripster.tripster.Constants.MY_ID;
import static tripster.tripster.Constants.PAUSE_SERVICE;
import static tripster.tripster.Constants.RESUME_SERVICE;
import static tripster.tripster.Constants.START_SERVICE;
import static tripster.tripster.Constants.STOP_SERVICE;
import static tripster.tripster.Constants.USER_AVATAR_K;
import static tripster.tripster.Constants.USER_EMAIL_K;
import static tripster.tripster.Constants.USER_ID_K;
import static tripster.tripster.Constants.USER_NAME_K;

public class TripsterActivity extends AppCompatActivity implements NavigationView.OnNavigationItemSelectedListener {

  private static final String TAG = TripsterActivity.class.getName();
  private static final int MY_PERMISSIONS_REQUEST = 47;

  public static TripsterDb tDb;
  public static String currentUserId;

  private AppPreferences pref = new AppPreferences(getApplicationContext());
  private LogoutProvider accountProvider;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);

    tDb = TripsterDb.getInstance(getApplicationContext());
    tDb.initAllViews();

    setContentView(R.layout.activity_tripster);
    askForPermissions();
    initializeDrawer();
    Fragment frag = new NewsfeedFragment();
    getSupportFragmentManager().beginTransaction().replace(R.id.main_content, frag).commit();
  }

  private void askForPermissions() {
    // Here, thisActivity is the current activity
    if (ContextCompat.checkSelfPermission(this, ACCESS_FINE_LOCATION) != PERMISSION_GRANTED
        || ContextCompat.checkSelfPermission(this, READ_EXTERNAL_STORAGE) != PERMISSION_GRANTED) {

      ActivityCompat.requestPermissions(this,
          new String[]{ACCESS_FINE_LOCATION, READ_EXTERNAL_STORAGE}, MY_PERMISSIONS_REQUEST);
    }
  }

  @Override
  public void onRequestPermissionsResult(int requestCode, @NonNull String perms[], @NonNull int[] grantResults) {
    switch (requestCode) {
      case MY_PERMISSIONS_REQUEST: {
        if (grantResults.length != 2 || grantResults[0] == PERMISSION_DENIED ||  grantResults[1] == PERMISSION_DENIED) {
          Toast.makeText(this, "Cannot proceed without your permission!", Toast.LENGTH_LONG).show();
          accountProvider.logOut();
        }
      }
    }
  }

  private void initializeDrawer() {
    Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
    setSupportActionBar(toolbar);
    DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
    ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
    drawer.addDrawerListener(toggle);
    toggle.syncState();
    NavigationView navigationView = (NavigationView) findViewById(R.id.nav_view);
    navigationView.setNavigationItemSelectedListener(this);
  }

  @Override
  protected void onStart() {
    super.onStart();

    String userId = getIntent().getStringExtra(USER_ID_K);
    String name = getIntent().getStringExtra(USER_NAME_K);
    String email = getIntent().getStringExtra(USER_EMAIL_K);
    String avatarUrl = getIntent().getStringExtra(USER_AVATAR_K);

    currentUserId = userId;
    pref.put(MY_ID, userId);
    Log.d(TAG, "The current user ID is: " + userId);

    recreateLoginSession();

    initializeFab();

    //Update or Create the current User
    Map<String, Object> props = new HashMap<>();
    props.put(USER_NAME_K, name);
    props.put(USER_EMAIL_K, email);
    props.put(USER_AVATAR_K, avatarUrl);
    tDb.upsertNewDocById(userId, props);
  }

  private void recreateLoginSession() {
    String loginProviderClassName = getIntent().getStringExtra("loginProvider");
    Log.d(TAG, "MeUser logged in with: " + loginProviderClassName);
    try {
      Class<?> loginProviderClass = Class.forName(loginProviderClassName);
      Constructor<?> cons = loginProviderClass.getConstructor(AppCompatActivity.class);
      Object obj = cons.newInstance(this);
      accountProvider = (LogoutProvider) obj;
    } catch (ClassNotFoundException | NoSuchMethodException | InstantiationException | IllegalAccessException | InvocationTargetException e) {
      e.printStackTrace();
    }
  }

  private void initializeFab() {
    updateFabState(pref.getString(CURR_TRIP, ""));

    findViewById(R.id.tracking_start).setOnClickListener(getFABListener(START_SERVICE));
    findViewById(R.id.tracking_pause).setOnClickListener(getFABListener(PAUSE_SERVICE));
    findViewById(R.id.tracking_resume).setOnClickListener(getFABListener(RESUME_SERVICE));
    findViewById(R.id.tracking_stop).setOnClickListener(getFABListener(STOP_SERVICE));
  }

  private View.OnClickListener getFABListener(final String flag) {
    return new View.OnClickListener() {
      @Override
      public void onClick(View v) {
        Log.d(TAG, "wanna switch to " + flag + " recording");
        Intent serviceIntent = new Intent(TripsterActivity.this, LocationService.class);
        serviceIntent.putExtra("flag", flag);
        TripsterActivity.this.startService(serviceIntent);
      }
    };
  }

  private void updateFabState(String currentTripDetails) {
    final FloatingActionButton startButton = (FloatingActionButton) findViewById(R.id.tracking_start);
    final FloatingActionButton pauseButton = (FloatingActionButton) findViewById(R.id.tracking_pause);
    final FloatingActionButton resumeButton = (FloatingActionButton) findViewById(R.id.tracking_resume);
    final FloatingActionButton endButton = (FloatingActionButton) findViewById(R.id.tracking_stop);

    if (currentTripDetails.isEmpty()) {
      //start button only
      startButton.showButtonInMenu(true);
      pauseButton.hideButtonInMenu(true);
      resumeButton.hideButtonInMenu(true);
      endButton.hideButtonInMenu(true);
    } else if (currentTripDetails.contains("paused")) {
      //resume and stop buttons
      startButton.hideButtonInMenu(true);
      pauseButton.hideButtonInMenu(true);
      resumeButton.showButtonInMenu(true);
      endButton.showButtonInMenu(true);
    } else if (currentTripDetails.contains("running")) {
      //pause and stop buttons
      startButton.hideButtonInMenu(true);
      resumeButton.hideButtonInMenu(true);
      pauseButton.showButtonInMenu(true);
      endButton.showButtonInMenu(true);
    }
  }

  @Override
  public void onBackPressed() {
    DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
    if (drawer.isDrawerOpen(GravityCompat.START)) {
      drawer.closeDrawer(GravityCompat.START);
    } else {
      super.onBackPressed();
    }
  }

  @SuppressWarnings("StatementWithEmptyBody")
  @Override
  public boolean onNavigationItemSelected(@NonNull MenuItem item) {
    // Handle navigation view item clicks here.
    int id = item.getItemId();

    Fragment frag = null;
    if (id == R.id.nav_camera) {
      frag = new UserProfileFragment();
      Bundle args = new Bundle();
      args.putString("userId", currentUserId);
      frag.setArguments(args);
      Log.d(TAG, "I want to switch to MyProfile fragment");
    } else if (id == R.id.nav_slideshow) {
      frag = new SearchForUsersFragment();
      Bundle args = new Bundle();
      args.putString("userId", "none");
      frag.setArguments(args);
      Log.d(TAG, "I want to switch to AllUsers fragment");
    } else if (id == R.id.news_feed) {
      frag = new NewsfeedFragment();
      Log.d(TAG, "I want to switch to NewsFeed fragment");
    } else if (id == R.id.nav_send) {
      frag = new NotificationsFragment();
      Log.d(TAG, "I want to switch to Notifications fragment");
    } else if (id == R.id.nav_logout) {
      Log.d(TAG, "I want to switch to logout");
      accountProvider.logOut();
      return true;
    }

    if (frag != null) {
      getSupportFragmentManager().beginTransaction().replace(R.id.main_content, frag).commit();
    }

    DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
    drawer.closeDrawer(GravityCompat.START);
    return true;
  }

  //----------------------------------------- LISTENERS ------------------------------------------//
  @Override
  protected void onResume() {
    super.onResume();
    tDb.getDocumentById(currentUserId).addChangeListener(currentUserChangeListener);
    pref.registerOnTrayPreferenceChangeListener(currentTripChangeListener);
  }

  OnTrayPreferenceChangeListener currentTripChangeListener = new OnTrayPreferenceChangeListener() {
    @Override
    public void onTrayPreferenceChanged(Collection<TrayItem> items) {
      for(TrayItem i : items) {
        if(i.key().equals(CURR_TRIP)) {
          updateFabState(pref.getString(CURR_TRIP, ""));
        }
      }
    }
  };

  private Document.ChangeListener currentUserChangeListener = new Document.ChangeListener() {
    @Override
    public void changed(Document.ChangeEvent event) {
      View header = ((NavigationView) findViewById(R.id.nav_view)).getHeaderView(0);

      Document me = tDb.getDocumentById(event.getChange().getDocumentId());
      Log.d(TAG, "Current user changed, id is:" + me.getId());

      ((TextView) header.findViewById(R.id.username)).setText((String) me.getProperty("name"));
      Log.d(TAG, "Current user's new name is:" + ((TextView) header.findViewById(R.id.username)).getText());

      new Image((String) me.getProperty("avatarUrl")).displayIn(((ImageView) header.findViewById(R.id.avatar)));
      Log.d(TAG, "Current user's new avatarUrl should be:" + me.getProperty("avatarUrl"));

      ((TextView) header.findViewById(R.id.email)).setText((String) me.getProperty("email"));
      Log.d(TAG, "Current user's new email is:" + ((TextView) header.findViewById(R.id.email)).getText());
    }
  };

  @Override
  protected void onPause() {
    tDb.getDocumentById(currentUserId).removeChangeListener(currentUserChangeListener);
    pref.unregisterOnTrayPreferenceChangeListener(currentTripChangeListener);
    super.onPause();
  }
}
