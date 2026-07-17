package com.codingproject.digitalbase.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;

@Configuration
@EnableAsync // 🌟 Spring ရဲ့ Async Feature ကို ဖွင့်လိုက်ခြင်း
public class AsyncConfig {

    @Bean(name = "notificationExecutor")
    public Executor notificationExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(5);      // ပုံမှန်အဆင်သင့်ရှိနေမည့် Thread အရေအတွက်
        executor.setMaxPoolSize(10);     // Notification တွေ အရမ်းများလာရင် အများဆုံး တိုးပေးမည့် Thread အရေအတွက်
        executor.setQueueCapacity(500);  // Thread တွေ အကုန်အလုပ်ရှုပ်နေရင် စောင့်ဆိုင်းရမည့် Queue စီတန်းမှု
        executor.setThreadNamePrefix("NotificationThread-");
        executor.initialize();
        return executor;
    }
    // ✉️ 🌟 အသစ်ထပ်ဖြည့်ရမည့် Email Executor (Error ပျောက်စေမည့်အပိုင်း)
    @Bean(name = "emailExecutor")
    public Executor emailExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(3);       // Email ပို့ရန် အဆင်သင့်ရှိမည့် Thread အရေအတွက်
        executor.setMaxPoolSize(5);        // အများဆုံး တိုးပေးမည့် Thread အရေအတွက်
        executor.setQueueCapacity(200);    // စောင့်ဆိုင်းရမည့် Queue အရေအတွက်
        executor.setThreadNamePrefix("EmailThread-");
        executor.initialize();
        return executor;
    }
}