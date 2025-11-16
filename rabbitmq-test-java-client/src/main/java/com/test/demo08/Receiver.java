package com.test.demo08;

import com.rabbitmq.stream.*;

import java.io.IOException;

/**
 * @ClassName Receiver
 * @Author liupanpan
 * @Date 2025/11/16
 * @Description stream java
 */
public class Receiver {
    public static void main(String[] args) throws IOException {
        Environment environment = Environment.builder()
                .host("172.16.30.21")
                .username("rabbitmq")
                .password("310012")
                .build();
        String stream = "lpp-stream-java-test";
        environment.streamCreator()
                .stream(stream)
                .maxLengthBytes(ByteCapacity.GB(5)) // 流的大小限制为5gib
                .create();
        // 消费消息 - 允许重复消费消息，直到消息过期
        Consumer consumer = environment.consumerBuilder() // 实例化流消费者
                .stream(stream)
                .offset(OffsetSpecification.first()) // 定义消费者的起始位置，此处消费者从流中第一条消息开始处理
                .messageHandler((unused, message) -> {
                    // 处理已传递消息
                    System.out.println("收到的消息：" + new String(message.getBodyAsBinary()));
                }).build();

        System.in.read();
        consumer.close();
        environment.close();
    }
}
