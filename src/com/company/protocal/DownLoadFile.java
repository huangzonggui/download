package com.company.protocal;

import com.company.util.Components;
import com.company.util.GetMainUIComponent;

import javax.lang.model.element.TypeElement;
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
    private volatile int runningThreadCount;//正在运行的线程数
    private Thread[] mThreads;
    private String stateDownload = DOWNLOAD_INIT;//当前线程状态

    private Item item;//一个任务
    private volatile int total = 0;//已经下载的大小

    private volatile JSONObject jsonToFile = new JSONObject();
    private JsonUtils jsonUtils = new JsonUtils();
    private JSONObject jsonFromFile;
    private File jsonFile;

    private Thread speedCountThread;
    private int speed;

    private int[] downedLength;//某个线程已经下载的长度

    private boolean isDelete = false;//是否删除
    private boolean isCancel = false;//是否删除

    public DownLoadFile(String url, File saveToFile, int threadCount) throws Exception {
        this.loadUrl = url;
        this.saveToFile = saveToFile;
        this.threadCount = threadCount;
        runningThreadCount = 0;
        this.filePath = saveToFile.getParent();
        this.fileName = url.substring(url.lastIndexOf("/") + 1);
        //判断存不存在文件，不存在就写入
        jsonFile = new File(filePath + "\\" + fileName + ".json");
        if (!jsonFile.exists() && !saveToFile.exists()) {
            //都不存在才创建任务
            jsonToFile.put("url", loadUrl);
            jsonToFile.put("filePath", filePath);
            jsonToFile.put("fileName", fileName);
            jsonToFile.put("threadCount", threadCount);
            jsonToFile.put("continue", false);
            jsonUtils.writeJson(filePath, jsonToFile, fileName);
        } else if (jsonFile.exists() && !saveToFile.exists()) {
            jsonToFile = new JSONObject(JsonUtils.readJson(jsonFile.getPath()));
        } else if (jsonFile.exists() && saveToFile.exists()) {
            //下载文件存在
            System.out.println("下载进度文件和目标文件存在，已经在任务里");
        } else if (!jsonFile.exists() && saveToFile.exists()) {
            System.out.println("下载文件存在，已下载完成的");
        }

        downedLength = new int[threadCount];
        for (int i = 0; i < threadCount; i++) {
            downedLength[i] = 0;
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
                    conn.setConnectTimeout(20000);
                    conn.setRequestMethod("GET");
                    int code = conn.getResponseCode();//TODO:网络没有连接的时候提示
                    if (code == 200) {
                        fileLength = conn.getContentLength();
                        System.out.println("文件总大小:" + fileLength);
                        int blockLength = fileLength / threadCount;//计算每个线程下载的数据段
                        System.out.println("blockLength * threadCount:" + (blockLength * threadCount));
                        long temp = fileLength - (blockLength * threadCount);

                        //todo:设置速度(新建一个线程来每一秒钟读取一次total)
                        speedCountThread = new Thread(new Runnable() {
                            @Override
                            public void run() {
                                int lastTotal;
                                try {
                                    while (true) {
                                        lastTotal = total;
                                        speedCountThread.sleep(500);
                                        if (total < lastTotal) {
                                            System.out.println("异常:total < lastTotal");
                                        } else {
                                            speed = 2 * (total - lastTotal) / 1024;
                                            item.setSpeed(speed);
                                        }
                                        if (fileLength == total) {
                                            //跳转到下载完成页面
                                            item.addToCompleteTasksContainer();
                                            //将那些按钮还原
                                            item.setCompleteStyle();
                                            break;
                                        }
                                    }
                                } catch (InterruptedException e) {
                                    System.out.println("catch interrupted exception");
                                    e.printStackTrace();
                                }

                            }
                        });
                        speedCountThread.start();

                        for (int i = 0; i < threadCount; i++) {
                            int startPosition = i * blockLength;
                            int endPosition = (i + 1) * blockLength - 1;//每个线程的终点
                            if ((i + 1) == threadCount && temp != 0) {
                                endPosition += temp;//endPosition = endPosition * 2;//将最后一个线程结束位置扩大，防止文件下载不完全，大了不影响，小了文件失效
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
//        item.setCancelStatus();
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

    protected synchronized void setProgressBar(int value, int threadId) throws InterruptedException {
        downedLength[threadId - 1] = value;//设置某个线程已下载的值
//        speedCountThread.wait();//想在改变total的同时不要设置speed    TODO：java.lang.IllegalMonitorStateException
        total = 0;//在改变total的值的时候，不要setSpeed,将setSpeed的线程wait()
        for (int i = 0; i < threadCount; i++) {
            total += downedLength[i];//计算总数
        }
//        speedCountThread.notifyAll();
//        System.out.println("下载总进度" + (int) ((float) total / fileLength * 100) + "   fileLength:" + fileLength + "    total:" + total);
        if (item != null) {
            item.setProgressBarValue((int) ((float) total / fileLength * 100));
        }
    }

    private class DownThread extends Thread {
        private boolean isGoOn = true;//是否继续下载
        private int threadId;
        //        private int firstStartPosition;//每一个块的起点  为什么多个线程共用了我这个线程的变量
//        private int firstEndPosition;//每一个块的终点
        private int startPosition;//开始下载点，从零开始
        private int endPosition;//结束下载点
        private int currPosition;//当前线程的下载进度。currposition是数组的位置，应该要减一，也就是从0开始
        private long contentLength;//当前线程需要下载的大小。当一小块的大小（数量，从1开始，没有减一）
        private int lastProgress;//上次的下载进度

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
                int firstStartPosition = startPosition;//每一个块的起点  为什么多个线程共用了我这个线程的变量
                int firstEndPosition = endPosition;//每一个块的终点
                //记录进度的文件
                lastProgress = startPosition;
                jsonFromFile = new JSONObject(jsonUtils.readJson(jsonFile.getPath()));
                if (jsonFromFile.get("continue").equals(true)) {
//                    firstStartPosition = startPosition;
//                    firstEndPosition = endPosition;
                    if (jsonFromFile.has(threadId + "")) {//如果有进度
                        lastProgress = Integer.parseInt(jsonFromFile.get(threadId + "").toString());//通过id获取json中的进度
                        //如果线程下载进度等于endPosition，证明此线程下载的块已经下载完成，关闭此线程
                        if (lastProgress == firstEndPosition) {
                            System.out.println("退出线程" + threadId + ",因为该线程已经下载完！");
                            setProgressBar(firstEndPosition - firstStartPosition + 1, threadId);//恢复进度
                            throw new InterruptedException();//中断线程
                        }
                    } else {
                        System.out.println("线程" + threadId + "进度不存在");
                    }
                }

                if (lastProgress < endPosition && lastProgress != startPosition) {
                    System.out.println("Thread--" + threadId + ":读取进度" + lastProgress);
                    setProgressBar(lastProgress - firstStartPosition, threadId);//恢复进度
                    startPosition = lastProgress;
                    currPosition = lastProgress;
                } else if (lastProgress == (endPosition + 1)) {
                    System.out.println("线程" + threadId + "已经下载完成！");
                    throw new InterruptedException();
                } else if (lastProgress > endPosition) {
                    System.out.println(threadId + ":--------------------------文件存储有问题,lastProgress > endPosition " + "lastProgress=" + lastProgress + " endPosition=" + endPosition);
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
                    handleResponseBodyToFile(in, saveToFile, firstStartPosition);
                    return saveToFile.getPath();
                }

                // 如果需要将响应内容解析为文本, 则限制最大长度
                if (contentLength > TEXT_REQUEST_MAX_LENGTH) {
                    throw new IOException("Response content length too large: " + contentLength);
                }
                return handleResponseBodyToString(in, contentType);

            } finally {
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
        private void handleResponseBodyToFile(InputStream in, File saveToFile, int firstStartPosition) throws Exception {

            RandomAccessFile raf = new RandomAccessFile(saveToFile, "rwd");
            try {
                raf.seek(startPosition);//跳到指定位置开始写数据
                byte[] buf = new byte[2048 * 10];//TODO:加大
                int len = -1;

                //TODO:分段下载的多个子线程
                System.out.println(fileName + "的第" + threadId + "条线程----" + "contentLength(这个线程需要下载的大小):" + contentLength);
                while ((currPosition - startPosition) < contentLength) {
                    if (!isGoOn) {//是否继续下载
                        break;
                    }

                    if ((len = in.read(buf)) == -1) {
                        System.out.println("in.read(buf)) == -1");
                        break;
                    }
                    //写完后将当前指针后移，为取消下载时保存当前进度做准备
                    currPosition += len;//currPosition这个位置是没有写入数据的
                    setProgressBar(currPosition - firstStartPosition, threadId);
//                    setProgressBar(len);
                    //System.out.println("第" + threadId + "条线程：读取一次  len == " + len + "  ");
                    raf.write(buf, 0, len);

                    //同步块 暂停线程 加synchronized
                    synchronized (DOWNLOAD_PAUSE) {
                        if (stateDownload.equals(DOWNLOAD_PAUSE)) {
                            saveProgressToFile(threadId);//不要每次循环都写，这样频繁写文件会很慢的
                            DOWNLOAD_PAUSE.wait();
                        }
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                closeStream(in);
                closeStream(raf);
                System.out.println(fileName + "的第" + threadId + "条线程的下载结束" + "   已经下载的大小:  " + (currPosition - startPosition) + "下载到位置：" + currPosition + "  需要下载的大小:  " + contentLength);
                //TODO:将文件中线程记录移除
                if ((currPosition - startPosition) != contentLength) {
                    System.out.println(fileName + "的第" + threadId + "条线程下载异常，没有下载完全！！！！！！\n");
                    System.out.println("目前下载的位置：第" + (float) currPosition / 1000000 + "M");
                }
                saveProgressToFile(threadId);//下载完的话也记录，但是这个currPosition是endPosition
            }

            runningThreadCount--;//线程计数器-1
            System.out.println("线程总数为：" + runningThreadCount);
            //取消下载
            if (!isGoOn) {
                //if (currPosition < endPosition) {
                //TODO:保存下载进度,暂停、取消的时候只会保存一个线程的进度,待解决。上面的finally里已经有了
//                saveProgressToFile(threadId, currPosition);
                //}
                return;
            }

            //下载成功
            if (runningThreadCount == 0) {
                if (total == fileLength) {
                    //TODO：通知叫别人下载成功了
                    if (jsonFile.exists()) {
                        jsonFile.delete();
                    }
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
                closeStream(in);
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

        private synchronized void saveProgressToFile(int threadId) throws JSONException {
            //TODO:保存下载进度,暂停、取消的时候只会保存一个线程的进度,待解决
            jsonToFile.put(threadId + "", currPosition + "");//TODO:会覆盖吗
            jsonToFile.put("continue", true);
//            if (jsonFromFile.get("continue").equals(false)) {
//                //只记录第一次
//                jsonToFile.put("firstStartPosition" + threadId, startPosition + "");
//                jsonToFile.put("firstEndPosition" + threadId, endPosition + "");
//            }
            jsonUtils.writeJson(filePath, jsonToFile, fileName);
        }
    }

    //内部类，item表示一个下载任务
    private class Item {
        private Container DownloadingContainer = GetMainUIComponent.getDownloading().container;
        private Container CompleteTasksContainer = GetMainUIComponent.getCompleteTasks().container;
        private Container DeleteTasksContainer = GetMainUIComponent.getDeleteTasks().container;
        private Container CancelTasksContainer = GetMainUIComponent.getCancelTasks().container;

        private Components components = new Components();
        private JPanel panel = components.getJPanel(new GridLayout(1, 10, 500, 100));
        private JLabel jb_fileName;
        private JTextField text_speed;
        private JProgressBar jProgressBar;
        private JButton btn_pause;
        private JButton btn_begin;
        private JButton btn_cancel;
        private JButton btn_delete;
        private JButton btn_openPath;

        public Item() {
            //TODO:文件大小，已下载的大小，剩余时间
            jb_fileName = components.getJLabel(10, 20, 200, 25, fileName + ":");
            jProgressBar = components.getJProgressBar(210, 20, 500, 25);
            text_speed = components.getJTextField(720, 20, 90, 25, 5);
            setSpeed("连接资源中...");
            btn_pause = components.getJButton(820, 20, 70, 25, "暂停");
            btn_begin = components.getJButton(900, 20, 70, 25, "继续");
            btn_begin.setEnabled(false);//初始化为不可点击，因为下载一开始的时候是正在下载
            btn_cancel = components.getJButton(980, 20, 70, 25, "取消");
            btn_delete = components.getJButton(1060, 20, 70, 25, "删除");
            btn_openPath = components.getJButton(1140, 20, 100, 25, "打开目录");
            //暂停按钮监听触发事件
            btn_pause.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    onPause();
//                    setSpeed("已暂停");
                    btn_pause.setEnabled(false);
                    btn_begin.setEnabled(true);
                }
            });

            btn_begin.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    btn_pause.setEnabled(true);
                    btn_begin.setEnabled(false);
                    btn_cancel.setEnabled(true);
                    if (isDelete || isCancel) {//已经删除或者取消，这个键是恢复键
                        if (isDelete) {
                            recoverTasks("delete");
                        } else if (isCancel) {
                            recoverTasks("cancel");
                        }
                        //TODO:如果线程为0，出现下载异常，那么重新建立新的线程继续下载。相当于点一次下载这个按钮。
                        if (runningThreadCount == 0) {
                            DownLoadFile.this.downLoad();//内部类调用外部类的方法
                            setSpeed("连接资源中...");
                        } else {
                            onStart();//唤醒线程
                            setSpeed("连接资源中...");
                        }
                    } else {//没有删除，这个键是继续键
                        //TODO:如果线程为0，出现下载异常，那么重新建立新的线程继续下载。相当于点一次下载这个按钮。
                        if (runningThreadCount == 0) {
                            DownLoadFile.this.downLoad();//内部类调用外部类的方法
                            setSpeed("连接资源中...");
                        } else {
                            onStart();//唤醒线程
                            setSpeed("连接资源中...");
                        }
                    }
                }
            });

            btn_cancel.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    onCancel();
                    setSpeed("已取消");
                    btn_cancel.setEnabled(false);
                    isCancel = true;
                    addToCancelTasksContainer();
                }
            });

            btn_delete.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    if (!isDelete) {//删除
                        onCancel();
                        item.addToDeleteTasksContainer();
//                        item.setDeleteBtnText();
                        isDelete = true;
                    } else {//彻底删除
                        onDestroy();
                        //TODO:有时候没有删除??????:直接点击删除有时候没有删除，有可能线程没有结束文件就delete，文件被占用所以删除失败。初步解决方案，定义一个标识，这个标识在文件全部关闭的时候为true,删除文件的时候判断文件的操作流是否全部关闭，再进行是否删除操作
                        if (saveToFile.exists()) {
                            saveToFile.delete();
                        }
                        if (jsonFile.exists()) {
                            jsonFile.delete();
                        }
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
            DownloadingContainer.add(panel);
        }

        public void addToCompleteTasksContainer() {
            btn_begin.setEnabled(false);
            btn_cancel.setEnabled(false);
            DownloadingContainer.remove(panel);
            allContainerRepaint();
            tabbedPane.setSelectedIndex(3);//跳转到下载完成的tab里
            CompleteTasksContainer.add(panel);
        }

        public void setCompleteStyle() {
            setSpeed("下载完成");
            btn_cancel.setVisible(false);
            btn_begin.setVisible(false);
            btn_pause.setVisible(false);
            btn_delete.setVisible(true);
            btn_delete.setText("删除");
            btn_delete.setBounds(820, 20, 70, 25);
            btn_openPath.setVisible(true);
            btn_openPath.setText("打开目录");
            btn_openPath.setBounds(900, 20, 150, 25);
        }


        public void addToCancelTasksContainer() {
            setCancelStyle();
            DownloadingContainer.remove(panel);
            CancelTasksContainer.add(panel);
            tabbedPane.setSelectedIndex(2);
        }

        public void addToDeleteTasksContainer() {
            DownloadingContainer.remove(panel);
            DeleteTasksContainer.add(panel);
            setDeleteStyle();
            allContainerRepaint();
        }

        public void deleteItem() {
            panel.removeAll();
            DownloadingContainer.remove(panel);
            DeleteTasksContainer.remove(panel);
            allContainerRepaint();
        }

        public void setSpeed(int speed) {
            text_speed.setText(speed + "KB/S");
        }

        public void setSpeed(String msg) {
            text_speed.setText(msg);
        }

        public void allContainerRepaint() {
            DownloadingContainer.repaint();
            DeleteTasksContainer.repaint();
            CancelTasksContainer.repaint();
            CompleteTasksContainer.repaint();
        }

        public void setDeleteStyle() {
            btn_begin.setEnabled(true);
            setSpeed("已删除");
            btn_delete.setText("彻底删除");
            btn_delete.setBounds(900, 20, 150, 25);
            btn_pause.setVisible(false);
            btn_cancel.setVisible(false);
            btn_openPath.setVisible(false);
            btn_begin.setVisible(true);
            btn_begin.setText("恢复");
            btn_begin.setBounds(820, 20, 70, 25);
        }

        public void setCancelStyle() {
            btn_begin.setEnabled(true);
            setSpeed("已取消");
            btn_delete.setText("删除");
            btn_delete.setBounds(900, 20, 70, 25);
            btn_pause.setVisible(false);
            btn_cancel.setVisible(false);
            btn_openPath.setVisible(false);
            btn_begin.setText("恢复");
            btn_begin.setBounds(820, 20, 70, 25);
        }

        public void recoverTasks(String type) {
            if (type.equals("delete")) {
                isDelete = false;
                DeleteTasksContainer.remove(panel);
            } else if (type.equals("cancel")) {
                isCancel = false;
                CancelTasksContainer.remove(panel);
            }
            if (fileLength == total) {
                CompleteTasksContainer.add(panel);
                setCompleteStyle();
                tabbedPane.setSelectedIndex(3);
            } else {
                DownloadingContainer.add(panel);
                setSpeed("连接资源中...");
                btn_delete.setBounds(1060, 20, 70, 25);
                btn_delete.setText("删除");
                btn_delete.setVisible(true);
                btn_pause.setVisible(true);
                btn_cancel.setVisible(true);
                btn_openPath.setVisible(true);
                btn_begin.setText("继续");
                btn_begin.setBounds(900, 20, 70, 25);
                allContainerRepaint();
                tabbedPane.setSelectedIndex(1);//跳转到下载完成的tab里
            }

        }
    }

}
