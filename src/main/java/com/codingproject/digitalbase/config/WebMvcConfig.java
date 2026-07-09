//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by Fernflower decompiler)
//

package com.codingproject.digitalbase.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.nio.file.Paths;

@Configuration
public class WebMvcConfig implements WebMvcConfigurer {
    public WebMvcConfig() {
    }

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // 🎯 URL ကနေ /uploads/filename.png လို့ ခေါ်ရင် local ထဲက uploads folder ထဲ သွားရှာခိုင်းတာပါ
        String uploadDirPath = Paths.get("uploads").toAbsolutePath().toUri().toString();

        registry.addResourceHandler("/uploads/**")
                .addResourceLocations(uploadDirPath);
    }
}
