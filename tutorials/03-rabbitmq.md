# Publish/Subscribe

在上一个教程中，我们创建了一个工作队列。后面的假设是基于工作队列:每个任务都被发送到确切的一个 Worker。

在这一部分，我们将做一些完全不同的事：我们将提供一个信息给多个消费者。这种模式被称为“发布/订阅”。

为了说明这种模式，我们将建立一个简单的日志记录系统。它将由两个程序组成，第一个将发出日志消息，第二个将接收并打印日志消息。

在我们的日志系统中，每一个运行的接收程序的副本将得到的消息。

这样，我们将能够运行一个接收器和直接记录日志到磁盘上，在同一时间，我们将能够运行另一个接收器，并看到屏幕上的日志。

从本质上讲，已发布的日志消息将被广播到所有的接收器上。


## 1、	Exchanges

在我们的教程的前面部分中，我们是从一个队列中发送和接收消息，现在介绍 Rabbit 中完整的消息传递模型。
回顾一下之前的概念：

`生产者`  是发送消息的用户应用程序。

`队列`    是存储消息的缓冲区。

`消费者`  是接收消息的用户应用程序。


`RabbitMQ` 消息传递模型的核心思想是，生产者不直接发送任何信息到队列。事实上，相当多的生产者甚至不知道一个消息是否将被传递到任何队列。

相反，生产者只能发送消息到一个交换器（exchange）  。一次交换是一件很简单的事。
一方面，它接收来自生产者的消息，另一方面则将它们推到队列。交换器（exchange）必须知道该如何处理它接收到的消息。

它应该被添加到一个特定的队列吗？

它应该被添加到许多队列吗？

或者它应该被丢弃？


这些规则是由交换器（exchange）类型定义的

<div align="center">
<img src="http://i.imgur.com/4kFQL8B.png" alt="exchange">
</div>

这些是可用的交换器(exchange)类型：`direct, topic, headers` 和 `fanout`.
我们将专注于最后一个 `fanout` 让我们创建一个这种类型的交换：

    channel.exchangeDeclare("logs", "fanout");‘


 `fanout exchange` 是非常简单的，正如它的名称一样，它只是广播它接收到的信息到`所有`它知道的队列。这正是我们需要的记录器（logger） 

### 1.1 列出交换器

使用 `rabbitmqctl` ，可以列出可以运行的 交换器 `exchange`

    rabbitmqctl list_exchanges

![](http://i.imgur.com/X9fK6uO.png)

在这个列表里有一些 `amq. *` 的交换器 `exchange` 和默认没有命名的交换器 `exchange`，他们是默认创建的，但未必是此刻你正在使用的。

### 1.2无名交换（`Nameless exchange` ）

在上一部分的教程中，我们不知道交换器 exchange ，但仍然能够发送消息到队列。这是可以的，因为我们使用了根据空字符串识别的默认交换(`exchange`),
回想我们以前发布的一个消息：

    channel.basicPublish("", "hello", null, message.getBytes());


![](http://i.imgur.com/4fSefMj.png)


第一个参数是交换器(exchage)的名称。空字符串表示默认或无名的交换器(exchage)：

**如果 routingkey 存在的话，消息被路由到指定了routingkey名称的队列中。**

![](http://i.imgur.com/Mu0z3f2.png)


**现在我们可以发布到自己命名的交换器(exchange)上**：

    channel.basicPublish( "logs", "", null, message.getBytes());

## 2、临时队列（`Temporary queues`）

也许你还记得以前我们使用指定名称的队列(hello /task_queue),对于我们来说，能够命名一个队列是至关重要的-----我们需要引导 workers 到同一个队列，当你想要去分享介于生产者和消费者（`producers and consumers`）之间的队列时，给队列一个名称是非常重要的。

但是对于我们的日志记录器来说，并不是我们想要的情况。我们需要得知所有的日志消息，而不是它们的子集。

我们更感兴趣的是当前流动的最新的消息，而不是过时的。

那我们需要解决两件事情：

**第一：**

	无论何时连接到 Rabbit 时，我们需要一个新的，空的队列，要做到这一点，我们可以创建一个随机名称的队列，或者最好让服务器为我们选择一个随机队列名称。

**其次：**

	一旦我们断开了 消费者(consumer)的队列时，应该自动删除。



当我们没有提供任何参数给queueDeclare()时，我们就以生成如下的名称

    String queueName = channel.queueDeclare().getQueue();

创建了一个 “非持久的”、”唯一的”、”自动删除的”队列。

`queueName` 包含了一个随机名称，看起来可能像是 ：

    amq.gen-JzTY20BRgKO-HjmUJj0wLg

## 3、绑定（`Bindings`）
<div align="center">
<img src="http://i.imgur.com/kpEn2oe.png" alt="bindings">
</div>


我们已经创建了一个 `fanout exchange` 和一个队列。现在我们需要告诉交换器(`exchange`)给我们的队列发送消息。
交换器(`exchange`)与队列（`queue`）之间的关系被称之为 `绑定(binding)`。
    
    channel.queueBind(queueName, "logs", "");

从现在开始日志交换器将向我们的队列添加消息。

## 4、代码实现

<div align="center">
<img src="http://i.imgur.com/0UOQsNu.png" alt="bindings">
</div>
> 
> 生产者 `producer` 程序，它发出日志消息。对比之前的代码其实没有多大的不同。
> 
> 最重要的变化是，现在我们想要发布消息到我们的日志交换器取代之前无名交换器。
> 
> 当我们发送的时候，我们需要提供一个 `routingKey `，但对于交换器来说，`它的值是可以忽略的`。


### 4.1 Emitlog:
    
    package com.symbol.rabbitmq.pubsub;
    
    import java.util.concurrent.TimeoutException;
    
    import com.rabbitmq.client.ConnectionFactory;
    import com.rabbitmq.client.MessageProperties;
    import com.symbol.rabbitmq.util.MessageUtil;
    import com.rabbitmq.client.Connection;
    import com.rabbitmq.client.Channel;
    
    public class EmitLog {

	private static final String EXCHANGE_NAME = "logs";

	public static void main(String[] argv) throws java.io.IOException, TimeoutException {
		//创建连接
		ConnectionFactory factory = new ConnectionFactory();
		factory.setHost("localhost");
		Connection connection = factory.newConnection();
		Channel channel = connection.createChannel();
		//声明交换器及类型
		channel.exchangeDeclare(EXCHANGE_NAME, "fanout");

		//发布消息
		for(int i=0;i<10;i++){
			String[] ms = {"Ja0ck5-say-hello..."+i};
			String message = getMessage(ms);
			channel.basicPublish(EXCHANGE_NAME, "", null, message.getBytes());
			System.out.println(" [x] Sent '" + message + "'");
		}

		channel.close();
		connection.close();
	}

	private static String getMessage(String[] strings) {
		if (strings.length < 1)
			return "Hello World!";
		return joinStrings(strings, " ");
	}

	private static String joinStrings(String[] strings, String delimiter) {
		int length = strings.length;
		if (length == 0)
			return "";
		StringBuilder words = new StringBuilder(strings[0]);
		for (int i = 1; i < length; i++) {
			words.append(delimiter).append(strings[i]);
		}
		return words.toString();
	}
    }
    

### 4.1 ReceiveLogs:
    package com.symbol.rabbitmq.pubsub;
    
    import com.rabbitmq.client.*;
    
    import java.io.IOException;
    
    public class ReceiveLogs {
	private static final String EXCHANGE_NAME = "logs";

	public static void main(String[] argv) throws Exception {
		//创建连接
		ConnectionFactory factory = new ConnectionFactory();
		factory.setHost("localhost");
		Connection connection = factory.newConnection();
		Channel channel = connection.createChannel();
		//声明交换器及类型
		channel.exchangeDeclare(EXCHANGE_NAME, "fanout");
		//创建一个非持久的、独立唯一的、自动删除的队列
		String queueName = channel.queueDeclare().getQueue();
		//绑定交换器和队列
		channel.queueBind(queueName, EXCHANGE_NAME, "");

		System.out.println(" [*] Waiting for messages. To exit press CTRL+C");

		Consumer consumer = new DefaultConsumer(channel) {
			@Override
			public void handleDelivery(String consumerTag, Envelope envelope, AMQP.BasicProperties properties,
					byte[] body) throws IOException {
				String message = new String(body, "UTF-8");
				System.out.println(" [x] Received '" + message + "'");
			}
		};
		channel.basicConsume(queueName, true, consumer);
    	}
    }


## 5、测试结果

接收日志
当启动 `ReceiveLogs`,使用命令


    rabbitmqctl list_bindings

查看绑定

![](http://i.imgur.com/jGs0kQc.png)


**开启一个** `ReceiveLogs` 接收到日志消息

![](http://i.imgur.com/fyg8gMT.png)

**开启两个** `ReceiveLogs`

控制台显示两个 绑定，而且交换器的名称都是 `logs`

![](http://i.imgur.com/Y4wwYmX.png)

控制台分别小时两个 `ReceiveLogs` 的输出

**第一个 接收日志**

![](http://i.imgur.com/2VgzGOw.png)

**第二个 接收日志**

![](http://i.imgur.com/4egbVW7.png)

两个都接都接收到所有有 `Emitlog` 发出的日志消息.










