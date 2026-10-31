package com.vetclinic.notification;

import com.vetclinic.notification.domain.Channel;
import jakarta.mail.Message;
import jakarta.mail.Session;
import jakarta.mail.internet.MimeMessage;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.javamail.JavaMailSender;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.Properties;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EmailSenderTest {

    @Mock
    private JavaMailSender mailSender;

    @Test
    void usesTheEmailChannel() {
        assertThat(new EmailSender(mailSender).channel()).isEqualTo(Channel.EMAIL);
    }

    @Test
    void sendsAMultipartMessageFromTheClinicAddress() throws Exception {
        MimeMessage message = new MimeMessage(Session.getInstance(new Properties()));
        when(mailSender.createMimeMessage()).thenReturn(message);

        new EmailSender(mailSender).send("ayse@example.com", "Your code", "Code: 123456 & <b>more</b>");

        verify(mailSender).send(message);
        assertThat(message.getFrom()[0].toString()).isEqualTo("VetClinic <noreply@vetclinic.local>");
        assertThat(message.getRecipients(Message.RecipientType.TO)[0].toString()).isEqualTo("ayse@example.com");
        assertThat(message.getSubject()).isEqualTo("Your code");

        message.saveChanges();
        ByteArrayOutputStream raw = new ByteArrayOutputStream();
        message.writeTo(raw);
        String mime = raw.toString(StandardCharsets.UTF_8);
        assertThat(mime).contains("text/plain").contains("text/html");
        assertThat(mime).contains("Code: 123456 & <b>more</b>");
        assertThat(mime).contains("Code: 123456 &amp; &lt;b&gt;more&lt;/b&gt;");
    }
}
