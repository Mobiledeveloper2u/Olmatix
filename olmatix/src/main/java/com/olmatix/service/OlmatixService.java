package com.olmatix.service;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Binder;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import android.widget.Toast;

import com.olmatix.database.dbNodeRepo;
import com.olmatix.lesjaw.olmatix.R;
import com.olmatix.model.Detail_NodeModel;
import com.olmatix.model.Installed_NodeModel;
import com.olmatix.ui.activity.MainActivity;
import com.olmatix.utils.Connection;

import org.eclipse.paho.android.service.MqttAndroidClient;
import org.eclipse.paho.client.mqttv3.IMqttActionListener;
import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.IMqttToken;
import org.eclipse.paho.client.mqttv3.MqttAsyncClient;
import org.eclipse.paho.client.mqttv3.MqttCallback;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.eclipse.paho.client.mqttv3.MqttSecurityException;

import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;


/**
 * Created              : Rahman on 12/2/2016.
 * Date Created         : 12/2/2016 / 4:29 PM.
 * ===================================================
 * Package              : com.olmatix.service.
 * Project Name         : Olmatix.
 * Copyright            : Copyright @ 2016 Indogamers.
 */
public class OlmatixService extends Service {


    private static String TAG = OlmatixService.class.getSimpleName();
    public final static String MY_ACTION = "MY_ACTION";

    private static boolean hasWifi = false;
    private static boolean hasMmobile = false;
    private Thread thread;
    private ConnectivityManager mConnMan;
    public volatile MqttAsyncClient mqttClient;
    private String deviceId;
    private String stateoffMqtt;
    public static dbNodeRepo dbNodeRepo;
    private Installed_NodeModel installedNodeModel;
    private Detail_NodeModel detailNodeModel;
    private static ArrayList<Installed_NodeModel> data;
    private String NodeID,Channel;
    private String mMessage;
    private NotificationManager mNM;
    private int NOTIFICATION = R.string.local_service_started;
    HashMap<String,String>  messageReceive = new HashMap<>();
    HashMap<String,String> message_topic = new HashMap<>();
    private String mNodeID;
    private String NodeIDSensor;
    private String TopicID;
    boolean flagAct=true;
    private String mChange="";

    /**
     * Class for clients to access.  Because we know this service always
     * runs in the same process as its clients, we don't need to deal with
     * IPC.
     */

    class OlmatixBroadcastReceiver extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {
            boolean hasConnectivity = false;
            boolean hasChanged = false;
            NetworkInfo nInfo = mConnMan.getActiveNetworkInfo();
            if (nInfo != null) {
                if (nInfo.getType() == ConnectivityManager.TYPE_WIFI) {
                    hasChanged = true;
                    hasWifi = nInfo.isConnected();
                } else if (nInfo.getType() == ConnectivityManager.TYPE_MOBILE) {
                    hasChanged = true;
                    hasMmobile = nInfo.isConnected();
                }
            } else {
                //Not Connected info
                String msg = getString(R.string.err_internet);
                Toast toast = Toast.makeText(context, msg, Toast.LENGTH_SHORT);
                toast.show();
                stateoffMqtt = "false";
                Log.d("Sender", "MQTT Status No Internet: " +stateoffMqtt);
                sendMessage();
            }

            hasConnectivity = hasMmobile || hasWifi;
            Log.d(TAG, "hasConn: " + hasConnectivity + " hasChange: " + hasChanged + " - " + (mqttClient == null || !mqttClient.isConnected()));
            if (hasConnectivity && hasChanged && (mqttClient == null || !mqttClient.isConnected())) {
                doConnect();

            } else if (!hasConnectivity && mqttClient != null && mqttClient.isConnected()) {
                doDisconnect();
            }
        }

    }

    private void doDisconnect() {
        IMqttToken token;
        Log.d(TAG, "doDisconnect()");
        try {
            token = mqttClient.disconnect();
            token.waitForCompletion(1000);
            stateoffMqtt = "false";
            sendMessage();
        } catch (MqttException e) {
            e.printStackTrace();
            Log.d(TAG, "onReceive: " + String.valueOf(e.getMessage()));
        }
    }

    public class OlmatixBinder extends Binder {
        public OlmatixService getService(){
            return OlmatixService.this;
        }
    }

    @Override
    public void onCreate() {

        IntentFilter intent = new IntentFilter();
        setClientID();
        intent.addAction(ConnectivityManager.CONNECTIVITY_ACTION);
        registerReceiver(new OlmatixBroadcastReceiver(), new IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION));
        mConnMan = (ConnectivityManager) getSystemService(CONNECTIVITY_SERVICE);
        mNM = (NotificationManager)getSystemService(NOTIFICATION_SERVICE);

        data = new ArrayList<>();
        dbNodeRepo = new dbNodeRepo(getApplicationContext());
        installedNodeModel = new Installed_NodeModel();
        detailNodeModel = new Detail_NodeModel();
        // Display a notification about us starting.  We put an icon in the status bar.
        showNotification();
    }
    private void showNotification() {
        // In this sample, we'll use the same text for the ticker and the expanded notification
        CharSequence text = getText(R.string.local_service_started);

        // The PendingIntent to launch our activity if the user selects this notification
        PendingIntent contentIntent = PendingIntent.getActivity(this, 0,
                new Intent(this, MainActivity.class), 0);

        // Set the info for the views that show in the notification panel.
        Notification notification = new Notification.Builder(this)
                .setSmallIcon(R.drawable.olmatixlogo)  // the status icon
                .setTicker(text)  // the status text
                .setWhen(System.currentTimeMillis())  // the time stamp
                .setContentTitle(getText(R.string.local_service_label))  // the label of the entry
                .setContentText(text)  // the contents of the entry
                .setContentIntent(contentIntent)  // The intent to send when the entry is clicked
                .setOngoing(true)
                .build();


        // Send the notification.
        mNM.notify(NOTIFICATION, notification);
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        Log.d(TAG, "onConfigurationChanged()");
        //android.os.Debug.waitForDebugger();
        super.onConfigurationChanged(newConfig);

    }

    private void setClientID() {
        // Context mContext;
        WifiManager wifiManager = (WifiManager) getSystemService(Context.WIFI_SERVICE);
        WifiInfo wInfo = wifiManager.getConnectionInfo();
        deviceId = "OlmatixApp-" + wInfo.getMacAddress();

        if (deviceId == null) {
            deviceId = MqttAsyncClient.generateClientId();
        }
    }

    private void doConnect() {
        Toast.makeText(getApplicationContext(), R.string.connecting, Toast.LENGTH_SHORT).show();

        SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(this);
        String mServerURL = sharedPref.getString("server_address", "cloud.olmatix.com");
        String mServerPort = sharedPref.getString("server_port", "1883");
        String mUserName = sharedPref.getString("user_name", "olmatix1");
        String mPassword = sharedPref.getString("password", "olmatix");

        final MqttConnectOptions options = new MqttConnectOptions();
        options.setUserName(mUserName);
        options.setPassword(mPassword.toCharArray());
        final MqttAndroidClient client = new MqttAndroidClient(getApplicationContext(),"tcp://"+mServerURL+":"+mServerPort,deviceId);
        options.setCleanSession(true);
        String topic = "status/"+deviceId+"/$online";
        byte[] payload = "false".getBytes();
        options.setWill(topic, payload ,1,true);
        options.setKeepAliveInterval(300);
        Connection.setClient(client);
        try {

            IMqttToken token = client.connect(options);
            token.setActionCallback(new IMqttActionListener() {

                @Override
                public void onSuccess(IMqttToken asyncActionToken) {
                    Toast.makeText(getApplicationContext(),  R.string.conn_success, Toast.LENGTH_SHORT).show();
                    flagAct = false;
                    if (flagAct==false) {
                        doSubAll();
                    }


                    Connection.getClient().setCallback(new MqttEventCallback());

                    try {
                        String topic = "status/"+deviceId+"/$online";
                        String payload = "true";
                        byte[] encodedPayload = new byte[0];
                        try {
                            encodedPayload = payload.getBytes("UTF-8");
                            MqttMessage message = new MqttMessage(encodedPayload);
                            message.setQos(1);
                            message.setRetained(true);
                            Connection.getClient().publish(topic, message);
                        }
                        catch (UnsupportedEncodingException | MqttException e)
                        {
                            e.printStackTrace();
                        }

                        Connection.getClient().subscribe("test", 0, getApplicationContext(), new IMqttActionListener() {
                            @Override
                            public void onSuccess(IMqttToken asyncActionToken) {
                                /*Intent intent = new Intent(getApplication(), MainActivity.class);
                                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                                startActivity(intent);*/
                                stateoffMqtt = "true";
                                Log.d("Sender", "MQTT Status after sub: " +stateoffMqtt);
                                sendMessage();
                            }

                            @Override
                            public void onFailure(IMqttToken asyncActionToken, Throwable exception) {
                                Toast.makeText(getApplicationContext(), R.string.sub_fail, Toast.LENGTH_SHORT).show();
                                Log.e("error",exception.toString());

                            }
                        });
                    } catch (MqttException e) {
                        e.printStackTrace();
                    }

                }

                @Override
                public void onFailure(IMqttToken asyncActionToken, Throwable exception) {
                    Toast.makeText(getApplicationContext(), R.string.conn_fail+exception.toString(), Toast.LENGTH_SHORT).show();
                    Log.e("mqtt",exception.toString());
                    stateoffMqtt = "false";
                    sendMessage();
                }
            });

        } catch (MqttSecurityException e) {
            e.printStackTrace();
        } catch (MqttException e) {
            switch (e.getReasonCode()) {
                case MqttException.REASON_CODE_BROKER_UNAVAILABLE:
                case MqttException.REASON_CODE_CLIENT_TIMEOUT:
                case MqttException.REASON_CODE_CONNECTION_LOST:
                case MqttException.REASON_CODE_SERVER_CONNECT_ERROR:
                    Log.v(TAG, "c" + e.getMessage());
                    Toast.makeText(getApplicationContext(), e.getMessage(), Toast.LENGTH_SHORT).show();
                    e.printStackTrace();
                    break;
                case MqttException.REASON_CODE_FAILED_AUTHENTICATION:
                    Intent i = new Intent("RAISEALLARM");
                    i.putExtra("ALLARM", e);
                    Log.e(TAG, "b" + e.getMessage());
                    Toast.makeText(getApplicationContext(), e.getMessage(), Toast.LENGTH_SHORT).show();
                    break;
                default:
                    Log.e(TAG, "a" + e.getMessage());
                    Toast.makeText(getApplicationContext(), e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        }
    }



    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(this);

        Log.v(TAG, "onStartCommand()");
        if (flagAct) {
            Toast.makeText(getApplicationContext(), R.string.service_start, Toast.LENGTH_SHORT).show();
            Log.d("Service = ", "Starting..");
            flagAct = false;
        }

        sendMessage();
        boolean mSwitch_Conn = sharedPref.getBoolean("switch_conn", true);
        Log.d("DEBUG", "SwitchConnPreff: " + mSwitch_Conn);

        return START_NOT_STICKY;
    }

    private void sendMessage() {
        Intent intent = new Intent("MQTTStatus");
        intent.putExtra("MQTT State", stateoffMqtt);
        intent.putExtra("NotifyChangeNode", mChange);

        LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
    }

    private void sendMessageDetail() {
        Intent intent = new Intent("MQTTStatusDetail");
        intent.putExtra("NotifyChangeDetail", mChange);

        LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
    }

    public String getThread(){
        return Long.valueOf(thread.getId()).toString();
    }

    @Override
    public void onDestroy() {
        // Cancel the persistent notification.
        // Tell the user we stopped.
        doDisconnect();
        Toast.makeText(this, R.string.service_stop, Toast.LENGTH_SHORT).show();
        messageReceive.clear();
        message_topic.clear();
        data.clear();
        super.onDestroy();
    }

    @Override
    public IBinder onBind(Intent intent) {
        Log.i(TAG, "onBind called");

        return null;
    }

    private class MqttEventCallback implements MqttCallback  {

        @Override
        public void connectionLost(Throwable cause) {

        }

        @Override
        public void messageArrived(String topic, final  MqttMessage message) throws Exception {
            Log.d("Receive MQTTMessage", " = " + topic + " message = " + message.toString());
            TopicID = topic;
            mNodeID = topic;
            mMessage = message.toString();
            String[] outputDevices = TopicID.split("/");
            NodeIDSensor = outputDevices[1];

            if (mNodeID.contains("$")) {
                addNode();
            } else if (mNodeID.contains("close")) {
                updateSensorDoor();
            } else if (mNodeID.contains("theft")) {
                updateSensorTheft();
            } else {
                updateDetail();
            }
        }
        @Override
        public void deliveryComplete(IMqttDeliveryToken token) {

        }
    }

    private void addNode (){
        String[] outputDevices = TopicID.split("/");
        NodeID = outputDevices[1];
        String  mNodeIdSplit = mNodeID;
        mNodeIdSplit = mNodeIdSplit.substring(mNodeIdSplit.indexOf("$")+1,mNodeIdSplit.length());
        messageReceive.put(mNodeIdSplit,mMessage);
        checkValidation();
    }

    private void checkValidation() {
        Log.d("messageReceive ", "= " + messageReceive);
        if (messageReceive.containsKey("online")) {
            Log.d("CheckValid online", "Passed");
            if (mMessage.equals("true")){
                Log.d("CheckValid online", " true Passed");
                saveFirst();

            } else {
                Toast.makeText(getApplicationContext(), R.string.deviceoffline, Toast.LENGTH_LONG).show();
                installedNodeModel.setNodesID(NodeID);
                if (dbNodeRepo.hasObject(installedNodeModel)) {

                    Toast.makeText(getApplicationContext(), "Updating status device : " + NodeID, Toast.LENGTH_LONG).show();
                    Log.d("Updating", "status device = " + NodeID);
                    statusDevices();

                } else {
                    doUnsubscribe();
                }
            }
        }
        saveDatabase();
    }

    private void toastAndNotif(){

        String nameNice = "";
        String state="";
        detailNodeModel.setNode_id(NodeID);
        detailNodeModel.setChannel(Channel);
        Log.d("DEBUG", "toastAndNotif: 5 "+NodeID+" / "+Channel+" / " +detailNodeModel.getNice_name_d());

        if (detailNodeModel.getNice_name_d().equals("")){
            nameNice = detailNodeModel.getName();
            Log.d("DEBUG", "toastAndNotif : 1");
        } else
            nameNice = detailNodeModel.getNice_name_d();
            Log.d("DEBUG", "toastAndNotif: 2");

        if (mNodeID.contains("light")) {
            if (mMessage.equals("true")){
                state = "ON";
                Log.d("DEBUG", "toastAndNotif: 3");
            }else
                state = "OFF";
            Log.d("DEBUG", "toastAndNotif: 4");
        }

        Log.d("DEBUG", "toastAndNotif: 5");

        Toast.makeText(getApplicationContext(), nameNice + state, Toast.LENGTH_LONG).show();
        messageReceive.clear();
        message_topic.clear();
        data.clear();
        Channel = "";

    }

    private void updateSensorDoor(){

        if (!mNodeID.contains("light")) {
            Log.d("DEBUG", "updateSensorDoor: ");
            detailNodeModel.setNode_id(NodeIDSensor);
            detailNodeModel.setChannel("0");
            detailNodeModel.setStatus_sensor(mMessage);
            Long currentDateTimeString = Calendar.getInstance().getTimeInMillis();
            detailNodeModel.setTimestamps(String.valueOf(currentDateTimeString));

            dbNodeRepo.update_detailSensor(detailNodeModel);
            mChange = "2";
            sendMessageDetail();
        }
    }

    private void updateSensorTheft() {
        if (!mNodeID.contains("light")) {
            Log.d("DEBUG", "updateSensorTheft: ");
            detailNodeModel.setNode_id(NodeIDSensor);
            detailNodeModel.setChannel("0");
            detailNodeModel.setStatus_theft(mMessage);
            Long currentDateTimeString = Calendar.getInstance().getTimeInMillis();
            detailNodeModel.setTimestamps(String.valueOf(currentDateTimeString));

            dbNodeRepo.update_detailSensor(detailNodeModel);
            mChange = "2";
            sendMessageDetail();
        }
    }

    private void updateDetail(){
        String[] outputDevices = TopicID.split("/");
        NodeID = outputDevices[1];
        Channel = outputDevices[3];
        message_topic.put(Channel, mMessage);
        saveDatabase_Detail();
        toastAndNotif();

    }

    private void saveFirst() {

            if (dbNodeRepo.getNodeList().isEmpty()) {
                installedNodeModel.setNodesID(NodeID);
                installedNodeModel.setNodes(messageReceive.get("online"));
                Calendar now = Calendar.getInstance();
                now.setTime(new Date());
                now.getTimeInMillis();
                installedNodeModel.setAdding(now.getTimeInMillis());

                dbNodeRepo.insertDb(installedNodeModel);
                Toast.makeText(getApplicationContext(), "Add Node Successfully", Toast.LENGTH_LONG).show();
                Log.d("saveFirst", "Add Node success, ");
                messageReceive.clear();
                data.clear();
                doSubscribeIfOnline();
                mChange="1";
                sendMessage();


            } else {
                installedNodeModel.setNodesID(NodeID);
                if (dbNodeRepo.hasObject(installedNodeModel)) {

                    if (flagAct) {
                        Toast.makeText(getApplicationContext(), "Checking this Node ID : " + NodeID + ", its exist, we are updating Node status", Toast.LENGTH_LONG).show();
                        flagAct = true;
                    }
                    //Log.d("saveFirst", "You already have this Node, DB = " + NodeID+", Exist, we are updating Node status");
                    saveDatabase();

                } else {
                    installedNodeModel.setNodesID(NodeID);
                    installedNodeModel.setNodes(messageReceive.get("online"));
                    Calendar now = Calendar.getInstance();
                    now.setTime(new Date());
                    now.getTimeInMillis();
                    installedNodeModel.setAdding(now.getTimeInMillis());

                    dbNodeRepo.insertDb(installedNodeModel);
                    Toast.makeText(getApplicationContext(), "Successfully Add Node", Toast.LENGTH_LONG).show();
                    Log.d("saveFirst", "Add Node success, ");
                    messageReceive.clear();
                    data.clear();
                    doSubscribeIfOnline();
                    mChange="1";
                    sendMessage();

                }
            }
    }

    private  void addNodeDetail() {
        Log.d("messageReceiveDetail ", "= " + message_topic);

        if(installedNodeModel.getFwName() != null) {
            Log.d("addNodeDetail", "fwname, "+installedNodeModel.getFwName());

            if (installedNodeModel.getFwName().equals("smartfitting")) {
                detailNodeModel.setNode_id(NodeID);
                detailNodeModel.setChannel("0");
                Log.d("addNodeDetail", "NodeID, "+NodeID + ", channel, "+Channel);

                if (dbNodeRepo.hasDetailObject(detailNodeModel)) {
                    saveDatabase_Detail();
                } else {
                    detailNodeModel.setNode_id(NodeID);
                    detailNodeModel.setChannel("0");
                    detailNodeModel.setStatus("false");

                    dbNodeRepo.insertInstalledNode(detailNodeModel);

                }

            } else if (installedNodeModel.getFwName().equals("smartadapter4ch")){
                detailNodeModel.setNode_id(NodeID);
                detailNodeModel.setChannel("0");
                if (dbNodeRepo.hasDetailObject(detailNodeModel)) {
                    saveDatabase_Detail();
                    }else {
                    for (int i = 0; i < 4; i++) {
                        String a = String.valueOf(i);

                        detailNodeModel.setNode_id(NodeID);
                        detailNodeModel.setChannel(String.valueOf(i));
                        detailNodeModel.setStatus("false");

                        dbNodeRepo.insertInstalledNode(detailNodeModel);
                    }
                }
            }else if (installedNodeModel.getFwName().equals("smartsensordoor")) {
                detailNodeModel.setNode_id(NodeID);
                detailNodeModel.setChannel("0");
                if (dbNodeRepo.hasDetailObject(detailNodeModel)) {
                   // saveDatabase_sensor();
                } else {
                    detailNodeModel.setNode_id(NodeID);
                    detailNodeModel.setChannel("0");
                    detailNodeModel.setSensor("close");
                    detailNodeModel.setStatus("false");
                    detailNodeModel.setStatus_sensor("false");
                    detailNodeModel.setStatus_theft("false");

                    dbNodeRepo.insertInstalledNode(detailNodeModel);

                }
            }
        }

    }

    private void statusDevices(){
        installedNodeModel.setNodesID(NodeID);
        if (dbNodeRepo.hasObject(installedNodeModel)) {
            if (messageReceive.get("online") != null) {
                installedNodeModel.setOnline(messageReceive.get("online"));
            }
            dbNodeRepo.update(installedNodeModel);
            messageReceive.clear();
            data.clear();
            mChange="2";
            sendMessage();
        }

    }

    private void saveDatabase() {

                    installedNodeModel.setNodesID(NodeID);
                    installedNodeModel.setNodes(messageReceive.get("nodes"));
                    installedNodeModel.setName(messageReceive.get("name"));
                    installedNodeModel.setLocalip(messageReceive.get("localip"));
                    installedNodeModel.setFwName(messageReceive.get("fwname"));
                    installedNodeModel.setFwVersion(messageReceive.get("fwversion"));
                    if(installedNodeModel.getFwName() != null) {
                        addNodeDetail();
                    }
                    installedNodeModel.setOnline(messageReceive.get("online"));
                    installedNodeModel.setSignal(messageReceive.get("signal"));
                    installedNodeModel.setUptime(messageReceive.get("uptime"));
                    if(messageReceive.containsKey("uptime")) {
                        if (mMessage != null) {
                            installedNodeModel.setOnline("true");
                        }
                    }


                    Calendar now = Calendar.getInstance();
                    now.setTime(new Date());
                    now.getTimeInMillis();
                    //System.out.println("data " + now.getTimeInMillis());
                    installedNodeModel.setAdding(now.getTimeInMillis());

                dbNodeRepo.update(installedNodeModel);
                messageReceive.clear();
                data.clear();
        mChange="2";
        sendMessage();

    }

    private void saveDatabase_Detail() {

        if (!mNodeID.contains("door")) {
            Log.d("DEBUG", "saveDatabase_Detail: ");
            detailNodeModel.setNode_id(NodeID);
            detailNodeModel.setChannel(Channel);
            if (mMessage.equals("ON")) {
                mMessage = "true";
                detailNodeModel.setStatus(mMessage);
            } else if (mMessage.equals("OFF")) {
                mMessage = "false";
                detailNodeModel.setStatus(mMessage);
            } else {
                detailNodeModel.setStatus(mMessage);
            }

            Long currentDateTimeString = Calendar.getInstance().getTimeInMillis();
            detailNodeModel.setTimestamps(String.valueOf(currentDateTimeString));

            dbNodeRepo.update_detail(detailNodeModel);
            mChange = "2";
            sendMessageDetail();
        }
    }

    private void doSubscribeIfOnline(){
        String topic = "devices/" + NodeID + "/#";
        int qos = 2;
        try {
            IMqttToken subToken = Connection.getClient().subscribe(topic, qos);
            subToken.setActionCallback(new IMqttActionListener() {
                @Override
                public void onSuccess(IMqttToken asyncActionToken) {
                    Log.d("Subscribe", " device = " + NodeID);
                }

                @Override
                public void onFailure(IMqttToken asyncActionToken, Throwable exception) {
                }
            });
        } catch (MqttException e) {
            e.printStackTrace();
        }

    }

    private void doUnsubscribe(){
        String topic = "devices/"+NodeID+"/$online";
        try {
            Connection.getClient().unsubscribe(topic);
            Log.d("Unsubscribe", " device = " + NodeID);
        } catch (MqttException e) {
            e.printStackTrace();
        }
    }

    private void doSubAll() {
        flagAct=false;
        int countDB = dbNodeRepo.getNodeList().size();
        Log.d("DEBUG", "Count list: " + countDB);
        data.addAll(dbNodeRepo.getNodeList());

        if (countDB != 0) {

            for (int i = 0; i < countDB; i++) {
                final String mNodeID = data.get(i).getNodesID();
                Log.d("DEBUG", "Count list: " + mNodeID);
                String topic = "devices/" + mNodeID + "/#";
                int qos = 2;
                try {
                    IMqttToken subToken = Connection.getClient().subscribe(topic, qos);
                    subToken.setActionCallback(new IMqttActionListener() {
                        @Override
                        public void onSuccess(IMqttToken asyncActionToken) {
                            Log.d("Subscribe", " device = " + mNodeID);
                        }

                        @Override
                        public void onFailure(IMqttToken asyncActionToken, Throwable exception) {
                        }
                    });
                } catch (MqttException e) {
                    e.printStackTrace();
                }
            }
        }
        data.clear();
    }
}






