package com.test.demo06;

import com.rabbitmq.client.*;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.TimeoutException;

/**
 * @ClassName RPCServer
 * @Author liupanpan
 * @Date 2025/11/14
 * @Description 既是生产者又是消费者
 */
public class RPCServer {
    private static final String RPC_QUEUE_NAME = "rpc_queue";

    public static int fib(int n) {
        if (n==0) return 0;
        if (n==1) return 1;
        return fib(n-1)+fib(n-2);
    }


    public static void main(String[] args) throws IOException, TimeoutException {
        ConnectionFactory factory = new ConnectionFactory();
        factory.setHost("172.16.30.21");
        factory.setUsername("rabbitmq");
        factory.setPassword("310012");
        Connection connection = factory.newConnection();
        Channel channel = connection.createChannel();
        channel.queueDeclare(RPC_QUEUE_NAME,false,false,false,null);
        channel.queuePurge(RPC_QUEUE_NAME);// 清除给定队列的内容

        channel.basicQos(1); // 一次只接受一条未确认的消息
        System.out.println("等待RPC请求");

        DeliverCallback deliverCallback = (consumerTag, delivery) -> {

            AMQP.BasicProperties replyProps = new AMQP.BasicProperties.Builder()
                    .correlationId(delivery.getProperties().getCorrelationId())// 获取correlationId
                    .build();

            String response = "";
            try {
                String message = new String(delivery.getBody(), StandardCharsets.UTF_8);// 消费消息
                int n = Integer.parseInt(message);
                System.out.println("接收的消息 --> "+message);
                response += fib(n);
            } catch (RuntimeException e) {
                System.out.println("异常--"+e);
            } finally {
                channel.basicPublish("",
                        delivery.getProperties().getReplyTo(),
                        replyProps,
                        response.getBytes(StandardCharsets.UTF_8));// 返回响应结果
                channel.basicAck(delivery.getEnvelope().getDeliveryTag(),false);
            }
        };

        channel.basicConsume(RPC_QUEUE_NAME,false,deliverCallback,(consumerTag -> {}));
    }
}
