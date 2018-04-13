//package com.company.OthersCode;
//
///**
// * Created by hzg on 2018/4/12.
// */
//import java.io.BufferedReader;
//import java.io.File;
//import java.io.FileInputStream;
//import java.io.IOException;
//import java.io.InputStream;
//import java.io.InputStreamReader;
//import java.io.RandomAccessFile;
//import java.net.HttpURLConnection;
//import java.net.MalformedURLException;
//import java.net.ProtocolException;
//import java.net.URL;
//
///*
// * 在这里我们是先完成多线程下载，再在此基础上添加少许代码完成断点续存
// */
//public class ContinueDownload extends Thread {
//
//    private int startPosition,endIndex,threadId;
//    private String path;
//
//    public ContinueDownload(int startPosition,int endIndex,String path,int threadId) {
//        this.startPosition=startPosition;
//        this.endIndex=endIndex;
//        this.path=path;
//        this.threadId=threadId;
//    }
//
//    @Override
//    public void run() {
//
//        URL url;
//
//        try {
//
//            ///---------------------------------------------
//            ////这写代码也是断点续存添加的代码
//            File file2=new File(threadId+".txt");
//            int lastProgress=0;
//
//            ///这里是先判断临时文件是否存在，如果存在，将上次下载的最后进度读出来，并且加上startIndex
//            ///以此保证断点恢复后开始下载的位置是上次中断前的位置
//            if(file2.exists()){
//                FileInputStream fileInputStream=new FileInputStream(file2);
//                BufferedReader br=new BufferedReader(new InputStreamReader(fileInputStream));
//                lastProgress=Integer.parseInt(br.readLine());
//                startPosition+=lastProgress;
//            }
//            ///--------------------------------------------
//
//            ///这里才是利用HttpURLConnection拿到需要下载的真正的输入流
//            url=new URL(path);
//            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
//            connection.setRequestMethod("GET");
//            connection.setConnectTimeout(8000);
//            connection.setReadTimeout(8000);
//
//            ///在这里用HttpURLConnection的设置请求属性的方法，来请求获取每一段的信息
//            ///在这里的格式书写是一样的，固定的。
//            connection.setRequestProperty("Range", "bytes="+startPosition+"-"+endIndex);
//
//            ///这里的206是代表上面的区段请求的响应的返回码
//            if(connection.getResponseCode()==206){
//
//                ///接下就是输入流的一些操作
//                InputStream is=connection.getInputStream();
//
//                File file=new File("nihao.zip");
//                RandomAccessFile ref=new RandomAccessFile(file,"rwd");
//                ref.seek(startPosition);///标志开始位置，可以让线程在文件中从不同位置开始存储
//
//
//                byte[] b=new byte[1024];
//                int len=0;
//                int total = lastProgress;//注意这里需要改变一下，才能完整的实现断点续存
//
//                while((len=is.read(b))!=-1){
//
//                    ref.write(b, 0, len);
//
//                    total+=len;
//                    System.out.println("第"+threadId+"条线程的下载"+total);
//
//                    ///断点续存的添加代码
//                    ///这段代码是将每个线程的Id的下载进度total记录保存为文件
//                    //--------------------------------------------------
//                    RandomAccessFile refprgrass=new RandomAccessFile(file2, "rwd");
//                    refprgrass.write((total+"").getBytes());
//                    refprgrass.close();
//                    //------------------------------------------------
//                }
//                ref.close();
//                System.out.println("第"+threadId+"条线程的下载结束");
//
//                ///------------------------------------
//                /*
//                 * 这里代码也是断点续存增加的代码，
//                 * 在这里用一个标志位保证线程全部下载完成，才能让其删除临时文件
//                 * 如果在这里不用标志位来保证线程全部的下载完成的话再删除，而是当一条线程一下载完后就删除了的话，
//                 * （在这里假设线程A刚被下载完就存储下载长度的临时文件就被删除，）
//                 * 那么接着一旦中断，等到下次恢复时，那么线程A的临时存储的下载完成进度的文件没有了，被删掉了，但是其他线程因为没下完那些存储下载进度的文件还在，那这样就又会默认的重新的又将线程A重新的下载
//                 */
//                Main.finishedThead++;
//
//                ///这就是上面说的善后的工作
//                if(Main.finishedThead==Main.threadCount){
//                    ///线程全都下完了
//                    for(int i=0;i<Main.threadCount;i++){
//                        File f=new File(i+".txt");//这里就是要用这种方式来找到那些临时文件进行一一的删除操作
//                        f.delete();///将新建的临时文件一一删除
//                    }
//                }
//
//                ///----------------------------------------------
//            }
//
//        } catch (MalformedURLException e) {
//            e.printStackTrace();
//        } catch (ProtocolException e) {
//            e.printStackTrace();
//        } catch (IOException e) {
//            e.printStackTrace();
//        }
//        super.run();
//    }
//}