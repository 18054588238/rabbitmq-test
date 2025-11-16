package com.test.demo08;

import com.rabbitmq.stream.ByteCapacity;
import com.rabbitmq.stream.Environment;
import com.rabbitmq.stream.Producer;

import java.io.IOException;

/**
 * @ClassName Send
 * @Author liupanpan
 * @Date 2025/11/16
 * @Description stream java
 */
public class Send {
    public static void main(String[] args) throws IOException {
        // 实例化一个环境 - 流式java客户端的入口点是环境，
        // 用于配置rabbitmq流发布者、流消费者以及流本身
        Environment environment = Environment.builder()
                .host("172.16.30.21")
                .username("rabbitmq")
                .password("310012")
                .build();
        String stream = "lpp-stream-java-test";
        environment.streamCreator()
                .stream(stream)
                .maxLengthBytes(ByteCapacity.GB(5)) //流的大小限制为5gib
                .create();
        // 发送消息 - 每次运行生产者时，它都会向服务器发送一条消息，该消息将被追加到流中。
        Producer producer = environment.producerBuilder().stream(stream).build();
        producer.send(producer.messageBuilder().addData("congratulations yyqx".getBytes()).build(),null);

        System.out.println("------消息发送------");
        System.in.read();
        producer.close();
        environment.close();
    }
}
