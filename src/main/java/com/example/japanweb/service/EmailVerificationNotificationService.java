package com.example.japanweb.service;

import com.example.japanweb.config.properties.EmailVerificationProperties;
import com.example.japanweb.entity.User;
import jakarta.mail.MessagingException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.mail.MailException;
import org.springframework.mail.MailPreparationException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.util.HtmlUtils;
import org.springframework.web.util.UriComponentsBuilder;

import java.nio.charset.StandardCharsets;

@Slf4j
@Service
@RequiredArgsConstructor
public class EmailVerificationNotificationService {

    private final ObjectProvider<JavaMailSender> mailSenderProvider;
    private final EmailVerificationProperties emailVerificationProperties;

    public void sendVerificationEmail(User user, String token) {
        String verificationLink = buildVerificationLink(user.getEmail(), token);
        JavaMailSender mailSender = mailSenderProvider.getIfAvailable();

        if (!isMailTransportConfigured(mailSender)) {
            log.info("Email verification link for {}: {}", user.getEmail(), verificationLink);
            return;
        }

        try {
            var message = mailSender.createMimeMessage();
            var helper = new MimeMessageHelper(message, MimeMessageHelper.MULTIPART_MODE_MIXED_RELATED,
                    StandardCharsets.UTF_8.name());
            helper.setFrom(emailVerificationProperties.getFromAddress());
            helper.setTo(user.getEmail());
            helper.setSubject("Xác nhận email để kích hoạt tài khoản MiraiGo");
            helper.setText(
                    buildPlainTextBody(user.getUsername(), verificationLink),
                    buildHtmlBody(user.getUsername(), user.getEmail(), verificationLink)
            );
            mailSender.send(message);
        } catch (MessagingException ex) {
            log.error("Failed to prepare verification email for {}", user.getEmail(), ex);
            throw new MailPreparationException("Unable to prepare verification email", ex);
        } catch (MailException ex) {
            log.error("Failed to send verification email to {}", user.getEmail(), ex);
            throw ex;
        }
    }

    private String buildVerificationLink(String email, String token) {
        return UriComponentsBuilder
                .fromUriString(emailVerificationProperties.getWebBaseUrl())
                .path(emailVerificationProperties.getVerificationPath())
                .queryParam("token", token)
                .queryParam("email", email)
                .toUriString();
    }

    private boolean isMailTransportConfigured(JavaMailSender mailSender) {
        if (mailSender == null) {
            return false;
        }

        if (!(mailSender instanceof JavaMailSenderImpl sender)) {
            return true;
        }

        return StringUtils.hasText(sender.getHost());
    }

    private String buildPlainTextBody(String username, String verificationLink) {
        long expiresInMinutes = emailVerificationProperties.getTokenExpiration().toMinutes();

        return """
                Xin chào %s,

                Chào mừng bạn đến với MiraiGo.

                Hãy mở liên kết dưới đây để xác nhận email và kích hoạt tài khoản:

                %s

                Liên kết này chỉ sử dụng được một lần và có hiệu lực trong %d phút.

                Nếu bạn không tạo tài khoản MiraiGo, bạn có thể bỏ qua email này.

                MiraiGo Team
                """.formatted(username, verificationLink, expiresInMinutes);
    }

    private String buildHtmlBody(String username, String email, String verificationLink) {
        String safeUsername = HtmlUtils.htmlEscape(username);
        String safeEmail = HtmlUtils.htmlEscape(email);
        String safeVerificationLink = HtmlUtils.htmlEscape(verificationLink);
        long expiresInMinutes = emailVerificationProperties.getTokenExpiration().toMinutes();

        return """
                <!doctype html>
                <html lang="vi">
                <head>
                  <meta charset="UTF-8" />
                  <meta name="viewport" content="width=device-width, initial-scale=1.0" />
                  <title>MiraiGo | Xác nhận email</title>
                </head>
                <body style="margin:0;padding:0;background-color:#eef4ff;font-family:Inter,Segoe UI,Roboto,Helvetica,Arial,sans-serif;color:#0f172a;">
                  <div style="display:none;max-height:0;overflow:hidden;opacity:0;color:transparent;">
                    Xác nhận email để kích hoạt tài khoản MiraiGo và bắt đầu học ngay.
                  </div>
                  <table role="presentation" width="100%%" cellspacing="0" cellpadding="0" style="background:linear-gradient(180deg,#eef4ff 0%%,#e8f0ff 100%%);background-color:#eef4ff;margin:0;padding:28px 0;">
                    <tr>
                      <td align="center">
                        <table role="presentation" width="100%%" cellspacing="0" cellpadding="0" style="width:100%%;max-width:640px;margin:0 auto;">
                          <tr>
                            <td style="padding:0 20px;">
                              <table role="presentation" width="100%%" cellspacing="0" cellpadding="0" style="background:linear-gradient(145deg,#162033 0%%,#101827 58%%,#2958d7 100%%);background-color:#111827;border-radius:28px;overflow:hidden;box-shadow:0 24px 54px rgba(15,23,42,0.22);">
                                <tr>
                                  <td style="padding:30px 32px 20px 32px;">
                                    <table role="presentation" cellspacing="0" cellpadding="0">
                                      <tr>
                                        <td style="vertical-align:middle;">
                                          <div style="width:56px;height:56px;border-radius:18px;background:linear-gradient(135deg,#79a8ff 0%%,#4d7dff 100%%);background-color:#4f83ff;display:flex;align-items:center;justify-content:center;box-shadow:0 14px 28px rgba(37,99,235,0.28);">
                                            <svg width="32" height="32" viewBox="0 0 40 40" fill="none" xmlns="http://www.w3.org/2000/svg" aria-hidden="true">
                                              <path d="M10 24.5C10 17.0442 16.0442 11 23.5 11H31" stroke="#FFFFFF" stroke-width="2.4" stroke-linecap="round" stroke-linejoin="round" stroke-opacity="0.92"/>
                                              <path d="M27 7L31 11L27 15" stroke="#FFFFFF" stroke-width="2.4" stroke-linecap="round" stroke-linejoin="round" stroke-opacity="0.92"/>
                                              <path d="M10 30L18.2 22L22.8 26.6L31 18.5" stroke="#FFFFFF" stroke-width="2.4" stroke-linecap="round" stroke-linejoin="round"/>
                                              <path d="M7.4 9.4L8.6 11.7L10.9 12.9L8.6 14.1L7.4 16.4L6.2 14.1L3.9 12.9L6.2 11.7L7.4 9.4Z" fill="#FFFFFF" fill-opacity="0.85"/>
                                            </svg>
                                          </div>
                                        </td>
                                        <td style="padding-left:14px;vertical-align:middle;">
                                          <div style="font-size:28px;font-weight:700;line-height:1.1;">
                                            <span style="color:#ffffff;">Mirai</span><span style="color:#93c5fd;">Go</span>
                                          </div>
                                          <div style="margin-top:6px;font-size:12px;letter-spacing:0.1em;text-transform:uppercase;color:#bfdbfe;">
                                            Xác nhận tài khoản
                                          </div>
                                        </td>
                                      </tr>
                                    </table>
                                  </td>
                                </tr>
                                <tr>
                                  <td style="padding:0 20px 20px 20px;">
                                    <table role="presentation" width="100%%" cellspacing="0" cellpadding="0" style="background:linear-gradient(180deg,#ffffff 0%%,#f8fbff 100%%);background-color:#ffffff;border:1px solid #d6dfeb;border-radius:24px;">
                                      <tr>
                                        <td style="padding:32px;">
                                          <div style="display:inline-block;padding:8px 14px;border-radius:999px;background-color:#eaf1ff;color:#2563eb;font-size:12px;font-weight:700;letter-spacing:0.06em;text-transform:uppercase;">
                                            Xác nhận email
                                          </div>
                                          <h1 style="margin:20px 0 12px 0;font-size:32px;line-height:1.2;color:#0f172a;font-weight:800;">
                                            Chào %s, hãy kích hoạt tài khoản của bạn
                                          </h1>
                                          <p style="margin:0 0 18px 0;font-size:16px;line-height:1.7;color:#475569;">
                                            Tài khoản MiraiGo cho <strong style="color:#0f172a;">%s</strong> đã sẵn sàng.
                                            Chỉ còn một bước nhỏ để bắt đầu học, lưu tiến độ và đồng bộ trên mọi thiết bị.
                                          </p>
                                          <table role="presentation" width="100%%" cellspacing="0" cellpadding="0" style="margin:24px 0;background-color:#f7faff;border:1px solid #d6dfeb;border-radius:18px;">
                                            <tr>
                                              <td style="padding:18px 20px;">
                                                <div style="font-size:13px;font-weight:700;letter-spacing:0.08em;text-transform:uppercase;color:#2563eb;margin-bottom:8px;">
                                                  Sẵn sàng để bắt đầu
                                                </div>
                                                <div style="font-size:15px;line-height:1.7;color:#475569;">
                                                  Liên kết này chỉ sử dụng được <strong style="color:#0f172a;">một lần</strong>
                                                  và sẽ hết hạn sau <strong style="color:#0f172a;">%d phút</strong>.
                                                </div>
                                              </td>
                                            </tr>
                                          </table>
                                          <table role="presentation" width="100%%" cellspacing="0" cellpadding="0" style="margin:0 0 12px 0;">
                                            <tr>
                                              <td style="padding:0 0 16px 0;">
                                                <div style="font-size:14px;line-height:1.7;color:#64748b;">
                                                  Nút bên dưới sẽ mở MiraiGo và xác nhận email của bạn tự động.
                                                </div>
                                              </td>
                                            </tr>
                                          </table>
                                          <table role="presentation" cellspacing="0" cellpadding="0" style="margin:0 0 28px 0;">
                                            <tr>
                                              <td align="center" bgcolor="#3d73f3" style="border-radius:16px;box-shadow:0 16px 30px -18px rgba(61,115,243,0.65);">
                                                <a href="%s" style="display:inline-block;padding:15px 24px;font-size:16px;font-weight:700;color:#ffffff;text-decoration:none;border-radius:16px;background:linear-gradient(135deg,#79a8ff 0%%,#3d73f3 100%%);background-color:#3d73f3;">
                                                  Xác nhận tài khoản
                                                </a>
                                              </td>
                                            </tr>
                                          </table>
                                          <table role="presentation" width="100%%" cellspacing="0" cellpadding="0" style="border-top:1px solid #e2e8f0;">
                                            <tr>
                                              <td style="padding-top:22px;">
                                                <p style="margin:0 0 8px 0;font-size:14px;line-height:1.7;color:#475569;">
                                                  Nếu bạn không tạo tài khoản MiraiGo, bạn có thể bỏ qua email này.
                                                </p>
                                                <p style="margin:0;font-size:13px;line-height:1.7;color:#94a3b8;">
                                                  MiraiGo Team
                                                </p>
                                              </td>
                                            </tr>
                                          </table>
                                        </td>
                                      </tr>
                                    </table>
                                  </td>
                                </tr>
                              </table>
                            </td>
                          </tr>
                        </table>
                      </td>
                    </tr>
                  </table>
                </body>
                </html>
                """.formatted(
                safeUsername,
                safeEmail,
                expiresInMinutes,
                safeVerificationLink
        );
    }
}
