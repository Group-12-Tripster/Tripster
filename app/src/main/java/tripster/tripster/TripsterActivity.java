package tripster.tripster;

import android.os.Bundle;
import android.support.design.widget.NavigationView;
import android.support.v4.app.Fragment;
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

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;

public class TripsterActivity extends AppCompatActivity
        implements NavigationView.OnNavigationItemSelectedListener {

    private AccountProvider accountProvider;
    private static final String TAG = TripsterActivity.class.getName();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        recreateLoginSession();

        setContentView(R.layout.activity_tripster);

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawer.addDrawerListener(toggle);
        toggle.syncState();

        NavigationView navigationView = (NavigationView) findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(this);

        View header = navigationView.getHeaderView(0);

        accountProvider.setUserAccountFields((TextView) header.findViewById(R.id.username),
                (TextView) header.findViewById(R.id.email), (ImageView) header.findViewById(R.id.avatar));

        // Check that the activity is using the layout version with
        // the fragment_container FrameLayout
        if (findViewById(R.id.main_content) != null) {
            if (savedInstanceState != null) { // This activity was already created once, so the fragment already exists
                return;
            }
            Fragment initial = new TripsterFragment();

            // Add the fragment to the 'main_container' FrameLayout
            getSupportFragmentManager().beginTransaction().add(R.id.main_content, initial).commit();
        }
    }

    private void recreateLoginSession() {
        String loginProviderClassName = getIntent().getStringExtra("loginProvider");
        Log.d(TAG, loginProviderClassName);
        try {
            Class<?> loginProviderClass = Class.forName(loginProviderClassName);
            Constructor<?> cons = loginProviderClass.getConstructor(AppCompatActivity.class);
            Object obj = cons.newInstance(this);
            accountProvider = (AccountProvider) obj;

        } catch (ClassNotFoundException | NoSuchMethodException | InstantiationException | IllegalAccessException | InvocationTargetException e) {
            e.printStackTrace();
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

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @SuppressWarnings("StatementWithEmptyBody")
    @Override
    public boolean onNavigationItemSelected(MenuItem item) {
        // Handle navigation view item clicks here.
        int id = item.getItemId();

        Fragment frag = new TripsterFragment();

        if (id == R.id.nav_camera) {
            Log.d(TAG, "creates gpstrackingfrag");
        } else if (id == R.id.nav_gallery) {

        } else if (id == R.id.nav_slideshow) {

        } else if (id == R.id.nav_manage) {

        } else if (id == R.id.nav_share) {

        } else if (id == R.id.nav_send) {

        } else if (id == R.id.nav_logout) {
            accountProvider.logOut();
            return true;
        }

        getSupportFragmentManager().beginTransaction().replace(R.id.content_tripster, frag).commit();

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        drawer.closeDrawer(GravityCompat.START);
        return true;
    }
}