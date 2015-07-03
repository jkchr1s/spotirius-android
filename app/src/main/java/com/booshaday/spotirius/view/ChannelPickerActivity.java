package com.booshaday.spotirius.view;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.booshaday.spotirius.R;
import com.booshaday.spotirius.data.Channel;
import com.booshaday.spotirius.data.Constants;

import java.util.ArrayList;

/**
 * Created by chris on 3/14/15.
 */
public class ChannelPickerActivity extends Activity {
    private ListView mListView;
    private ArrayList<String> mChannels;
//    private ArrayAdapter<String> mListViewAdapter;
    private ChannelsAdapter mListViewAdapter;

    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_channel_picker);

        Bundle bundle = this.getIntent().getExtras();
        ArrayList<String> items = bundle.getStringArrayList("descriptions");
        mChannels = bundle.getStringArrayList("channels");

        mListView = (ListView) findViewById(R.id.channel_picker_listview);
//        mListViewAdapter = new ArrayAdapter<>(this, R.layout.channel_picker_row, items);
        mListViewAdapter = new ChannelsAdapter(this, items);


        mListView.setAdapter(mListViewAdapter);

        mListView.setClickable(true);
        mListView.setOnItemClickListener(new ListView.OnItemClickListener() {

            @Override
            public void onItemClick(AdapterView<?> arg0, View arg1, int position, long arg3) {
                Intent intent = new Intent();
                setResult(Integer.valueOf(mChannels.get(position)), intent);
                finish();
            }
        });
    }

    private class ChannelsAdapter extends ArrayAdapter<String> {
        public ChannelsAdapter(Context context, ArrayList<String> channels) {
            super(context, 0, channels);
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            // Get the data item for this position
            String channel = getItem(position);
            // Check if an existing view is being reused, otherwise inflate the view
            if (convertView == null) {
                convertView = LayoutInflater.from(getContext()).inflate(R.layout.channel_row, parent, false);
            }
            // Lookup view for data population
            TextView title = (TextView) convertView.findViewById(R.id.item_title);
            TextView desc = (TextView) convertView.findViewById(R.id.item_desc);
            // Populate the data into the template view using the data object
            desc.setText("Found on DogStarRadio.com");
            title.setText(channel);
            // Return the completed view to render on screen
            return convertView;
        }
    }
}
