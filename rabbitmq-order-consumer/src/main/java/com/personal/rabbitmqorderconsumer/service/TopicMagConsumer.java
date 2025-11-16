package com.personal.rabbitmqorderconsumer.service;

import org.springframework.amqp.rabbit.annotation.RabbitHandler;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

/**
 * @ClassName TopicMagConsumer
 * @Author liupanpan
 * @Date 2025/11/16
 * @Description
 */
@Component
@RabbitListener(queues = "lpp-msg-order-queue")
public class TopicMagConsumer {
    @RabbitHandler
    public void receiveOrder(String message) {
        System.out.println("msg接收的消息："+message);
    }
}
