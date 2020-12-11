package com.example.exp4;


import android.Manifest;
import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.os.Environment;
import android.os.IBinder;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.SimpleAdapter;
import android.widget.Toast;

import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.appcompat.app.AppCompatActivity;

import com.alibaba.fastjson.JSON;

import java.io.File;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Vector;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class MainActivity extends AppCompatActivity {
    ListView lv;
    EditText et_search;
    Button btn_search;
    ImageButton ib_back1;

    List<String> url_list = new Vector<String>();

    ArrayList<String> song_list = new ArrayList<>();
    ArrayList<String> artist_list = new ArrayList<>();
    ArrayList<Integer> id_list = new ArrayList<Integer>();
    File mFile;
    private String url1, song1,artist1;
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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        androidx.appcompat.app.ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.hide();
        }

        Intent intent1 = new Intent(MainActivity.this, DownloadService.class);
        startService(intent1);//启动服务
        Log.d("Main", "已启动服务");
        bindService(intent1, connection, BIND_AUTO_CREATE);//绑定服务
        Log.d("Main", "已绑定服务");
        if (ContextCompat.checkSelfPermission(MainActivity.this,
                Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(MainActivity.this,
                    new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, 1);
        }
        sendRequestWithOkHttp_search("薛之谦");
//        parseHTMLwithJSOUP();//将html数据解析出来并传到界面上
    }

    private void sendRequestWithOkHttp_search(String keywords) {
        new Thread(() -> {
            try {
                OkHttpClient client = new OkHttpClient();
                Request request = new Request.Builder()
                        .url("https://autumnfish.cn/search?keywords=" + keywords)
                        .build();
                Response response = client.newCall(request).execute();
                String responseData = Objects.requireNonNull(response.body()).string();
                Log.d("data is", responseData);
                SearchResult sr = JSON.parseObject(responseData, SearchResult.class);
                SearchResult.Result result = sr.getResult();
                ArrayList<SearchResult.Result.Song> songs = result.getSongs();
                url_list.clear();
                id_list.clear();
                song_list.clear();
                artist_list.clear();
                for (SearchResult.Result.Song song : songs) {
                    id_list.add(song.getId());
                    song_list.add(song.getName());
                    artist_list.add(song.getArtists().get(0).getName());
                }
                showResponse();
                sendRequestWithOkHttp_url2();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();

    }


    private void sendRequestWithOkHttp_url2() throws InterruptedException {

        for (int i = 0; i < id_list.size(); i++) {

            int id = id_list.get(i);
            UrlThread urlThread = new UrlThread(id);
            Thread thread = new Thread(urlThread);
            thread.start();
            thread.join();
            url_list.add(urlThread.getUrl());

        }

    }


    //显示在界面上
    private void showResponse() {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                lv = (ListView) findViewById(R.id.lv);
                List<Map<String, Object>> listItems = new ArrayList<>();
                for (int i = 0; i < song_list.size(); i++) {
                    //实例化Map对象
                    Map<String, Object> map = new HashMap<String, Object>();
                    map.put("songName", song_list.get(i));
                    map.put("artist", artist_list.get(i));
                    //将map对象添加到List集合
                    listItems.add(map);
                }
                SimpleAdapter adapter = new SimpleAdapter(MainActivity.this, listItems,
                        android.R.layout.simple_list_item_2, new String[]{"songName", "artist"},
                        new int[]{android.R.id.text1, android.R.id.text2});
                lv.setAdapter(adapter);

                //点击item事件
                lv.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                    @Override
                    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                        Log.d("EEE", url_list.size() + "" + id_list.size());
                        //得到当前歌曲的相关信息
                        if (url_list.size() < id_list.size()) {
                            Toast.makeText(MainActivity.this, "下载链接获取中…", Toast.LENGTH_SHORT).show();
                        } else
                            url1 = url_list.get(position);

                        if (url1 == null || url1.equals(""))
//                            url1 = "http://m7.music.126.net/20201208024246/4df5d8ce26de006d58e4b64c0e0fd10d/ymusic/obj/w5zDlMODwrDDiGjCn8Ky/3253341973/5943/72ba/4b1a/d6d37d98608e19974706ab5ef488aefb.mp3";
                            Toast.makeText(MainActivity.this, "下载链接获取s中…", Toast.LENGTH_SHORT).show();
//                        url1 = url_list.get(position);//得到歌曲下载链接
                        String name = url1.substring(url1.length() - 10);
//                        String name = url1;
                        // int id1=id_list.get(position);
                        song1 = song_list.get(position);//得到song
                        artist1=artist_list.get(position);
                        Log.d("MainActivity:", "url is " + url1 + "\nname is " + name);

                        //如果歌曲不存在，则先下载，如果存在，则直接跳转
                        if (downloadBinder == null) {
                            Log.d("Main", "未绑定");
                            return;
                        }
                        File file = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), name);
//                        File file = new File(getExternalFilesDir(null), name);
                        String path = file.getAbsolutePath();
                        Log.d("Main", file.getAbsolutePath());
                        if (!file.exists()) {
                            downloadBinder.startDownload(url1);//若音乐文件不存在，则进行下载
//                            downloadFile3(url1, name);
//                            downLoadMusic(url1,file);
                        }

                        Intent intent = new Intent(MainActivity.this, PlayActivity.class);
                        intent.putExtra("url", url1);
                        intent.putExtra("song", song1);
                        intent.putExtra("artist",artist1);
                        // intent.putExtra("id",id1);
                        intent.putExtra("url_list", (Serializable) url_list);
                        intent.putExtra("as_list", song_list);

                        startActivity(intent);

                    }
                });
            }
        });
    }


}

class UrlThread implements Runnable {
    private final int songId;
    private String url;

    public UrlThread(int songId) {
        this.songId = songId;
    }

    public String getUrl() {
        return url;
    }

    @Override
    public void run() {
        try {
            OkHttpClient client = new OkHttpClient();
            Request request = new Request.Builder()
                    .url("https://autumnfish.cn/song/url?id=" + songId)
                    .build();
            Log.e("EEE", request + "");

            Response response = client.newCall(request).execute();
            Log.e("EEE d", response + "");
            String responseData = Objects.requireNonNull(response.body()).string();
            Log.e("EEE data is", responseData);
            SongURL songURL = JSON.parseObject(responseData, SongURL.class);
            ArrayList<SongURL.Data> dataArrayList = songURL.getData();
            url = dataArrayList.get(0).getUrl();
        } catch (Exception e) {
            Log.e("EEE", e.getMessage() + "end");
            e.printStackTrace();
        }
    }
}