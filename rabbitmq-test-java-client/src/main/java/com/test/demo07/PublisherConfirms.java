package com.test.demo07;

import com.rabbitmq.client.*;

import java.io.IOException;
import java.time.Duration;
import java.util.LinkedList;
import java.util.UUID;
import java.util.concurrent.ConcurrentNavigableMap;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.concurrent.TimeoutException;
import java.util.function.BooleanSupplier;

/**
 * @ClassName PublisherConfirms
 * @Author liupanpan
 * @Date 2025/11/14
 * @Description
 */
public class PublisherConfirms {

    static final int MESSAGE_COUNT = 50_000; // 消息数量50,000
    static final int MAX_OUTSTANDING = 1000; // 确认窗口
    static final int THROTTLING_PERCENTAGE = 50; // 从 50% 容量开始限速
    static final int MAX_DELAY_MS = 1000; // 最大延迟

    public static Connection createConnection() throws IOException, TimeoutException {
        ConnectionFactory factory = new ConnectionFactory();
        factory.setHost("172.16.30.21");
        factory.setUsername("rabbitmq");
        factory.setPassword("310012");
        return factory.newConnection();
    }

    public static void main(String[] args) throws Exception{
        publishMessagesIndividually();//逐个发布
        publishMessagesInBatch();//批量发布
        handlePublishConfirmsAsynchronously();//异步发布
        handlePublishConfirmsWithWindow();
        handlePublishConfirmsWithAdaptiveThrottling();
    }

    private static void handlePublishConfirmsAsynchronously() throws Exception {
        try (Connection connection = createConnection()) {
            Channel channel = connection.createChannel();
            // 生成随机队列名
            String queue = UUID.randomUUID().toString();
            channel.queueDeclare(queue,false,false,true,null);

            channel.confirmSelect();// 启用发布者确认
            // 使用映射将 发布序列号 和 消息的字符串正文 关联
            ConcurrentNavigableMap<Long, String> outstandingConfirms = new ConcurrentSkipListMap<>();

            ConfirmCallback cleanOutstandingConfirms = (sequenceNumber,multiple) -> {
                if (multiple) { //false：只确认/否认一条消息；true：确认/否认所有序列号<=该序列号的消息
                    // 序列号:表示已确认或未确认消息的编号
                    /*
                    * 如果 `inclusive` 为真，则返回映射中键值小于或等于 `toKey` 的部分视图；
                    * 如果 `inclusive` 为假，则返回键值严格小于 `toKey` 的部分视图。
                    * */
                    ConcurrentNavigableMap<Long, String> confirmed = outstandingConfirms.headMap(sequenceNumber, true);
                    confirmed.clear();
                } else {
                    outstandingConfirms.remove(sequenceNumber);
                }
            };
            // 注册一个确认监听器，以便在发布者收到确认/否定消息时收到通知
            /*
            * 有两个回调函数：
            *   第一个：用于已确认的消息
            *   第二个：用于未确认的消息
            * */
            channel.addConfirmListener(cleanOutstandingConfirms,(sequenceNumber,multiple) -> {
                String body = outstandingConfirms.get(sequenceNumber);// 该序号关联的消息
                System.err.format(
                        "Message with body %s has been nack-ed. Sequence number: %d, multiple: %b%n",
                        body, sequenceNumber, multiple
                );
                /*
                * 实现此接口以接收 Confirm 事件通知。
                * ACK 表示消息已成功处理；NACK 表示消息代理丢失。。
                * */
                cleanOutstandingConfirms.handle(sequenceNumber,multiple);
            });
            long start = System.nanoTime();
            for (int i = 0; i < MESSAGE_COUNT; i++) {
                String body = String.valueOf(i);
                outstandingConfirms.put(channel.getNextPublishSeqNo(),body);// 发布前，获取序列号
                channel.basicPublish("",
                        queue,
                        null,
                        body.getBytes());
            }
            if (!waitUntil(Duration.ofSeconds(60), outstandingConfirms::isEmpty)) {
                throw new IllegalStateException("");
            }
            long end = System.nanoTime();
            System.out.format("逐个发布 %,d 个消息，用时: %,d ms%n",MESSAGE_COUNT, Duration.ofNanos(end-start).toMillis());
        }

    }

    static void handlePublishConfirmsWithWindow() throws Exception {
        try (Connection connection = createConnection()) {
            Channel ch = connection.createChannel();

            String queue = UUID.randomUUID().toString();
            ch.queueDeclare(queue, false, false, true, null);
            ch.confirmSelect();

            ConcurrentNavigableMap<Long, String> outstandingConfirms = new ConcurrentSkipListMap<>();

            ConfirmCallback cleanOutstandingConfirms = (sequenceNumber, multiple) -> {
                if (multiple) {
                    outstandingConfirms.headMap(sequenceNumber, true).clear();
                } else {
                    outstandingConfirms.remove(sequenceNumber);
                }
                synchronized (outstandingConfirms) {
                    outstandingConfirms.notifyAll();
                }
            };

            ch.addConfirmListener(cleanOutstandingConfirms, (sequenceNumber, multiple) -> {
                System.err.format("Message nacked. Sequence: %d, multiple: %b%n", sequenceNumber, multiple);
                cleanOutstandingConfirms.handle(sequenceNumber, multiple);
            });

            long start = System.nanoTime();
            for (int i = 0; i < MESSAGE_COUNT; i++) {
                // Wait if window is full
                synchronized (outstandingConfirms) {
                    while (outstandingConfirms.size() >= MAX_OUTSTANDING) {
                        outstandingConfirms.wait();
                    }
                }

                String body = String.valueOf(i);
                outstandingConfirms.put(ch.getNextPublishSeqNo(), body);
                ch.basicPublish("", queue, null, body.getBytes());
            }

            // Wait for remaining confirmations
            synchronized (outstandingConfirms) {
                while (!outstandingConfirms.isEmpty()) {
                    outstandingConfirms.wait();
                }
            }

            long end = System.nanoTime();
            System.out.format("Published %,d messages with confirmation window in %,d ms%n",
                    MESSAGE_COUNT, Duration.ofNanos(end - start).toMillis());
        }
    }

    static void handlePublishConfirmsWithAdaptiveThrottling() throws Exception {
        try (Connection connection = createConnection()) {
            Channel ch = connection.createChannel();

            String queue = UUID.randomUUID().toString();
            ch.queueDeclare(queue, false, false, true, null);
            ch.confirmSelect();

            LinkedList<Long> outstandingConfirms = new LinkedList<>();
            int throttlingThreshold = MAX_OUTSTANDING * THROTTLING_PERCENTAGE / 100;

            ConfirmCallback cleanOutstandingConfirms = (sequenceNumber, multiple) -> {
                synchronized (outstandingConfirms) {
                    if (multiple) {
                        outstandingConfirms.removeIf(seqNo -> seqNo <= sequenceNumber);
                    } else {
                        outstandingConfirms.removeFirstOccurrence(sequenceNumber);
                    }
                    outstandingConfirms.notifyAll();
                }
            };

            ch.addConfirmListener(cleanOutstandingConfirms, (sequenceNumber, multiple) -> {
                System.err.format("Message nacked. Sequence: %d, multiple: %b%n", sequenceNumber, multiple);
                cleanOutstandingConfirms.handle(sequenceNumber, multiple);
            });

            long start = System.nanoTime();
            for (int i = 0; i < MESSAGE_COUNT; i++) {
                String body = String.valueOf(i);

                synchronized (outstandingConfirms) {
                    while (outstandingConfirms.size() >= MAX_OUTSTANDING) {
                        outstandingConfirms.wait();
                    }

                    int availablePermits = MAX_OUTSTANDING - outstandingConfirms.size();
                    if (availablePermits < throttlingThreshold) {
                        double percentageUsed = 1.0 - (availablePermits / (double) MAX_OUTSTANDING);
                        int delay = (int) (percentageUsed * MAX_DELAY_MS);
                        if (delay > 0) {
                            outstandingConfirms.wait(delay);
                        }
                    }

                    long seqNo = ch.getNextPublishSeqNo();
                    outstandingConfirms.addLast(seqNo);
                }

                ch.basicPublish("", queue, null, body.getBytes());
            }

            synchronized (outstandingConfirms) {
                while (!outstandingConfirms.isEmpty()) {
                    outstandingConfirms.wait();
                }
            }

            long end = System.nanoTime();
            System.out.format("Published %,d messages with adaptive throttling in %,d ms%n",
                    MESSAGE_COUNT, Duration.ofNanos(end - start).toMillis());
        }
    }
    static boolean waitUntil(Duration timeout, BooleanSupplier condition) throws InterruptedException {
        int waited = 0;
        while (!condition.getAsBoolean() && waited < timeout.toMillis()) {
            Thread.sleep(100L);
            waited += 100;
        }
        return condition.getAsBoolean();
    }

    private static void publishMessagesInBatch() throws Exception {
        try (Connection connection = createConnection()) {
            Channel channel = connection.createChannel();
            // 生成随机队列名
            String queue = UUID.randomUUID().toString();
            channel.queueDeclare(queue,false,false,true,null);

            channel.confirmSelect();// 启用发布者确认
            int batchSize = 100;
            int outstandingMessageCount = 0; //统计批量处理的数量

            long start = System.nanoTime();
            for (int i = 0; i < MESSAGE_COUNT; i++) {
                String body = String.valueOf(i);
                channel.basicPublish("",
                        queue,
                        null,
                        body.getBytes());
                outstandingMessageCount++;
                if (outstandingMessageCount == batchSize) {
                    channel.waitForConfirmsOrDie(5_000);
                    outstandingMessageCount = 0;
                }
            }
            if (outstandingMessageCount > 0) {
                channel.waitForConfirmsOrDie(5_000);
            }
            long end = System.nanoTime();
            System.out.format("逐个发布 %,d 个消息，用时: %,d ms%n",MESSAGE_COUNT, Duration.ofNanos(end-start).toMillis());
        }
    }

    private static void publishMessagesIndividually() throws IOException, TimeoutException, InterruptedException {
        try (Connection connection = createConnection()) {
            Channel channel = connection.createChannel();
            // 生成随机队列名
            String queue = UUID.randomUUID().toString();
            channel.queueDeclare(queue,false,false,true,null);

            channel.confirmSelect();// 启用发布者确认
            long start = System.nanoTime();
            for (int i = 0; i < MESSAGE_COUNT; i++) {
                String body = String.valueOf(i);
                channel.basicPublish("",
                        queue,
                        null,
                        body.getBytes());
                /*
                * 如果超时，则会抛出 TimeoutException 异常。
                * 如果任何消息被拒绝，waitForConfirmsOrDie 方法将抛出 IOException 异常。
                * */
                channel.waitForConfirmsOrDie(5_000); // flag
            }
            long end = System.nanoTime();
            System.out.format("逐个发布 %,d 个消息，用时: %,d ms%n",MESSAGE_COUNT, Duration.ofNanos(end-start).toMillis());
        }
    }
}
