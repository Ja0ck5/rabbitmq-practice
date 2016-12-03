package com.symbol.rabbitmq.sendrecv;

import java.io.IOException;
import java.util.concurrent.TimeoutException;

import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;

public class Send {   
    private final static String QUEUE_NAME = "hello";
    
    
    public static void main(String[] args) throws IOException, TimeoutException {
    	
        ConnectionFactory factory = new ConnectionFactory();
        // 设置MabbitMQ, 指定它的名称或IP地址
        factory.setHost("localhost");
        // 创建连接 
        Connection connection = factory.newConnection();
        // 创建通道 
        Channel channel = connection.createChannel();    
        // 指定队列并发送消息
        channel.queueDeclare(QUEUE_NAME, false, false, false, null);
        String message = "Hello World!";
        channel.basicPublish("", QUEUE_NAME, null, message.getBytes());
        System.out.println(" [x] Sent '" + message + "'");
        // 关闭频道和连接  
        channel.close();
        connection.close();
    }
}