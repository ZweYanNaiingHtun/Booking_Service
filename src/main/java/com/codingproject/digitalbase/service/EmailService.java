//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by Fernflower decompiler)
//

package com.codingproject.digitalbase.service;

public interface EmailService {
    void sendSimpleMessage(String to, String subject, String text);

    void sendOtpEmail(String to, String otp);

    void sendResetPasswordEmail(String to, String otp);

    void sendVerificationEmail(String to, String token);

    void sendStaffWelcomeEmail(String to, String temporaryPassword);
}
