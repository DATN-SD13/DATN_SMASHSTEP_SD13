package com.smashstep.datn.promotion;
import com.smashstep.datn.promotion.service.EmailService;
import jakarta.mail.Session;
import jakarta.mail.internet.MimeMessage;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.mail.javamail.JavaMailSender;
import java.util.Properties;
import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.*;

class EmailServiceTest {
    @Test void voucherMailUsesMockSenderAndEscapesUntrustedHtmlWithoutNetwork() throws Exception {
        @SuppressWarnings("unchecked") ObjectProvider<JavaMailSender> provider=mock(ObjectProvider.class);
        JavaMailSender sender=mock(JavaMailSender.class);when(provider.getIfAvailable()).thenReturn(sender);
        MimeMessage message=new MimeMessage(Session.getInstance(new Properties()));when(sender.createMimeMessage()).thenReturn(message);
        new EmailService(provider).sendVoucherEmail("fixture@example.invalid","<script>unsafe</script>","Voucher","PGG123","10%","100000","0","2098-01-01","2098-01-31","javascript:alert(1)");
        verify(sender).send(message);message.saveChanges();
        var bytes=new java.io.ByteArrayOutputStream();message.writeTo(bytes);String mime=bytes.toString(java.nio.charset.StandardCharsets.UTF_8);
        assertNotNull(message.getSubject());assertEquals("fixture@example.invalid",message.getAllRecipients()[0].toString());
        var multipart=(jakarta.mail.Multipart)message.getContent();var related=(jakarta.mail.Multipart)multipart.getBodyPart(0).getContent();
        var alternative=(jakarta.mail.Multipart)related.getBodyPart(0).getContent();String html=alternative.getBodyPart(1).getContent().toString();
        assertTrue(html.contains("&lt;script&gt;unsafe&lt;/script&gt;"));assertFalse(html.contains("javascript:alert"));
    }
    @Test void missingSmtpReportsConfigurationErrorInsteadOfPreventingApplicationStartup() {
        @SuppressWarnings("unchecked") ObjectProvider<JavaMailSender> provider=mock(ObjectProvider.class);
        assertThrows(IllegalStateException.class,()->new EmailService(provider).sendVoucherEmail("fixture@example.invalid","Name","Voucher","PGG","10%","100","0","start","end",""));
    }
}
