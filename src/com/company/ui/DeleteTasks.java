package com.company.ui;

import javax.swing.*;
import java.awt.*;

/**
 * Created by hzg on 2018/4/10.
 */
public class DeleteTasks extends JFrame{
//    private Components components = new Components();
//    public JPanel panel = components.getJPanel();
    public Container container = getContentPane();

    public DeleteTasks() {
        container.setLayout(new GridLayout(15, 1, 20, 10));//hgap: 竖直方向的间隔 vgap:横方向间
    }
}
