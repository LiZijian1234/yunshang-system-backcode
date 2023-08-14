package com.xuecheng.content.feignclient;

import com.xuecheng.content.config.MultipartSupportConfig;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.multipart.MultipartFile;

/**
 * @description 媒资管理服务远程接口,用于从MInio下载静态html页面到nginx，这样速度更快
 * @author Mr.M
 * @date 2022/9/20 20:29
 * @version 1.0
 */
//feign注解，value属于哪个服务？写上configuration来让feign支持Multipart格式来传文件
@FeignClient(value = "media-api",configuration = MultipartSupportConfig.class,fallbackFactory = MediaServiceClientFallbackFactory.class)
public interface MediaServiceClient {
    //先把远程要调用的方法先写上，返回类型为string类型
    @RequestMapping(value = "/media/upload/coursefile",consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    String uploadFile(@RequestPart("filedata") MultipartFile upload,@RequestParam(value = "objectName",required=false) String objectName);
}

