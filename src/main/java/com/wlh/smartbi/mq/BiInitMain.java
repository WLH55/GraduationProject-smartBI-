package com.wlh.smartbi.mq;

import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import com.wlh.smartbi.common.constant.BiMqConstant;

import java.util.HashMap;
import java.util.Map;

public class BiInitMain {
    public static void main(String[] args) {
        try {
            ConnectionFactory factory = new ConnectionFactory();
        	// 设置 rabbitmq 对应的信息
       	 	factory.setHost(BiMqConstant.BI_HOST);
        	factory.setUsername(BiMqConstant.BI_USERNAME);
        	factory.setPassword(BiMqConstant.BI_PASSWORD);

            // 创建与 RabbitMQ 服务器的连接
            Connection connection = factory.newConnection();

            // 创建一个通道
            Channel channel = connection.createChannel();

            String BI_EXCHANGE_NAME = BiMqConstant.BI_EXCHANGE_NAME;
            // 声明一个直连交换机
            channel.exchangeDeclare(BI_EXCHANGE_NAME, BiMqConstant.BI_DIRECT_EXCHANGE);
            // 创建一个队列，随机分配一个队列名称
            String queueName = BiMqConstant.BI_QUEUE_NAME;
            // 通过设置 x-message-ttl 参数来指定消息的过期时间
            Map<String, Object> queueArgs = new HashMap<>();
            queueArgs.put("x-message-ttl", 60000); // 过期时间为 60 秒
            // 参数解释：queueDeclare(String queue, boolean durable, boolean exclusive, boolean autoDelete, Map<String, Object> arguments)
            // durable: 持久化队列（重启后依然存在）
            // exclusive: 排他性队列（仅限此连接可见，连接关闭后队列删除）
            // autoDelete: 自动删除队列（无消费者时自动删除）
            channel.queueDeclare(queueName, true, false, false, queueArgs);

            // 将队列与交换机进行绑定
            channel.queueBind(queueName, BI_EXCHANGE_NAME, BiMqConstant.BI_ROUTING_KEY);



        }catch (Exception e){
            
        }
    }

}