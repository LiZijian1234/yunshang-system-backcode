package com.xuecheng.learning.service.impl;

import com.alibaba.fastjson.JSON;
import com.rabbitmq.client.Channel;
import com.xuecheng.base.exception.XueChengPlusException;
import com.xuecheng.learning.config.PayNotifyConfig;
import com.xuecheng.learning.service.MyCourseTablesService;
import com.xuecheng.messagesdk.model.po.MqMessage;
import com.xuecheng.messagesdk.service.MqMessageService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.IOException;


/** 接收order服务发送的消息队列的通知
 *  监听配置类中的支付通知队列
 * @author zijianLi
 * @create 2023- 05- 12- 20:15
 */
@Service
@Slf4j
public class ReceivePayNotifyService {
    @Autowired
    MyCourseTablesService myCourseTablesService;

    @Autowired
    private RabbitTemplate rabbitTemplate;

    @Autowired
    MqMessageService mqMessageService;


    //监听接收支付结果通知PAYNOTIFY_QUEUE的消息队列
    /**
     *
     * @param message 这个是spring框架的message对象，不是我们发的message对象
     */
    @RabbitListener(queues = PayNotifyConfig.PAYNOTIFY_QUEUE)
    public void receive(Message message) {
        //收到消息执行receive方法之前先进行休眠5秒,
        // 目的是当接收消息队列失败时到重新接收消息队列中间间隔5秒
        //消息队列在接收失败，抛出异常时仍会重复发送直到接收成功
        try {
            Thread.sleep(5000);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
        //获取消息
        byte[] body = message.getBody();
        String jsonString = new String(body);
        MqMessage mqMessage = JSON.parseObject(jsonString, MqMessage.class);
        log.debug("学习中心服务接收支付结果:{}", mqMessage);

        //根据message的内容，更新选课记录表为已购买状态，并且向我的课程表插入记录
        //消息类型
        String messageType = mqMessage.getMessageType();
        //businessKey2是订单类型,  60201表示购买课程
        String businessKey2 = mqMessage.getBusinessKey2();
        //这里只处理支付结果通知
        if (PayNotifyConfig.MESSAGE_TYPE.equals(messageType) && "60201".equals(businessKey2)) {
            //getBusinessKey1就是选课记录id，因为order服务设置消息的时候就这么设置的
            String choosecourseId = mqMessage.getBusinessKey1();
            //添加选课
            boolean b = myCourseTablesService.saveChooseCourseSuccess(choosecourseId);
            if(!b){
                //添加选课失败，抛出异常，消息重回队列
                XueChengPlusException.cast("收到支付结果成功，但是添加选课失败，方法为receive方法");
            }
        }
    }

}

