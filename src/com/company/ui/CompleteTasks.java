package com.company.ui;

import com.company.util.Components;

import javax.swing.*;
import java.awt.*;

/**
 * Created by hzg on 2018/4/10.
 * 1.打开文件
 * 2.打开文件所在目录
 * 3.删除
 * 4.修改文件名
 */
public class CompleteTasks extends JFrame{
//    private Components components = new Components();
//    public JPanel panel = components.getJPanel();

    public Container container = getContentPane();

    public CompleteTasks() {
        container.setLayout(new GridLayout(15, 1, 20, 10));//hgap: 竖直方向的间隔 vgap:横方向间隔
    }
}
