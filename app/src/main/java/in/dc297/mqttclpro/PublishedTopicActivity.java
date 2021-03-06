package in.dc297.mqttclpro;

import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.os.IBinder;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.Toast;

import org.eclipse.paho.client.mqttv3.MqttTopic;

public class PublishedTopicActivity extends AppCompatActivity {

    private DBHelper db;
    private String topic;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_published_topic);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        db = new DBHelper(getApplicationContext());
        /*FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
                        .setAction("Action", null).show();
            }
        });*/
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        topic = null;
        Intent pubTopInt = getIntent();
        if(pubTopInt!=null){
            topic = pubTopInt.getStringExtra("topic");
        }

        if(topic==null || topic.equals("")) finish();

        setTitle("'"+topic+"' Published Messages");
        ListView pubTopsLV = (ListView)findViewById(R.id.pub_tops_lv);
        String[] from = new String[] { "message", "timestamp","display_topic","retained","status"};
        int[] to = new int[]{R.id.mmessage_tv,R.id.mtimestamp_tv,R.id.mtopic_tv,R.id.mretained_tv,R.id.mstatustv};
        final MessagesListAdapter pubmlvs = new MessagesListAdapter(getApplicationContext(),R.layout.messages_list_item,db.getMessages(topic,1),from,to,0);
        pubTopsLV.setAdapter(pubmlvs);

        pubTopsLV.setOnItemClickListener(
                new AdapterView.OnItemClickListener() {
                    @Override
                    public void onItemClick(AdapterView<?> adapterView, View view, int i, long l){
                        Cursor cursor = pubmlvs.getCursor();
                        cursor.moveToPosition(i);
                        final String topictopublish = cursor.getString(4);
                        final String message = cursor.getString(1);
                        final int qos =  cursor.getInt(5);
                        final boolean retained = cursor.getInt(6)==0?false:true;

                        bindService(new Intent(getApplicationContext(), MQTTService.class), new ServiceConnection() {
                            @Override
                            public void onServiceConnected(ComponentName name, IBinder service) {
                                MQTTService mqttService = ((MQTTService.LocalBinder<MQTTService>) service).getService();
                                if(mqttService.getConnectionStatus().equals(MQTTService.MQTTConnectionStatus.CONNECTED)){
                                    if(topictopublish==null || topictopublish.equals("")){
                                        Toast.makeText(getApplicationContext(),"Invalid topic value",Toast.LENGTH_SHORT).show();
                                        return;
                                    }
                                    if(message==null){
                                        Toast.makeText(getApplicationContext(),"Invalid message value",Toast.LENGTH_SHORT).show();
                                        return;
                                    }
                                    try{
                                        MqttTopic.validate(topictopublish,false);
                                    }
                                    catch(IllegalArgumentException ila){
                                        Toast.makeText(getApplicationContext(),"Invalid topic",Toast.LENGTH_SHORT).show();
                                        return;
                                    }
                                    catch(IllegalStateException ise){
                                        Toast.makeText(getApplicationContext(),"Invalid topic",Toast.LENGTH_SHORT).show();
                                        return;
                                    }
                                    db.addTopic(topictopublish,1,qos);
                                    long message_id = db.addMessage(topictopublish,message,1,qos,retained);

                                    mqttService.publishMessage(topictopublish, message, String.valueOf(qos),message_id,retained);
                                    pubmlvs.swapCursor(db.getMessages(topic,1));

                                }
                                else{
                                    Toast.makeText(getApplicationContext(),"Oops seems like we are not connected! Please wait for connection.",Toast.LENGTH_SHORT).show();
                                    mqttService.defineConnectionToBroker();
                                    mqttService.handleStart();
                                }
                                unbindService(this);
                            }

                            @Override
                            public void onServiceDisconnected(ComponentName name) {
                            }
                    },0);
                }
                }
        );

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_message_activity, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_delete) {
            db.deleteMessages(topic,1);
            finish();
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

}
