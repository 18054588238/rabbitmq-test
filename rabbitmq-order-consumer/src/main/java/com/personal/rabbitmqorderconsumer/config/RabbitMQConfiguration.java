package com.personal.rabbitmqorderconsumer.config;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * @ClassName RabbitMQConfiguration
 * @Author liupanpan
 * @Date 2025/11/16
 * @Description 放在消费者端，因为消费者是先开启的，如果没有交换机和队列，则会报错
 */
@Configuration
public class RabbitMQConfiguration {
    // 队列
    @Bean
    Queue smsQueue() {
        return new org.springframework.amqp.core.Queue("lpp-sms-order-queue",false);
    }
    @Bean
    Queue emailQueue() {
        return new org.springframework.amqp.core.Queue("lpp-email-order-queue",false);
    }
    @Bean
    Queue msgQueue() {
        return new org.springframework.amqp.core.Queue("lpp-msg-order-queue",false);
    }
    // 交换机
    @Bean
    TopicExchange exchange() {
        return new TopicExchange("lpp-test-order-exchange");
    }
    // 队列和交换机绑定
    @Bean
    Binding smsBinding() {
        return BindingBuilder.bind(smsQueue()).to(exchange()).with("#.sms.#.#");// 使用通配符匹配多个队列
    }
    @Bean
    Binding emailBinding() {
        return BindingBuilder.bind(emailQueue()).to(exchange()).with("#.email.#.#");
    }
    @Bean
    public Binding msgBinding() {
        return BindingBuilder.bind(msgQueue()).to(exchange()).with("#.mag.#.#");
    }
}
