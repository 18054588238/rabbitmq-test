package com.test.demo01;

import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import com.rabbitmq.client.DeliverCallback;

import java.nio.charset.StandardCharsets;

/**
 * @ClassName Recv
 * @Author liupanpan
 * @Date 2025/11/12
 * @Description 消费者
 */
public class Recv {
    // 队列名称
    private static final String QUEUE_NAME = "lpp-rabbitmq-test";

    public static void main(String[] args) throws Exception {
        // 不使用try-with-resource 是因为 希望进程 在消费者异步监听消息到达时 保持运行
        ConnectionFactory factory = new ConnectionFactory();
        factory.setHost("172.16.30.21");
        factory.setUsername("rabbitmq");
        factory.setPassword("310012");
        Connection connection = factory.newConnection();
        Channel channel = connection.createChannel();
        // 这里创建队列 是因为消费者可能会在生产者之前启动，是为了确保队列存在后再尝试从中消费消息
        channel.queueDeclare(QUEUE_NAME,false,false,false,null);
        System.out.println(" [*] Waiting for messages. To exit press CTRL+C");
        // 异步消息--使用回调函数
        DeliverCallback deliverCallback = (consumerTag,delivery) -> {
            String message = new String(delivery.getBody(), StandardCharsets.UTF_8);
            System.out.println("received--->"+message);
        };
        channel.basicConsume(QUEUE_NAME,true,deliverCallback,consumerTag -> {});
    }
}
