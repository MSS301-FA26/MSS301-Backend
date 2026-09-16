package com.cinemaai.identity.service.impl;

import com.cinemaai.identity.config.MailProperties;
import com.cinemaai.identity.exception.BadRequestException;
import com.cinemaai.identity.service.MailService;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.mail.MailException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class MailServiceImpl implements MailService {

    private final ObjectProvider<JavaMailSender> mailSenderProvider;
    private final MailProperties mailProperties;

    @Override
    public void sendOtp(String to, String otp, String purpose) {
        log.info("========== [OTP EMAIL] Recipient: {}, Purpose: {}, OTP Code: {} ==========", to, purpose, otp);

        if (!mailProperties.isEnabled()) {
            log.warn("OTP email was not sent via SMTP because app.mail.enabled is false. Use the logged OTP code above.");
            return;
        }

        if (to == null || to.isBlank()) {
            throw new BadRequestException("Recipient email is required");
        }

        JavaMailSender mailSender = mailSenderProvider.getIfAvailable();
        if (mailSender == null) {
            log.warn("JavaMailSender is not configured. Falling back to console OTP: {}", otp);
            return;
        }

        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, "UTF-8");
            helper.setFrom(mailProperties.getFrom());
            helper.setTo(to);
            helper.setSubject("CinemaAI - Mã xác minh của bạn");
            helper.setText(buildOtpEmail(purpose, otp), true);
            mailSender.send(message);
            log.info("OTP email successfully sent to {}", to);
        } catch (MailException | MessagingException exception) {
            log.warn("Could not send OTP email via SMTP to {}: {}. OTP is logged in console.", to, exception.getMessage());
        }
    }

    private String buildOtpEmail(String purpose, String otp) {
        return """
                <!doctype html>
                <html lang="vi">
                <head>
                    <meta charset="UTF-8">
                    <style>
                        body { margin: 0; padding: 0; background: #f4f6f8; color: #1f2937; font-family: Arial, sans-serif; }
                        .container { max-width: 560px; margin: 30px auto; background: #ffffff; border: 1px solid #e5e7eb; border-radius: 8px; overflow: hidden; }
                        .header { padding: 24px 28px; background: #111827; color: #ffffff; }
                        .brand { margin: 0; font-size: 24px; font-weight: 700; }
                        .content { padding: 28px; }
                        .title { margin: 0 0 12px; color: #111827; font-size: 20px; font-weight: 700; }
                        .otp-box { margin: 24px 0; padding: 20px; background: #fff7ed; border: 1px solid #fed7aa; border-radius: 8px; text-align: center; }
                        .otp-label { margin: 0 0 8px; color: #9a3412; font-size: 13px; font-weight: 700; }
                        .otp-code { margin: 0; color: #111827; font-size: 34px; font-weight: 700; letter-spacing: 6px; }
                        .footer { padding: 18px 28px; background: #f9fafb; border-top: 1px solid #e5e7eb; color: #6b7280; font-size: 13px; }
                    </style>
                </head>
                <body>
                    <div class="container">
                        <div class="header">
                            <p class="brand">CinemaAI</p>
                        </div>
                        <div class="content">
                            <h1 class="title">Mã xác minh của bạn</h1>
                            <p>Vui lòng sử dụng mã bên dưới để tiếp tục thao tác: <strong>%s</strong>.</p>
                            <div class="otp-box">
                                <p class="otp-label">Mã xác minh</p>
                                <p class="otp-code">%s</p>
                            </div>
                            <p>Mã này có hiệu lực trong 90 giây. Vui lòng không chia sẻ mã với bất kỳ ai.</p>
                        </div>
                        <div class="footer">CinemaAI - Nền tảng điện ảnh trực tuyến</div>
                    </div>
                </body>
                </html>
                """.formatted(purpose, otp);
    }
}
