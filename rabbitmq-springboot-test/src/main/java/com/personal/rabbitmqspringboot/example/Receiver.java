package com.personal.rabbitmqspringboot.example;

import org.springframework.stereotype.Component;

import java.util.concurrent.CountDownLatch;

/**
 * @ClassName Receiver
 * @Author liupanpan
 * @Date 2025/11/16
 * @Description 消息接收
 * 接收器是一个 POJO（普通 Java 对象），它定义了一个接收消息的方法。
 */
@Component
public class Receiver {

    // 线程同步工具，允许一个或多个线程等待其他线程完成操作
    private CountDownLatch count =  new CountDownLatch(1);
    public void receiveMessage(String message){
        System.out.println("received----->"+message);
        // 递减锁存器的计数，如果计数达到零，则释放所有等待线程。
        count.countDown();// 在收到第一条消息时通知等待的线程（通过countDown()），
    }
    public CountDownLatch getCount() {
        return count;
    }

}
