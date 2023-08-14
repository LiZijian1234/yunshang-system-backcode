package com.xuecheng.learning.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.*;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** 消息队列的配置类，实现方法在 ReceivePayNotifyService里面
 * 生产方是order微服务，消费方是learning微服务
 * 在生产方和消费方都要创建消息队列。
 *  如果消费方先启动的话，开始监听消息队列。如果只在生产方有消息队列的话，消息队列还没启动，导致出错
 *  交换机和队列的名字启动之后，都能在rabbitmq的网站上查询到
 * @author Mr.M
 * @version 1.0
 * @description TODO
 * @date 2023/2/23 16:59
 */
@Slf4j
@Configuration
public class PayNotifyConfig {

    //交换机的名字
    public static final String PAYNOTIFY_EXCHANGE_FANOUT = "paynotify_exchange_fanout";
    //支付结果通知消息类型
    public static final String MESSAGE_TYPE = "payresult_notify";
    //支付通知队列的名字
    public static final String PAYNOTIFY_QUEUE = "paynotify_queue";

    //声明交换机，且持久化
    @Bean(PAYNOTIFY_EXCHANGE_FANOUT)
    public FanoutExchange paynotify_exchange_fanout() {
        // 三个参数：交换机名称、是否持久化、当没有queue与其绑定时是否自动删除
        return new FanoutExchange(PAYNOTIFY_EXCHANGE_FANOUT, true, false);
    }
    //支付通知队列,且持久化
    @Bean(PAYNOTIFY_QUEUE)
    public Queue course_publish_queue() {
        return QueueBuilder.durable(PAYNOTIFY_QUEUE).build();
    }

    //交换机和支付通知队列绑定
    @Bean
    public Binding binding_course_publish_queue(@Qualifier(PAYNOTIFY_QUEUE) Queue queue, @Qualifier(PAYNOTIFY_EXCHANGE_FANOUT) FanoutExchange exchange) {
        return BindingBuilder.bind(queue).to(exchange);
    }
}
