package com.xuecheng.ucenter.service.impl;

import com.alibaba.fastjson.JSON;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.xuecheng.ucenter.mapper.XcMenuMapper;
import com.xuecheng.ucenter.mapper.XcUserMapper;
import com.xuecheng.ucenter.model.dto.AuthParamsDto;
import com.xuecheng.ucenter.model.dto.XcUserExt;
import com.xuecheng.ucenter.model.po.XcMenu;
import com.xuecheng.ucenter.model.po.XcUser;
import com.xuecheng.ucenter.service.AuthService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;


/**
 * @author Mr.M
 * @version 1.0
 * @description TODO
 * @date 2022/9/28 18:09
 */
@Service
@Slf4j
public class UserServiceImpl implements UserDetailsService {

    @Autowired//这个是spring容器，通过容器来确定选取哪个AuthService的service实现类
    ApplicationContext applicationContext;
    @Autowired
    XcMenuMapper xcMenuMapper;

    @Autowired
    XcUserMapper xcUserMapper;

    /**
     * @param s 账号 这个方法查询数据库 要将用户登录的s串转成authParamsDto对象
     *          根据不同类型登录调用不同类型的execute方法
     * @return org.springframework.security.core.userdetails.UserDetails
     * @description 根据账号查询用户信息
     * @author Mr.M
     * @date 2022/9/28 18:30
     */
    @Override
    public UserDetails loadUserByUsername(String s) throws UsernameNotFoundException {
        AuthParamsDto authParamsDto = null;
        try {
            //将认证参数转为AuthParamsDto类型
            authParamsDto = JSON.parseObject(s, AuthParamsDto.class);
        } catch (Exception e) {
            log.info("认证请求不符合要求:{}",s);
            throw new RuntimeException("认证请求数据格式不对");
        }
        //认证方法
        String authType = authParamsDto.getAuthType();
        AuthService authService =  applicationContext.getBean(authType + "_authservice",AuthService.class);
        XcUserExt user = authService.execute(authParamsDto);

        return getUserPrincipal(user);

    }

    /**
     * @description 查询用户信息，且在这个方法给用户增加权限，
     * 这个用户权限是设置在user里面是对的还是设置在userDetails.authorities里面是对的？
     * @param user  用户id，主键
     * @return com.xuecheng.ucenter.model.po.XcUser 用户信息
     * @author Mr.M
     * @date 2022/9/29 12:19
     */
    public UserDetails getUserPrincipal(XcUserExt user){
        //用户权限,如果不加报Cannot pass a null GrantedAuthority collection
        List<XcMenu> xcMenus = xcMenuMapper.selectPermissionByUserId(user.getId());
        List<String> permissions = new ArrayList<>();
        if (xcMenus.size()>0){

            xcMenus.forEach(m->{
                //拿到用户权限表示了
                permissions.add(m.getCode());
            });


        }else{
            //用户权限,如果不加则报Cannot pass a null GrantedAuthority collection
            permissions.add("p1");
        }
        //将用户权限放在XcUserExt中
        user.setPermissions(permissions);

        String[] authorities = permissions.toArray(new String[0]);

        String password = user.getPassword();
        //为了安全在令牌中不放密码
        user.setPassword(null);
        //将user对象转json
        String userString = JSON.toJSONString(user);
        //创建UserDetails对象
        UserDetails userDetails =
                User.withUsername(userString).password(password ).authorities(authorities).build();
        return userDetails;
    }


}

