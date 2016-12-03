package com.symbol.rabbitmq.sendrecv;

import java.io.IOException;
import java.util.concurrent.TimeoutException;

import com.rabbitmq.client.AMQP;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import com.rabbitmq.client.Consumer;
import com.rabbitmq.client.DefaultConsumer;
import com.rabbitmq.client.Envelope;

public class Recv {
    private final static String QUEUE_NAME = "hello";
    
    
    public static void main(String[] args) throws IOException,TimeoutException {
    	
        ConnectionFactory factory = new ConnectionFactory();
        // 设置MabbitMQ, 指定它的名称或IP地址
        factory.setHost("localhost");
        // 创建连接
        Connection connection = factory.newConnection();
        // 创建通道
        Channel channel = connection.createChannel();
        // 指定队列
        /**
         * 第一个参数：队列名字
         * 第二个参数：队列是否可持久化即重启后该队列是否依然存在
         * 第三个参数：该队列是否时独占的即连接上来时它占用整个网络连接
         * 第四个参数：是否自动销毁即当这个队列不再被使用的时候即没有消费者对接上来时自动删除
         * 第五个参数：其他参数如TTL（队列存活时间）等。
         */
        channel.queueDeclare(QUEUE_NAME, false, false, false, null);
        System.out.println(" [*] Waiting for messages. To exit press CTRL+C");
        
        // 创建队列消费者
        Consumer consumer = new DefaultConsumer(channel) {
            @Override
            public void handleDelivery(String consumerTag, Envelope envelope, AMQP.BasicProperties properties, byte[] body)
                throws IOException {
              String message = new String(body, "UTF-8");
              System.out.println(" [x] Received '" + message + "'");
            }
          };
          /**
           * 第一个参数：队列名字
           * 第二个参数：是否自动应答
           * 		如果为真，消息一旦被消费者收到，服务端就知道该消息已经投递
           * 		从而从队列中将消息剔除，否则，需要在消费者端手工调用channel.basicAck()方法通知服务端
           * 		如果没有调用，消息将会进入unacknowledged状态
           * 		并且当消费者连接断开后变成ready状态重新进入队列
           * 第三个参数，具体消费者类。
           */
          channel.basicConsume(QUEUE_NAME, true, consumer);
        
    }
}