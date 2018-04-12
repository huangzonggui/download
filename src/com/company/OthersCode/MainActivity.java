//package com.company.OthersCode;
//
////import android.os.Bundle;
//import android.os.Environment;
//import android.support.v7.app.AppCompatActivity;
//import android.view.View;
//import android.widget.TextView;
//import android.widget.Toast;
//
////下载一个文件的启动方法
////这里是一个线程，downLoadFile是一个线程里的全局变量
//public class MainActivity extends AppCompatActivity {
//    DownLoadFileTest downLoadFile;
//    private String loadUrl = "http://gdown.baidu.com/data/wisegame/d2fbbc8e64990454/wangyiyunyinle_87.apk";
//    private String filePath = Environment.getExternalStorageDirectory()+"/"+"网易云音乐.apk";
//
//    @Override
//    protected void onCreate(Bundle savedInstanceState) {
//        super.onCreate(savedInstanceState);
//        setContentView(R.layout.activity_main);
//        final TextView tvprogress = (TextView) findViewById(R.id.tv_progress);
//        downLoadFile = new DownLoadFileTest(this,loadUrl, filePath, 3);
//        downLoadFile.setOnDownLoadListener(new DownLoadFileTest.DownLoadListener() {
//            @Override
//            public void getProgress(int progress) {
//                tvprogress.setText("当前进度 ："+progress+" %");
//            }
//
//            @Override
//            public void onComplete() {
//                Toast.makeText(MainActivity.this,"下载完成",Toast.LENGTH_SHORT).show();
//            }
//
//            @Override
//            public void onFailure() {
//                Toast.makeText(MainActivity.this,"下载失败",Toast.LENGTH_SHORT).show();
//            }
//        });
//        findViewById(R.id.bt).setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View v) {
//                downLoadFile.downLoad();
//            }
//        });
//        findViewById(R.id.bt_pause).setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View v) {
//                downLoadFile.onPause();
//            }
//        });
//        findViewById(R.id.bt_start).setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View v) {
//                downLoadFile.onStart();
//            }
//        });
//        findViewById(R.id.bt_cancel).setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View v) {
//                downLoadFile.cancel();
//            }
//        });
//    }
//
//    @Override
//    protected void onDestroy() {
//        downLoadFile.onDestroy();
//        super.onDestroy();
//    }
//}