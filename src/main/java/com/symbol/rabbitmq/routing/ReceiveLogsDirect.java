package com.symbol.rabbitmq.routing;

import com.rabbitmq.client.*;

import java.io.IOException;

public class ReceiveLogsDirect {

	private static final String EXCHANGE_NAME = "direct_logs";
	// severity list
	private static final String[] LOG_SEVERITY = { "info", "warning", "error" };

	public static void main(String[] argv) throws Exception {
		ConnectionFactory factory = new ConnectionFactory();
		factory.setHost("localhost");
		Connection connection = factory.newConnection();
		Channel channel = connection.createChannel();

		channel.exchangeDeclare(EXCHANGE_NAME, "direct");
		String queueName = channel.queueDeclare().getQueue();

		if (LOG_SEVERITY.length < 1) {
			System.err.println("Usage: ReceiveLogsDirect [info] [warning] [error]");
			System.exit(1);
		}
		
		//根据 severit 为 binding key 创建多个绑定
		for (String severity : LOG_SEVERITY) {
			channel.queueBind(queueName, EXCHANGE_NAME, severity);
		}
		System.out.println(" [*] Waiting for messages. To exit press CTRL+C");

		Consumer consumer = new DefaultConsumer(channel) {
			@Override
			public void handleDelivery(String consumerTag, Envelope envelope, AMQP.BasicProperties properties,
					byte[] body) throws IOException {
				String message = new String(body, "UTF-8");
				//输出接收到的 routing key 和 message
				System.out.println(" [x] Received '" + envelope.getRoutingKey() + "':'" + message + "'");
			}
		};
		channel.basicConsume(queueName, true, consumer);
	}
}