package com.test.demo05;

import com.rabbitmq.client.BuiltinExchangeType;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;

import java.nio.charset.StandardCharsets;

/**
 * @ClassName EmitLogTopic
 * @Author liupanpan
 * @Date 2025/11/13
 * @Description
 */
public class EmitLogTopic {
    // 交换机名称
    private static final String EXCHANGE_NAME = "lpp-exchange-test-topic";
    public static void main(String[] args) throws Exception{
        // 连接rabbitmq服务器
        ConnectionFactory factory = new ConnectionFactory();
        factory.setHost("172.16.30.21");
        factory.setUsername("rabbitmq");
        factory.setPassword("310012");

        try(Connection connection = factory.newConnection();
            Channel channel = connection.createChannel()) {
            // 声明交换机
            channel.exchangeDeclare(EXCHANGE_NAME, BuiltinExchangeType.TOPIC);//直接交换机
            String severity = "anonymous.info";
            // 发送消息
            String message = "exchange message test...";
            channel.basicPublish(EXCHANGE_NAME,severity,null,message.getBytes(StandardCharsets.UTF_8));// 指定routingKey
            System.out.println("send message --- > "+message);
        }
    }
}
