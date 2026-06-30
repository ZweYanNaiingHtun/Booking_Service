//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by Fernflower decompiler)
//

package com.codingproject.digitalbase.service;

import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.FirebaseMessagingException;
import com.google.firebase.messaging.Message;
import com.google.firebase.messaging.Notification;
import lombok.Generated;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class FCMService {
    private static final Logger log = LoggerFactory.getLogger(FCMService.class);

    public void sendPushNotification(String targetToken, String title, String body) {
        Notification notification = Notification.builder().setTitle(title).setBody(body).build();
        Message message = Message.builder().setToken(targetToken).setNotification(notification).build();

        try {
            String response = FirebaseMessaging.getInstance().send(message);
            log.info("✅ Successfully sent message to token: {}", response);
        } catch (FirebaseMessagingException e) {
            log.error("❌ Error sending FCM message to token: {}", e.getMessage());
        }

    }

    public void sendPushNotificationToTopic(String topic, String title, String body) {
        Notification notification = Notification.builder().setTitle(title).setBody(body).build();
        Message message = Message.builder().setTopic(topic).setNotification(notification).build();

        try {
            String response = FirebaseMessaging.getInstance().send(message);
            log.info("✅ Firebase Topic Connection Success! Message ID: {}", response);
        } catch (FirebaseMessagingException e) {
            log.error("❌ Firebase Topic Error: {}", e.getMessage());
        }

    }
}
