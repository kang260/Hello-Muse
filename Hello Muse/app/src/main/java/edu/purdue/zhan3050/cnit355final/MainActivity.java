package edu.purdue.zhan3050.cnit355final;

import android.Manifest;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.SimpleAdapter;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.app.ActivityCompat;
import androidx.fragment.app.DialogFragment;

import com.google.gson.Gson;

import org.jmusixmatch.MusixMatch;
import org.jmusixmatch.MusixMatchException;
import org.jmusixmatch.entity.lyrics.Lyrics;
import org.jmusixmatch.entity.track.Track;
import org.jmusixmatch.entity.track.TrackData;

import java.io.File;
import java.io.Serializable;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Music Player
 * Read all music from external storage and show all information of the audio file
 */
public class MainActivity extends AppCompatActivity implements playlistDialog.NoticeDialogListener, deleteDialog.deleteDialogListener, SearchDialog.SearchDialogListener {

    ConstraintLayout back;
    RelativeLayout back2;
    TextView musicname, musicsize;
    private List<String> playlist;
    // store the results that fetched from system database
    private Cursor mCursor;
    // store the information in mCursor into list with a hashMap
    private List<Map<String, String>> List_map;
    // ListView in layout
    private ListView MusicListView;
    // A ContentResolver that access to system database
    private ContentResolver contentResolver;
    // A list to store MusicInfo object
    private List<MusicInfo> musicInfos;
    // SimpleAdapter
    private SimpleAdapter simpleAdapter;
    // requestCode for permission
    private final static int STORGE_REQUEST = 1;
    // check if the music is playing
    private boolean isPlyer = false;
    // A intent to start new activity
    private Intent intent;
    //Boolean to check night mode
    boolean nightModeOn = false;
    //MUSIC API KEY
    private  final String api_key = "e6c5782fe8133664fea1ea421d9fc825";
   //MUSIC API OBJECT
    private MusixMatch m;
    // ARRAYLIST OF SONGNAMES IN AN PLAYLIST
    private  ArrayList<String> SongNames;

    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);
        back = findViewById(R.id.back);
        back2 = findViewById(R.id.back2);
        musicname = findViewById(R.id.MusicName);
        musicsize = findViewById(R.id.MusicSize);
        menu.add(0, 1, 0, "Night Mode");
        menu.add(0,2,1,"Create Playlist");
        menu.add(0,3,1,"Delete Playlist");
        menu.add(0,4,1,"Show Playlist");
        menu.add(0,5,1,"Search Online");
        menu.add(0,6,1,"Back to Main View");
        return true;
    }
    //SHOW CREATE PLAYLIST DIALOG BOX
    public void showAlert(){
        DialogFragment pDialog = new playlistDialog();
        pDialog.show(getSupportFragmentManager(),"alert");
    }
    //SHOW DELETE PLAYLIST DIALOG BOX
    public void showDelete(){
        DialogFragment pDialog = new deleteDialog();
        pDialog.show(getSupportFragmentManager(),"bad");
    }
    //SHOW SEARCH ONLINE DIALOG BOX
    public void showSearch(){
        DialogFragment pDialog = new SearchDialog();
        pDialog.show(getSupportFragmentManager(), "search");
    }


    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        switch (item.getItemId()) {
            case 1:
                if(!nightModeOn){
                    back.setBackgroundResource(R.drawable.sky);
                    //Code to change the second background
                    //back2.setBackgroundResource(R.drawable.sky);
                    nightModeOn = true;
                    return true;
                }else{
                    back.setBackgroundResource(R.drawable.music);
                    //Code to change the second background
                    //back2.setBackgroundResource(R.drawable.sky);
                    nightModeOn = false;
                    return true;
                }
            case 2:
                showAlert();
                break;

            case 3:
                showDelete();
                break;
            case 4:
                getPlaylists();
                break;
            case 5:
                showSearch();
                break;
            case 6:
                init();
                break;

        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onResume() {
        super.onResume();
    }

    @RequiresApi(api = Build.VERSION_CODES.M)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        MusicListView = (ListView) findViewById(R.id.MusicListView);
        int check = checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE);
        if (check != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, STORGE_REQUEST);
        } else {
            init();
        }

        // start intent
        intent = new Intent();
        intent.setClass(MainActivity.this, MusicPlayerService.class);

        startService(intent);

        m = new MusixMatch(api_key);

    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // when onDestroy stop the intent
        stopService(intent);
    }

    /**
     * initiate ListView
     */

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

        List_map = new ArrayList<Map<String, String>>();
        musicInfos = new ArrayList<>();




        for (int i = 0; i < mCursor.getCount(); i++) {
            Map<String, String> map = new HashMap<>();
            MusicInfo musicInfo = new MusicInfo();

            // move the cursor to the next
            mCursor.moveToNext();

            // store musicInfo into List<MusicInfo>中
            musicInfo.set_id(mCursor.getInt(0));
            musicInfo.setTitle(mCursor.getString(1));
            musicInfo.setAlbum(mCursor.getString(2));
            musicInfo.setArtist(mCursor.getString(3));
            musicInfo.setDuration(mCursor.getInt(4));
            musicInfo.setMusicName(mCursor.getString(5));
            musicInfo.setSize(mCursor.getInt(6));
            musicInfo.setData(mCursor.getString(7));
            // store data into List<Map<String ,String>>
            // get the music image
            String MusicImage = getAlbumImage(mCursor.getInt(8));
            // check if the music has a image
            if (MusicImage == null) {
                // if null, use a default image
                map.put("image", String.valueOf(R.mipmap.timg));
                musicInfo.setAlbum_id(String.valueOf(R.mipmap.timg));
            } else {
                // if not null set the music
                map.put("image", MusicImage);
                musicInfo.setAlbum_id(MusicImage);
            }
            // musicInfo.setAlbum_id(mCursor.getInt(8));
            musicInfos.add(musicInfo);


            map.put("name", mCursor.getString(5));
            // convert the size of the music from byte to MB
            Float size = (float) (mCursor.getInt(6) * 1.0 / 1024 / 1024);
            BigDecimal b = new BigDecimal(size);
            Float f1 = b.setScale(2, BigDecimal.ROUND_HALF_UP).floatValue();
            map.put("size", f1.toString() + "MB");
            List_map.add(map);





        }
        // instantiate SimpleAdapter
        simpleAdapter = new SimpleAdapter(this, List_map, R.layout.music_adapte_view,
                new String[]{"image", "name", "size"}, new int[]{R.id.MusicImage,
                R.id.MusicName, R.id.MusicSize});
        // set adapter for ListView
        MusicListView.setAdapter(simpleAdapter);
        // set onClick listener for MusicList item
        MusicListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            /**
             * perform actions when item clicked
             */
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {

                String MusicData = musicInfos.get(position).getData();
                System.out.println("THE MUSIC DATA IS " + MusicData);

                // send the position of the clicked item to MusicPlay, get MusicInfo and play music
                Bundle bundle = new Bundle();
                bundle.putInt("position", position);

                bundle.putSerializable("musicinfo", (Serializable) getMusicInfos());
                Intent intent = new Intent();
                intent.putExtras(bundle);
                intent.setClass(MainActivity.this, MusicPlay.class);
                startActivity(intent);
            }
        });




    }

    /**
     * get the image of the music
     */
    private String getAlbumImage(int album_id) {
        String UriAlbum = "content://media/external/audio/albums";
        String projection[] = new String[]{"album_art"};
        // use cursor to fetch album image
        Cursor cursor = contentResolver.query(Uri.parse(UriAlbum + File.separator + Integer.toString(album_id)),
                projection, null, null, null);
        String album = null;
        if (cursor.getCount() > 0 && cursor.getColumnCount() > 0) {
            cursor.moveToNext();
            album = cursor.getString(0);
        }
        // close cursor
        cursor.close();
        return album;
    }

    /**
     * result for permission request
     */
    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        switch (requestCode) {
            case STORGE_REQUEST:
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    // initiate the app
                    init();
                    System.out.println("Get permissions");
                } else {
                    System.out.println("Didn't get permissions");
                }
                break;
        }

    }

    // Method to get MusicInfo
    public List<MusicInfo> getMusicInfos() {
        return musicInfos;
    }


    //-- NEW STUFF BELOW--//

    //GETS THE LIST OF PLAYLISTS STORED ON DEVICE
    public void getPlaylists(){
        contentResolver = getContentResolver();
        Uri uri = MediaStore.Audio.Playlists.EXTERNAL_CONTENT_URI;
        final String id = MediaStore.Audio.Playlists._ID;
        final String name = MediaStore.Audio.Playlists.NAME;
        final String[] columns = { id,name };
        final String criteria = null;


       Cursor c =  contentResolver.query(uri, columns, criteria, null,
                name + " ASC");
        playlist = new ArrayList<String>();
        for (int i = 0; i < c.getCount(); i++) {

            c.moveToNext();
            playlist.add(c.getString(1));
        }
        //ADAPTER WITH PLAYLIST NAMES
        ArrayAdapter<String> a = new ArrayAdapter<String>(getApplicationContext(),android.R.layout.simple_list_item_1,playlist);

        MusicListView.setAdapter(a);
        //SETS ONCLCK TO OPEN PLAYLIST
        MusicListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            /**
             * perform actions when item clicked
             */
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                String playName = playlist.get(position);
                String ID = getPlayListId(playName);
                openPlaylist(ID);

            }
        });
    }
    //OPENS THE PLAYLIST TO VIEW SONGS
    private void openPlaylist(String Id){
        contentResolver = getContentResolver();
        Long ids = Long.parseLong(Id);

        //INFO NEEDED FOR EACH SONG TO PLAY
        String[] proj = {
                MediaStore.Audio.Playlists.Members.AUDIO_ID,
                MediaStore.Audio.Playlists.Members.TITLE, MediaStore.Audio.Playlists.Members.ALBUM,
                MediaStore.Audio.Playlists.Members.ARTIST, MediaStore.Audio.Playlists.Members.DURATION,
                MediaStore.Audio.Playlists.Members.DISPLAY_NAME, MediaStore.Audio.Playlists.Members.SIZE,
                MediaStore.Audio.Playlists.Members.DATA, MediaStore.Audio.Playlists.Members.ALBUM_ID

        };


        Cursor c = contentResolver.query(MediaStore.Audio.Playlists.Members.getContentUri("external",ids),
               proj, null, null, null);

            List<MusicInfo >PlaylistInfo = new ArrayList<>();
         SongNames = new ArrayList<String>();

         c.moveToFirst();
        for (int i = 0; i < c.getCount(); i++) {
            MusicInfo musicInfo = new MusicInfo();

            if(c.getCount() == 0){break;}

            musicInfo.set_id(c.getInt(0));
            musicInfo.setTitle(c.getString(1));
            musicInfo.setAlbum(c.getString(2));
            musicInfo.setArtist(c.getString(3));
            musicInfo.setDuration(c.getInt(4));
            musicInfo.setMusicName(c.getString(5));
            musicInfo.setSize(c.getInt(6));
            musicInfo.setData(c.getString(7));



            SongNames.add(c.getString(1));
            String MusicImage = null;
            try{
                MusicImage = getAlbumImage(mCursor.getInt(8));
            }
            catch (Exception e){
        }


            // check if the music has a image
            if (MusicImage == null) {
                // if null, use a default image

                musicInfo.setAlbum_id(String.valueOf(R.mipmap.timg));
            } else {
                // if not null set the music
                musicInfo.setAlbum_id(MusicImage);
            }
            PlaylistInfo.add(musicInfo);
           c.moveToNext();
        }

        //PREPARES NEW INTENT BY LOADING SONGNAMES AND SONG DATA
        Bundle bundle = new Bundle();
        bundle.putSerializable("musicinfo", (Serializable) PlaylistInfo );
        bundle.putStringArrayList("songNames",SongNames);
        bundle.putLong("id",ids);
        Intent intent = new Intent();
        intent.putExtras(bundle);
        intent.setClass(MainActivity.this, TestPlaylist.class);
        startActivity(intent);

    }

    //CREATES A NEW PLAYLIST USING URI
    public void createPlaylist(String playName){
        Uri uri = MediaStore.Audio.Playlists.EXTERNAL_CONTENT_URI;
        contentResolver = getContentResolver();;
        ContentValues values = new ContentValues(1);
        values.put(MediaStore.Audio.Playlists.NAME, playName);
        contentResolver.insert(uri,values);
    }

    //TRIGGERS WHEN YOU CLICK CREATE IN CREATE DIALOG BOX
    @Override
    public void onDialogPositiveClick( String name) {
        createPlaylist(name);
    }

    //DELETES SELECTED PLAYLIST
    private void deletePlaylist(String selectedplaylist)
    {
        String playlistid = getPlayListId(selectedplaylist);
        ContentResolver resolver = this.getContentResolver();
        String where = MediaStore.Audio.Playlists._ID + "=?";
        String[] whereVal = {playlistid};
        resolver.delete(MediaStore.Audio.Playlists.EXTERNAL_CONTENT_URI, where, whereVal);
        return ;
    }
    //GETS PLAYLIST ID FROM THE PLAYLIST NAME
    public String getPlayListId(String playlist )

    {

        //  read this record and get playlistid

        Uri newuri =MediaStore.Audio.Playlists.EXTERNAL_CONTENT_URI;

        final String playlistid = MediaStore.Audio.Playlists._ID;

        final String playlistname = MediaStore.Audio.Playlists.NAME;

        String where = MediaStore.Audio.Playlists.NAME + "=?";

        String[] whereVal = {playlist};

        String[] projection = {playlistid, playlistname};

        ContentResolver resolver = getContentResolver();

        Cursor record = resolver.query(newuri , projection, where, whereVal, null);

        int recordcount = record.getCount();

        String foundplaylistid = "";

        if (recordcount > 0)

        {
            record.moveToFirst();

            int idColumn = record.getColumnIndex(playlistid);

            foundplaylistid = record.getString(idColumn);

            record.close();
        }

        return foundplaylistid;
    }

    //DELETES PLAYLIST OCCURS WHEN YOU CLICK YES ON DELETE DIALOG
    @Override
    public void DeletePlaylistk(String Name) {
        deletePlaylist(Name);
    }


    //STARTS NETWORK ASYNC IN BACKGROUND TO GET DATA FROM API
    @Override
    public void searchArtist(String Name) {
        NetworkAsyncTask netWorkAsyncTask = new NetworkAsyncTask(MainActivity.this, Name);
        netWorkAsyncTask.execute();

    }
    //STARTS NETWORK ASYNC IN BACKGROUND TO GET DATA FROM API
    @Override
    public void searchSong(String Name, String songName) {
        NetworkAsyncTask netWorkAsyncTask = new NetworkAsyncTask(MainActivity.this, Name, songName);
        netWorkAsyncTask.execute();

    }
    public class NetworkAsyncTask extends AsyncTask<Integer, String, Integer> {

        Context mContext = null;
        String Name;
        MusixMatch m = new MusixMatch(api_key);
        String songName;
        //CONSTRUCTER USED FOR JUST SEARCHING ARTIST
        public NetworkAsyncTask(Context c, String a) {
            mContext= c; Name= a;
        }
        //CONSTRUCTER USED FOR JUST SEARCHING SONG
        public NetworkAsyncTask(Context c, String a, String b) {
            mContext= c; Name= a;
            songName = b;
        }

        @Override
        protected Integer doInBackground(Integer... integers) {

            //GETS SONG DATA AND LYRICS
            if(songName !=null){
                Track track = null;
                Lyrics l = null;
                TrackData data = null;
                final ArrayList<String> Song = new ArrayList<>();
                try {
                    track = m.getMatchingTrack(songName, Name);
                     data = track.getTrack();
                } catch (MusixMatchException e) {
                    e.printStackTrace();
                }

                try {
                    l = m.getLyrics(data.getTrackId());
                    Song.add(data.getAlbumName() + " - " + data.getTrackName() + "\n" + l.getLyricsBody());
                } catch (MusixMatchException e) {
                    e.printStackTrace();
                    Song.add("Could'nt find the song");
                }
                //UPDATES UI
                MainActivity.this.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        ArrayAdapter<String> a = new ArrayAdapter<String>(getApplicationContext(),android.R.layout.simple_list_item_1,Song);
                        MusicListView.setAdapter(a);
                        MusicListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                            /**
                             * perform actions when item clicked
                             */
                            @Override
                            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {

                            }
                        });
                    }
                });

            }
            //GETS A LIST OF SONGS FROM HE ARTIST
            else{
            //LIST OF TRACKS
            List<Track> tracks = null;
            try {
                tracks = m.searchTracks("", Name, "", 20, 20, true);
            } catch (MusixMatchException e) {
                e.printStackTrace();
            }
            final ArrayList<String> Songs = new ArrayList<>();
            for (Track trk : tracks) {
                TrackData trkData = trk.getTrack();

                Songs.add(trkData.getAlbumName() + " - " + trkData.getTrackName());
            }
            //UPDATES UI
            MainActivity.this.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    ArrayAdapter<String> a = new ArrayAdapter<String>(getApplicationContext(),android.R.layout.simple_list_item_1,Songs);
                    MusicListView.setAdapter(a);
                    MusicListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                        /**
                         * perform actions when item clicked
                         */
                        @Override
                        public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                            NetworkAsyncTask netWorkAsyncTask = new NetworkAsyncTask(MainActivity.this, Name, Songs.get(position));
                            netWorkAsyncTask.execute();
                        }
                    });
                }
            });
            }
            return null;
        }
    }
}

