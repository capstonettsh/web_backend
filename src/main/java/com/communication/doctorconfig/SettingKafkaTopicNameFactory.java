package com.communication.doctorconfig;

import org.springframework.stereotype.Component;

@Component
public class SettingKafkaTopicNameFactory {
    public SettingKafkaTopicName create(String sessionDateTime, String userId) {
        return new SettingKafkaTopicName(sessionDateTime, userId);
    }
}
