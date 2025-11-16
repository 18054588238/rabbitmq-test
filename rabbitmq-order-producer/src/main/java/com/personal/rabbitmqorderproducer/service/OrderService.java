package com.personal.rabbitmqorderproducer.service;

import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.UUID;

/**
 * @ClassName OrderService
 * @Author liupanpan
 * @Date 2025/11/16
 * @Description
 */
@Service
public class OrderService {
    @Autowired
    private RabbitTemplate rabbitTemplate;
    public void sendOrder(){
        String orderId = UUID.randomUUID().toString().replaceAll("-", "");
        System.out.println("订单id："+orderId);
        String exchangeName = "lpp-test-order-exchange";
        String routingKey = "lpp.mag.queue.test";
        rabbitTemplate.convertAndSend(exchangeName,routingKey,orderId);
    }
}
