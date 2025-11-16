package com.personal.rabbitmqorderconsumer.service;

import org.springframework.amqp.rabbit.annotation.RabbitHandler;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

/**
 * @ClassName TopicSmsConsumer
 * @Author liupanpan
 * @Date 2025/11/16
 * @Description
 */
@Component
@RabbitListener(queues = "lpp-sms-order-queue")
public class TopicSmsConsumer {
    @RabbitHandler
    public void receiveOrder(String message) {
        System.out.println("sms接收的消息："+message);
    }
}
