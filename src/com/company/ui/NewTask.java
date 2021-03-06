package com.company.ui;

import com.company.protocal.DownLoadFile;
import com.company.util.Components;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.IOException;

/**
 * Created by hzg on 2018/4/8.
 */
public class NewTask {
    private Components components = new Components();
    public JPanel panel = components.getJPanel();
    private static JTextField urlField;
    private static JComboBox url_jComboBox;
    private static JTextField thread_count_Field;
    private static JTextField pathField;
    private String url;
    private String filePath;
    private String fileName;
    private File saveToFile;

    public NewTask() {

        //创建文本框，指定可见列数为多少列
        JLabel urlLabel = components.getJLabel(50, 10, 100, 25, "url:");
        urlField = components.getJTextField(96, 10, 700, 20, 50);
        url_jComboBox = components.getJComboBox(96, 10, 700, 25);
        urlField.setText("http://sw.bos.baidu.com/sw-search-sp/software/f7d2fccd118f8/WeChat_C1003.exe");
        urlField.setFont(new Font(null, Font.PLAIN, 15));
        url_jComboBox.setFont(new Font(null, Font.PLAIN, 15));
        url_jComboBox.setVisible(false);

        JLabel thread_count_label = components.getJLabel(800, 10, 70, 25,"线程数：");
        thread_count_Field = components.getJTextField(880, 10, 50, 25, 10);//需要用多少个线程来进行下载
        thread_count_Field.setText("4");

        //路径path
        JLabel pathLabel = components.getJLabel(50, 50, 100, 22, "path:");
        pathField = components.getJTextField(96, 50, 700, 20, 50);
        pathField.setText("H:\\hzg\\Desktop");//默认下载的路径是桌面

        //下载路径存放位置
        final JFileChooser jFileChooser = components.getJFileChooser();
        jFileChooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);

        //选择下载路径按钮
        JButton btn_path = components.getJButton(800, 50, 150, 22, "选择下载路径");
        btn_path.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                jFileChooser.showOpenDialog(null);
                //创建一个按钮，点击后获取文本框中的文本
                pathField.setText(jFileChooser.getSelectedFile().getPath());
            }
        });

        //下载按钮
        JButton btn_download = components.getJButton(800, 90, 150, 22, "下载");
        btn_download.setFont(new Font(null, Font.PLAIN, 15));
        btn_download.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
//                System.out.println("下载url: " + urlField.getText());
                url = getURL();
                filePath = getPath();
                fileName = url.substring(url.lastIndexOf("/") + 1);//截取最后一个、后面的所有字符

                saveToFile = new File(filePath + "\\" + fileName);
                try {
                    if (!url.equals("") || filePath.equals("")) {
                        DownLoadFile downLoadFile = new DownLoadFile(url, saveToFile, Integer.parseInt(thread_count_Field.getText()));
                        downLoadFile.downLoad();
                    } else {
                        JOptionPane.showMessageDialog(null, "url或path不能为空，请输入url和path！", "error", 0);
                    }
                } catch (Exception e1) {
                    e1.printStackTrace();
                }
            }
        });

        //打开文件所在目录按钮
//        JButton btn_openFileDir = components.getJButton(50, 130, 180, 22, "打开文件所在目录");
//        btn_openFileDir.setFont(new Font(null, Font.PLAIN, 15));
//        btn_openFileDir.addActionListener(new ActionListener() {
//            @Override
//            public void actionPerformed(ActionEvent e) {
//                String filePath = getPath();
//                try {
//                    Runtime.getRuntime().exec("cmd /c start explorer " + filePath);
//                } catch (IOException e1) {
//                    e1.printStackTrace();
//                }
//            }
//        });

        panel.add(urlLabel);
        panel.add(urlField);
        panel.add(url_jComboBox);
        panel.add(btn_path);
        panel.add(pathLabel);
        panel.add(pathField);
        panel.add(btn_download);
        panel.add(thread_count_Field);
        panel.add(thread_count_label);
    }


    private static String getURL() {
        String url = urlField.getText();
//        String url = url_jComboBox.getSelectedItem().toString();
        System.out.println(url_jComboBox.getSelectedItem().toString());
        return url;
    }

    private static String getPath() {
        String path = pathField.getText();
        return path;
    }

}
