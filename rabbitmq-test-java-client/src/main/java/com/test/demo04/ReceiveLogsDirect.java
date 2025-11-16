package com.test.demo04;

import com.rabbitmq.client.*;

import java.nio.charset.StandardCharsets;

/**
 * @ClassName ReceiveLogsDirect
 * @Author liupanpan
 * @Date 2025/11/13
 * @Description 只接收roukingKey完全匹配的消息
 */
public class ReceiveLogsDirect {
    // 交换机名称
    private static final String EXCHANGE_NAME = "lpp-exchange-test-direct";
    public static void main(String[] args) throws Exception{
        // 连接rabbitmq服务器
        ConnectionFactory factory = new ConnectionFactory();
        factory.setHost("172.16.30.21");
        factory.setUsername("rabbitmq");
        factory.setPassword("310012");
        Connection connection = factory.newConnection();
        Channel channel = connection.createChannel();
        // 声明交换机
        channel.exchangeDeclare(EXCHANGE_NAME, BuiltinExchangeType.DIRECT);//扇出交换机
        /*
         * queueDeclare()不传参数时，默认创建一个非持久化、独占、自动删除的队列，系统自动生成队列名称
         * 消费者一旦断开连接后，队列会自动删除，每次都会创建一个全新的空队列
         * 保证得到的都是当前正在传输的消息，满足日志的要求
         * */
        String queue = channel.queueDeclare().getQueue();
        String severity = "info";
        // 交换机和队列绑定
        channel.queueBind(queue,EXCHANGE_NAME,severity);//队列只接收来自该交换机的消息, 指定routingKey
        System.out.println("自动生成的队列名称："+queue);
        // 接收消息
        DeliverCallback deliverCallback = (consumerTag, delivery) -> {
            String message = new String(delivery.getBody(), StandardCharsets.UTF_8);
            System.out.println("routingKey"+delivery.getEnvelope().getRoutingKey()+"收到的消息--->"+message);
        };
        channel.basicConsume(queue,true,deliverCallback,consumerTag -> {});
    }
}
