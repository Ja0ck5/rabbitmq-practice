package com.symbol.rabbitmq.rpc;


import com.rabbitmq.client.AMQP.BasicProperties;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import com.rabbitmq.client.QueueingConsumer;

public class RPCServer {

	private static final String RPC_QUEUE_NAME = "rpc_queue";

	public static void main(String[] args) throws Exception {

		//创建连接
		ConnectionFactory factory = new ConnectionFactory();
		factory.setHost("localhost");
		Connection connection = factory.newConnection();
		//创建通道
		Channel channel = connection.createChannel();
		//创建并声明队列
		channel.queueDeclare(RPC_QUEUE_NAME, false, false, false, null);
		//服务端发送信息的最大数量，如果 0 则是没有限制
		channel.basicQos(1);
		//创建消费者
		QueueingConsumer consumer = new QueueingConsumer(channel);
		//这里关闭了自动确认应答(其作用见在 worker 队列中说明)
		channel.basicConsume(RPC_QUEUE_NAME, false, consumer);

		System.out.println(" [x] Awaiting RPC requests");

		while (true) {
			QueueingConsumer.Delivery delivery = consumer.nextDelivery();
			
			//根据 delivery 发送，得到 BasicProperties 属性对象，获取到客户端锁设置的 联合id 属性
			BasicProperties props = delivery.getProperties();
			//根据联合 id 重新创建一个 BasicProperties 属性对象(为了让回复的响应辨别是哪个请求)
			BasicProperties replyProps = new BasicProperties.Builder().correlationId(props.getCorrelationId()).build();
			
			String message = new String(delivery.getBody());
			int n = Integer.parseInt(message);

			System.out.println(" [.] fib(" + message + ")");
			String response = "" + fib(n);
			
			//返回消息给客户端
			channel.basicPublish("", props.getReplyTo(), replyProps, response.getBytes());
			//处理完消息，手动确认应答
			channel.basicAck(delivery.getEnvelope().getDeliveryTag(), false);
		}
	}

	private static int fib(int n) throws Exception {
		if (n == 0)
			return 0;
		if (n == 1)
			return 1;
		return fib(n - 1) + fib(n - 2);
	}

}
