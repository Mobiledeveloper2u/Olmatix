package com.olmatix.adapter;

/**
 * Created by Lesjaw on 04/12/2016.
 */

import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.olmatix.helper.ItemTouchHelperAdapter;
import com.olmatix.model.Installed_NodeModel;
import com.olmatix.lesjaw.olmatix.R;
import com.olmatix.ui.fragment.Installed_Node;
import com.olmatix.utils.Connection;

import org.eclipse.paho.client.mqttv3.MqttException;

import java.util.Collections;
import java.util.List;

public class OlmatixAdapter extends RecyclerView.Adapter<OlmatixAdapter.OlmatixHolder>  implements ItemTouchHelperAdapter {

    List<Installed_NodeModel> nodeList;



    public class OlmatixHolder extends RecyclerView.ViewHolder {
        public TextView fwName, ipAddrs, upTime, siGnal, nodeid;
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
        }
    }

    public OlmatixAdapter(List<Installed_NodeModel> nodeList) {
        this.nodeList = nodeList;
    }



    @Override
    public OlmatixHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View itemView = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.frag_nodeitemlist, parent, false);

        return new OlmatixHolder(itemView);
    }

    @Override
    public void onBindViewHolder(OlmatixHolder holder, int position) {

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
            } else if (mInstalledNodeModel.getFwName().equals("smartadapter4ch")){
                holder.imgNode.setImageResource(R.mipmap.ic_adapter);
            }
        }
        holder.fwName.setText(mInstalledNodeModel.getFwName());
        holder.ipAddrs.setText("IP : "+mInstalledNodeModel.getLocalip());
        holder.siGnal.setText("Signal : "+mInstalledNodeModel.getSignal()+"%");
        long seconds = Long.parseLong(mInstalledNodeModel.getUptime());
        calculateTime(seconds);
        //Log.d("DEBUG", "onBindViewHolder: " + calculateTime(updSec));

        holder.upTime.setText("Uptime : "+calculateTime(seconds));
        holder.nodeid.setText(mInstalledNodeModel.getNodesID());
    }

    public static String calculateTime(long seconds) {
        Log.d("DEBUG", "calculateTime: " + seconds);
        long sec = seconds % 60;
        long minutes = seconds % 3600 / 60;
        long hours = seconds % 86400 / 3600;
        long days = seconds / 86400;
        String uptimeUpd;
        if(days!= 0){
            uptimeUpd = days + " D " + hours + " H";
        } else if (hours != 0){
            uptimeUpd = hours + " H " + minutes + " M";
        } else if (minutes != 0){
            uptimeUpd = minutes + " M " + sec + " s";
        } else {
            uptimeUpd = sec + " s" ;
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
        Installed_Node.dbNodeRepo.delete(nodeList.get(position).getNodesID());
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