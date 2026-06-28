//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by Fernflower decompiler)
//

package com.codingproject.digitalbase.service;

import com.codingproject.digitalbase.repository.UserRepository;
import jakarta.mail.internet.MimeMessage;
import lombok.Generated;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Service
public class EmailServiceImpl implements EmailService {
    @Generated
    private static final Logger log = LoggerFactory.getLogger(EmailServiceImpl.class);
    private final JavaMailSender emailSender;
    private final UserRepository userRepository;

    @Async("emailExecutor")
    public void sendSimpleMessage(String to, String subject, String text) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom("yannaing7269@gmail.com");
            message.setTo(to);
            message.setSubject(subject);
            message.setText(text);
            this.emailSender.send(message);
            log.info("Simple email sent successfully to: {}", to);
        } catch (Exception e) {
            log.error("Failed to send simple email to: {}. Error: {}", new Object[]{to, e.getMessage(), e});
        }

    }

    @Async("emailExecutor")
    public void sendOtpEmail(String to, String otp) {
        try {
            MimeMessage message = this.emailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true);
            helper.setTo(to);
            helper.setSubject("Your Login Verification Code");
            String htmlContent = "<!DOCTYPE html>\n<html>\n<head>\n<style>\nbody {\nfont-family: Arial, sans-serif;\n            background-color: #f4f4f4;\n            padding: 20px;\n        }\n\n        .container {\n            max-width: 600px;\n            margin: auto;\n            background-color: white;\n            border-radius: 10px;\n            padding: 40px;\n            text-align: center;\n            box-shadow: 0 2px 10px rgba(0,0,0,0.1);\n        }\n\n        .title {\n            font-size: 28px;\n            font-weight: bold;\n            color: #333333;\n            margin-bottom: 20px;\n        }\n\n        .message {\n            font-size: 16px;\n            color: #555555;\n            margin-bottom: 30px;\n        }\n\n        .otp-box {\n            display: inline-block;\n            background-color: #000000;\n            color: white;\n            font-size: 32px;\n            letter-spacing: 8px;\n            padding: 15px 30px;\n            border-radius: 8px;\n            font-weight: bold;\n            margin-bottom: 30px;\n        }\n\n        .footer {\n            font-size: 14px;\n            color: #999999;\n            margin-top: 30px;\n        }\n    </style>\n</head>\n\n<body>\n\n    <div class=\"container\">\n\n        <div class=\"title\">\n            OTP Verification\n        </div>\n\n        <div class=\"message\">\n            Use the following OTP code to complete your login.\n            This code will expire in 5 minutes.\n        </div>\n\n        <div class=\"otp-box\">\n            %s\n        </div>\n\n        <div class=\"footer\">\n            If you did not request this login,\n            please ignore this email.\n        </div>\n\n    </div>\n\n</body>\n</html>\n".formatted(otp);
            helper.setText(htmlContent, true);
            this.emailSender.send(message);
            log.info("OTP email sent successfully to: {}", to);
        } catch (Exception e) {
            log.error("Failed to send OTP email to: {}. Error: {}", new Object[]{to, e.getMessage(), e});
        }

    }

    @Async("emailExecutor")
    public void sendResetPasswordEmail(String to, String otp) {
        try {
            MimeMessage message = this.emailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true);
            helper.setTo(to);
            helper.setSubject("Reset Your Password Verification Code");
            String htmlContent = "<!DOCTYPE html>\n<html>\n<head>\n    <style>\n        body {\n            font-family: Arial, sans-serif;\n            background-color: #f4f4f4;\n            padding: 20px;\n        }\n\n        .container {\n            max-width: 600px;\n            margin: auto;\n            background-color: white;\n            border-radius: 10px;\n            padding: 40px;\n            text-align: center;\n            box-shadow: 0 2px 10px rgba(0,0,0,0.1);\n        }\n\n        .title {\n            font-size: 28px;\n            font-weight: bold;\n            color: #333333;\n            margin-bottom: 20px;\n        }\n\n        .message {\n            font-size: 16px;\n            color: #555555;\n            margin-bottom: 30px;\n        }\n\n        .otp-box {\n            display: inline-block;\n            background-color: #000000;\n            color: white;\n            font-size: 32px;\n            letter-spacing: 8px;\n            padding: 15px 30px;\n            border-radius: 8px;\n            font-weight: bold;\n            margin-bottom: 30px;\n        }\n\n        .footer {\n            font-size: 14px;\n            color: #999999;\n            margin-top: 30px;\n        }\n    </style>\n</head>\n\n<body>\n\n    <div class=\"container\">\n\n        <div class=\"title\">\n            Password Reset OTP\n        </div>\n\n        <div class=\"message\">\n            Use the following OTP verification code to reset your password.\n            This code will expire in 5 minutes.\n        </div>\n\n        <div class=\"otp-box\">\n            %s\n        </div>\n\n        <div class=\"footer\">\n            If you did not request a password reset,\n            please ignore this email.\n        </div>\n\n    </div>\n\n</body>\n</html>\n".formatted(otp);
            helper.setText(htmlContent, true);
            this.emailSender.send(message);
            log.info("Reset Password OTP email sent successfully to: {}", to);
        } catch (Exception e) {
            log.error("Failed to send reset password email to: {}. Error: {}", new Object[]{to, e.getMessage(), e});
        }

    }

    @Async("emailExecutor")
    public void sendVerificationEmail(String to, String otp) {
        try {
            MimeMessage message = this.emailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true);
            helper.setTo(to);
            helper.setSubject("Verify Your Account - Registration OTP");
            String htmlContent = "<!DOCTYPE html>\n<html>\n<head>\n    <style>\n        body {\n            font-family: Arial, sans-serif;\n            background-color: #f4f4f4;\n            padding: 20px;\n        }\n        .container {\n            max-width: 600px;\n            margin: auto;\n            background-color: white;\n            border-radius: 10px;\n            padding: 40px;\n            text-align: center;\n            box-shadow: 0 2px 10px rgba(0,0,0,0.1);\n        }\n        .title {\n            font-size: 28px;\n            font-weight: bold;\n            color: #333333;\n            margin-bottom: 20px;\n        }\n        .message {\n            font-size: 16px;\n            color: #555555;\n            margin-bottom: 30px;\n        }\n        .otp-box {\n            display: inline-block;\n            background-color: #000000;\n            color: white;\n            font-size: 32px;\n            letter-spacing: 8px;\n            padding: 15px 30px;\n            border-radius: 8px;\n            font-weight: bold;\n            margin-bottom: 30px;\n        }\n        .footer {\n            font-size: 14px;\n            color: #999999;\n            margin-top: 30px;\n        }\n    </style>\n</head>\n<body>\n    <div class=\"container\">\n        <div class=\"title\">\n            Account Verification\n        </div>\n        <div class=\"message\">\n            Thank you for registering! Please use the following OTP code to activate your account.\n            This code will expire in 5 minutes.\n        </div>\n        <div class=\"otp-box\">\n            %s\n        </div>\n        <div class=\"footer\">\n            If you did not initiate this registration, please ignore this email.\n        </div>\n    </div>\n</body>\n</html>\n".formatted(otp);
            helper.setText(htmlContent, true);
            this.emailSender.send(message);
            log.info("Registration Verification OTP email sent successfully to: {}", to);
        } catch (Exception e) {
            log.error("Failed to send verification email to: {}. Error: {}", new Object[]{to, e.getMessage(), e});
        }

    }

    @Async("emailExecutor")
    public void sendStaffWelcomeEmail(String to, String temporaryPassword) {
        try {
            MimeMessage message = this.emailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true);
            helper.setTo(to);
            helper.setSubject("Welcome to the Team! Your Staff Account Credentials");
            String htmlContent = "<!DOCTYPE html>\n<html>\n<head>\n    <style>\n        body {\n            font-family: Arial, sans-serif;\n            background-color: #f4f4f4;\n            padding: 20px;\n        }\n        .container {\n            max-width: 600px;\n            margin: auto;\n            background-color: white;\n            border-radius: 10px;\n            padding: 40px;\n            text-align: center;\n            box-shadow: 0 2px 10px rgba(0,0,0,0.1);\n        }\n        .title {\n            font-size: 26px;\n            font-weight: bold;\n            color: #333333;\n            margin-bottom: 15px;\n        }\n        .welcome {\n            font-size: 18px;\n            font-weight: bold;\n            color: #2c3e50;\n            margin-bottom: 20px;\n        }\n        .message {\n            font-size: 15px;\n            color: #555555;\n            line-height: 1.6;\n            margin-bottom: 30px;\n            text-align: left;\n        }\n        .credential-box {\n            display: inline-block;\n            background-color: #000000;\n            color: #ffffff;\n            font-size: 22px;\n            padding: 15px 25px;\n            border-radius: 8px;\n            font-weight: bold;\n            font-family: 'Courier New', Courier, monospace;\n            margin-bottom: 30px;\n            word-break: break-all;\n            letter-spacing: 1px;\n        }\n        .warning-text {\n            font-size: 14px;\n            color: #e74c3c;\n            font-weight: bold;\n            margin-bottom: 20px;\n        }\n        .footer {\n            font-size: 13px;\n            color: #999999;\n            margin-top: 30px;\n            border-top: 1px solid #eeeeee;\n            padding-top: 20px;\n        }\n    </style>\n</head>\n<body>\n    <div class=\"container\">\n        <div class=\"title\">Digital Base Team</div>\n        <div class=\"welcome\">Welcome to the Team! \ud83c\udf89</div>\n\n        <div class=\"message\">\n            Hello,<br><br>\n            Your official staff account has been successfully created by the administrator.\n            Please use the temporary password configuration below to log into your account dashboard.\n        </div>\n\n        <div class=\"credential-box\">\n            %s\n        </div>\n\n        <div class=\"warning-text\">\n            ⚠️ For security reasons, please change this temporary password immediately after your first successful login.\n        </div>\n\n        <div class=\"footer\">\n            This is an automated system email, please do not reply directly to this message.\n        </div>\n    </div>\n</body>\n</html>\n".formatted(temporaryPassword);
            helper.setText(htmlContent, true);
            this.emailSender.send(message);
            log.info("Staff welcome & credential email sent successfully to: {}", to);
        } catch (Exception e) {
            log.error("Failed to send staff welcome email to: {}. Error: {}", new Object[]{to, e.getMessage(), e});
        }

    }

    @Generated
    public EmailServiceImpl(final JavaMailSender emailSender, final UserRepository userRepository) {
        this.emailSender = emailSender;
        this.userRepository = userRepository;
    }
}
