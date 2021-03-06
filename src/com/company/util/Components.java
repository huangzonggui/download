package com.company.util;

import javax.swing.*;
import java.awt.*;

/**
 * Created by hzg on 2018/4/10.
 */
public class Components {

    public static JFrame getJFrame(int width, int height,int WindowConstantsValue, String str) {
        JFrame jf = new JFrame(str);
        jf.setSize(width, height);
        jf.setDefaultCloseOperation(WindowConstantsValue);
        //jf.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);//用户单击窗口的关闭按钮时程序执行的操作

        return jf;
    }

    public static JPanel getJPanel() {
        JPanel panel = new JPanel();
        panel.setLayout(null);
        return panel;
    }

    public static JPanel getJPanel(GridLayout gridLayout) {
        JPanel panel = new JPanel(gridLayout);
        panel.setLayout(null);
        return panel;
    }

    public static JTabbedPane getJTabbedPane(){
        JTabbedPane tabbedPane = new JTabbedPane();//创建选项卡面板对象
        return tabbedPane;
    }


    /*
    * TODO:columns:
    * */
    public static JTextField getJTextField(int x, int y, int width, int height, int columns) {
        JTextField textField = new JTextField(columns);
        textField.setBounds(x, y, width, height);
        return textField;
    }

    public static JFileChooser getJFileChooser() {
        JFileChooser jFileChooser = new JFileChooser();
        return jFileChooser;
    }

    public static JButton getJButton(int x, int y, int width, int height, String str) {
        JButton jButton = new JButton(str);
        jButton.setBounds(x, y, width, height);
        jButton.setFont(new Font(null, Font.PLAIN, 15));
        return jButton;
    }

    public static JLabel getJLabel(int x, int y, int width, int height, String str) {
        JLabel jLabel = new JLabel(str);
        jLabel.setBounds(x, y, width, height);
        return jLabel;
    }

    public static JProgressBar getJProgressBar(int x, int y, int width, int height) {
        JProgressBar jProgressBar = new JProgressBar();
        jProgressBar.setBounds(x, y, width, height);
        jProgressBar.setMinimum(0);
        jProgressBar.setMaximum(100);
        jProgressBar.setValue(0);
        jProgressBar.setStringPainted(true);//设置显示提示信息
        //jProgressBar.setIndeterminate(true);//设置采用不确定进度条
        //jProgressBar.setString("下载进行中......");//设置提示信息

        return jProgressBar;
    }

    public JComboBox getJComboBox(int x, int y, int width, int height) {
        JComboBox jComboBox = new JComboBox();
        jComboBox.setBounds(x, y, width, height);
        jComboBox.addItem("http://sw.bos.baidu.com/sw-search-sp/software/8ecb3fd19eb5a/QQ_9.0.2.23468_setup.exe");
        jComboBox.addItem("http://sw.bos.baidu.com/sw-search-sp/software/d8371d3561bcc/PACNPro_6.3.0.0.exe");
        jComboBox.addItem("http://sw.bos.baidu.com/sw-search-sp/software/f7d2fccd118f8/WeChat_C1003.exe");
        jComboBox.addItem("http://sw.bos.baidu.com/sw-search-sp/software/66050b95b8da0/cloudmusicsetup_2.4.0.196402.exe");
        jComboBox.addItem("http://sw.bos.baidu.com/sw-search-sp/software/7a36c2542867a/QQMusic_15.9.0.0_Setup.exe");
        jComboBox.addItem("http://sw.bos.baidu.com/sw-search-sp/software/0426ef0f4473b/iTunesSetup_12.7.4.76.exe");
        return jComboBox;
    }
}
