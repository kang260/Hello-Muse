package edu.purdue.zhan3050.cnit355final;

import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.SimpleAdapter;
import android.widget.TextView;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class AddSong extends AppCompatActivity {


    TextView musicname, musicsize;
    // store the results that fetched from system database
    private Cursor mCursor;
    // store the information in mCursor into list with a hashMap
    private ListView MusicListView;
    // A ContentResolver that access to system database
    private ContentResolver contentResolver;
    // A list to store MusicInfo object

    private Bundle bundle;

    private  ArrayList<String> SongNames;
    // A list to store MusicInfo object
    private List<MusicInfo> musicInfos;
    private long playlist_id;
    private String play_order;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_song);
        MusicListView = (ListView) findViewById(R.id.MusicListView);
        init();
        bundle = new Bundle();
        bundle = getIntent().getExtras();
        try {
            playlist_id = (Long) bundle.getLong("id");
            play_order = bundle.getString("order");

        } catch (Exception e) {
            System.out.println("failed to get position info");
            e.printStackTrace();
        }
    }



    private void init() {
        // use ContentResolver connect to system database
        contentResolver = getContentResolver();

        // fetch music information from external storage using cursor
        mCursor = contentResolver.query(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                new String[]{MediaStore.Audio.Media._ID,
                        MediaStore.Audio.Media.TITLE, MediaStore.Audio.Media.ALBUM,
                        MediaStore.Audio.Media.ARTIST, MediaStore.Audio.Media.DURATION,
                        MediaStore.Audio.Media.DISPLAY_NAME, MediaStore.Audio.Media.SIZE,
                        MediaStore.Audio.Media.DATA, MediaStore.Audio.Media.ALBUM_ID}, null, null, null);




        musicInfos = new ArrayList<MusicInfo>();
        SongNames = new ArrayList<String>();
        for (int i = 0; i < mCursor.getCount(); i++) {
            MusicInfo musicInfo = new MusicInfo();

            // move the cursor to the next
            mCursor.moveToNext();
            musicInfo.set_id(mCursor.getInt(0));
            musicInfo.setTitle(mCursor.getString(1));
            SongNames.add(mCursor.getString(1));
            musicInfo.setArtist(mCursor.getString(3));
            musicInfos.add(musicInfo);


        }

        ArrayAdapter<String> a = new ArrayAdapter<String>(getApplicationContext(),android.R.layout.simple_list_item_1,SongNames);
        MusicListView.setAdapter(a);
        MusicListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            /**
             * perform actions when item clicked
             */
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                Uri newuri = MediaStore.Audio.Playlists.Members.getContentUri(
                        "external", playlist_id);
                contentResolver = getContentResolver();
                ContentValues values = new ContentValues();
                values.put(MediaStore.Audio.Playlists.Members.PLAY_ORDER, play_order);
                values.put(MediaStore.Audio.Playlists.Members.AUDIO_ID, musicInfos.get(position).get_id());
                values.put(MediaStore.Audio.Playlists.Members.PLAYLIST_ID,
                        playlist_id);
                contentResolver.insert(newuri, values);
                finish();
            }
        });




    }

}
