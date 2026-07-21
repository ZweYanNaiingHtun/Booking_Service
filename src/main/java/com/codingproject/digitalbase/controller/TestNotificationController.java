
package com.codingproject.digitalbase.controller;

import com.codingproject.digitalbase.service.FCMService;
import lombok.Generated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping({"/api/test-notification"})
public class TestNotificationController {
    private final FCMService fcmService;

    @PostMapping({"/send-topic"})
    public String sendTopicNotification() {
        this.fcmService.sendPushNotificationToTopic("test-topic", "Backend Test Successful \ud83c\udf89", "Your Spring Boot is sending messages to Firebase!");
        return "Topic push request sent to Firebase! Check your IntelliJ console.";
    }

    @PostMapping({"/send-token"})
    public String sendTokenNotification(@RequestParam String token) {
        this.fcmService.sendPushNotification(token, "Direct Message", "This message is sent directly to your phone.");
        return "Direct push request sent to Firebase!";
    }

    @Generated
    public TestNotificationController(final FCMService fcmService) {
        this.fcmService = fcmService;
    }
}
