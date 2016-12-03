package com.symbol.rabbitmq.routing;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeoutException;

import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;

public class EmitLogDirect {

	private static final String EXCHANGE_NAME = "direct_logs";
	// severity list
	private static final String[] LOG_SEVERITY = { "info", "warning", "error" };

	public static void main(String[] argv) throws java.io.IOException, TimeoutException {

		ConnectionFactory factory = new ConnectionFactory();
		factory.setHost("localhost");
		Connection connection = factory.newConnection();
		Channel channel = connection.createChannel();

		//创建并指定交换类型
		channel.exchangeDeclare(EXCHANGE_NAME, "direct");
		// 发布消息
		for (int i = 0; i < 10; i++) {
			String severity = LOG_SEVERITY[(i%LOG_SEVERITY.length)];
			String[] ms = { "Ja0ck5-Log-..." +severity+"-"+ i };
			String message = getMessage(ms);
			channel.basicPublish(EXCHANGE_NAME, severity, null, message.getBytes());
			System.out.println(" [x] Sent '" + severity + "':'" + message + "'");
		}

		channel.close();
		connection.close();
	}

	public static String getMessage(String[] strings) {
		if (strings.length < 1)
			return "Hello World!";
		return joinStrings(strings, " ");
	}

	public static String joinStrings(String[] strings, String delimiter) {
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