package edu.purdue.zhan3050.cnit355final;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class TestPlaylist extends AppCompatActivity {

    private List<MusicInfo> musicInfosList;
    // position of current music
    private Bundle bundle;
    // MediaPlayer to play the music
    private ArrayList<String> songNames;
    private ListView MusicListView;
    private Long playlist_id;
    Button b;
    String play_order;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_test_playlist);
        bundle = new Bundle();
        bundle = getIntent().getExtras();
        try {
            musicInfosList = (List<MusicInfo>) bundle.getSerializable("musicinfo");
            songNames = bundle.getStringArrayList("songNames");
            playlist_id = (Long) bundle.getLong("id");

        } catch (Exception e) {
            System.out.println("failed to get position info");
            e.printStackTrace();
        }

        if(songNames.size() == 0){
            play_order =" 1";
        }
        else{
            play_order = Integer.toString(songNames.size() + 1);
        }
        MusicListView = findViewById(R.id.MusicListView);
        ArrayAdapter<String> a = new ArrayAdapter<String>(getApplicationContext(),android.R.layout.simple_list_item_1,songNames);
        MusicListView.setAdapter(a);
        MusicListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            /**
             * perform actions when item clicked
             */
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {

                String MusicData = musicInfosList.get(position).getData();
                System.out.println("THE MUSIC DATA IS " + MusicData);

                // send the position of the clicked item to MusicPlay, get MusicInfo and play music
                Bundle bundle = new Bundle();
                bundle.putInt("position", position);
                bundle.putSerializable("musicinfo", (Serializable) musicInfosList);
                Intent intent = new Intent();
                intent.putExtras(bundle);
                intent.setClass(TestPlaylist.this, MusicPlay.class);
                startActivity(intent);
            }
        });
        Button b = findViewById(R.id.button);
        b.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                bundle.putLong("id",playlist_id);
                bundle.putString("order",play_order);
                Intent intent = new Intent();
                intent.putExtras(bundle);

                intent.setClass(TestPlaylist.this, AddSong.class);
                startActivity(intent);

            }
        });

    }
}
