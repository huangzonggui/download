package com.company.ui;

import org.json.JSONObject;

import javax.swing.*;
import java.awt.*;
import java.io.File;

/**
 * 这是一个Container,正在下载中的一个item
 * Created by hzg on 2018/4/9.
 * TODO: 1.进度条 2.下载速度 3.暂停 4.断点下载 5.删除任务 6.下载完成跳转 7.
 * item: 1.url 2.path 3.
 */
public class Downloading extends JFrame {
    public Container container = getContentPane();
//    private JSONObject jsonObject = new JSONObject();
//    private File jsonMsgFile = new File("DownloaderJson.json");

    public Downloading() {
        container.setLayout(new GridLayout(10, 1, 10, 10));
        //TODO:初始化这个界面的时候，需要判断有没有还没有下载完成的item。用一个json文件来记录，正在下载的项目，已完成的项目，垃圾箱里的项目
//        if (jsonMsgFile.exists()) {
//            System.out.println("jsonMsgFile");
//        }

    }

}
