package com.glodon.pcop.spacialimportsvc.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Component
public class KafkaSender {
    private static Logger log = LoggerFactory.getLogger(KafkaSender.class);
    @Autowired
    private KafkaTemplate kafkaTemplate;

    /**
     * 发送消息到kafka,主题为test
     */
    public void sendTest() {
        log.info("===start to send message...");
        // kafkaTemplate.send("test", "hello,kafka " + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS")));
        // kafkaTemplate.send("gis_data", "test data hello,kafka " + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS")));
        // log.info("complete to send message...");
    }
}
