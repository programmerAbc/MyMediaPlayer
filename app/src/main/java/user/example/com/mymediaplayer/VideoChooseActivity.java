package user.example.com.mymediaplayer;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.TextView;

import java.util.LinkedList;

public class VideoChooseActivity extends AppCompatActivity {

    private LinkedList<MovieInfo> mLinkedList;
    private LayoutInflater mLayoutInflater;
    View root;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.dialog);
        getWindow().requestFeature(Window.FEATURE_NO_TITLE);
        mLinkedList = VideoPlayerActivity.playerList;
        mLayoutInflater = LayoutInflater.from(this);
        ImageButton iButton = (ImageButton) findViewById(R.id.cancelButton);
        iButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                VideoChooseActivity.this.finish();
            }
        });
        ListView myListView = (ListView) findViewById(R.id.list);
        myListView.setAdapter(new BaseAdapter() {
            @Override
            public int getCount() {
                return mLinkedList.size();
            }

            @Override
            public Object getItem(int position) {
                return position;
            }

            @Override
            public long getItemId(int position) {
                return position;
            }

            @Override
            public View getView(int position, View convertView, ViewGroup parent) {
                if (convertView == null) {
                    convertView = mLayoutInflater.inflate(R.layout.list, null);
                }
                TextView text = (TextView) convertView.findViewById(R.id.text);
                text.setText(mLinkedList.get(position).displayName);
                return convertView;
            }
        });
        myListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                Intent intent = new Intent();
                intent.putExtra("CHOOSE", position);
                setResult(Activity.RESULT_OK, intent);
                finish();
            }
        });
    }
}
