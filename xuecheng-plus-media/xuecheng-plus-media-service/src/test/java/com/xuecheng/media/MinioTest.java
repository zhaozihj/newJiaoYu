package com.xuecheng.media;

import com.j256.simplemagic.ContentInfo;
import com.j256.simplemagic.ContentInfoUtil;
import io.minio.*;
import io.minio.errors.*;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.io.IOUtils;
import org.bouncycastle.jcajce.provider.digest.SHA512;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import java.io.*;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;

/**
 * @description 测试MinIO
 * @author Mr.M
 * @date 2022/9/11 21:24
 * @version 1.0
 */
public class MinioTest {

    //这个操作就相当于获取与minio服务器的连接
    static MinioClient minioClient =
            MinioClient.builder()
                    .endpoint("http://192.168.101.65:9000")
                    .credentials("minioadmin", "minioadmin")
                    .build();

    //上传文件
    @Test
    public  void upload() {
        try {

            //通过扩展名得到媒体资源类型  媒体资源类型也就是mimeType
            //根据扩展名取出mimeType
            ContentInfo extensionMatch = ContentInfoUtil.findExtensionMatch(".xls");
            //这个mimeType是默认的mimeType，就是不知道文件类型的时候的mimeType
            String mimeType = MediaType.APPLICATION_OCTET_STREAM_VALUE;//通用mimeType，字节流

            //能通过扩展名拿到mimeType
            if(extensionMatch!=null){
               //通过扩展名拿到mimeType
                mimeType=extensionMatch.getMimeType();
            }

            UploadObjectArgs testbucket = UploadObjectArgs.builder()
                    .bucket("testbucket")//上传到哪个桶
                    //.object("test001.xls") //这个是本地文件上传到minio之后的对象名,也就是在minio上显示的文件名,同时这种存储方法就是在桶的根目录下存储该文件
                    .object("001/test001.xls")//添加子目录,就是把test001.xls文件存储在桶下的001目录下，想要把文件存储在多层目录下也可以，在文件名前面加多个包名就可以了
                    .filename("D:\\赵梓皓.xls") //指定上传的本地文件的路径
                    .contentType(mimeType)//默认根据扩展名确定文件内容类型，也可以指定
                    .build();
            minioClient.uploadObject(testbucket);
            System.out.println("上传成功");
        } catch (Exception e) {
            e.printStackTrace();
            System.out.println("上传失败");
        }

    }

    //删除文件
    @Test
    public void test_delete() throws  Exception{
        //RemoveObjectArgs
        //.bucket("testbucket")是指定要删除哪个桶中的文件，.object("1.mp4")是指定要删除桶中的哪一个文件，test001.mp4是文件在nacos上的文件名
        RemoveObjectArgs removeObjectArgs = RemoveObjectArgs.builder().bucket("testbucket").object("test001.mp4").build();

        //删除文件
        minioClient.removeObject(removeObjectArgs);
    }

    @Test
    //查询文件  从minio中下载文件
    public void test_getFile() throws Exception{

        //.bucket("testbucket").object("test001.xls")  锁定要查询的文件
        GetObjectArgs getObjectArgs = GetObjectArgs.builder().bucket("testbucket").object("test001.xls").build();

        //获取指定文件输入流,这个是查询远程服务获取到的一个流对象
        FilterInputStream inputStream=minioClient.getObject(getObjectArgs);

        //指定输出流
        FileOutputStream outputStream=new FileOutputStream(new File("D:\\test001.xls"));
        //将输入流中的内容拷贝到输出流，完成文件的下载
        IOUtils.copy(inputStream,outputStream);

        //校验文件的完整性：对文件的内容进行md5
        //不能直接用上面的从nacos中获取的文件输入流(远程流)来和本地下载文件的文件输入流进行比较，因为传远程流会不稳定
        String source_md5=DigestUtils.md5Hex(new FileInputStream(new File("D:\\赵梓皓.xls")));//本地上传文件的md5
        String local_md5=DigestUtils.md5Hex(new FileInputStream(new File("D:\\test001.xls")));//本地下载文件的md5
        if(source_md5.equals(local_md5)){
            System.out.println("下载成功");
        }
    }

    //将分块文件上传到minio
    @Test
    public void uploadChunk() throws IOException, ServerException, InsufficientDataException, ErrorResponseException, NoSuchAlgorithmException, InvalidKeyException, InvalidResponseException, XmlParserException, InternalException {
        for(int i=0;i<12;i++){
            //上传文件的参数信息
          UploadObjectArgs uploadObjectArgs=UploadObjectArgs.builder()
                  .bucket("testbucket")  //桶
                  .filename("F:\\chunk\\"+i)  //指定本地文件路径
                  .object("chunk/"+i) //对象名 放在子目录下
                  .build();

          //上传文件
            minioClient.uploadObject(uploadObjectArgs);
            System.out.println("上传分块"+i+"成功");

        }


    }

    //调用minio接口合并分块
    @Test
    public void testMerge() throws  Exception{


        //存储分块文件的ComposeSource类型的对象的list
        List<ComposeSource> sources=new ArrayList<>();
        for (int i = 0; i < 12; i++) {
         //指定分块文件的信息，并封装为ComposeSource类型的对象
            ComposeSource composeSource = ComposeSource.builder()
                    .bucket("testbucket")
                    .object("chunk/" + i)
                    .build();
            sources.add(composeSource);

        }


        //指定合并之后的文件的信息， .bucket是指定合并后文件所在的桶，.object就是合并后文件的所在目录位置以及名称  ， .sources中参数是存储分块文件的ComposeSource类型的对象的list
        ComposeObjectArgs composeObjectArgs = ComposeObjectArgs.builder()
                .bucket("testbucket")
                .object("merge01.mp4")
                .sources(sources)//指定源文件
                .build();
        //合并文件
        //报错：size 1048576 must be greater than 5242880 ，minio默认的分块文件的大小为5M
        //所以又去把分块的大小调整为5M，这个分块的大小必须大于等于5M
        minioClient.composeObject(composeObjectArgs);
    }



}
