package com.vetclinic.notification;

import com.vetclinic.common.LogMasker;
import com.vetclinic.notification.domain.Channel;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Component;
import org.springframework.web.util.HtmlUtils;

import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;

/** Sends a multipart e-mail (plain text + simple HTML) through {@link JavaMailSender}. */
@Component
public class EmailSender implements NotificationSender {

    static final String FROM_ADDRESS = "noreply@vetclinic.local";
    static final String FROM_NAME = "VetClinic";

    private static final Logger log = LoggerFactory.getLogger(EmailSender.class);

    private final JavaMailSender mailSender;

    public EmailSender(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    @Override
    public Channel channel() {
        return Channel.EMAIL;
    }

    @Override
    public void send(String destination, String subject, String body) {
        MimeMessage message = mailSender.createMimeMessage();
        try {
            MimeMessageHelper helper = new MimeMessageHelper(message, true, StandardCharsets.UTF_8.name());
            helper.setFrom(FROM_ADDRESS, FROM_NAME);
            helper.setTo(destination);
            helper.setSubject(subject);
            helper.setText(body, toHtml(body));
        } catch (MessagingException | UnsupportedEncodingException e) {
            throw new IllegalStateException("Could not build the e-mail message", e);
        }
        mailSender.send(message);
        log.info("E-mail sent to {}", LogMasker.maskEmail(destination));
    }

    private static String toHtml(String body) {
        return "<html><body><p>" + HtmlUtils.htmlEscape(body).replace("\n", "<br>") + "</p></body></html>";
    }
}
