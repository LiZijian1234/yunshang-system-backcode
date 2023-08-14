package com.xuecheng.auth.controller;

import com.xuecheng.ucenter.model.po.XcUser;
import com.xuecheng.ucenter.service.WxAuthService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;

import java.io.IOException;

/**
 * @author zijianLi
 * @create 2023- 05- 06- 21:25
 */
@Slf4j
@Controller
public class WxLoginController {

    @Autowired
    WxAuthService wxAuthService;

    /**
     * 前端扫描二维码之后获取到授权码code，然后带着这个code请求这个接口
     * @param code
     * @param state
     * @return
     * @throws IOException
     */
    @RequestMapping("/wxLogin")
    public String wxLogin(String code, String state) throws IOException {
        log.debug("微信扫码回调,code:{},state:{}",code,state);
        //TODO 请求微信申请令牌，拿到令牌查询用户信息，将用户信息写入本项目数据库,里面wxAuth方法有三步
        XcUser xcUser1 = wxAuthService.wxAuth(code);

        XcUser xcUser = new XcUser();
        //暂时硬编写，目的是调试环境
        xcUser.setUsername("t1");
        if(xcUser==null){
            return "redirect:http://www.51xuecheng.cn/error.html";
        }
        String username = xcUser.getUsername();
        //重定向到下面网址，自动登录，之后就跳转到execute方法，之后跳转到WxAuthServiceImpl方法
        //重定向到登录的接口，此时就相当于已经输入了用户名密码了，然后就重新登录，
        // 之后会自动进入springsecurity的认证流程，调用UserServiceImpl类的loadUserByUsername方法，进一步调用execute方法完成登录。
        return "redirect:http://www.51xuecheng.cn/sign.html?username="+username+"&authType=wx";
    }
}

