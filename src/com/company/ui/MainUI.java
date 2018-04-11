package com.company.ui;

import com.company.util.Components;

import javax.swing.*;
import java.awt.*;

/**
 * Created by hzg on 2018/4/10.
 */
public class MainUI extends JFrame{
    public static JTabbedPane tabbedPane;
    private static Components components = new Components();
    //四个tab
    private static NewTask newTask = new NewTask();//新建任务tab
    public static Downloading downloading = new Downloading();//正在下载tab
    private static CompleteTasks completeTasks = new CompleteTasks();//已经完成任务tab
    private static DeleteTasks deleteTasks = new DeleteTasks();//删除任务tab

    public MainUI(){
        super("文件下载器");
        setSize(1200,800);

        Container c = getContentPane();

        tabbedPane = components.getJTabbedPane();
        tabbedPane.addTab("新建下载任务", null, newTask.panel, "新建下载任务");
        tabbedPane.addTab("正在下载", null, downloading.container, "正在下载");
        tabbedPane.addTab("已完成", null, completeTasks.panel, "已完成任务");
        tabbedPane.addTab("垃圾箱", null, deleteTasks.panel, "垃圾箱");

        tabbedPane.setSelectedIndex(0);// 选择第一个选项页为当前选择的选项页
        //tabbedPane.setSelectedComponent(downloading.panel);

        c.add(tabbedPane);

        setVisible(true);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
    }
}
