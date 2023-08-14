package com.xuecheng.orders.service.impl;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.alipay.api.AlipayApiException;
import com.alipay.api.AlipayClient;
import com.alipay.api.DefaultAlipayClient;
import com.alipay.api.request.AlipayTradeQueryRequest;
import com.alipay.api.response.AlipayTradeQueryResponse;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.xuecheng.base.exception.XueChengPlusException;
import com.xuecheng.base.utils.IdWorkerUtils;
import com.xuecheng.base.utils.QRCodeUtil;
import com.xuecheng.messagesdk.model.po.MqMessage;
import com.xuecheng.messagesdk.service.MqMessageService;
import com.xuecheng.orders.config.AlipayConfig;
import com.xuecheng.orders.config.PayNotifyConfig;
import com.xuecheng.orders.mapper.XcOrdersGoodsMapper;
import com.xuecheng.orders.mapper.XcOrdersMapper;
import com.xuecheng.orders.mapper.XcPayRecordMapper;
import com.xuecheng.orders.model.dto.AddOrderDto;
import com.xuecheng.orders.model.dto.PayRecordDto;
import com.xuecheng.orders.model.dto.PayStatusDto;
import com.xuecheng.orders.model.po.XcOrders;
import com.xuecheng.orders.model.po.XcOrdersGoods;
import com.xuecheng.orders.model.po.XcPayRecord;
import com.xuecheng.orders.service.OrderService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageBuilder;
import org.springframework.amqp.core.MessageDeliveryMode;
import org.springframework.amqp.rabbit.connection.CorrelationData;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/** 订单相关的接口
 * @author zijianLi
 * @create 2023- 05- 08- 21:32
 */
@Slf4j
@Service
public class OrderServiceImpl implements OrderService {
    @Autowired
    XcOrdersMapper ordersMapper;
    @Autowired
    XcOrdersGoodsMapper ordersGoodsMapper;

    @Autowired
    XcPayRecordMapper payRecordMapper;

    @Value("${pay.qrcodeurl}")
    String qrcodeurl;

    @Value("${pay.alipay.APP_ID}")
    String APP_ID;
    @Value("${pay.alipay.APP_PRIVATE_KEY}")
    String APP_PRIVATE_KEY;

    @Value("${pay.alipay.ALIPAY_PUBLIC_KEY}")
    String ALIPAY_PUBLIC_KEY;

    @Autowired
    OrderServiceImpl currentProxy;

    @Autowired//rabbitmq的模板类
    RabbitTemplate rabbitTemplate;

    @Autowired//消息服务的service
    MqMessageService mqMessageService;

    @Transactional
    @Override
    /** 完成创建商品订单、创建支付记录
     * @param addOrderDto 订单信息
     * @return PayRecordDto 支付记录(包括二维码)
     * @description 创建商品订单
     * @author LiZijian
     */
    public PayRecordDto createOrder(String userId, AddOrderDto addOrderDto) {
        //幂等性判断，同一个选课记录只能有一个订单，即根据business_id的唯一性来实现。
        //先判断business_id是不是有了，有了的话就不插入了
        //添加商品订单,在订单表和订单明细表插入
        XcOrders xcOrders = saveXcOrders(userId, addOrderDto);
        if(xcOrders==null){
            XueChengPlusException.cast("订单创建失败");
        }

        //添加支付记录，在支付记录表插入
        XcPayRecord payRecord = createPayRecord(xcOrders);
        //生成二维码
        String qrCode = null;
        try {
            //url要可以被模拟器访问到，url为下单接口(稍后定义)
            String url = String.format(qrcodeurl, payRecord.getPayNo());
            qrCode = new QRCodeUtil().createQRCode(url, 200, 200);
        } catch (IOException e) {
            XueChengPlusException.cast("生成订单二维码出错");
        }
        PayRecordDto payRecordDto = new PayRecordDto();
        BeanUtils.copyProperties(payRecord,payRecordDto);
        payRecordDto.setQrcode(qrCode);

        return payRecordDto;

    }


    /**
     * 生成支付二维码的时候，在订单表保存订单信息,在订单明细表插入记录
     * @param userId
     * @param addOrderDto
     * @return
     */
    public XcOrders saveXcOrders(String userId, AddOrderDto addOrderDto){
        //幂等性处理，如果根据选课的id查询order表，order已存在，就直接返回
        XcOrders order = getOrderByBusinessId(addOrderDto.getOutBusinessId());
        if(order!=null){
            return order;
        }


        order = new XcOrders();
        //雪花算法，生成订单表的id
        long orderId = IdWorkerUtils.getInstance().nextId();
        order.setId(orderId);
        order.setTotalPrice(addOrderDto.getTotalPrice());
        order.setCreateDate(LocalDateTime.now());
        order.setStatus("600001");//未支付状态
        order.setUserId(userId);
        order.setOrderType(addOrderDto.getOrderType());
        order.setOrderName(addOrderDto.getOrderName());
        order.setOrderDetail(addOrderDto.getOrderDetail());
        order.setOrderDescrip(addOrderDto.getOrderDescrip());
        order.setOutBusinessId(addOrderDto.getOutBusinessId());//这里的业务id是选课记录id
        //插入订单主表
        int insert = ordersMapper.insert(order);
        if(insert<=0){
            XueChengPlusException.cast("订单插入订单表失败");
        }

        String orderDetailJson = addOrderDto.getOrderDetail();
        //将前端传入的string串转为list，类型就是XcOrdersGoods
        List<XcOrdersGoods> xcOrdersGoodsList = JSON.parseArray(orderDetailJson, XcOrdersGoods.class);
        xcOrdersGoodsList.forEach(goods->{
            XcOrdersGoods xcOrdersGoods = new XcOrdersGoods();
            BeanUtils.copyProperties(goods,xcOrdersGoods);
            xcOrdersGoods.setOrderId(orderId);//订单号
            //插入订单明细表
            ordersGoodsMapper.insert(xcOrdersGoods);
        });
        return order;
    }

    //根据业务id查询订单,来判断幂等性
    public XcOrders getOrderByBusinessId(String businessId) {
        XcOrders orders = ordersMapper.selectOne(
                new LambdaQueryWrapper<XcOrders>().eq(XcOrders::getOutBusinessId, businessId));
        return orders;
    }

    /**
     * 生成支付二维码的时候在支付记录表插入数据，保存支付记录
     * @param orders
     * @return
     */
    public XcPayRecord createPayRecord(XcOrders orders){
        //幂等性处理，如果已经存在了，就不添加了。根据order表的id是唯一的来确定。这个order表的id就是record的order_id
        Long id = orders.getId();
        XcOrders xcOrders = ordersMapper.selectById(id);

        if(xcOrders==null){
            //说明是脏数据
            XueChengPlusException.cast("订单不存在");
        }
        //数据库的这个订单已经是支付成功了
        if(xcOrders.getStatus().equals("600002")){
            XueChengPlusException.cast("订单已支付");
        }

        //添加支付记录表
        XcPayRecord payRecord = new XcPayRecord();
        //使用雪花算法生成支付交易流水号，这个要传给支付宝
        long payNo = IdWorkerUtils.getInstance().nextId();
        payRecord.setPayNo(payNo);
        payRecord.setOrderId(orders.getId());//商品订单号
        payRecord.setOrderName(orders.getOrderName());
        payRecord.setTotalPrice(orders.getTotalPrice());
        payRecord.setCurrency("CNY");
        payRecord.setCreateDate(LocalDateTime.now());
        payRecord.setStatus("601001");//未支付
        payRecord.setUserId(orders.getUserId());
        //添加支付记录表
        int insert = payRecordMapper.insert(payRecord);
        if (insert<=0){
            XueChengPlusException.cast("插入支付记录表失败");
        }
        return payRecord;

    }

    @Override
    public XcPayRecord getPayRecordByPayno(String payNo) {
        XcPayRecord xcPayRecord = payRecordMapper.selectOne(new LambdaQueryWrapper<XcPayRecord>().eq(XcPayRecord::getPayNo, payNo));
        return xcPayRecord;
    }

    @Override
    /**
     * 主动请求支付宝查询支付结果
     * @param payNo 支付记录id
     * @return 支付记录信息
     */
    public PayRecordDto queryPayResult(String payNo) {

        //调用支付宝的接口查询支付结果
        PayStatusDto payStatusDto = queryPayResultFromAlipay(payNo);

        //拿到支付结果更新支付记录表和订单表的支付状态
        //order表的订单状态改为支付成功,pay_record支付记录改为支付成功
        currentProxy.saveAliPayStatus(payStatusDto);

        //返回参数，从数据库查询最新的支付信息
        //从数据库查询最新的信息
        XcPayRecord payRecordByPayno = getPayRecordByPayno(payNo);
        PayRecordDto payRecordDto = new PayRecordDto();
        BeanUtils.copyProperties(payRecordByPayno, payRecordDto);


        return payRecordDto;
    }


    /**
     * 请求支付宝查询支付结果
     * @param payNo 支付交易号
     * @return 支付结果
     */
    public PayStatusDto queryPayResultFromAlipay(String payNo){
        //========请求支付宝查询支付结果=============
        AlipayClient alipayClient = new DefaultAlipayClient(
                AlipayConfig.URL, APP_ID, APP_PRIVATE_KEY, "json",
                AlipayConfig.CHARSET, ALIPAY_PUBLIC_KEY, AlipayConfig.SIGNTYPE); //获得初始化的AlipayClient
        AlipayTradeQueryRequest request = new AlipayTradeQueryRequest();
        JSONObject bizContent = new JSONObject();
        bizContent.put("out_trade_no", payNo);
        request.setBizContent(bizContent.toString());
        AlipayTradeQueryResponse response = null;
        try {
            response = alipayClient.execute(request);
            if (!response.isSuccess()) {
                XueChengPlusException.cast("请求支付宝查询支付结果queryPayResultFromAlipay查询失败");
            }
        } catch (AlipayApiException e) {
            log.error("请求支付宝查询支付结果异常:{}", e.toString(), e);
            XueChengPlusException.cast("请求支付查询queryPayResultFromAlipay查询失败");
        }

        //获取支付结果
        String resultJson = response.getBody();
        //转map
        Map resultMap = JSON.parseObject(resultJson, Map.class);
        Map alipay_trade_query_response = (Map) resultMap.get("alipay_trade_query_response");
        //支付结果
        String trade_status = (String) alipay_trade_query_response.get("trade_status");
        String total_amount = (String) alipay_trade_query_response.get("total_amount");
        String trade_no = (String) alipay_trade_query_response.get("trade_no");
        //保存支付结果
        PayStatusDto payStatusDto = new PayStatusDto();
        payStatusDto.setOut_trade_no(payNo);
        payStatusDto.setTrade_status(trade_status);
        payStatusDto.setApp_id(APP_ID);
        payStatusDto.setTrade_no(trade_no);
        payStatusDto.setTotal_amount(total_amount);
        return payStatusDto;

    }

    /**
     * @description 保存查询支付宝得到的支付结果到数据表
     *  order表的订单状态改为支付成功,pay_record支付记录改为支付成功，且在保存支付结果后向学习服务发送消息队列
     * @param payStatusDto  支付结果信息
     * @return void
     * @author Mr.M
     * @date 2022/10/4 16:52
     */
    @Transactional
    @Override
    public void saveAliPayStatus(PayStatusDto payStatusDto){
        //如果支付成功就
        String payNo = payStatusDto.getOut_trade_no();//这个就是支付记录号
        //根据支付流水号从数据库PayRecord查询到的payRecordByPayno对象，目的是要把支付宝返回的结果封装到payRecordByPayno对象，然后更新payRecord表
        XcPayRecord payRecordByPayno = getPayRecordByPayno(payNo);
        if (payRecordByPayno==null){
            XueChengPlusException.cast("找不到相关的支付记录");
        }
        //拿到相关联的订单id
        Long orderId = payRecordByPayno.getOrderId();
        //在order表里面找到对应的数据
        XcOrders xcOrders = ordersMapper.selectById(orderId);
        if (xcOrders==null){
            XueChengPlusException.cast("找不到相关的订单");
        }
        //支付状态
        String statusFromDB = payRecordByPayno.getStatus();
        if ("601002".equals(statusFromDB)){
            //如果数据库已经是成功了,说明已经成功了
            return;
        }

        //开始修改数据
        String trade_status = payStatusDto.getTrade_status();//这个是从支付宝查询到的状态
        if (trade_status.equals("TRADE_SUCCESS")){
            //pay_record支付记录改为支付成功

            payRecordByPayno.setStatus("601002");
            payRecordByPayno.setOutPayNo(payStatusDto.getTrade_no());//支付宝的订单号
            //第三方支付渠道编号
            payRecordByPayno.setOutPayChannel("Alipay");//这个方法是阿里支付宝方法，所以渠道名写死
            payRecordByPayno.setPaySuccessTime(LocalDateTime.now());
            payRecordMapper.updateById(payRecordByPayno);

            //order表的订单状态改为支付成功
            xcOrders.setStatus("600002");
            //只更新状态
            ordersMapper.updateById(xcOrders);


            //发送支付消息到learning服务
            //将消息内容写入数据库mqMessage
            //保存消息记录,参数1：支付结果通知类型，payresult_notify   businessKey1: 业务id，即选课的id。  businessKey2:业务类型 ，购买课程的业务类型60201
            MqMessage mqMessage = mqMessageService.addMessage("payresult_notify", xcOrders.getOutBusinessId(), xcOrders.getOrderType(), null);
            //通知消息,发送支付消息
            notifyPayResult(mqMessage);

        }
    }

    @Override
    /**
     * 通过消息队列发送通知结果 这个方法在saveAliPayStatus中调用
     * @param message 是写的消息sdk，里面增删改查都有
     */
    public void notifyPayResult(MqMessage message) {

        //1、消息体message，转json msg
        String msg = JSON.toJSONString(message);
        //设置消息持久化，withBody参数是一个消息message的byte数组，指定字符编码
        Message msgObj = MessageBuilder.withBody(msg.getBytes(StandardCharsets.UTF_8))
                .setDeliveryMode(MessageDeliveryMode.PERSISTENT)//设置消息的投递类型为持久化
                .build();

        // 2.全局唯一的消息ID，需要封装到CorrelationData中
        //这个CorrelationData可以指定回调方法
        //new CorrelationData的构造方法中的参数需要指定全局消息id,即这个消息id不能重复
        //可以用message的自增主键message.getId()来当做消息id，保证不重复
        CorrelationData correlationData = new CorrelationData(message.getId().toString());
        // 3.添加callback,指定回调方法
        correlationData.getFuture().addCallback(
                result -> {
                    if(result.isAck()){
                        //此时消息发送成功了
                        // 3.1.ack，消息成功
                        log.debug("通知支付结果消息发送成功, ID:{}", correlationData.getId());
                        //删除消息表中的记录
                        mqMessageService.completed(message.getId());//消息发送成功了就调用completed方法，删除消息表mq_message的记录
                    }else{
                        // 3.2.nack，消息失败
                        log.error("通知支付结果消息发送失败, ID:{}, 原因{}",correlationData.getId(), result.getReason());
                    }
                },
                ex -> log.error("消息发送异常, ID:{}, 原因{}",correlationData.getId(),ex.getMessage())
        );
        // 发送消息
        //目前是广播模式，要指定交换机。第一个参数是交换机，第二个是路由。目前是广播，所有的都发，不需要路由，第三个是消息本身msgObj
        //第四个参数是用于接收消息回调的对象correlationData
        rabbitTemplate.convertAndSend(PayNotifyConfig.PAYNOTIFY_EXCHANGE_FANOUT, "", msgObj,correlationData);

    }




}
