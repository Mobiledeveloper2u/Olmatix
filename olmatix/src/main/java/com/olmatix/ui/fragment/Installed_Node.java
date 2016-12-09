package com.olmatix.ui.fragment;

/**
 * Created by Lesjaw on 05/12/2016.
 */

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.Fragment;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.helper.ItemTouchHelper;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.olmatix.adapter.OlmatixAdapter;
import com.olmatix.database.dbNodeRepo;
import com.olmatix.helper.OnStartDragListener;
import com.olmatix.helper.SimpleItemTouchHelperCallback;
import com.olmatix.lesjaw.olmatix.R;
import com.olmatix.model.Installed_NodeModel;
import com.olmatix.service.OlmatixService;
import com.olmatix.utils.Connection;

import org.eclipse.paho.client.mqttv3.IMqttActionListener;
import org.eclipse.paho.client.mqttv3.IMqttToken;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.w3c.dom.Node;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;


public class Installed_Node extends Fragment implements OnStartDragListener {

    private View mView;
    private List<Installed_NodeModel> nodeList = new ArrayList<>();
    private RecyclerView mRecycleView;
    private FloatingActionButton mFab;
    private AlertDialog.Builder alertDialog;
    private View view;
    private static OlmatixAdapter adapter;
    private TextView etTopic,version;
    ImageView icon_node;
    private RecyclerView.LayoutManager layoutManager;
    private static ArrayList<Installed_NodeModel> data;
    private Paint p = new Paint();
    private ItemTouchHelper mItemTouchHelper;
    HashMap<String,String> messageReceive = new HashMap<>();
    public static dbNodeRepo dbNodeRepo;
    private Installed_NodeModel installedNodeModel;
    private String inputResult;
    private String NodeID;
    private  String mMessage;
    private String NodeSplit;
    int flag =0;
    int flagNodeAdd =0;

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        mView = inflater.inflate(R.layout.frag_installed_node, container, false);
        return mView;
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        data = new ArrayList<>();
        dbNodeRepo = new dbNodeRepo(getActivity());
        installedNodeModel = new Installed_NodeModel();
        initDialog();
        setupView();

    onClickListener();
    }


    private void onClickListener() {
        mFab.setOnClickListener(mFabClickListener());
    }

    private BroadcastReceiver mMessageReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {

            String device = intent.getStringExtra("MQTT devices");
            String message = intent.getStringExtra("MQTT message");
            Log.d("receiver", " = " + device + " : "+ message);
            NodeSplit = device;
            String[] outputDevices = NodeSplit.split("/");
            NodeID = outputDevices[1];
            mMessage = message;
            device = device.substring(device.indexOf("$")+1,device.length());
            messageReceive.put(device,message);
            if (flagNodeAdd==1) {
                addCheckValidation();
            }
            if(flagNodeAdd == 0 ) {
                saveandpersist();
            }
        }

    };

    private void addCheckValidation(){
        if(messageReceive.containsKey("online")){
            Log.d("addCheckValid 1", "Passed");
            if (inputResult.equals(NodeID)){
                Log.d("addCheckValid 2", "Passed");
                if (mMessage.equals("true")){
                    Log.d("addCheckValid 3", "Passed");
                    Log.d("Running = ", "saveIfOnline");
                    saveIfOnline();

                }
            }
        }

    }

    private void saveIfOnline() {

        if (messageReceive.containsKey("online")) {

            for (int i = 0; i < dbNodeRepo.getNodeList().size(); i++) {
                if (data.get(i).getNodesID().equals(NodeID)) {
                    Toast.makeText(getActivity(), "You already have this Node ID", Toast.LENGTH_LONG).show();
                    flag = 1;
                    Log.d("saveIfOnline", "You already have this Node, DB = " + i + " flag = " + flag);
                }
            }

            if (flag != 1) {
                installedNodeModel.setNodesID(NodeID);
                installedNodeModel.setOnline(messageReceive.get("online"));


                dbNodeRepo.insertDb(installedNodeModel);
                flagNodeAdd = 0;
                Toast.makeText(getActivity(), "Add Node Successfully", Toast.LENGTH_LONG).show();
                Log.d("saveIfOnline", "Add Node success, " + " flag = " + flag);
                messageReceive.clear();
                doSubcribeIfOnline();

            }

        }

    }

    private void doSubcribeIfOnline(){
        String topic = "devices/" + inputResult + "/#";
        int qos = 1;
        try {
            IMqttToken subToken = Connection.getClient().subscribe(topic, qos);
            subToken.setActionCallback(new IMqttActionListener() {
                @Override
                public void onSuccess(IMqttToken asyncActionToken) {
                    messageReceive.put("NodeId",inputResult);
                }

                @Override
                public void onFailure(IMqttToken asyncActionToken, Throwable exception) {
                }
            });
        } catch (MqttException e) {
            e.printStackTrace();
        }

    }

    private void saveandpersist() {


        installedNodeModel.setNodesID(NodeID);

        if (messageReceive.get("nodes") != null) {
            installedNodeModel.setNodes(messageReceive.get("nodes"));
        }
        if (messageReceive.get("name") != null) {
            installedNodeModel.setName(messageReceive.get("name"));
        }
        if (messageReceive.get("localip") != null) {
            installedNodeModel.setLocalip(messageReceive.get("localip"));
        }
        if (messageReceive.get("fwname") != null) {
            installedNodeModel.setName(messageReceive.get("fwname"));
            String mFwName =  messageReceive.get("fwname");
            Log.d("SaveAndPersist", "Executed by = " +mFwName);
        }
        if (messageReceive.get("fwversion") != null) {
            installedNodeModel.setFwVersion(messageReceive.get("fwversion"));
        }
        if (messageReceive.get("online") != null) {
            installedNodeModel.setOnline(messageReceive.get("online"));
        }
        if (messageReceive.get("signal") != null) {
            installedNodeModel.setSignal(messageReceive.get("signal"));
        }
        if (messageReceive.get("uptime") != null) {
            installedNodeModel.setUptime(messageReceive.get("uptime"));
        }
        if (messageReceive.get("reset") != null) {
            installedNodeModel.setReset(messageReceive.get("reset"));
        }
        if (messageReceive.get("ota") != null) {
            installedNodeModel.setOta(messageReceive.get("ota"));
        }

        String mNodeid = installedNodeModel.getNodesID();
        Log.d("SaveAndPersist", "Executed by = " +mNodeid);

        dbNodeRepo.update(installedNodeModel);
        adapter = new OlmatixAdapter(dbNodeRepo.getNodeList());
        mRecycleView.setAdapter(adapter);
        data.clear();
        data.addAll(dbNodeRepo.getNodeList());
        messageReceive.clear();

    }


    @Override
    public void onStart() {
        Intent i = new Intent(getActivity(), OlmatixService.class);
        getActivity().startService(i);

        /*LocalBroadcastManager.getInstance(getActivity()).registerReceiver(
                mMessageReceiver, new IntentFilter("messageMQTT"));*/
        super.onStart();
    }

    @Override
    public void onPause() {
        LocalBroadcastManager.getInstance(getContext()).unregisterReceiver(mMessageReceiver);

        super.onPause();
    }

    @Override
    public void onResume() {
        LocalBroadcastManager.getInstance(getActivity()).registerReceiver(
                mMessageReceiver, new IntentFilter("messageMQTT"));
        super.onResume();
    }



        private View.OnClickListener mFabClickListener() {
        return new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                final EditText mEditText = new EditText(getContext());
                new AlertDialog.Builder(getContext())
                        .setTitle("Add Node")
                        .setMessage("Please type Olmatix product ID!")
                        .setView(mEditText)
                        .setPositiveButton("ADD", new DialogInterface.OnClickListener() {

                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                inputResult =mEditText.getText().toString();
                                String topic = "devices/" + inputResult + "/$online";
                                int qos = 1;
                                try {
                                    IMqttToken subToken = Connection.getClient().subscribe(topic, qos);
                                    subToken.setActionCallback(new IMqttActionListener() {
                                        @Override
                                        public void onSuccess(IMqttToken asyncActionToken) {
                                            messageReceive.put("NodeId",inputResult);
                                            flagNodeAdd = 1;
                                        }

                                        @Override
                                        public void onFailure(IMqttToken asyncActionToken, Throwable exception) {
                                        }
                                    });
                                } catch (MqttException e) {
                                    e.printStackTrace();
                                }
                            }
                        }).setNegativeButton("CANCEL", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {
                    }
                }).show();

            }
        };
    }

    private void setupView() {
        mRecycleView    = (RecyclerView) mView.findViewById(R.id.rv);
        mFab            = (FloatingActionButton) mView.findViewById(R.id.fab);

        mRecycleView.setHasFixedSize(true);

        layoutManager = new LinearLayoutManager(mView.getContext());
        mRecycleView.setLayoutManager(layoutManager);
        mRecycleView.setItemAnimator(new DefaultItemAnimator());


        data.addAll(dbNodeRepo.getNodeList());

        adapter = new OlmatixAdapter(dbNodeRepo.getNodeList());
        mRecycleView.setAdapter(adapter);

        initSwipe();

        ItemTouchHelper.Callback callback = new SimpleItemTouchHelperCallback(adapter);
        mItemTouchHelper = new ItemTouchHelper(callback);
        mItemTouchHelper.attachToRecyclerView(mRecycleView);



    }

    private void initDialog(){
        alertDialog = new AlertDialog.Builder(getActivity());
        LayoutInflater myLayout = LayoutInflater.from(getActivity());
        view = myLayout.inflate(R.layout.dialog_layout,null);
        alertDialog.setView(view);
        alertDialog.setPositiveButton("Save", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {

                adapter.notifyDataSetChanged();
                dialog.dismiss();

            }
        });
        etTopic = (TextView) view.findViewById(R.id.et_topic);
        version = (TextView) view.findViewById(R.id.version);
        icon_node = (ImageView) view.findViewById(R.id.icon_node);
    }



    private void initSwipe(){
        ItemTouchHelper.SimpleCallback simpleItemTouchCallback = new ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT | ItemTouchHelper.RIGHT) {

            @Override
            public boolean onMove(RecyclerView recyclerView, RecyclerView.ViewHolder viewHolder, RecyclerView.ViewHolder target) {
                return false;
            }

            @Override
            public void onSwiped(RecyclerView.ViewHolder viewHolder, int direction) {
                int position = viewHolder.getAdapterPosition();

                if (direction == ItemTouchHelper.LEFT){
                    adapter.removeItem(position);
                } else {
                    //removeView();
                    etTopic.setText(data.get(position).getName());
                    version.setText(data.get(position).getFwVersion());
                    icon_node.setImageResource(R.drawable.olmatixlogo);
                    alertDialog.show();

                }
            }

            @Override
            public void onChildDraw(Canvas c, RecyclerView recyclerView, RecyclerView.ViewHolder viewHolder, float dX, float dY, int actionState, boolean isCurrentlyActive) {

                Bitmap icon;
                if(actionState == ItemTouchHelper.ACTION_STATE_SWIPE){

                    View itemView = viewHolder.itemView;
                    float height = (float) itemView.getBottom() - (float) itemView.getTop();
                    float width = height / 3;

                    if(dX > 0){
                        p.setColor(Color.parseColor("#388E3C"));
                        RectF background = new RectF((float) itemView.getLeft(), (float) itemView.getTop(), dX,(float) itemView.getBottom());
                        c.drawRect(background,p);
                        icon = BitmapFactory.decodeResource(getResources(), R.drawable.ic_edit_white);
                        RectF icon_dest = new RectF((float) itemView.getLeft() + width ,(float) itemView.getTop() + width,(float) itemView.getLeft()+ 2*width,(float)itemView.getBottom() - width);
                        c.drawBitmap(icon,null,icon_dest,p);
                    } else {
                        p.setColor(Color.parseColor("#D32F2F"));
                        RectF background = new RectF((float) itemView.getRight() + dX, (float) itemView.getTop(),(float) itemView.getRight(), (float) itemView.getBottom());
                        c.drawRect(background,p);
                        icon = BitmapFactory.decodeResource(getResources(), R.drawable.ic_delete_white);
                        RectF icon_dest = new RectF((float) itemView.getRight() - 2*width ,(float) itemView.getTop() + width,(float) itemView.getRight() - width,(float)itemView.getBottom() - width);
                        c.drawBitmap(icon,null,icon_dest,p);
                    }
                }
                super.onChildDraw(c, recyclerView, viewHolder, dX, dY, actionState, isCurrentlyActive);
            }
        };
        ItemTouchHelper itemTouchHelper = new ItemTouchHelper(simpleItemTouchCallback);
        itemTouchHelper.attachToRecyclerView(mRecycleView);
    }

    @Override
    public void onStartDrag(RecyclerView.ViewHolder viewHolder) {
        mItemTouchHelper.startDrag(viewHolder);
    }

}
