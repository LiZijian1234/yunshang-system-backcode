package com.xuecheng.orders;

import com.xuecheng.base.utils.QRCodeUtil;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.io.IOException;

/**
 * @description TODO
 * @author Mr.M
 * @date 2022/10/2 10:32
 * @version 1.0
 */
 @SpringBootTest
public class Test1 {


  @Test
  //下单支付
 public void test() throws IOException {
      QRCodeUtil qrCodeUtil = new QRCodeUtil();
      System.out.println(qrCodeUtil.createQRCode("http://192.168.101.1:63030/orders/alipaytest",
              200, 200));
  }

}
