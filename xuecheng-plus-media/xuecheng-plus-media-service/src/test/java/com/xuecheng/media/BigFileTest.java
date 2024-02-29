package com.xuecheng.media;

import org.apache.commons.codec.digest.DigestUtils;
import org.junit.jupiter.api.Test;

import java.io.*;
import java.util.*;

/**
 * 测试大文件上传方法
 */
public class BigFileTest {

    @Test
    public  void testChunk() throws IOException {

        //源文件
        File sourceFile=new File("F:\\迅雷下载\\[2.2.1]--2.传输介质和物理层设备.mp4");

        //分块文件存储路径
        String chunkFilePath="F:\\chunk\\";

        //分块文件大小
        int chunkSize=1024*1024*5;

        //分块文件个数
        int chunkNum= (int) Math.ceil((sourceFile.length()*1.0) / chunkSize);

        //使用流从源文件读数据，向分块文件中写数据
        //RandomAccessFile既可以作为输入流也可以作为输出流，第一个参数是文件对象，第二个参数是读取或者写入模式,r代表读，w代表写
        RandomAccessFile raf_r = new RandomAccessFile(sourceFile,"r");

        //缓存区
        //这里还真是就得用小byte，用大Byte不行
        byte[] bytes=new byte[1024];


        for(int i=0;i<chunkNum;i++){

            //分块文件
            File chunkFile=new File(chunkFilePath + i);

            //分块文件写入流
            RandomAccessFile raf_rw = new RandomAccessFile(chunkFile, "rw");

            int len=-1;
            //raf_r.read把文件读取到bytes中
            //当raf_r.read读取不到内容的时候会返回-1
            while((len=raf_r.read(bytes))!=-1)
            {
                raf_rw.write(bytes,0,len);
                //如果分块文件大小大于等于规定就停止循环
                if(chunkFile.length()>=chunkSize){
                    break;
                }
            }
            raf_rw.close();

        }
        raf_r.close();

    }

    @Test

    public void testMerge() throws IOException{
        //块文件目录
        File chunkFile=new File("F:\\chunk");
        //源文件
        File sourceFile=new File("F:\\迅雷下载\\[2.2.1]--2.传输介质和物理层设备.mp4");
        //合并后的文件
        File mergeFile=new File("F:\\迅雷下载\\[2.2.1]--2.传输介质和物理层设备_2.mp4");

        //取出所有的分块文件
        File[] files = chunkFile.listFiles();
        //将数组转为list
        List<File> filesList = Arrays.asList(files);

        //因为文件分块合并的时候是要按照顺序来合并的，所以要先把分块文件进行排序
        Collections.sort(filesList, new Comparator<File>() {
            @Override
            public int compare(File o1, File o2) {

                return Integer.parseInt(o1.getName())-Integer.parseInt(o2.getName());
            }
        });

        //缓存区
        byte[] bytes=new byte[1024];

        //向合并文件写的流
        RandomAccessFile raf_rw=new RandomAccessFile(mergeFile,"rw");
        //遍历分块文件，向合并的文件写
        for (File file : files) {
            //读分块的流
            RandomAccessFile raf_r = new RandomAccessFile(file, "r");
            int len=-1;
            while((len=raf_r.read(bytes))!=-1){
                raf_rw.write(bytes,0,len);
            }

            raf_r.close();

        }
        raf_rw.close();

        //合并文件完成后对合并的文件检验
        //比较合并之后的文件和源文件的md5值是否相等，相等则说明文件合并成功
        FileInputStream fileInputStream_merge = new FileInputStream(mergeFile);
        FileInputStream fileInputStream_source = new FileInputStream(sourceFile);
        String md5_merge = DigestUtils.md5Hex(fileInputStream_merge);
        String md5_source = DigestUtils.md5Hex(fileInputStream_source);
        if(md5_merge.equals(md5_source)){
            System.out.println("文件合并成功");
        }

    }
}


