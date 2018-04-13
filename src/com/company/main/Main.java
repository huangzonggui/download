package com.company.main;

import com.company.ui.Downloading;
import com.company.util.GetMainUIComponent;
import com.company.ui.MainUI;

import javax.swing.*;

public class Main {
    public static void main(String[] args) throws Exception {

        MainUI mainUI = new MainUI();

        Downloading downloading = mainUI.downloading;
        JTabbedPane tabbedPane = mainUI.tabbedPane;

        new GetMainUIComponent(downloading, tabbedPane);//将MainUI中正在下载这个对象保存到GetMainDownloading对象中

    }

}
