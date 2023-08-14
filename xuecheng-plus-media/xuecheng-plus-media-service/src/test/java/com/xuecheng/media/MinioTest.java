package com.xuecheng.media;

import com.j256.simplemagic.ContentInfo;
import com.j256.simplemagic.ContentInfoUtil;
import io.minio.GetObjectArgs;
import io.minio.MinioClient;
import io.minio.RemoveObjectArgs;
import io.minio.UploadObjectArgs;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.compress.utils.IOUtils;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FilterInputStream;

/**
 * @author zijianLi 测试MinIO
 * @create 2023- 05- 01- 11:24
 */
public class MinioTest {
//    static MinioClient minioClient =
//            MinioClient.builder()
//                    .endpoint("http://192.168.101.65:9000")
//                    .credentials("minioadmin", "minioadmin")
//                    .build();
//
//    //上传文件
//    @Test
//    public  void upload() {// .contentType("video/mp4")//默认根据扩展名确定文件内容类型，也可以指定
//        try {
//            UploadObjectArgs testbucket = UploadObjectArgs.builder()
//                    .bucket("testbucket")
////                    .object("test001.mp4")
//                    .object("1.png")//添加子目录D:\develope
//                    .filename("D:\\develope\\1.png")
//
//                    .build();
//            minioClient.uploadObject(testbucket);
//            System.out.println("上传成功");
//        } catch (Exception e) {
//            e.printStackTrace();
//            System.out.println("上传失败");
//        }
//
//    }
//
//    @Test
//    public void delete(){
//        try {
//            //根据扩展名取出mimeType
//            ContentInfo extensionMatch = ContentInfoUtil.findExtensionMatch(".mp4");
//            String mimeType = MediaType.APPLICATION_OCTET_STREAM_VALUE;//通用mimeType，字节流
//            if(extensionMatch!=null){
//                mimeType = extensionMatch.getMimeType();
//            }
//
//            minioClient.removeObject(
//                    RemoveObjectArgs.builder().bucket("testbucket").object("1.png").build()
//            );
//            System.out.println("删除成功");
//        } catch (Exception e) {
//            e.printStackTrace();
//            System.out.println("删除失败");
//        }
//    }
//    //查询文件
//    @Test
//    public void getFile() {
//        GetObjectArgs getObjectArgs = GetObjectArgs.builder().bucket("testbucket").object("test001.mp4").build();
//        try(
//                FilterInputStream inputStream = minioClient.getObject(getObjectArgs);
//                FileOutputStream outputStream = new FileOutputStream(new File("D:\\develope\\12.png"));
//        ) {
//            IOUtils.copy(inputStream,outputStream);
//            //校验文件的完整性对文件的内容进行md5
////            FileInputStream fileInputStream1 = new FileInputStream(new File("D:\\develop\\upload\\1.mp4"));
//            String source_md5 = DigestUtils.md5Hex(inputStream);
//            FileInputStream fileInputStream = new FileInputStream(new File("D:\\develope\\12.png"));
//            String local_md5 = DigestUtils.md5Hex(fileInputStream);
//            if(source_md5.equals(local_md5)){
//                System.out.println("下载成功");
//            }
//
//
//        } catch (Exception e) {
//            e.printStackTrace();
//        }
//    }



}
