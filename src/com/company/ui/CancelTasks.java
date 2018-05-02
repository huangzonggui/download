package com.company.ui;

import javax.swing.*;
import java.awt.*;

/**
 * Created by hzg on 2018/4/29.
 */
public class CancelTasks extends JFrame {
    public Container container = getContentPane();

    public CancelTasks() {
        container.setLayout(new GridLayout(15, 1, 20, 10));//hgap: 竖直方向的间隔 vgap:横方向间
    }
}
