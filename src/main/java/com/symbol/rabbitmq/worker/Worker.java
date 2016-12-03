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
					//需要在处理完消息之后，发送一次应答
					channel.basicAck(envelope.getDeliveryTag(), false);
				}
			}
		};
		/**
		 * 设置为 false 一旦某个消费者死亡，RabbitMq会重新发送它的消息给其他消费者(确保消息不丢失)
		 * 
		 * 设置为 true 一旦某个消费者死亡, RabbitMq 还是会将消息发送给它，导致消息丢失，并且会报错
		 */
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