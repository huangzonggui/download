package com.company.ui;

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
public class Downloading extends JFrame{
    public Container container = getContentPane();

    public Downloading() {
        container.setLayout(new GridLayout(10,1,10,10));
    }

    //内部类，item表示一个下载任务
    public class Item {
        private Components components = new Components();
        private JPanel panel = components.getJPanel(new GridLayout(1,1,100,100));
        private JTextField jTextField;
        private JProgressBar jProgressBar;
        private JButton btn_pause;

        public Item(int n){//n应该可以去掉
            jProgressBar = components.getJProgressBar(10,20*n,700,25);
            btn_pause = components.getJButton(850, 20 * n, 100, 25, "暂停");
            jTextField = components.getJTextField(720, 20 * n, 60, 25,5);
            jTextField.setText("下载速度");

            //暂停
            btn_pause.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {

                }
            });

            panel.add(jProgressBar);
            panel.add(btn_pause);
            panel.add(jTextField);

            JTabbedPane tabbedPane = GetMainUIComponent.getJTabbedPane();
            tabbedPane.setSelectedIndex(1);//跳转到正在下载
        }

        public void setProgressBarValue(int value) {
            jProgressBar.setValue(value);
        }

        public void addToContainer(){
            container.add(panel);
        }
    }
}
