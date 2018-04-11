package com.company.protocal;

import com.company.util.SimpleHttpUtils;

import java.io.File;

/**
 * 下载一个文件的线程
 * Created by hzg on 2018/4/9.
 * 主线程：下面分几个子线程下载一个文件
 */
public class DownloadThread extends Thread {
    private String url;
    private File saveToFile;

    public DownloadThread(String url, File saveToFile) {
        this.url = url;
        this.saveToFile = saveToFile;
    }

    @Override
    public void run() {
        super.run();
        try {
            SimpleHttpUtils.get(this.url, this.saveToFile);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
