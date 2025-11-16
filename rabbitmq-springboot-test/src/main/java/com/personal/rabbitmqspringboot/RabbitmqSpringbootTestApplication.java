package com.personal.rabbitmqspringboot;

import com.personal.rabbitmqspringboot.example.Receiver;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.listener.SimpleMessageListenerContainer;
import org.springframework.amqp.rabbit.listener.adapter.MessageListenerAdapter;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

/** 执行流程
 * 'main（）、方法首先创建一个 Spring 应用程序上下文，然后启动消息监听器容器
 * 该容器开始监听消息。接下来，会自动运行一个 Runner bean。
 * 它会从应用程序上下文中检索 RabbitTemplate，并向Spring Boot 队列发送一条“Hello from RabbitMQ！”消息。
 * 最后，它会关闭 Spring 应用程序上下文，应用程序结束
 */

@SpringBootApplication
public class RabbitmqSpringbootTestApplication {

//    springboot会自动创建连接工厂和RabbitTemplate
    public static final String topicExchangeName = "lpp-spring-boot-exchange";
    public static final String queueName = "lpp-spring-boot-queue";

    // 队列
    @Bean
    Queue queue() {
        return new org.springframework.amqp.core.Queue(queueName,false);
    }
    // 交换机
    @Bean
    TopicExchange exchange() {
        return new TopicExchange(topicExchangeName);
    }
    // 队列和交换机绑定
    @Bean
    Binding binding(Queue queue, TopicExchange exchange) {
        // 任何以 foo.bar. 开头的路由键发送的消息都会被路由到该队列
        return BindingBuilder.bind(queue).to(exchange).with("foo.bar.#");// 使用通配符匹配多个队列
    }

    @Bean
    SimpleMessageListenerContainer container(ConnectionFactory connectionFactory, MessageListenerAdapter listenerAdapter) {
        SimpleMessageListenerContainer container = new SimpleMessageListenerContainer();
        container.setConnectionFactory(connectionFactory);
        container.setQueueNames(queueName);
        container.setMessageListener(listenerAdapter);
        return container;
    }
    // 监听spring boot 队列中的消息
    @Bean
    MessageListenerAdapter listenerAdapter(Receiver receiver) {
        // 向消息监听器注册一个Receiver来接收消息
        return new MessageListenerAdapter(receiver, "receiveMessage");// 指定调用的方法
    }
    public static void main(String[] args) throws InterruptedException {
        SpringApplication.run(RabbitmqSpringbootTestApplication.class, args).close();
    }

}
