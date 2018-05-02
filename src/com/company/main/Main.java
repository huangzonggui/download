package com.company.main;

import com.company.ui.*;
import com.company.util.GetMainUIComponent;

import javax.swing.*;

public class Main {
    public static void main(String[] args) throws Exception {

        MainUI mainUI = new MainUI();//程序初始化的时候，新建一个主面板

        Downloading downloading = mainUI.downloading;
        JTabbedPane tabbedPane = mainUI.tabbedPane;
        CompleteTasks completeTasks = mainUI.completeTasks;
        DeleteTasks deleteTasks = mainUI.deleteTasks;
        CancelTasks cancelTasks = mainUI.cancelTasks;

        new GetMainUIComponent(downloading, tabbedPane, completeTasks, deleteTasks, cancelTasks);//将MainUI中正在下载这个对象保存到GetMainDownloading对象中

    }

}
