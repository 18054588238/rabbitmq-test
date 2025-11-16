package com.personal.rabbitmqspringboot.example;

import com.personal.rabbitmqspringboot.RabbitmqSpringbootTestApplication;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.rabbit.listener.adapter.MessageListenerAdapter;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

/**
 * @ClassName Runner
 * @Author liupanpan
 * @Date 2025/11/16
 * @Description 测试消息由CommandLineRunner发送
 * 监听消息需要消息监听容器和接收器
 * 发送消息需要一个RabbitTemplate
 */
// 会自定运行
@Component
public class Runner implements CommandLineRunner {
    private final RabbitTemplate rabbitTemplate;
    private final Receiver receiver;

    public Runner(RabbitTemplate rabbitTemplate, Receiver receiver) {
        this.rabbitTemplate = rabbitTemplate;
        this.receiver = receiver;
    }

    @Override
    public void run(String... args) throws Exception {
        System.out.println("sending message......");
        rabbitTemplate.convertAndSend(RabbitmqSpringbootTestApplication.topicExchangeName,
                "foo.bar.baz",
                "hello from RabbitMQ");
//      count.await(); // 等待计数变为0然后继续执行
        receiver.getCount().await(10000, TimeUnit.MILLISECONDS);
    }
}
