package com.personal.rabbitmqorderconsumer.service;

import org.springframework.amqp.rabbit.annotation.RabbitHandler;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

/**
 * @ClassName TopicEmailConsumer
 * @Author liupanpan
 * @Date 2025/11/16
 * @Description
 */
@Component
@RabbitListener(queues = "lpp-email-order-queue")
public class TopicEmailConsumer {
    // 此注解用于 标记类 中被注解为 RabbitListener 的方法，使其成为 Rabbit 消息监听器的目标。
    @RabbitHandler
    public void receiveOrder(String message){
        System.out.println("email接收的消息："+message);
    }
}
