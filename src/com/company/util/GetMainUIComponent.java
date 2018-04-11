package com.company.util;

import com.company.ui.Downloading;

import javax.swing.*;

/**
 * Created by hzg on 2018/4/10.
 */
public class GetMainUIComponent {
    private static Downloading downloading;
    private static JTabbedPane tabbedPane;

    public GetMainUIComponent(Downloading downloading, JTabbedPane tabbedPane) {
        this.downloading = downloading;
        this.tabbedPane = tabbedPane;
    }

    public static Downloading getDownloading() {
        return downloading;
    }

    public static JTabbedPane getJTabbedPane() {
        return tabbedPane;
    }
}
