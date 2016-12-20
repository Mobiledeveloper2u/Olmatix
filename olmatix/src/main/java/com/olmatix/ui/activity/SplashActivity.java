package com.olmatix.ui.activity;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v7.app.AppCompatActivity;

import com.olmatix.service.OlmatixService;

/**
 * Created by Lesjaw on 04/12/2016.
 */

public class SplashActivity extends AppCompatActivity {

    int flagReceiver =0;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(this);
        String mUserName = sharedPref.getString("user_name", "olmatix1");

        if (mUserName.equals("olmatix1") ) {
            Intent intent = new Intent(this, SettingsActivity.class);
            startActivity(intent);

        } else {
            if (flagReceiver == 0);
            {
                Intent i = new Intent(this, OlmatixService.class);
                i.putExtra("node_id", "true");
                startService(i);
                /*LocalBroadcastManager.getInstance(this).registerReceiver(
                        mMessageReceiver, new IntentFilter("MQTTStatus"));
                flagReceiver = 1;
                Log.d("Splash = ", "Starting OlmatixService");*/
                flagReceiver = 1;
            }
            Intent intent = new Intent(this, MainActivity.class);
            startActivity(intent);
        }

    }

    /*private BroadcastReceiver mMessageReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {

            String message = intent.getStringExtra("MQTT State");
            if (message==null){
                message = "false";

            } else if (message.equals("false")){
                Toast.makeText(getApplicationContext(),"No Internet connection, Olmatix closing the App", Toast.LENGTH_LONG).show();
                finish();

            } else if (message.equals("true")) {
                Intent i = new Intent(getApplication(), MainActivity.class);
                startActivity(i);
                LocalBroadcastManager.getInstance(getApplicationContext()).unregisterReceiver(mMessageReceiver);
                finish();
            }
        }
    };*/

    @Override
    protected void onStop() {
        super.onStop();

    }
}
