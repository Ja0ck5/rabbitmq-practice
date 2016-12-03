package com.symbol.rabbitmq.topic;

import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;

public class EmitLogTopic {

	private static final String EXCHANGE_NAME = "topic_logs";

	private static final String[] ROUTING_KEYS = { "Model.auth", "Model.kern", "Model.critical", 
												   "View.auth","View.kern", "View.critical", 
												   "Ctl.auth", "Ctl.kern", "Ctl.critical",
												   "Ctl.auth.Model", "Ctl.kern.View", "Ctl.critical.Ctl",
												   "Model.auth.Model", "Model.kern.View", "Model.critical.Ctl",
												   "View.auth.Model.haha.hehe", "View.kern.View.haha.hehe", "View.critical.Ctl.haha.hehe"};

	public static void main(String[] argv) throws Exception {

		ConnectionFactory factory = new ConnectionFactory();
		factory.setHost("localhost");
		Connection connection = factory.newConnection();
		Channel channel = connection.createChannel();

		channel.exchangeDeclare(EXCHANGE_NAME, "topic");

		for (int i= 0; i< ROUTING_KEYS.length; i++) {
			String message = getLogMessage(ROUTING_KEYS[i],i);
			channel.basicPublish(EXCHANGE_NAME, ROUTING_KEYS[i], null, message.getBytes());
			System.out.println(" [x] Sent '" + ROUTING_KEYS[i] + "':'" + message + "'");
		}

		connection.close();
	}
	// ...

	private static String getLogMessage(String rk,int idx) {
		return "facility source: ["+rk+"]"+"----- " + idx;
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