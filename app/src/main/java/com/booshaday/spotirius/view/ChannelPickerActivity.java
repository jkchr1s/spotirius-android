package com.booshaday.spotirius.view;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.Toast;

import com.booshaday.spotirius.R;
import com.booshaday.spotirius.data.Constants;

import java.util.ArrayList;

/**
 * Created by chris on 3/14/15.
 */
public class ChannelPickerActivity extends Activity {
    private ListView mListView;
    private ArrayList<String> mChannels;
    private ArrayAdapter<String> mListViewAdapter;

    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_channel_picker);

        Bundle bundle = this.getIntent().getExtras();
        ArrayList<String> items = bundle.getStringArrayList("descriptions");
        mChannels = bundle.getStringArrayList("channels");

        mListView = (ListView) findViewById(R.id.channel_picker_listview);
        mListViewAdapter = new ArrayAdapter<>(this, R.layout.channel_picker_row, items);
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
}
