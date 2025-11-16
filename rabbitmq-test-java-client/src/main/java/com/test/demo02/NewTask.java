package com.test.demo02;

import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import com.rabbitmq.client.MessageProperties;

import java.nio.charset.StandardCharsets;

/**
 * @ClassName NewTask
 * @Author liupanpan
 * @Date 2025/11/12
 * @Description 生产者
 */
public class NewTask {
    // 队列名称
    private static final String TASK_QUEUE_NAME = "lpp-rabbitmq-test";
    public static void main(String[] args) throws Exception {
        // 连接rabbitmq服务器
        ConnectionFactory factory = new ConnectionFactory();
        factory.setHost("172.16.30.21");
        factory.setUsername("rabbitmq");
        factory.setPassword("310012");
        // 创建一个通道，用于完成各种操作
        try(Connection connection = factory.newConnection();
            Channel channel = connection.createChannel()) {

            channel.queueDeclare(TASK_QUEUE_NAME,true,false,false,null);
            String message = "hello......";
            // 将消息传到队列中
            channel.basicPublish("",
                    TASK_QUEUE_NAME,
                    MessageProperties.PERSISTENT_TEXT_PLAIN, // 设置持久化消息
                    message.getBytes(StandardCharsets.UTF_8));
            System.out.println("send----->"+message);
        }
    }
}
