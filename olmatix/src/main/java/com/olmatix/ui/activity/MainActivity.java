package com.olmatix.ui.activity;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.design.widget.CoordinatorLayout;
import android.support.design.widget.TabLayout;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import com.androidadvance.topsnackbar.TSnackbar;
import com.olmatix.adapter.OlmatixPagerAdapter;
import com.olmatix.database.dbNodeRepo;
import com.olmatix.lesjaw.olmatix.R;
import com.olmatix.model.InstalledNodeModel;
import com.olmatix.service.OlmatixService;
import com.olmatix.ui.fragment.DashboardNode;
import com.olmatix.ui.fragment.InstalledNode;
import com.olmatix.ui.fragment.Scene;
import com.olmatix.utils.Connection;

import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;

import java.io.UnsupportedEncodingException;
import java.util.List;

/**
 * Created by Lesjaw on 02/12/2016.
 */

public class MainActivity extends AppCompatActivity {

    boolean serverconnected;
    int backButtonCount;
    private TabLayout tabLayout;
    private ImageView imgStatus;
    private TextView connStat;
    private Animation animConn;
    private Toolbar mToolbar;
    public static final String UE_ACTION = "com.olmatix.ui.activity.inforeground";
    private IntentFilter mIntentFilter;
    SharedPreferences sharedPref;
    Boolean mStatusServer;
    public static dbNodeRepo dbNodeRepo;
    private InstalledNodeModel installedNodeModel;
    CoordinatorLayout coordinatorLayout;


    public static int[] tabIcons = {
            R.drawable.ic_dashboard,
            R.drawable.ic_scene,
            R.drawable.ic_node,
    };

    private ViewPager mViewPager;
    private OlmatixPagerAdapter mOlmatixAdapter;


    private BroadcastReceiver mMessageReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {

            Boolean message = intent.getBooleanExtra("MqttStatus", false);
            if (message!=null) {
                Log.d("DEBUG", "MainActivity Onreceive: "+message);
                if (message) {
                    serverconnected = true;
                    imgStatus.setImageResource(R.drawable.ic_conn_green);
                    connStat.setText("Connected");
                    TSnackbar snackbar = TSnackbar.make((coordinatorLayout),"Olmatix connected"
                            ,TSnackbar.LENGTH_LONG);
                    View snackbarView = snackbar.getView();
                                snackbarView.setBackgroundColor(Color.parseColor("#FF4081"));
                    snackbar.show();

                } else if (!message) {
                    serverconnected = false;
                    imgStatus.setImageResource(R.drawable.ic_conn_red);
                    imgStatus.startAnimation(animConn);
                    connStat.setText("Not Connected");
                    connStat.startAnimation(animConn);
                    TSnackbar snackbar = TSnackbar.make((coordinatorLayout),"Olmatix disconnected"
                            ,TSnackbar.LENGTH_LONG);
                    View snackbarView = snackbar.getView();
                                snackbarView.setBackgroundColor(Color.parseColor("#FF4081"));
                    snackbar.show();
                }
            }

        }
    };
    private BroadcastReceiver mIntentReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (action.equals(UE_ACTION)) {
                //Log.d("Olmatix", "i'm in the foreground");
                this.setResultCode(Activity.RESULT_OK);
            }
        }
    };


    @Override
    protected void onSaveInstanceState(final Bundle outState) {
        // super.onSaveInstanceState(outState);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Intent i = new Intent(this, OlmatixService.class);
        startService(i);

        coordinatorLayout=(CoordinatorLayout)findViewById(R.id.main_content);

        mIntentFilter = new IntentFilter();
        mIntentFilter.addAction(UE_ACTION);

        initView();
        setupToolbar();
        setupTabs();


    }

    @Override
    protected void onStop() {
        super.onStop();
        LocalBroadcastManager.getInstance(getApplicationContext()).unregisterReceiver(mMessageReceiver);
    }
    private void initView() {
        mToolbar = (Toolbar) findViewById(R.id.toolbar);
        imgStatus = (ImageView) findViewById(R.id.conn_state);
        connStat = (TextView) findViewById(R.id.conn_state1);
        animConn = AnimationUtils.loadAnimation(getApplicationContext(), R.anim.blink);

        tabLayout = (TabLayout) findViewById(R.id.tab_layout);
        mOlmatixAdapter = new OlmatixPagerAdapter(getSupportFragmentManager());
        mViewPager = (ViewPager) findViewById(R.id.container);
        setupViewPager(mViewPager);

        sharedPref = PreferenceManager.getDefaultSharedPreferences(this);
        mStatusServer = sharedPref.getBoolean("conStatus", false);
        Log.d("DEBUG", "MainActivity status connection: "+mStatusServer);

        if (mStatusServer) {
            imgStatus.setImageResource(R.drawable.ic_conn_green);
            connStat.setText("Connected");
        } else {
            imgStatus.setImageResource(R.drawable.ic_conn_red);
            imgStatus.startAnimation(animConn);
            connStat.setText("Not Connected");
            connStat.startAnimation(animConn);
        }

    }

    private void setupToolbar() {
        setSupportActionBar(mToolbar);
    }

    private void setupTabs(){
        tabLayout.setupWithViewPager(mViewPager);
        //setupTabIcons();
    }

    private void setupTabIcons() {

        tabLayout.getTabAt(0).setIcon(tabIcons[0]);
        tabLayout.getTabAt(1).setIcon(tabIcons[1]);
        tabLayout.getTabAt(2).setIcon(tabIcons[2]);
    }

    private void setupViewPager(ViewPager viewPager) {

        mOlmatixAdapter.addFrag(new DashboardNode(), "Dashboard");
        mOlmatixAdapter.addFrag(new Scene(), "Scenes");
        mOlmatixAdapter.addFrag(new InstalledNode(), "Nodes");
        viewPager.setAdapter(mOlmatixAdapter);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        LocalBroadcastManager.getInstance(getApplicationContext()).unregisterReceiver(mMessageReceiver);
    }

    @Override
    protected void onStart() {
        super.onStart();
    }

    @Override
    protected void onPause() {
        super.onPause();
        unregisterReceiver(mIntentReceiver);
        LocalBroadcastManager.getInstance(getApplicationContext()).unregisterReceiver(mMessageReceiver);
    }

    @Override
    protected void onResume() {
        super.onResume();
        registerReceiver(mIntentReceiver, mIntentFilter);
        LocalBroadcastManager.getInstance(this).registerReceiver(
                mMessageReceiver, new IntentFilter("MQTTStatus"));
        Log.d("Receiver ", "MainActivity = Starting..");
    }

    // Override this method to do what you want when the menu is recreated
    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        return super.onPrepareOptionsMenu(menu);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement

        if (id == R.id.action_settings) {
            Intent i = new Intent(this, SettingsActivity.class);
            startActivity(i);
            return true;
        }

        if (id == R.id.action_exit) {
            finish();
            System.exit(0);
            return true;
        }

        if (id == R.id.action_reset) {
            resetNode();
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    private void resetNode (){
        final String[] nodelist;
        List<InstalledNodeModel> NodeID;

        dbNodeRepo = new dbNodeRepo(this);
        installedNodeModel = new InstalledNodeModel();
        final ListView listView = new ListView(this);

        NodeID = dbNodeRepo.getNodeList();

        nodelist = new String[NodeID.size()];
        for (int i = 0; i < NodeID.size(); i++) {
            nodelist[i] = String.valueOf(NodeID.get(i).getNodesID() + " || " + NodeID.get(i).getNice_name_n());
            System.out.println(nodelist[i]);
        }
        ArrayAdapter<String> testadap = (new ArrayAdapter<>(this,
                R.layout.list_view_reset, nodelist));

        listView.setAdapter(testadap);

        // Set grid view to alertDialog
        final AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setView(listView);
        builder.setTitle("Pick Nodes");
        final AlertDialog ad = builder.show();


        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> arg0, View arg1, int arg2, long arg3) {
                // TODO Auto-generated method stub
                Log.d("DEBUG", "onClick1: " + listView.getItemAtPosition(arg2));
                arg1.setSelected(true);

                TSnackbar snackbar = TSnackbar.make((coordinatorLayout),"You have pick " + listView.getItemAtPosition(arg2)
                        ,TSnackbar.LENGTH_LONG);
                View snackbarView = snackbar.getView();
                            snackbarView.setBackgroundColor(Color.parseColor("#FF4081"));
                snackbar.setIconLeft(R.drawable.ic_light_black, 24);
                snackbar.show();

                String node = (String) listView.getItemAtPosition(arg2);
                int iend = node.indexOf("|");
                if (iend != -1) {
                    String nodeid = node.substring(0, iend);
                    resetConfirm(nodeid, node);
                    Log.d("DEBUG", "onItemClick: "+nodeid);
                    ad.dismiss();
                }

            }
        });

    }

    private void resetConfirm(final String NodeID, final String node){
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Reset this Node?");
        builder.setMessage(node);
        builder.setPositiveButton("Reset", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                sharedPref = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
                mStatusServer = sharedPref.getBoolean("conStatus", false);
                if (mStatusServer) {
                    String topic = "devices/" + String.valueOf(NodeID).trim() + "/$reset";
                    String payload = "true";
                    byte[] encodedPayload = new byte[0];
                    try {
                        encodedPayload = payload.getBytes("UTF-8");
                        MqttMessage message = new MqttMessage(encodedPayload);
                        message.setQos(1);
                        message.setRetained(true);
                        Connection.getClient().publish(topic, message);
                        TSnackbar snackbar = TSnackbar.make((coordinatorLayout),node+ " succesfully reset"
                                ,TSnackbar.LENGTH_LONG);
                        View snackbarView = snackbar.getView();
                        snackbar.setIconLeft(R.drawable.ic_light_black, 24);
                                    snackbarView.setBackgroundColor(Color.parseColor("#FF4081"));
                        snackbar.show();
                    } catch (UnsupportedEncodingException | MqttException e) {
                        e.printStackTrace();
                    }
                } else {
                    TSnackbar snackbar = TSnackbar.make((coordinatorLayout),"You don't connect to server"
                            ,TSnackbar.LENGTH_LONG);
                    View snackbarView = snackbar.getView();
                                snackbarView.setBackgroundColor(Color.parseColor("#FF4081"));
                    snackbar.show();
                }
            }
        });
        builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.cancel();
            }
        });

        builder.show();

    }

    @Override
    public void onBackPressed() {

    int tabpos = tabLayout.getSelectedTabPosition();
        Log.d("DEBUG", "onBackPressed: "+tabpos);

    if (tabpos==2){
        mViewPager.setCurrentItem(1);
    }else if (tabpos==1){
        mViewPager.setCurrentItem(0);
    }else {
        if (backButtonCount >= 1) {
            Intent intent = new Intent(Intent.ACTION_MAIN);
            intent.addCategory(Intent.CATEGORY_HOME);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
            finish();
            System.exit(0);
        } else {
            TSnackbar snackbar = TSnackbar.make((coordinatorLayout), R.string.backbutton
                    ,TSnackbar.LENGTH_LONG);
            View snackbarView = snackbar.getView();
            snackbar.setIconRight(R.drawable.olmatixsmall, 24);
                        snackbarView.setBackgroundColor(Color.parseColor("#FF4081"));
            snackbar.show();

            backButtonCount++;
        }
    }
    }

}
