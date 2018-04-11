package com.company.main;

import com.company.ui.Downloading;
import com.company.util.GetMainUIComponent;
import com.company.ui.MainUI;

import javax.swing.*;

public class Main {
    public static void main(String[] args) throws Exception {
        //http://sw.bos.baidu.com/sw-search-sp/software/8ecb3fd19eb5a/QQ_9.0.2.23468_setup.exe
        //http://sw.bos.baidu.com/sw-search-sp/software/f7d2fccd118f8/WeChat_C1003.exe
        //http://sw.bos.baidu.com/sw-search-sp/software/d8371d3561bcc/PACNPro_6.3.0.0.exe
            //http://sw.bos.baidu.com/sw-search-sp/software/7a36c2542867a/QQMusic_15.9.0.0_Setup.exe
        //http://sw.bos.baidu.com/sw-search-sp/software/66050b95b8da0/cloudmusicsetup_2.4.0.196402.exe

        MainUI mainUI = new MainUI();

        Downloading downloading = mainUI.downloading;
        JTabbedPane tabbedPane = mainUI.tabbedPane;

        new GetMainUIComponent(downloading, tabbedPane);//将MainUI中正在下载这个对象保存到GetMainDownloading对象中

    }

}
