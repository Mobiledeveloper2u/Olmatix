package com.olmatix.adapter;

import android.content.Context;
import android.location.Address;
import android.location.Geocoder;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.olmatix.database.dbNodeRepo;
import com.olmatix.helper.ItemTouchHelperAdapter;
import com.olmatix.helper.OnStartDragListener;
import com.olmatix.helper.PreferenceHelper;
import com.olmatix.lesjaw.olmatix.R;
import com.olmatix.model.DurationModel;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Created by Rahman on 12/27/2016.
 */

public class InfoAdapter extends RecyclerView.Adapter<InfoAdapter.ViewHolder> implements ItemTouchHelperAdapter {

    private final OnStartDragListener mDragStartListener;
    public static final int mLOCATION = 1;
    private static dbNodeRepo DbNodeRepo;
    private static ArrayList<DurationModel> data ;
    private int[] mDataSetTypes;
    Context context;
    String loc = null;
    String distance;
    private String z;


    public InfoAdapter(String distance, int[] mDataTypes, Context context, OnStartDragListener mDragStartListener) {
        this.context = context;
        this.mDataSetTypes = mDataTypes;
        this.mDragStartListener = mDragStartListener;
        this.distance = distance;
    }

    @Override
    public int getItemViewType(int viewType) {
        return mDataSetTypes[viewType];
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup viewGroup, int viewType) {
        View v;

        if (viewType == mLOCATION) {

            v = LayoutInflater.from(viewGroup.getContext())
                    .inflate(R.layout.frag_info_location, viewGroup, false);
            v.setMinimumWidth(viewGroup.getMeasuredWidth());
            v.setLayoutParams(new RecyclerView.LayoutParams(RecyclerView.LayoutParams.MATCH_PARENT,
                    RecyclerView.LayoutParams.WRAP_CONTENT));

            return new LocationInfoHolder(v);
        }

        return null;
    }

    @Override
    public void onBindViewHolder(ViewHolder viewHolder, int position) {

        if (viewHolder.getItemViewType() == mLOCATION) {
            final LocationInfoHolder holder = (LocationInfoHolder) viewHolder;
            PreferenceHelper mPrefHelper = new PreferenceHelper(context.getApplicationContext());
            double mLat = mPrefHelper.getHomeLatitude();
            double mLong = mPrefHelper.getHomeLongitude();
            Geocoder geocoder = new Geocoder(context, Locale.getDefault());
            String adString = "";

            List<Address> list;
            try {
                list = geocoder.getFromLocation(mPrefHelper.getHomeLatitude(), mPrefHelper.getHomeLongitude(), 1);
                if (list != null && list.size() > 0) {
                    Address address = list.get(0);
                    loc = address.getLocality();
                    if (address.getAddressLine(0) != null)
                        adString = ", " + address.getAddressLine(0);
                }
            } catch (IOException e) {
                Log.e("DEBUG", "LOCATION ERR:" + e.getMessage());
            }

            if (loc==null){
                holder.location.setText("No location set");
                holder.distance.setText("No location found");
            } else {
                holder.location.setText("at " + loc + adString + " | " + String.valueOf((Double) mLat) + " : " + String.valueOf((Double) mLong));
                if (distance==null) {
                    holder.distance.setText("you are at unknown from home, waiting update");
                } else {
                    holder.distance.setText("you are at " + distance + " from home");
                }
            }
            holder.location.setSelected(true);
        }
    }


    @Override
    public int getItemCount() {
        return mDataSetTypes.length;
    }

    @Override
    public boolean onItemMove(int fromPosition, int toPosition) {
        //Collections.swap(.length, fromPosition, toPosition);
        notifyItemMoved(fromPosition, toPosition);
        return true;
    }

    @Override
    public void onItemDismiss(int position) {

    }

    public void notifyDistance(String Dist) {
        this.distance = Dist;
        notifyDataSetChanged();
    }

    public class ViewHolder extends RecyclerView.ViewHolder {
        public ViewHolder(View v) {
            super(v);
        }
    }

    public class ButtonInfoHolder extends ViewHolder {

        public ButtonInfoHolder(View view) {
            super(view);


        }
    }

    public class LocationInfoHolder extends ViewHolder {
        public TextView location, distance;
        public ImageView imgNodes;

        public LocationInfoHolder(View view) {
            super(view);
            location = (TextView) view.findViewById(R.id.location);
            distance = (TextView) view.findViewById(R.id.distance);

        }
    }

}
