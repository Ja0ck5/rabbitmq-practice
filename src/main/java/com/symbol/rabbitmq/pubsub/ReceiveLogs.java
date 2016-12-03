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