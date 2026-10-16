package com.smashstep.datn.employee;

import com.smashstep.datn.employee.mail.EmployeeMailService;
import jakarta.mail.*;
import jakarta.mail.internet.MimeMessage;
import org.junit.jupiter.api.*;
import org.springframework.mail.MailSendException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.util.ReflectionTestUtils;
import java.util.Properties;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/** Inspect MIME messages with a mocked sender; never contact SMTP. */
class EmployeeAccountEmailTemplateTest {
    final JavaMailSender sender=mock(JavaMailSender.class);
    final EmployeeMailService service=new EmployeeMailService(sender);
    MimeMessage message;
    @BeforeEach void setup(){
        message=new MimeMessage(Session.getInstance(new Properties()));
        when(sender.createMimeMessage()).thenReturn(message);
        ReflectionTestUtils.setField(service,"fromEmail","noreply@example.test");
        ReflectionTestUtils.setField(service,"frontendUrl","http://localhost:5173");
    }
    String html(Part part)throws Exception{
        if(part.isMimeType("text/html"))return part.getContent().toString();
        if(part.getContent() instanceof Multipart multi){
            for(int i=0;i<multi.getCount();i++){String value=html(multi.getBodyPart(i));if(!value.isEmpty())return value;}
        }
        return "";
    }
    @Test void employeeTemplateIncludesRoleLoginUrlAndEscapesInterpolatedValues()throws Exception{
        service.sendEmployeeAccountEmail("staff@example.test","Staff <script>","Nhân viên <b>","NV&0088","Test<>&Password");
        message.saveChanges();String body=html(message);
        assertTrue(body.contains("Staff &lt;script&gt;"));assertFalse(body.contains("Staff <script>"));
        assertTrue(body.contains("Nhân viên &lt;b&gt;"));assertTrue(body.contains("NV&amp;0088"));
        assertTrue(body.contains("Test&lt;&gt;&amp;Password"));assertTrue(body.contains("http://localhost:5173"));
        assertEquals("staff@example.test",message.getAllRecipients()[0].toString());
        verify(sender).send(message);
    }
    @Test void existingManagerApiUsesTheNewTemplateWithManagerRole()throws Exception{
        service.sendManagerAccountEmail("manager@example.test","Manager","NV0001","Temporary123@");
        message.saveChanges();assertTrue(html(message).contains("Quản lý"));verify(sender).send(message);
    }
    @Test void transportFailureIsReportedWithoutLeakingTemporaryPassword(){
        doThrow(new MailSendException("mock SMTP unavailable")).when(sender).send(any(MimeMessage.class));
        var ex=assertThrows(RuntimeException.class,()->service.sendEmployeeAccountEmail("staff@example.test","Staff","Nhân viên","NV0088","Temporary123@"));
        assertTrue(ex.getMessage().contains("staff@example.test"));assertFalse(ex.getMessage().contains("Temporary123@"));
    }
}
