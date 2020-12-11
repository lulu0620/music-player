package com.example.exp4;

import android.Manifest;
import android.content.ComponentName;
import android.content.ContentValues;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.media.MediaPlayer;
import android.os.Environment;
import android.os.IBinder;

import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import com.google.gson.Gson;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Timer;
import java.util.TimerTask;

public class PlayActivity extends AppCompatActivity implements View.OnClickListener {
    TextView tv_song, tv_end, tv_begin, tv_song_name, tv_artist;
    ImageButton ib_previous, ib_play, ib_next, ib_back, ib_guanzhu;
    SeekBar sb_progress;
    ImageView iv_disk;
    //RotateAnimation animation=new RotateAnimation(0,360,50,50);
    // Button bt_down;
    private String url1;
    private String song1;
    private String artist1;
    private String name1;
    private int id1;
    ArrayList<String> url_list = new ArrayList<String>();
    ArrayList<String> as_list = new ArrayList<String>();
    //private int id1;
    private MediaPlayer mediaPlayer = new MediaPlayer();//实例化mediaplayer
    private DownloadService.DownloadBinder downloadBinder;//服务与活动间的通信
    private ServiceConnection connection = new ServiceConnection() {//ServiceConnection匿名类，
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            downloadBinder = (DownloadService.DownloadBinder) service;//获取downloadBinder实例，用于在活动中调用服务提供的各种方法
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
        }
    };

    Timer timer;
    TimerTask timerTask;
    Boolean isChanged = false;
    private long currentPosition = 0;

   /* private DatabaseHelper dbHelper;*/
    private boolean click = true;//判断是否第一次点击关注
    // Gson gson=new Gson();
    String json_as_list, json_url_list;

    ImageButton ib_load;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_play);
        androidx.appcompat.app.ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.hide();
        }
        /*dbHelper = new DatabaseHelper(this, "LIKE.db", null, 1);*/
        //绑定监听事件的id
        tv_song = (TextView) findViewById(R.id.tv_song);
        tv_song_name = findViewById(R.id.song_name);
        tv_artist = findViewById(R.id.artist);
        tv_end = (TextView) findViewById(R.id.tv_end);
        tv_begin = (TextView) findViewById(R.id.tv_begin);
        ib_previous = (ImageButton) findViewById(R.id.ib_previous);
        ib_play = (ImageButton) findViewById(R.id.ib_play);
//        ib_play.setImageResource(R.drawable.play);
        ib_play.setBackgroundResource(R.drawable.play);
        ib_next = (ImageButton) findViewById(R.id.ib_next);
        ib_back = (ImageButton) findViewById(R.id.ib_back);



        sb_progress = (SeekBar) findViewById(R.id.sb_progress);
        ib_load = (ImageButton) findViewById(R.id.ib_load);
        iv_disk = (ImageView) findViewById(R.id.iv_disk);

        //设置点击事件
        ib_previous.setOnClickListener(this);
        ib_play.setOnClickListener(this);
        ib_next.setOnClickListener(this);
        ib_back.setOnClickListener(this);
        /*ib_guanzhu.setOnClickListener(this);*/
        ib_load.setOnClickListener(this);
        //seekebar的监听事件
        sb_progress.setOnSeekBarChangeListener(new MySeekBar());

        //得到下载链接
        Intent intent = this.getIntent();
        url1 = intent.getStringExtra("url");//得到下载链接
        name1 = url1.substring(url1.length() - 10);//得到音乐文件名
        song1 = intent.getStringExtra("song");//设置播放界面的音乐名
        artist1=intent.getStringExtra("artist");
        id1 = intent.getIntExtra("id", 0);
        url_list = intent.getStringArrayListExtra("url_list");
        as_list = intent.getStringArrayListExtra("as_list");
        //打印验证
        Log.e("PlayActivity", "url is " + url1);
        Log.e("PlayActivity", "as is " + song1);
        Log.e("PlayActivity", "name is " + name1);
        Gson gson = new Gson();
        json_as_list = gson.toJson(as_list);
        json_url_list = gson.toJson(url_list);
//        json_as_list = JSONObject.toJSONString(as_list);
//        json_url_list = JSONObject.toJSONString(url_list);
        Log.e("PlayActivity,", "as_list is " + json_as_list);
        Log.e("PlayActivity,", "url_list is " + json_url_list);
        //tv_song.setText(as1);
        InitData();

        Intent intent1 = new Intent(PlayActivity.this, DownloadService.class);
        startService(intent1);//启动服务
        bindService(intent1, connection, BIND_AUTO_CREATE);//绑定服务
        if (ContextCompat.checkSelfPermission(PlayActivity.this,
                Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(PlayActivity.this,
                    new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, 1);
        } else {
            initMediaPlayer();//初始化MediaPlayer
        }
//        if (ActivityCompat.checkSelfPermission(PlayActivity.this,
//                Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
//            ActivityCompat.requestPermissions(PlayActivity.this,
//                    new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, REQ);
//        } else {
//            initMediaPlayer();//初始化MediaPlayer
//        }


        tv_end.setText(formatime(mediaPlayer.getDuration()));//总时间
        timer = new Timer();
        timerTask = new TimerTask() {
            @Override
            public void run() {
                if (isChanged) {
                    return;
                }
                currentPosition = mediaPlayer.getCurrentPosition();
                sb_progress.setProgress(mediaPlayer.getCurrentPosition());//设置进度
                showCurrentTime();
            }
        };
        timer.schedule(timerTask, 0, 10);
    }

    @Override
    public void onClick(View v) {
        if (downloadBinder == null) {
            return;
        }
        File file = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), name1);//当前歌曲路径
//        File file = new File(getExternalFilesDir(null), name1);

        switch (v.getId()) {
            case R.id.ib_back:
                PlayActivity.this.finish();
                break;

            case R.id.ib_load:
                if (!file.exists()) {
                    downloadBinder.startDownload(url1);
                } else {
                    Toast.makeText(PlayActivity.this, "该歌曲已经下载！", Toast.LENGTH_SHORT).show();
                }
                break;
            case R.id.ib_play:
                if (file.exists()) {//文件存在，则可以播放
                    if (!mediaPlayer.isPlaying()) {
                        initMediaPlayer();//刷新界面
                        mediaPlayer.start();//开始播放
//                        ib_play.setImageResource(R.drawable.pause);
                        ib_play.setBackgroundResource(R.drawable.pause);
                        Toast.makeText(PlayActivity.this, "开始播放！", Toast.LENGTH_SHORT).show();
                    } else {
                        mediaPlayer.pause();//暂停播放
//                        ib_play.setImageResource(R.drawable.play);
                        ib_play.setBackgroundResource(R.drawable.play);

                        Toast.makeText(PlayActivity.this, "暂停播放！", Toast.LENGTH_SHORT).show();
                    }
                } else {//文件不存在，则提示
                    Toast.makeText(PlayActivity.this, "该歌曲未下载！", Toast.LENGTH_SHORT).show();
                }
                break;
            case R.id.ib_previous:
                mediaPlayer.reset();//音乐重置
//                ib_play.setImageResource(R.drawable.play);
                ib_play.setBackgroundResource(R.drawable.play);
                id1 = as_list.indexOf(song1);
                if (id1 == 0) {
                    song1 = as_list.get(as_list.size() - 1);
                    url1 = url_list.get(url_list.size() - 1);
                    name1 = url1.substring(120);//得到音乐文件名
                    Log.e("PlayActivity::", "url1 is " + url1);
                    Log.e("PlayActivity::", "as1 is " + song1);
                } else {
                    song1 = as_list.get(id1 - 1);
                    url1 = url_list.get(id1 - 1);
                    name1 = url1.substring(120);//得到音乐文件名
                    Log.e("PlayActivity::", "url1 is " + url1);
                    Log.e("PlayActivity::", "as1 is " + song1);

                }
                Log.e("PlayActivity:", "as1 is :" + song1);
                InitData();//修改UI界面的信息
                initMediaPlayer();//初始化MediaPlayer
                break;
            case R.id.ib_next:
                mediaPlayer.reset();
//                ib_play.setImageResource(R.drawable.play);
                ib_play.setBackgroundResource(R.drawable.play);

                id1 = as_list.indexOf(song1);
                if (id1 == as_list.size() - 1) {
                    song1 = as_list.get(0);
                    url1 = url_list.get(0);
                    name1 = url1.substring(url1.length() - 10);//得到音乐文件名
                    Log.e("PlayActivity::", "url1 is " + url1);
                    Log.e("PlayActivity::", "as1 is " + song1);
                } else {
                    song1 = as_list.get(id1 + 1);
                    url1 = url_list.get(id1 + 1);
                    name1 = url1.substring(url1.length() - 10);//得到音乐文件名
                    Log.e("PlayActivity::", "url1 is " + url1);
                    Log.e("PlayActivity::", "as1 is " + song1);
                }
                Log.e("PlayActivity:", "as1 is :" + song1);
                InitData();
                initMediaPlayer();//初始化MediaPlayer
                break;
        }
    }

    //设置UI界面的显示数据
    private void InitData() {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                tv_song.setText(song1);
                tv_song_name.setText(song1);
                tv_artist.setText(artist1);
                //tv_end.setText(formatime(mediaPlayer.getDuration()));
                tv_begin.setText("00:00");
            }
        });
    }

    //初始化音乐播放器
    private void initMediaPlayer() {
        try {
            //  File file=new File(Environment.getExternalStorageDirectory(),""+name+"");
            File file = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), name1);
//            File file = new File(getExternalFilesDir(null), name1);
            Log.d("MMM", file.getPath());
            mediaPlayer.setDataSource(file.getPath());//指定音频文件的路径
            mediaPlayer.prepare();//让mediaPlayer进入到准备状态

            sb_progress.setMax(mediaPlayer.getDuration());//设置进度条的最大值
            tv_end.setText(formatime(mediaPlayer.getDuration()));

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    //更新播放的时间
    private void showCurrentTime() {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                tv_begin.setText(formatime(currentPosition));
            }
        });
    }

    //时间转换类，将得到的音乐时间毫秒转换为时分秒格式
    private String formatime(long length) {
        Date date = new Date(length);
        SimpleDateFormat sdf = new SimpleDateFormat("mm:ss");
        String totaltime = sdf.format(date);
        return totaltime;
    }

    class MySeekBar implements SeekBar.OnSeekBarChangeListener {
        @Override
        public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {

        }

        @Override
        public void onStartTrackingTouch(SeekBar seekBar) {
            isChanged = true;
        }

        @Override
        public void onStopTrackingTouch(SeekBar seekBar) {
            mediaPlayer.seekTo(seekBar.getProgress());
            isChanged = false;
        }
    }

    //权限申请
    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        switch (requestCode) {
            case 1:
                if (grantResults.length > 0 && grantResults[0] != PackageManager.PERMISSION_GRANTED) {
                    Toast.makeText(PlayActivity.this, "拒绝权限将无法使用程序", Toast.LENGTH_SHORT).show();
                    finish();
                } else {
                    initMediaPlayer();
                }
                break;
            default:
        }
    }

    //若活动销毁则对服务进行解绑
    @Override
    public void onDestroy() {
        super.onDestroy();
        unbindService(connection);

        if (timerTask != null) {
            timerTask.cancel();
            timerTask = null;
        }
        if (mediaPlayer != null) {
            if (mediaPlayer.isPlaying()) {
                mediaPlayer.stop();
            }
            mediaPlayer.reset();
            mediaPlayer.release();
        }

    }
}

