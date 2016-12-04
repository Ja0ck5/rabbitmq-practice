title: RabbitMQ入门教程(二)
categories:
- RabbitMQ
tags:
- RabbitMQ
- Java

date: 12/3/2016 2:22:19 PM  
---
<Excerpt in index | 首页摘要> 



# Work Queues 工作队列

## 1、实现代码
### 1.1  NewTask 创建任务

    package com.symbol.rabbitmq.worker;
    
    import java.util.concurrent.TimeoutException;
    
    import com.rabbitmq.client.Channel;
    import com.rabbitmq.client.Connection;
    import com.rabbitmq.client.ConnectionFactory;
    import com.rabbitmq.client.MessageProperties;
    
    public class NewTask {

	private static final String TASK_QUEUE_NAME = "task_queue";

	public static void main(String[] argv) throws java.io.IOException, TimeoutException {

		ConnectionFactory factory = new ConnectionFactory();
		factory.setHost("localhost");
		Connection connection = factory.newConnection();
		Channel channel = connection.createChannel();

<!-- more -->
<The rest of contents | 余下全文>


		boolean durable = true;
		channel.queueDeclare(TASK_QUEUE_NAME, durable, false, false, null);
		for(int i=0;i<10;i++){
			String[] ms = {"Ja0ck5-say-hello..."+i};
			String message = getMessage(ms);
			channel.basicPublish("", TASK_QUEUE_NAME, MessageProperties.PERSISTENT_TEXT_PLAIN, message.getBytes());
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


### 1.2 工人消费处理任务


    package com.symbol.rabbitmq.worker;
    
    import java.io.IOException;
    
    import com.rabbitmq.client.AMQP;
    import com.rabbitmq.client.Channel;
    import com.rabbitmq.client.Connection;
    import com.rabbitmq.client.ConnectionFactory;
    import com.rabbitmq.client.Consumer;
    import com.rabbitmq.client.DefaultConsumer;
    import com.rabbitmq.client.Envelope;
    
    public class Worker {
	private static final String TASK_QUEUE_NAME = "task_queue";

	public static void main(String[] argv) throws Exception {
		ConnectionFactory factory = new ConnectionFactory();
		factory.setHost("localhost");
		final Connection connection = factory.newConnection();
		final Channel channel = connection.createChannel();

		boolean durable = true;
		channel.queueDeclare(TASK_QUEUE_NAME, durable, false, false, null);
		System.out.println(" [*] Waiting for messages. To exit press CTRL+C");

		int prefetchCount = 1;
		channel.basicQos(prefetchCount);
		final Consumer consumer = new DefaultConsumer(channel) {
			@Override
			public void handleDelivery(String consumerTag, Envelope envelope, AMQP.BasicProperties properties,
					byte[] body) throws IOException {
				String message = new String(body, "UTF-8");

				System.out.println(" [x] Received '" + message + "'");
				try {
					doWork(message);
				} finally {
					System.out.println(" [x] Done");
					channel.basicAck(envelope.getDeliveryTag(), false);
				}
			}
		};
		boolean autoAck = false;
		channel.basicConsume(TASK_QUEUE_NAME, autoAck, consumer);
	}

	private static void doWork(String task) {
		for (char ch : task.toCharArray()) {
			if (ch == '.') {
				try {
					Thread.sleep(1000);
				} catch (InterruptedException _ignored) {
					Thread.currentThread().interrupt();
				}
			}
		}
	}
    }


### 1.3、测试
### 1.3.1 执行 `NewTask`
复杂度说明：
模拟的每条消息的复杂度为 3 ，即三个 `.` (点)

![](http://i.imgur.com/kZz8DLg.png)


### 1.3.2执行 Worker

每隔三秒打印输出一条消息

![](http://i.imgur.com/97ny5Fa.png)


### 1.3.3、代码解析

> 在本教程的前面部分中，我们发送了一个包含 “Hello World!”的消息。 
> 
> 现在我们将发送支持复杂任务的字符串。我们没有一个真实世界的任务，
> 
> 如图像进行调整或PDF文件的渲染，让我们假装很忙-采用 Thread.sleep()。
> 
> 我们将把字符串中的点的数量作为它的复杂性，每一个点将占一秒钟的“工作”。
> 
> 例如，一个假任务描述的 `…`(三个点) 将需要三秒。

**NewTask:**

		for(int i=0;i<10;i++){
			String[] ms = {"Ja0ck5-say-hello..."+i};
			String message = getMessage(ms);
			channel.basicPublish("", TASK_QUEUE_NAME, MessageProperties.PERSISTENT_TEXT_PLAIN, message.getBytes());
			System.out.println(" [x] Sent '" + message + "'");
		}     



**Worker:**
	
	private static void doWork(String task) {
		for (char ch : task.toCharArray()) {
			if (ch == '.') {
				try {
					Thread.sleep(1000);
				} catch (InterruptedException _ignored) {
					Thread.currentThread().interrupt();
				}
			}
		}
	}

## 2、Round-robin dispatching 轮询调度

>
> 使用任务队列的优点是容易并行工作的能力。如果我们积累了大量的工作，我们可以增加更多的工人，这样的方式，使得规模伸缩容易。

> 默认情况下，RabbitMQ将会依次发送每一条消息给下一个消费者，平均每一个消费者将获得相同数量的消息。
> 
> 这种分配消息方式称为**轮询**。

### 2.1 先开启两个 Worker 

 
> 即 Worker 代码执行两遍

![](http://i.imgur.com/mT1oOyM.png)

### 2.2 再开启NewTask

![](http://i.imgur.com/e338kH5.png)


### 2.3 查看两个 Worker 接收任务的情况

**第一个 Worker 接收 Taksk 情况如下：**

![](http://i.imgur.com/kFtVRlz.png)

**第二个 Worker 接收 Taksk 情况如下：**

![](http://i.imgur.com/hY6Ziok.png)

这是非常典型的轮询调度。


## 3、Message acknowledgment 消息确认


> 做一个任务可以花几秒。你可能会想知道，
> 
> 如果一个消费者开始一个长的任务并且它只完成部分就死亡了，会发生什么事。
> 
> 我们目前的代码，一旦 RabbitMQ 发送一个消息给客户立即从内存中移除。
> 
> 在这种情况下，如果你 kill 一个 worker ，我们将失去 当前worker 正在处理的消息。
> 
> 我们也将失去所有被发送给这个独有的工人，但尚未处理的消息。

> 但我们不想失去任何任务。如果一个 worker 死亡，我们希望将这项任务交给另一个工人。
> 
> 如果一个消费者死亡（其通道关闭，连接被关闭，或TCP连接丢失）不发送ACK(确认字符)，
> 
> RabbitMQ将会认为这个消息并没有完全处理，将它重
> 
> 新排队。如果有其他用户同时在线，它就会快速地传递到另一个消费者。
> 
> 这样，你可以肯定没有消息丢失，即使偶尔有 Worker 死亡。
> 
> 没有任何消息超时；当消费者死亡时 RabbitMQ 将会重新发送消息。
> 
> 这是很好的，即使处理一个消息需要一个非常，非常长的时间。
> 
> `Message acknowledgments` 默认是打开的。


	   /**
		 * 设置为 false 一旦某个消费者死亡，RabbitMq会重新发送它的消息给其他消费者(确保消息不丢失)
		 * 
		 * 设置为 true 一旦某个消费者死亡, RabbitMq 还是会将消息发送给它，导致消息丢失，并且会报错
		 */
		boolean autoAck = true;
		channel.basicConsume(TASK_QUEUE_NAME, autoAck, consumer);

### 3.1设置为true 

打开两个 worker ,并当接收消息时，kill其中一个 worker。会出现以下情况：

**即，当某一个worker 死亡，RabbitMq 还是会将消息发送给它，导致消息丢失，另一个 Worker 继续接收 轮询调度 而来的消息**

![](http://i.imgur.com/2CYc5iz.png)
 
### 3.2设置为false

       /**
		 * 设置为 false 一旦某个消费者死亡，RabbitMq会重新发送它的消息给其他消费者(确保消息不丢失)
		 * 
		 * 设置为 true 一旦某个消费者死亡, RabbitMq 还是会将消息发送给它，导致消息丢失，并且会报错
		 */
		boolean autoAck = false;
		channel.basicConsume(TASK_QUEUE_NAME, autoAck, consumer);

**第一个 Worker is killed**

![](http://i.imgur.com/ZC16sWH.png)

**第二个 Worker 接收消息的情况**

![](http://i.imgur.com/NnKrjm3.png)


**由此可见，设置 autoAck 为 false .即是 本该由当前 Worker 接收的消息，可是由于特殊原因死亡之后，被 RabbitMq 转发到其他的 Worker.**

**设置 autoAck 为 false，我们可以确定，即使是 kill 一个正在处理消息的Worker ,什么也不会丢失，很快，那些不被确认的消息会被再次投递。**


> 错过 basicAck这是常见的错误。
> 
> 这是一个简单的错误，但后果是严重的。
> 
> 当你的 Client 端退出时消息依然会被重新投递发送（这看起来像是随机重新发送的），
> 
> 但是当 RabbitMQ 不能够释放任何不被确认的消息时会吃掉越来越多的内存。
> 
> 为了调试这种错误，你可以使用
> 
>  `rabbitmqctl `
>  
>  打印 
>  
>  `messages_unacknowledged`

    rabbitmqctl list_queues name messages_ready messages_unacknowledged

## 4、Message durability 消息持久化

> 
> 我们已经学会了如何确保即使消费者死了，任务也不会丢失. 如果RabbitMQ服务器停止了,我们的消息仍然会被丢失。
> 
> 当RabbitMQ退出或死机会忘记队列和消息，除非你告诉它不要忘记。
> 
> 为了确保消息不被丢失，有两件事情是必须的：

1.	标记队列
2.	消息的持久化

首先，我们需要确保我们的队列在 RabbitMQ中不会丢失。为了做到这一要求，我们需要声明它是 durable (持久的)：

    boolean durable = true;
    channel.queueDeclare("hello", durable, false, false, null);
    


第二个参数表示是否持久化。
虽然这个命令本身是正确的，但它不会在我们目前的设置中工作。这是因为我们已经定义了一个不持久的队列 “Hello” 

![](http://i.imgur.com/1nzoHb4.png)

### 4.1 声明一个新的队列 `task_queue`


RabbitMQ不允许你用不同的参数重新定义现有队列，并且会返回错误到试图这么做的任何的程序。有一个快速解决的方法 就是 重新声明一个名称不同的队列：

这个 `queueDeclare` 的改变需要应用到 `producer`  生产者和 `consumer`  消费者的代码中。

**NewTask:**

![](http://i.imgur.com/xPpZOGv.png)

**Worker:**
![](http://i.imgur.com/8Etubyj.png)

### 4.2 标记队列


就此，我们确定即使 `RabbitMQ` 重启了 `task_queue` 队列也不会被丢失。

现在我们要将我们的消息标记为 `持久的`，

通过设置 `MessageProperties` （实现接口 `BasicProperties`） 的值 `PERSISTENT_TEXT_PLAIN`


    import com.rabbitmq.client.MessageProperties;

    channel.basicPublish("", "task_queue",
            MessageProperties.PERSISTENT_TEXT_PLAIN,
            message.getBytes());


即是这段代码：

![](http://i.imgur.com/pdl4Gqu.png)



**关于消息持久性的标记**

> 将消息标记为持久性的并不能完全保证消息不被丢失。
> 
> 虽然它告诉 RabbitMQ 保存信息到磁盘上，
> 
> 但是当 RabbitMQ 接收消息还没有保存的时候仍然有个短时窗口。
> 
> 此外，RabbitMQ 不为每条消息做 fsync(2)  
> 
> 它可能只是保存到缓存并没有真正写入到磁盘。
> 
> 持久性保证不强，但对于简单的任务队列已经非常足够了，
> 
> 如果你需要一个更强大的保证，你可以使用 [publisher confirms](https://www.rabbitmq.com/confirms.html ). 
> 

## 5、Fair dispatch 公平转发

你也许注意到调度仍然不像我们期望的那样准确地工作，

例如在某种情况下，有两个 Workers,当所有剩余的消息时繁重的，或者甚至是轻量的，

而一个 Worker 不断的处理这些消息，而另一个 Worker 基本不工作。

`RabbitMQ` 不知道发生了什么事，仍将消息均匀发送。

那么可能导致一个 Worker 一直接收繁重的消息，另一个 Worker 接收轻量的消息，甚至是不怎么工作。

这是因为当消息进入队列时， `RabbitMQ` 仅仅只是调度消息。

它不替消费者 `consumer` 去查看未确认的消息的数量。它只是盲目地转发消息到消费者 。

<div align="center">
<img src="http://i.imgur.com/UnUp07S.png" alt="fair dispatch">
</div>


> 为了解决这种情况，我们可以使用  `basicQos` 方法，
> 
> 设置参数 
> 
> `prefetchCount = 1` 
> 
> 这是告诉 `RabbitMQ `在一个时间内，不给超过一条的消息到一个 Worker。
> 
> 换句话说，
> 
> 不要向一个 Worker 转发一个新的消息直到这个 Worker 处理并确认完上一个消息。`RabbitMQ `将转发消息到下一个不是很忙的 Worker。

    
    int prefetchCount = 1;
    channel.basicQos(prefetchCount);


Worker 接收代码中：

![](http://i.imgur.com/CuJ4LQP.png)

> 关于队列大小的说明
> 
> 如果所有的 Workers 都很忙，你的队列就要填满了。你会想要知道这种情况，并且也许会增加 Workers ,或者换另一种策略。
> 



### 教程源码：

**教程源码在我的 github**：[https://github.com/Ja0ck5/rabbitmq-practice](https://github.com/Ja0ck5/rabbitmq-practice "https://github.com/Ja0ck5/rabbitmq-practice")























