package com.xuecheng.content.feignclient;

import org.springframework.web.multipart.MultipartFile;

/** 熔断后降级的方法，熔断后就执行这个方法。但是无法拿到熔断的异常信息
 * @author zijianLi
 * @create 2023- 05- 05- 21:10
 */
public class MediaServiceClientFallback implements MediaServiceClient{
    @Override
    public String uploadFile(MultipartFile upload, String objectName) {
        return null;
    }
}
