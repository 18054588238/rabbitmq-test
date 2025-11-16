package com.personal.rabbitmqorderproducer;

import com.personal.rabbitmqorderproducer.service.OrderService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
class RabbitmqOrderProducerApplicationTests {

    @Autowired
    OrderService orderService;
    @Test
    void contextLoads() {
        orderService.sendOrder();
    }

}
