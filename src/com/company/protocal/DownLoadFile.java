package com.company.protocal;

import com.company.util.Components;
import com.company.util.GetMainUIComponent;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.company.util.JsonUtils;
import org.json.JSONException;
import org.json.JSONObject;

//一个DownLoadFile类，就是一个下载任务
public class DownLoadFile {

    JTabbedPane tabbedPane = GetMainUIComponent.getJTabbedPane();

    private static final int DEFAULT_THREAD_COUNT = 4;//默认下载线程数

    //下载状态
    private static final String DOWNLOAD_INIT = "1";//初始化
    private static final String DOWNLOAD_ING = "2";//正在下载
    private static final String DOWNLOAD_PAUSE = "3";//暂停

    private String loadUrl;//网络获取的url
    private File saveToFile;//下载到本地的path
    private String filePath;
    private String fileName;
    private int threadCount = DEFAULT_THREAD_COUNT;//下载线程数

    private int fileLength;//文件总大小
    //使用volatile防止多线程不安全
    private volatile long contentLength;//当前总共下载的大小
    private volatile int runningThreadCount;//正在运行的线程数
    private Thread[] mThreads;
    private String stateDownload = DOWNLOAD_INIT;//当前线程状态

    private Item item;//进度条
    private int total = 0;//已经下载的大小

    private JSONObject jsonToFile = new JSONObject();
    private JsonUtils jsonUtils = new JsonUtils();
    private JSONObject jsonFromFile;
    private File jsonFile;

    public DownLoadFile(String url, File saveToFile, int threadCount) throws Exception {
        this.loadUrl = url;
        this.saveToFile = saveToFile;
        this.threadCount = threadCount;
        runningThreadCount = 0;
        this.filePath = saveToFile.getParent();
        this.fileName = url.substring(url.lastIndexOf("/") + 1);
        //判断存不存在文件，不存在就写入
        jsonFile = new File(filePath + "\\" + fileName + ".json");
        if (!jsonFile.exists()) {
            jsonToFile.put("url", loadUrl);
            jsonToFile.put("filePath", filePath);
            jsonToFile.put("fileName", fileName);
            jsonToFile.put("threadCount", threadCount);
            jsonToFile.put("continue", false);
            jsonUtils.writeJson(filePath, jsonToFile, fileName);
        } else {
            jsonToFile = new JSONObject(JsonUtils.readJson(jsonFile.getPath()));
        }
    }

    public void downLoad() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    //初始化数据
                    if (mThreads == null)
                        mThreads = new Thread[threadCount];

                    //建立连接请求
                    URL url = new URL(loadUrl);
                    HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                    conn.setConnectTimeout(10000);
                    conn.setReadTimeout(10000);
                    conn.setRequestMethod("GET");
                    int code = conn.getResponseCode();
                    if (code == 200) {
                        fileLength = conn.getContentLength();
                        int blockLength = fileLength / threadCount;//计算每个线程下载的数据段

                        for (int i = 0; i < threadCount; i++) {
                            int startPosition = i * blockLength;
                            int endPosition = (i + 1) * blockLength - 1;//每个线程的终点
                            if ((i + 1) == threadCount) {//TODO:这样子会导致文件不可用，进度条不完全
//                                endPosition = endPosition * 2;//将最后一个线程结束位置扩大，防止文件下载不完全，大了不影响，小了文件失效
                            }
                            mThreads[i] = new DownThread(i + 1, startPosition, endPosition);
                            mThreads[i].start();

                        }
                        if (item == null) {
                            //生成一个任务就产生一个进度条
                            item = new Item();
                            item.addToContainer();//添加到容器里显示
                        }
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }).start();
    }

    // TODO:取消下载
    protected void onCancel() {
        if (mThreads != null) {
            //若线程处于等待状态，则while循环处于阻塞状态，无法跳出循环，必须先唤醒线程，才能执行取消任务
            if (stateDownload.equals(DOWNLOAD_PAUSE))
                onStart();
            for (Thread dt : mThreads) {
                ((DownThread) dt).cancel();
            }

        }
    }

    //TODO:暂停下载
    protected void onPause() {
        if (mThreads != null)
            stateDownload = DOWNLOAD_PAUSE;
    }

    //TODO:继续下载
    protected void onStart() {
        if (mThreads != null)
            synchronized (DOWNLOAD_PAUSE) {
                stateDownload = DOWNLOAD_ING;
                DOWNLOAD_PAUSE.notifyAll();
            }
    }

    protected void onDestroy() {
        if (mThreads != null) {
            onCancel();
            mThreads = null;
            item.deleteItem();
        } else {
            //当先点击取消的按钮的时候，下载线程都被break,所以mThreads为null，删除Item
            item.deleteItem();
        }
    }

    protected void setProgressBar(int addValue) {
        total += addValue;
//        System.out.println("下载总进度" + (int) ((float) total / fileLength * 100) + "   fileLength:" + fileLength + "    total:" + total);
        if (item != null) {
            item.setProgressBarValue((int) ((float) total / fileLength * 100));
        }
    }

    private class DownThread extends Thread {
        private boolean isGoOn = true;//是否继续下载
        private int threadId;
        private int startPosition;//开始下载点
        private int endPosition;//结束下载点
        private int currPosition;//当前线程的下载进度

        /**
         * Created by hzg on 2018/4/3.
         * <p>
         * 简易的 HTTP 请求工具, 一个静态方法完成请求, 支持 301, 302 的重定向, 支持自动识别 charset, 支持同进程中 Cookie 的自动保存与发送
         * <p>
         * // (可选) 设置默认的User-Agent是否为移动浏览器模式, 默认为false(PC浏览器模式)
         * SimpleHttpUtils.setMobileBrowserModel(true);
         * // (可选) 设置默认的请求头, 每次请求时都将会 添加 并 覆盖 原有的默认请求头
         * SimpleHttpUtils.setDefaultRequestHeader("header-key", "header-value");
         * // (可选) 设置 连接 和 读取 的超时时间, 连接超时时间默认为15000毫秒, 读取超时时间为0(即不检查超时)
         * SimpleHttpUtils.setTimeOut(15000, 0);
         * <p>
         * // GET 请求, 下载文件, 返回文件路径
         * SimpleHttpUtils.get("http://blog.csdn.net/", new File("csdn.txt"));
         * <p>
         * // 还有其他若干 get(...) 和 post(...) 方法的重载(例如请求时单独添加请求头), 详见代码实现
         *
         * @author hzg
         * <p>
         * 下载一个文件的线程
         * 主线程：下面分几个子线程下载一个文件
         */
        //文本请求时所限制的最大响应长度, 5MB
        private static final int TEXT_REQUEST_MAX_LENGTH = 5 * 1024 * 1024;

        //默认的请求头
        private final Map<String, String> DEFAULT_REQUEST_HEADERS = new HashMap<String, String>();

        //操作默认请求头的读写锁
        private final ReadWriteLock RW_LOCK = new ReentrantReadWriteLock();

        // 连接超时时间, 单位: ms, 0 表示无穷大超时(即不检查超时), 默认 15s 超时
        private int CONNECT_TIME_OUT = 15 * 1000;

        // 读取超时时间, 单位: ms, 0 表示无穷大超时(即不检查超时), 默认为 0
        private int READ_TIME_OUT = 0;

        private DownThread(int threadId, int startPosition, int endPosition) {
            this.threadId = threadId;
            this.startPosition = startPosition;
            currPosition = startPosition;
            this.endPosition = endPosition;
            runningThreadCount++;
        }

        @Override
        public void run() {
            try {
                get(loadUrl, saveToFile);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        public String get(String url, File saveToFile) throws Exception {
            return get(url, null, saveToFile);
        }

        public String get(String url, Map<String, String> headers, File saveToFile) throws Exception {
            return sendRequest(url, "GET", headers, null, saveToFile);
        }

        /**
         * 执行一个通用的 http/https 请求, 支持 301, 302 的重定向, 支持自动识别 charset, 支持同进程中 Cookie 的自动保存与发送
         *
         * @param url        请求的链接, 只支持 http 和 https 链接
         * @param method     (可选) 请求方法, 可以为 null
         * @param headers    (可选) 请求头 (将覆盖默认请求), 可以为 null
         * @param bodyStream (可选) 请求内容, 流将自动关闭, 可以为 null
         * @param saveToFile (可选) 响应保存到该文件, 可以为 null
         * @return 如果响应内容保存到文件, 则返回文件路径, 否则返回响应内容的文本 (自动解析 charset 进行解码)
         * @throws Exception http 响应 code 非 200, 或发生其他异常均抛出异常
         */
        public String sendRequest(String url, String method, Map<String, String> headers, InputStream bodyStream, File saveToFile) throws Exception {
            assertUrlValid(url);

            HttpURLConnection conn = null;

            try {
                //记录进度的文件
                int lastProgress = startPosition;
                jsonFromFile = new JSONObject(jsonUtils.readJson(jsonFile.getPath()));
                if (jsonFromFile.get("continue").equals(true)) {
                    lastProgress = Integer.parseInt(jsonFromFile.get(threadId + "").toString());//通过id获取json中的进度
                }

                if (lastProgress < endPosition) {
                    System.out.println("读取进度");
                    int temp = startPosition;
                    startPosition = lastProgress;
                    currPosition = lastProgress;
                    if (temp != startPosition) {
                        setProgressBar(startPosition - temp);//恢复进度
                    }
                } else {
                    System.out.println("文件存储有问题");
                }

                // 打开链接
                URL urlObj = new URL(url);
                conn = (HttpURLConnection) urlObj.openConnection();

                // 设置各种默认属性
                setDefaultProperties(conn);

                // 设置Range,分段下载(覆盖默认属性里的设置)
                conn.setRequestProperty("Range", "bytes=" + startPosition + "-" + endPosition);

                // 设置请求方法
                if (method != null && method.length() > 0) {
                    conn.setRequestMethod(method);
                }

                // 添加请求头
                if (headers != null && headers.size() > 0) {
                    for (Map.Entry<String, String> entry : headers.entrySet()) {
                        conn.setRequestProperty(entry.getKey(), entry.getValue());
                    }
                }

                // 设置请求内容
                if (bodyStream != null) {
                    conn.setDoOutput(true);
                    copyStreamAndClose(bodyStream, conn.getOutputStream());
                }

                // 获取响应code
                int code = conn.getResponseCode();

                //若请求头加上Range这个参数，则返回状态码为206，而不是200
                System.out.println("response code:" + code);

                // 处理重定向 301:永久重定向 302:临时重定向
                if (code == HttpURLConnection.HTTP_MOVED_PERM || code == HttpURLConnection.HTTP_MOVED_TEMP) {
                    String location = conn.getHeaderField("Location");
                    if (location != null) {
                        closeStream(bodyStream);
                        // 重定向为 GET 请求
                        return sendRequest(location, "GET", headers, null, saveToFile);
                    }
                }

                // 获取响应内容长度
                contentLength = conn.getContentLengthLong();
                System.out.println("contentLength：" + contentLength);

                // 获取内容类型
                String contentType = conn.getContentType();

                // 获取响应内容输入流
                InputStream in = conn.getInputStream();

                // 没有响应成功, 均抛出异常
                if (code != HttpURLConnection.HTTP_OK && code != HttpURLConnection.HTTP_PARTIAL) {
                    throw new IOException("Http Error: " + code + "; Desc: " + handleResponseBodyToString(in, contentType));
                }

                // TODO：如果文件参数不为null, 则把响应内容保存到文件
                if (saveToFile != null) {
                    handleResponseBodyToFile(in, saveToFile);
                    return saveToFile.getPath();
                }

                // 如果需要将响应内容解析为文本, 则限制最大长度
                if (contentLength > TEXT_REQUEST_MAX_LENGTH) {
                    throw new IOException("Response content length too large: " + contentLength);
                }
                return handleResponseBodyToString(in, contentType);

            } finally

            {
                closeConnection(conn);
            }
        }

        //判断url是否有效，http和https协议
        private void assertUrlValid(String url) throws IllegalAccessException {
            boolean isValid = false;
            if (url != null) {
                url = url.toLowerCase();
                if (url.startsWith("http://") || url.startsWith("https://")) {
                    isValid = true;
                }
            }
            if (!isValid) {
                throw new IllegalAccessException("Only support http or https url: " + url);
            }
        }

        private void setDefaultProperties(HttpURLConnection conn) {
            RW_LOCK.readLock().lock();
            try {
                // 设置连接超时时间
                conn.setConnectTimeout(CONNECT_TIME_OUT);

                // 设置读取超时时间
                conn.setReadTimeout(READ_TIME_OUT);

                // 添加默认的请求头
                if (DEFAULT_REQUEST_HEADERS.size() > 0) {
                    for (Map.Entry<String, String> entry : DEFAULT_REQUEST_HEADERS.entrySet()) {
                        conn.setRequestProperty(entry.getKey(), entry.getValue());
                    }
                }
            } finally {
                RW_LOCK.readLock().unlock();
            }
        }

        //TODO:下载写入文件的方法。添加最大长度——hzg
        private void handleResponseBodyToFile(InputStream in, File saveToFile) throws Exception {

            RandomAccessFile raf = new RandomAccessFile(saveToFile, "rwd");
            try {
                raf.seek(startPosition);//跳到指定位置开始写数据
                byte[] buf = new byte[1024];
                int len = -1;

                //TODO:分段下载的多个子线程
                System.out.println(fileName + "的第" + threadId + "条线程----" + "contentLength:" + contentLength);
                while ((len = in.read(buf)) != -1) {
                    if (!isGoOn) {//是否继续下载
                        break;
                    }
                    raf.write(buf, 0, len);//将已经下载的大小调用外面的一个方法，外面的一个方法记录着下载的总数，调用方法累加
//                    System.out.print("第" + threadId + "条线程：读取一次的  len: " + len + "  ");
                    //写完后将当前指针后移，为取消下载时保存当前进度做准备
                    currPosition += len;
                    setProgressBar(len);//TODO:不同步的话进度PACNPro这个分区助手无法达到100%？？？
                    //TODO:同步块 暂停线程？？？？？为什么要加synchronized
                    synchronized (DOWNLOAD_PAUSE) {
                        if (stateDownload.equals(DOWNLOAD_PAUSE)) {
                            saveProgressToFile(threadId, currPosition);
                            DOWNLOAD_PAUSE.wait();
                        }
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                closeStream(in);
                closeStream(raf);
                System.out.println("第" + threadId + "条线程的下载结束" + "   已经下载的大小:  " + total + "  需要下载的大小:  " + (endPosition - startPosition));
                //TODO:将文件中线程记录移除
            }

            runningThreadCount--;//线程计数器-1
            System.out.println("线程总数为：" + runningThreadCount);
            //取消下载
            if (!isGoOn) {
                //if (currPosition < endPosition) {
                //TODO:保存下载进度,暂停、取消的时候只会保存一个线程的进度,待解决
                saveProgressToFile(threadId, currPosition);
                //}
                return;
            }

            //下载成功
            if (runningThreadCount == 0) {
                //TODO：通知叫别人下载成功了
                if (jsonFile.exists()) {
                    jsonFile.delete();
                }
                mThreads = null;
            }
        }

        private String handleResponseBodyToString(InputStream in, String contentType) throws Exception {
            ByteArrayOutputStream bytesOut = null;

            try {
                bytesOut = new ByteArrayOutputStream();

                // 读取响应内容
                copyStreamAndClose(in, bytesOut);

                // 响应内容的字节序列
                byte[] contentBytes = bytesOut.toByteArray();

                // 解析文本内容编码格式
                String charset = parseCharset(contentType);
                if (charset == null) {
                    charset = parseCharsetFromHtml(contentBytes);
                    if (charset == null) {
                        charset = "utf-8";
                    }
                }

                // 解码响应内容
                String content = null;
                try {
                    content = new String(contentBytes, charset);
                } catch (UnsupportedEncodingException e) {
                    content = new String(contentBytes);
                }

                return content;

            } finally {
                closeStream(bytesOut);
            }
        }

        private String parseCharset(String content) {
            // text/html; charset=iso-8859-1
            // <meta charset="utf-8">
            // <meta charset='utf-8'>
            // <meta http-equiv="Content-Type" content="text/html; charset=gbk" />
            // <meta http-equiv="Content-Type" content='text/html; charset=gbk' />
            // <meta http-equiv=content-type content=text/html;charset=utf-8>
            if (content == null) {
                return null;
            }
            content = content.trim().toLowerCase();
            Pattern p = Pattern.compile("(?<=((charset=)|(charset=')|(charset=\")))[^'\"/> ]+(?=($|'|\"|/|>| ))");
            Matcher m = p.matcher(content);
            String charset = null;
            while (m.find()) {
                charset = m.group();
                if (charset != null) {
                    break;
                }
            }
            return charset;
        }

        private String parseCharsetFromHtml(byte[] htmlBytes) {
            if (htmlBytes == null || htmlBytes.length == 0) {
                return null;
            }
            String html = null;
            try {
                // 先使用单字节编码的 ISO-8859-1 去尝试解码
                html = new String(htmlBytes, "ISO-8859-1");
                return parseCharsetFromHtml(html);
            } catch (UnsupportedEncodingException e) {
                html = new String(htmlBytes);
            }
            return parseCharsetFromHtml(html);
        }

        private String parseCharsetFromHtml(String html) {
            if (html == null || html.length() == 0) {
                return null;
            }
            html = html.toLowerCase();
            Pattern p = Pattern.compile("<meta [^>]+>");
            Matcher m = p.matcher(html);
            String meta = null;
            String charset = null;
            while (m.find()) {
                meta = m.group();
                charset = parseCharset(meta);
                if (charset != null) {
                    break;
                }
            }
            return charset;
        }

        private void copyStreamAndClose(InputStream in, OutputStream out) {
            try {
                byte[] buf = new byte[1024];
                int len = -1;
                while ((len = in.read(buf)) != -1) {
                    out.write(buf, 0, len);
                }
                out.flush();
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                closeStream(in);
                closeStream(out);
            }
        }

        private void closeConnection(HttpURLConnection conn) {
            if (conn != null) {
                try {
                    conn.disconnect();
                } catch (Exception e) {
                    // nothing
                }
            }
        }

        private void closeStream(Closeable stream) {
            if (stream != null) {
                try {
                    stream.close();
                } catch (Exception e) {
                    // nothing
                }
            }
        }

        public void cancel() {
            isGoOn = false;
        }

        private void saveProgressToFile(int threadId, int currPosition) throws JSONException {
            //TODO:保存下载进度,暂停、取消的时候只会保存一个线程的进度,待解决
            jsonToFile.put(threadId + "", currPosition + "");//TODO:会覆盖吗
            jsonToFile.put("continue", true);
            jsonUtils.writeJson(filePath, jsonToFile, fileName);
        }
    }

    //内部类，item表示一个下载任务
    private class Item {
        private Container container = GetMainUIComponent.getDownloading().container;
        private Components components = new Components();
        private JPanel panel = components.getJPanel(new GridLayout(1, 10, 100, 100));
        private JLabel jb_fileName;
        private JTextField text_speed;
        private JProgressBar jProgressBar;
        private JButton btn_pause;
        private JButton btn_begin;
        private JButton btn_cancel;
        private JButton btn_delete;
        private JButton btn_openPath;

        public Item() {
            jb_fileName = components.getJLabel(10, 20, 200, 25, fileName + ":");
            jProgressBar = components.getJProgressBar(210, 20, 500, 25);
            text_speed = components.getJTextField(720, 20, 60, 25, 5);
            text_speed.setText("下载速度");
            btn_pause = components.getJButton(800, 20, 80, 25, "暂停");
            btn_begin = components.getJButton(900, 20, 80, 25, "继续");
            btn_cancel = components.getJButton(1000, 20, 80, 25, "取消");
            btn_delete = components.getJButton(1090, 20, 80, 25, "删除");
            btn_openPath = components.getJButton(800, 55, 100, 25, "打开目录");
            //暂停按钮监听触发事件
            btn_pause.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    onPause();
                }
            });

            btn_begin.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    onStart();
                }
            });

            btn_cancel.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    onCancel();
                }
            });

            btn_delete.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    onDestroy();
                    if (saveToFile.exists()) {
                        saveToFile.delete();
                    }
                    //TODO:有时候没有删除
                    if (jsonFile.exists()) {
                        jsonFile.delete();
                    }
                }
            });

            btn_openPath.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    try {
                        Runtime.getRuntime().exec("cmd /c start explorer " + filePath);
                    } catch (IOException e1) {
                        e1.printStackTrace();
                    }
                }
            });

            panel.add(jb_fileName);
            panel.add(jProgressBar);
            panel.add(btn_pause);
            panel.add(btn_begin);
            panel.add(btn_cancel);
            panel.add(btn_delete);
            panel.add(btn_openPath);
            panel.add(text_speed);

            tabbedPane.setSelectedIndex(1);//跳转到正在下载tab
        }

        public void setProgressBarValue(int value) {
            jProgressBar.setValue(value);
        }

        public void addToContainer() {
            container.add(panel);
        }

        public void deleteItem() {
            panel.removeAll();
            container.remove(panel);
            container.repaint();
        }

    }

}
