package com.symbol.rabbitmq.rpc;

public class TestRPCMessage {
	public static void main(String[] args) throws Exception {
		RPCClient fibonacciRpc = new RPCClient();

		System.out.println(" [x] Requesting fib(30)");   
		String response = fibonacciRpc.call("30");
		System.out.println(" [.] Got '" + response + "'");

		fibonacciRpc.close();
	}
}
