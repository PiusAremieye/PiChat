package com.github.pius.pichats.service.implementation;

import java.net.URI;

import javax.jms.Connection;
import javax.jms.ConnectionFactory;
import javax.jms.Message;
import javax.jms.MessageConsumer;
import javax.jms.MessageProducer;
import javax.jms.Queue;
import javax.jms.Session;
import javax.jms.TextMessage;

import org.apache.activemq.ActiveMQConnectionFactory;
import org.apache.activemq.broker.BrokerFactory;
import org.apache.activemq.broker.BrokerService;
import org.springframework.stereotype.Service;

@Service
public class NotificationQueueServiceImpl {
    public NotificationQueueServiceImpl() {
        // tdod
    }

    public synchronized void sendNotification(String message) {
        // todo
    }

    public synchronized void recieveNotification() throws Exception {
        // todo
        BrokerService broker = BrokerFactory.createBroker(new URI("broker:(tcp://localhost:61616)"));
        broker.start();
        Connection connection = null;
        try {
            // Producer
            ConnectionFactory connectionFactory = new ActiveMQConnectionFactory("tcp://localhost:61616");
            connection = connectionFactory.createConnection();
            Session session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);
            Queue queue = session.createQueue("notificationQueue");
            String payload = "Important Task";
            Message msg = session.createTextMessage(payload);
            MessageProducer producer = session.createProducer(queue);
            System.out.println("Sending text '" + payload + "'");
            producer.send(msg);

            // Consumer
            MessageConsumer consumer = session.createConsumer(queue);
            connection.start();
            TextMessage textMsg = (TextMessage) consumer.receive();
            System.out.println(textMsg);
            System.out.println("Received: " + textMsg.getText());
            session.close();
        } finally {
            if (connection != null) {
                connection.close();
            }
            broker.stop();
        }
    }
}