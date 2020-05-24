package edu.purdue.zhan3050.cnit355final;

import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.speech.RecognitionListener;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;
import android.util.Log;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;
import android.view.animation.AnimationUtils;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Timer;
import java.util.TimerTask;

import androidx.appcompat.app.AppCompatActivity;

/**
 * A class that handle the music play
 */

public class MusicPlay extends AppCompatActivity implements GestureDetector.OnGestureListener, BottomSheet.BottomSheetListener {

    // list for MusicInfo
    private List<MusicInfo> musicInfosList;
    // position of current music
    private int position;
    // Bundle to get parameter to start the activity
    private Bundle bundle;
    // MediaPlayer to play the music
    private MediaPlayer mediaPlayer = null;
    // current time of the music
    public static int currentTime;
    // duration of the music
    private int duration;
    // Self-defined handler objects
    private MyHandle handler;
    private MyHandle2 handle2;

    private TextView MusicName;
    private TextView MusicArtist;
    private ImageView MusicImage;


    Button buttonSleep;
    AudioManager audioManager;
    int timer;
    Boolean sleep = false;
    public long sleepPeriod = 0;

    // AudioControl class
    private AudioControl audioControl;

    // Music player service
    private MusicPlayerService playerService;
    private ServiceConnection serviceConnection;
    private Intent intent;
    private MusicReceiver musicReceiver;
    private MusicSwitcherReceiver switcherReceiver;

    private GestureDetector detector;
    //Speech Recognizer
    private SpeechRecognizer speechRecognizer;
    private Intent speechRecognizerIntent;
    private String keepter = "";

    // check if audio controller is clicked
    private static boolean AUDIO_STATE = false;

    Button btnEnableSpeech;
    //TELLS THE SPEECHRECOGNIZER WHEN TO START LISTENING
    Boolean listeningMode = false;


    @Override
    public void takeKeyEvents(boolean get) {
        super.takeKeyEvents(get);
    }

    @SuppressLint({"WrongViewCast", "ClickableViewAccessibility"})
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.music_detail_layout);
        setVolumeControlStream(AudioManager.STREAM_MUSIC);
        RelativeLayout back = findViewById(R.id.back2);


        bundle = new Bundle();
        bundle = getIntent().getExtras();
        try {
            musicInfosList = (List<MusicInfo>) bundle.getSerializable("musicinfo");
            position = bundle.getInt("position");
            /*
             * Check if the get the position info
             */
            System.out.println("position is " + position + "" +
                    "\n MUSICINFOLIST DATA IS  " + musicInfosList.get(position).getData());

        } catch (Exception e) {
            System.out.println("failed to get position info");
            e.printStackTrace();
        }
        btnEnableSpeech = findViewById(R.id.button2);

        detector = new GestureDetector(this.getApplicationContext(), (GestureDetector.OnGestureListener) this);

        init();
        /*
         * connect to MusicPlayerService
         */
        serviceConnection = new ServiceConnection() {
            @Override
            public void onServiceConnected(ComponentName name, IBinder service) {
                MusicPlayerService.MusicBind bind = (MusicPlayerService.MusicBind) service;
                /*
                 * sync the current music to MusicPlayerService
                 */
                playerService = bind.getService();
                playerService.setIndex(position);
                playerService.setMusicInfoList(musicInfosList);
                playerService.setMediaPlayer(mediaPlayer);
                System.out.println("MUSIC PLAY connected");
            }

            @Override
            public void onServiceDisconnected(ComponentName name) {
                playerService = null;
                System.out.println("MUSIC PLAY fail to connect");
            }
        };

        /**
         * bind service
         */
        intent = new Intent();
        intent.setClass(MusicPlay.this, MusicPlayerService.class);
        bindService(intent, serviceConnection, Service.BIND_AUTO_CREATE);

        /**
         * this broadcast receiver used to handle select music event
         */
        musicReceiver = new MusicReceiver();
        IntentFilter filter = new IntentFilter("MUSIC_BROADCAST");
        registerReceiver(musicReceiver, filter);

        /**
         * this broadcast receiver used to handle next/previous music event
         */
        switcherReceiver = new MusicSwitcherReceiver();
        IntentFilter filter1 = new IntentFilter("MUSIC_SWITCH");
        registerReceiver(switcherReceiver, filter1);

        /**
         * this timer is used to control SeekBar
         */
        handler = new MyHandle();
        new Timer().schedule(new TimerTask() {
            @Override
            public void run() {
                handler.sendEmptyMessage(0x111);
            }
        }, 100, 1000);

        /**
         * this timer is used to handle album image animation
         */
        handle2 = new MyHandle2();
        new Timer().schedule(new TimerTask() {
            @Override
            public void run() {
                handle2.sendEmptyMessage(0x112);
            }
        }, 0, 8000);
        audioControl.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                if (hasFocus) {
                    AUDIO_STATE = true;
                } else {
                    AUDIO_STATE = false;
                }
            }
        });
        audioControl.setFocusableInTouchMode(true);

        //Button for sleep mode
        buttonSleep = findViewById(R.id.btnSleep);
        //onclick listener for sleep mode
        buttonSleep.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (!sleep) {
                    BottomSheet bottomSheet = new BottomSheet();
                    bottomSheet.show(getSupportFragmentManager(), "BottomSheet");
                    sleep = true;
                    buttonSleep.setText("Sleeping");
                } else {
                    timer = 0;
                    sleep = false;
                    buttonSleep.setText("Sleep Mode");
                }
            }
        });

        speechRecognizer = speechRecognizer.createSpeechRecognizer(this);
        speechRecognizerIntent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        speechRecognizerIntent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        speechRecognizerIntent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault());

        speechRecognizer.setRecognitionListener(new RecognitionListener() {
            @Override
            public void onReadyForSpeech(Bundle params) {

            }

            @Override
            public void onBeginningOfSpeech() {

            }

            @Override
            public void onRmsChanged(float rmsdB) {

            }

            @Override
            public void onBufferReceived(byte[] buffer) {

            }

            @Override
            public void onEndOfSpeech() {

            }

            @Override
            public void onError(int error) {

            }

            @Override
            public void onResults(Bundle results) {
                ArrayList<String> matchesfound = results.getStringArrayList(speechRecognizer.RESULTS_RECOGNITION);
                if(matchesfound != null){
                    keepter = matchesfound.get(0);
                    Intent intent;
                    Log.i("SEE MEEEE", keepter);
                    switch (keepter){
                       //PLAYS SONG IF STOPPED
                        case "play":
                            if (AudioControl.isPause) {
                                AudioControl.isPause = false;
                                mediaPlayer.start();
                                Intent intentPlay = new Intent("MUSICPLAY_BROADCAST");
                                intentPlay.putExtra("AudioControl", "AudioControl");
                                sendBroadcast(intentPlay);
                            }
                            break;
                            //STOPS SONG IF PLAYING
                        case "pause":
                            if (!AudioControl.isPause) {
                                AudioControl.isPause = true;
                                mediaPlayer.pause();
                                Intent intentPlay = new Intent("MUSICPLAY_BROADCAST");
                                intentPlay.putExtra("AudioControl", "AudioControl");
                                sendBroadcast(intentPlay);
                            }


                            break;
                            //GOES TO NEXT SONG
                        case "next":
                             intent = new Intent();
                            intent.setAction("MUSIC_SWITCH");
                            intent.putExtra("NEXT", "NEXT");
                            intent.putExtra("PRE", "");
                            sendBroadcast(intent);
                            break;
                            //GOES TO PREVIOUS SONG
                        case "previous":
                            intent = new Intent();
                            intent.setAction("MUSIC_SWITCH");
                            intent.putExtra("NEXT", "");
                            intent.putExtra("PRE", "PRE");
                            sendBroadcast(intent);
                            break;
                    }

                }
            }

            @Override
            public void onPartialResults(Bundle partialResults) {
                Log.i("SEEE MEEE", "HEEEEY");
            }

            @Override
            public void onEvent(int eventType, Bundle params) {

            }
        });
        btnEnableSpeech.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(listeningMode){
                    listeningMode = false;
                    btnEnableSpeech.setText("Listening Mode: Disabled");
                }
                else {
                    listeningMode = true;
                    btnEnableSpeech.setText("Listening Mode: Enabled");
                }


            }
        });
        //HOLD THE SCREEN TO ACTIVATE SPEECH TO TEXT ONLY ACTIVATES IF LISTENINGMODE IS ENABLED
        back.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                switch (event.getAction()){
                    case MotionEvent.ACTION_DOWN:
                        if(listeningMode ){
                            speechRecognizer.startListening(speechRecognizerIntent);
                            keepter ="";
                        }

                        break;
                    case MotionEvent.ACTION_UP:
                        if(listeningMode ){
                            speechRecognizer.stopListening();
                        }
                        break;
                }
                return true;
            }
        });

    }

    private void init() {
        mediaPlayer = MediaPlayer.create(this, Uri.parse(musicInfosList.get(position).getData()));

        mediaPlayer.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
            @Override
            public void onCompletion(MediaPlayer mp) {
                int size = musicInfosList.size();
                position++;
                if (position == size) {
                    position = 0;
                }
                currentTime = 0;
                init();
                playerService.setIndex(position);
                playerService.setMusicInfoList(musicInfosList);
                playerService.setMediaPlayer(mediaPlayer);
            }
        });
        duration = musicInfosList.get(position).getDuration();

        MusicArtist = (TextView) findViewById(R.id.MusicArtist);
        MusicName = (TextView) findViewById(R.id.MusicName);
        MusicName.setText(musicInfosList.get(position).getTitle());
        MusicArtist.setText(musicInfosList.get(position).getArtist());

        MusicImage = (ImageView) findViewById(R.id.MusicImage);

        try {
            int imagePath = Integer.parseInt(musicInfosList.get(position).getAlbum_id());
            MusicImage.setImageResource(imagePath);
        } catch (Exception e) {
            Bitmap bt = BitmapFactory.decodeFile(musicInfosList.get(position).getAlbum_id());
            MusicImage.setImageBitmap(bt);
        }
        audioControl = (AudioControl) findViewById(R.id.AudioControlId);
        audioControl.setMediaPlayer(mediaPlayer);
        audioControl.invalidate();
        audioControl.requestLayout();
    }

    /**
     * slides right or left to change song
     */
    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (event.getAction() == MotionEvent.ACTION_MOVE && AUDIO_STATE) {
            audioControl.requestFocus();

            return true;
        }
        return detector.onTouchEvent(event);
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (mediaPlayer != null) {
            System.out.println("ONRESUME AND MEDIAPLAYER IS NOT NULL");
        }
    }

    /**
     * Gesture control
     */
    @Override
    public boolean onDown(MotionEvent e) {
        return false;
    }

    @Override
    public void onShowPress(MotionEvent e) {

    }

    @Override
    public boolean onSingleTapUp(MotionEvent e) {
        return false;
    }

    @Override
    public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY) {

        return false;
    }

    @Override
    public void onLongPress(MotionEvent e) {

    }

    @Override
    public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
        if (null == e1 || e2 == null) {
            System.out.println("MOTIONEVENT OBJECT IS NULL");
            return false;
        }
        if (e1.getX() - e2.getX() >= 300 && Math.abs(velocityX) >= 40) {
            Intent intent = new Intent();
            intent.setAction("MUSIC_SWITCH");
            intent.putExtra("NEXT", "NEXT");
            intent.putExtra("PRE", "");
            this.sendBroadcast(intent);
        } else if (e2.getX() - e1.getX() >= 300 && Math.abs(velocityX) >= 40) {
            Intent intent = new Intent();
            intent.setAction("MUSIC_SWITCH");
            intent.putExtra("NEXT", "");
            intent.putExtra("PRE", "PRE");
            this.sendBroadcast(intent);
        }
        return false;
    }

    public class MyHandle extends Handler {
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            if (msg.what == 0x111) {
                audioControl.setCurrentTime(mediaPlayer.getCurrentPosition());
            }
        }
    }

    public class MyHandle2 extends Handler {
        @Override
        public void handleMessage(Message msg) {
            if ((msg.what == 0x112)) {
                MusicImage.setAnimation(AnimationUtils.loadAnimation(MusicPlay.this, R.anim.image_rotate));

            }
        }
    }

    public class MusicReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {

            System.out.println(" MUSIC broadcast received");
            int tempTime = intent.getIntExtra("currentTime", 0);
            System.out.println("THE TEMPTIME IS " + tempTime);
            mediaPlayer.start();
            mediaPlayer.seekTo(tempTime);
            audioControl.setCurrentTime(mediaPlayer.getCurrentPosition());

        }
    }

    public class MusicSwitcherReceiver extends BroadcastReceiver {
        @TargetApi(Build.VERSION_CODES.O)
        @Override
        public void onReceive(Context context, Intent intent) {
            System.out.println("MUSICSWICTH broadcast received");
            if (!intent.getStringExtra("NEXT").isEmpty() && intent.getStringExtra("NEXT").equals("NEXT")) {
                mediaPlayer.seekTo(mediaPlayer.getDuration());
                if (AudioControl.isPause) {
                    AudioControl.isPause = false;
                    mediaPlayer.start();
                    Intent intentPlay = new Intent("MUSICPLAY_BROADCAST");
                    intentPlay.putExtra("AudioControl", "AudioControl");
                    sendBroadcast(intentPlay);
                }

            } else if (intent.getStringExtra("PRE").equals("PRE")) {
                position--;
                if (position < 0) {
                    position = musicInfosList.size() - 1;
                }
                currentTime = 0;
                init();

                AudioControl.isPause = false;
                playerService.setIndex(position);
                playerService.setMusicInfoList(musicInfosList);
                playerService.setMediaPlayer(mediaPlayer);
                Intent intentPlay = new Intent("MUSICPLAY_BROADCAST");
                intentPlay.putExtra("AudioControl", "AudioControl");
                sendBroadcast(intentPlay);
            }
        }
    }

    public void backToMain(View view) {
        finish();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unbindService(serviceConnection);
        unregisterReceiver(musicReceiver);
        unregisterReceiver(switcherReceiver);
    }

    /**
     * A method that start the sleep mode
     */
    public void startSleep() {
        timer = 15;
        audioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
        Thread thread = new Thread() {
            @Override
            public void run() {
                while (timer > 0) {
                    // update the music progress every 1 second
                    try {
                        Thread.sleep(1000);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    TimerTask repeatedTask = new TimerTask() {
                        public void run() {
                            System.out.println("Task performed on ");
                            audioManager.adjustStreamVolume(AudioManager.STREAM_MUSIC, AudioManager.ADJUST_LOWER, AudioManager.FLAG_SHOW_UI);
                            timer -= 1;
                        }
                    };
                    Timer timer = new Timer("Timer");

                    long delay = 5000L;
                    timer.scheduleAtFixedRate(repeatedTask, delay, sleepPeriod);
                }
                android.os.Process.killProcess(android.os.Process.myPid());
                sleep = false;
            }
        };
        thread.start();
    }

    @Override
    public void onBottonClicked(String Text) {
        sleepPeriod = Long.valueOf(Text);
        startSleep();
    }

}
