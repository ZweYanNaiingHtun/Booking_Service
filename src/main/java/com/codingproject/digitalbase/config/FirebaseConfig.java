//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by Fernflower decompiler)
//

package com.codingproject.digitalbase.config;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import jakarta.annotation.PostConstruct;
import java.io.IOException;
import java.io.InputStream;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;

@Configuration
public class FirebaseConfig {
    public FirebaseConfig() {
    }

    @PostConstruct
    public void initializeFirebase() {
        try {
            String firebaseConfigPath = "booking-service-firebase-adminsdk.json";
            ClassPathResource resource = new ClassPathResource(firebaseConfigPath);
            if (!resource.exists()) {
                System.err.println("❌ Error: Firebase JSON file not found in resources folder!");
                return;
            }

            InputStream serviceAccount = resource.getInputStream();
            FirebaseOptions options = FirebaseOptions.builder().setCredentials(GoogleCredentials.fromStream(serviceAccount)).build();
            if (FirebaseApp.getApps().isEmpty()) {
                FirebaseApp.initializeApp(options);
                System.out.println("✅ [Firebase] Firebase Admin SDK initialized successfully with project: " + options.getProjectId());
            } else {
                System.out.println("ℹ️ [Firebase] Firebase App already initialized.");
            }
        } catch (IOException e) {
            System.err.println("❌ [Firebase] Initialization Error: " + e.getMessage());
        }

    }
}
