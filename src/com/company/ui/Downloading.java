package com.company.ui;

import com.company.protocal.DownLoadFile;
import com.company.util.Components;
import com.company.util.GetMainUIComponent;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

/**
 * 这是一个Container,正在下载中的一个item
 * Created by hzg on 2018/4/9.
 * TODO: 1.进度条 2.下载速度 3.暂停 4.断点下载 5.删除任务 6.下载完成跳转 7.
 * item: 1.url 2.path 3.
 */
public class Downloading extends JFrame {
    public Container container = getContentPane();

    public Downloading() {
        container.setLayout(new GridLayout(10, 1, 10, 10));
    }

}
