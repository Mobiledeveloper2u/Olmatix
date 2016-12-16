package com.olmatix.adapter;

/**
 * Created by Lesjaw on 04/12/2016.
 */

import android.support.v4.view.MotionEventCompat;
import android.support.v7.widget.RecyclerView;
import android.telecom.Call;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.olmatix.helper.ItemTouchHelperAdapter;
import com.olmatix.helper.OnStartDragListener;
import com.olmatix.lesjaw.olmatix.R;
import com.olmatix.model.Installed_NodeModel;
import com.olmatix.ui.fragment.Installed_Node;
import com.olmatix.utils.ClickListener;
import com.olmatix.utils.Connection;
import com.olmatix.utils.OlmatixUtils;

import org.eclipse.paho.client.mqttv3.MqttException;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.List;

public class OlmatixAdapter extends RecyclerView.Adapter<OlmatixAdapter.OlmatixHolder>  implements ItemTouchHelperAdapter
{

    List<Installed_NodeModel> nodeList;
    private final OnStartDragListener mDragStartListener;
    private ClickListener clicklistener = null;

    public class OlmatixHolder extends RecyclerView.ViewHolder implements View.OnClickListener {
        public TextView fwName, ipAddrs, upTime, siGnal, nodeid,lastAdd;
        public ImageView imgNode, imgStatus;

        public OlmatixHolder(View view) {
            super(view);
            imgNode     = (ImageView) view.findViewById(R.id.icon_node);
            imgStatus   = (ImageView) view.findViewById(R.id.icon_status);
            fwName    = (TextView) view.findViewById(R.id.fw_name);
            ipAddrs     = (TextView) view.findViewById(R.id.ipaddrs);
            siGnal      = (TextView) view.findViewById(R.id.signal);
            upTime      = (TextView) view.findViewById(R.id.uptime);
            nodeid      = (TextView) view.findViewById(R.id.nodeid);
            lastAdd     = (TextView) view.findViewById(R.id.latestAdd);

            view.setOnClickListener(this);
        }

        @Override
        public void onClick(View v) {
            if (clicklistener != null) {
                clicklistener.itemClicked(v, getAdapterPosition());
            }
        }
    }
    public void setClickListener(ClickListener clicklistener) {
        this.clicklistener = clicklistener;
    }


    public OlmatixAdapter(List<Installed_NodeModel> nodeList,OnStartDragListener dragStartListener) {

        this.nodeList = nodeList;
        mDragStartListener = dragStartListener;

    }

    @Override
    public OlmatixHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View itemView = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.frag_nodeitemlist, parent, false);

        return new OlmatixHolder(itemView);
    }

    @Override
    public void onBindViewHolder(final OlmatixHolder holder, int position) {

        final Installed_NodeModel mInstalledNodeModel = nodeList.get(position);
        if(mInstalledNodeModel.getOnline() != null) {
            if (mInstalledNodeModel.getOnline().equals("true")) {
                holder.imgStatus.setImageResource(R.drawable.ic_node_online);
            } else {
                holder.imgStatus.setImageResource(R.drawable.ic_node_offline);
            }
        }
        holder.imgNode.setImageResource(R.drawable.olmatixlogo);
        if(mInstalledNodeModel.getFwName() != null) {
            if (mInstalledNodeModel.getFwName().equals("smartfitting")) {
                holder.imgNode.setImageResource(R.mipmap.ic_light);
            } else if (mInstalledNodeModel.getFwName().equals("smartadapter4ch")) {
                holder.imgNode.setImageResource(R.mipmap.ic_adapter);
            } else if (mInstalledNodeModel.getFwName().equals("smartsensordoor")) {
                //holder.imgNode.setImageResource(R.mipmap.door);
            }
        }
        holder.imgStatus.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if (MotionEventCompat.getActionMasked(event) == MotionEvent.ACTION_DOWN) {
                    mDragStartListener.onStartDrag(holder);
                }
                return false;
            }
        });


        if(mInstalledNodeModel.getNice_name_n() != null) {
            holder.fwName.setText(mInstalledNodeModel.getNice_name_n());
        } else holder.fwName.setText(mInstalledNodeModel.getFwName());

        holder.ipAddrs.setText("IP : "+mInstalledNodeModel.getLocalip());
        holder.siGnal.setText("Signal : "+mInstalledNodeModel.getSignal()+"%");
        if (mInstalledNodeModel.getUptime()!=null) {
            long seconds = Long.parseLong(mInstalledNodeModel.getUptime());
            calculateTime(seconds);
            holder.upTime.setText("Uptime : " + calculateTime(seconds));

        }

        if(mInstalledNodeModel.getAdding() != null) {
            //String dateTimeAgo = timeAgo(Long.parseLong(mInstalledNodeModel.getAdding()));
            holder.nodeid.setText(mInstalledNodeModel.getNodesID());
            Long dateTimeAgo =  mInstalledNodeModel.getAdding();
            Calendar cal = Calendar.getInstance();
            cal.setTime(new Date(dateTimeAgo));
            cal.getTimeInMillis();
            holder.lastAdd.setText(OlmatixUtils.getTimeAgo(cal));
            System.out.println(OlmatixUtils.getTimeAgo(cal));
        }

    }

    private String timeAgo(long time_ago) {
        long cur_time = (Calendar.getInstance().getTimeInMillis()) / 1000;
        long time_elapsed = cur_time - time_ago;
        long seconds = time_elapsed;
        int minutes = Math.round(time_elapsed / 60);
        int hours = Math.round(time_elapsed / 3600);
        int days = Math.round(time_elapsed / 86400);
        int weeks = Math.round(time_elapsed / 604800);
        int months = Math.round(time_elapsed / 2600640);
        int years = Math.round(time_elapsed / 31207680);

        // Seconds
        if (seconds <= 60) {
            return "just now";
        }
        //Minutes
        else if (minutes <= 60) {
            if (minutes == 1) {
                return "one minute ago";
            } else {
                return minutes + " minutes ago";
            }
        }
        //Hours
        else if (hours <= 24) {
            if (hours == 1) {
                return "an hour ago";
            } else {
                return hours + " hrs ago";
            }
        }
        //Days
        else if (days <= 7) {
            if (days == 1) {
                return "yesterday";
            } else {
                return days + " days ago";
            }
        }
        //Weeks
        else if (weeks <= 4.3) {
            if (weeks == 1) {
                return "a week ago";
            } else {
                return weeks + " weeks ago";
            }
        }
        //Months
        else if (months <= 12) {
            if (months == 1) {
                return "a month ago";
            } else {
                return months + " months ago";
            }
        }
        //Years
        else {
            if (years == 1) {
                return "one year ago";
            } else {
                return years + " years ago";
            }
        }
    }

    public static String calculateTime(long seconds) {
        //Log.d("DEBUG", "calculateTime: " + seconds);
        long sec = seconds % 60;
        long minutes = seconds % 3600 / 60;
        long hours = seconds % 86400 / 3600;
        long days = seconds / 86400;
        String uptimeUpd;
        if(days!= 0){
            uptimeUpd = days + "d " + hours + "h";
        } else if (hours != 0){
            uptimeUpd = hours + "h " + minutes + "m";
        } else if (minutes != 0){
            uptimeUpd = minutes + "m " + sec + "s";
        } else {
            uptimeUpd = sec + "s" ;
        }


               // System.out.println(uptimeUpd);
        return uptimeUpd;
    }

    @Override
    public int getItemCount() {
        return nodeList.size();
    }

    public void removeItem(int position) {
        //Installed_Node.dbNodeRepo.delete("809ed5e0");
        String inputResult;
        Installed_Node.dbNodeRepo.deleteNode(nodeList.get(position).getNodesID());
        String topic = "devices/"+nodeList.get(position).getNodesID()+"/#";
        try {
            Connection.getClient().unsubscribe(topic);
        } catch (MqttException e) {
            e.printStackTrace();
        }
        String topic1 = "devices/"+nodeList.get(position).getNodesID()+"/$online";
        try {
            Connection.getClient().unsubscribe(topic1);
        } catch (MqttException e) {
            e.printStackTrace();
        }
        nodeList.remove(position);

        notifyItemRemoved(position);
        notifyItemRangeChanged(position, nodeList.size());

    }

    @Override
    public boolean onItemMove(int fromPosition, int toPosition) {
        Collections.swap(nodeList, fromPosition, toPosition);
        notifyItemMoved(fromPosition, toPosition);
        return true;
    }

    @Override
    public void onItemDismiss(int position) {

    }



}