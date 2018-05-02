package com.company.util;

import com.company.ui.CancelTasks;
import com.company.ui.CompleteTasks;
import com.company.ui.DeleteTasks;
import com.company.ui.Downloading;

import javax.swing.*;

/**
 * Created by hzg on 2018/4/10.
 */
public class GetMainUIComponent {
    private static Downloading downloading;
    private static JTabbedPane tabbedPane;
    private static CompleteTasks completeTasks;
    private static DeleteTasks deleteTasks;
    private static CancelTasks cancelTasks;


    public GetMainUIComponent(Downloading downloading, JTabbedPane tabbedPane, CompleteTasks completeTasks, DeleteTasks deleteTasks, CancelTasks cancelTasks) {
        this.downloading = downloading;
        this.tabbedPane = tabbedPane;
        this.completeTasks = completeTasks;
        this.deleteTasks = deleteTasks;
        this.cancelTasks = cancelTasks;
    }

    public static Downloading getDownloading() {
        return downloading;
    }

    public static JTabbedPane getJTabbedPane() {
        return tabbedPane;
    }

    public static CompleteTasks getCompleteTasks() {
        return completeTasks;
    }

    public static DeleteTasks getDeleteTasks() {
        return deleteTasks;
    }
    public static CancelTasks getCancelTasks() {
        return cancelTasks;
    }


}
