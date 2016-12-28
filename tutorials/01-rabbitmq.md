title: RabbitMQ入门教程(一)
categories:
- RabbitMQ
tags:
- RabbitMQ
- Java

date: 12/3/2016 2:19:56 PM 
---
<Excerpt in index | 首页摘要> 




## 1、准备

### 1.1 依赖

> 
> ![](http://i.imgur.com/gSTjU1I.png)

		<!--rabbitmq -->
		<dependency>
			<groupId>com.rabbitmq</groupId>
			<artifactId>amqp-client</artifactId>
			<version>3.6.3</version>
		</dependency>


### 1.2 开启服务器


> 开启监控管理


    rabbitmq-plugins enable rabbitmq_management


> 启用监控管理，然后重启 RabbitMQ 服务器。 

    rabbitmq-server

<!-- more -->
<The rest of contents | 余下全文>

打开网址` http://localhost:55672`
用户名： `guest`
密码： `guest`

![](http://i.imgur.com/HTTFvgW.png)

## 2、	代码实现

这里的案例是参考官网的案例

[http://www.rabbitmq.com/tutorials/tutorial-one-java.html](http://www.rabbitmq.com/tutorials/tutorial-one-java.html "tutorials-one-java")

我们会调用我们的 消息发送器 Send 并且 我们的 消息接收器 Recv 。发送器会连接 RabbitMQ，发送一条消息，然后退出

在 `Send.java` 导入的主要的包

    import com.rabbitmq.client.ConnectionFactory;
    import com.rabbitmq.client.Connection;
    import com.rabbitmq.client.Channel;
    

### 2.1 发送(Sending)

    package com.symbol.rabbitmq;
    
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


### 2.2 接收（Receiving）

主要使用到的包

    import com.rabbitmq.client.ConnectionFactory;
    import com.rabbitmq.client.Connection;
    import com.rabbitmq.client.Channel;
    import com.rabbitmq.client.Consumer;
    import com.rabbitmq.client.DefaultConsumer;
    
> 
> 具体接收端的代码：
>


    package com.symbol.rabbitmq;
    
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
          channel.basicConsume(QUEUE_NAME, true, consumer);
        
    }
    }
 

## 3、	测试

### 3.1  Send 发送器发送消息 “Hello World!” 并且退出

![](http://i.imgur.com/XC3WORD.png)


### 3.2 接收器接收消息

> 接收到一次消息，并等待消息

![](http://i.imgur.com/ZCECj2P.png)

### 3.3 再次发送，接收器接收消息

![](http://i.imgur.com/84ilQgE.png)

基本的实现已经完成了

## 4、基本原理

> RabbitMQ 相当于一个消息的代理人，其中最主要的想法是非常简单的：
> 
> 接收并转发消息。
> 
> 你可以把它想成是一个邮局：当你寄一封邮件到邮箱，你会非常确定快递员最终会帮你送到接收方。
> 
> 如此而来，可以象征性地把 RabbitMQ 当成是一个邮箱、一个邮局和快递员。

### 4.1 生产者 `producer`
> 
> 生产仅仅意味着发送。发送消息的程序是一个生产者，这里用 P 代替说明：
<div align="center">
<img src="http://i.imgur.com/R48K9Dd.png" />
</div>

### 4.2 队列 `queue` 

> 一个队列相当于邮箱，它生存在 RabbitMQ 的内部，尽管信息流通过RabbitMQ和应用程序，
> 
> 他们可以只存储在一个队列的内部，队列是不受任何限制的，你喜欢存多少就存多少。
> 
> 它本质上是一个无限的缓冲区。许多生产者可以发送消息到一个队列，许多消费者可以尝试从一个队列中接收数据。



<div align="center">
<img src="http://i.imgur.com/VNt6pEc.png" />
</div>

### 4.3 消费者 `consumer` 


> 消费的意思可以看做是接收。消费者是一个主要是等待接收消息的程序。

> 请注意，生产者，消费者和代理人不必驻留在同一台机器上，事实上，在大多数应用中，他们没有。

<div align="center">
<img src="http://i.imgur.com/mw1725b.png" />
</div>

### 4.4 生产者推送消息

> 生产者发送消息，推到RabbitMQ队列中。
<div align="center">
<img src="http://i.imgur.com/6PROpWT.png" />
</div>

### 4.5 消费者接收消息

> 接收器接收从 RabbitMQ队列 中推送过来的消息，
> 
> 接收器不像发送器那样仅仅只是发送单一的消息就结束了，
> 
> 而是一直保持运行监听并打印输出消息

<div align="center">
<img src="http://i.imgur.com/AtlMbls.png" />
</div>


### 教程源码：

**教程源码在我的 github**：[https://github.com/Ja0ck5/rabbitmq-practice](https://github.com/Ja0ck5/rabbitmq-practice "https://github.com/Ja0ck5/rabbitmq-practice")