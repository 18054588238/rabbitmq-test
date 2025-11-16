package com.test.demo02;

import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import com.rabbitmq.client.DeliverCallback;

import java.nio.charset.StandardCharsets;

/**
 * @ClassName Worker
 * @Author liupanpan
 * @Date 2025/11/12
 * @Description 消费者 - Work Queues避免立即执行那些需要消耗大量资源的任务
 */
public class Worker {
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
        /*
        * durable:true,保证 队列 在服务器重启后仍然存在，防止在rabbitmq服务器崩溃后 消息丢失
        * */
        channel.queueDeclare(QUEUE_NAME,true,false,false,null);
        System.out.println(" [*] Waiting for messages. To exit press CTRL+C");
        /*
        * 保证一次只向一个 工作进程 发送一条消息
        * 也就是在工作进程处理并确认前一条消息之前，不向它发送新消息，
        * 会将新消息发送给下一个空闲的工作进程，可以避免一个进程一直处于忙碌状态另一个进程一直处于空闲状态的情况
        * */
        channel.basicQos(1); // 一次只接受一条未确认的消息，0 表示无限制发送。
        // 异步消息--使用回调函数
        DeliverCallback deliverCallback = (consumerTag, delivery) -> {
            String message = new String(delivery.getBody(), StandardCharsets.UTF_8);
            System.out.println("received--->"+message);
            try {
                doWork(message); // 通过休眠模拟处理复杂任务
            } finally {
                System.out.println("---Done---");

                // deliveryTag – 来自接收到的 AMQP.Basic.GetOk 或 AMQP.Basic 的标签。
                // multiple – true 表示确认所有消息，包括指定的 deliveryTag；false 表示仅确认指定的 deliveryTag。
                /* 消息确认机制 -- 消费者向rabbitmq发送确认消息，告知它已经接收并处理了特定消息，可以将其删除了
                * 目的：保证消息不丢失
                * 场景：消费者在消费消息时被终止了，这时消息并没有被处理完成，消息队列中又将该消息删除了，这就导致了消息的丢失
                *
                * 如果确认消息没有发送成功，rabbitmq会重新将消息放到队列中，供其他消费者消费
                * */
                channel.basicAck(delivery.getEnvelope().getDeliveryTag(),false);
            }
        };
        //autoAck:false,显示确认，保证数据不丢失
        channel.basicConsume(QUEUE_NAME,false,deliverCallback,consumerTag -> {});
    }

    public static void doWork(String task) {
        for (char ch : task.toCharArray()) {
            if (ch == '.') {
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException _ignored) {
                    Thread.currentThread().interrupt();
                }
            }
        }
    }
}
