package rw.terimbere.csams.modules.notification;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import jakarta.mail.Session;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;
import java.util.Properties;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.mail.javamail.JavaMailSender;
import rw.terimbere.csams.modules.notification.mail.AccountEmailService;
import rw.terimbere.csams.modules.notification.mail.MailProperties;

class AccountEmailServiceTest {

    @Test
    @SuppressWarnings("unchecked")
    void signInDetailsComeFromOuWealthCommunityWithUsernamePasswordAndLink() throws Exception {
        JavaMailSender sender = mock(JavaMailSender.class);
        when(sender.createMimeMessage()).thenReturn(new MimeMessage(Session.getInstance(new Properties())));
        ObjectProvider<JavaMailSender> provider = mock(ObjectProvider.class);
        when(provider.getIfAvailable()).thenReturn(sender);
        MailProperties props = new MailProperties();
        props.setHost("smtp-relay.brevo.com");
        props.setFrom("no-reply@example.org");
        AccountEmailService service = new AccountEmailService(provider, props, Runnable::run);

        service.sendLoginDetails("odile@example.org", "Odile", "odile.umuhoza", "Abc2-Def3-Ghi4", "THE COMRADES");

        ArgumentCaptor<MimeMessage> sent = ArgumentCaptor.forClass(MimeMessage.class);
        verify(sender).send(sent.capture());
        MimeMessage m = sent.getValue();
        m.saveChanges();
        assertThat(((InternetAddress) m.getFrom()[0]).getPersonal()).isEqualTo("OuWealth Community");
        assertThat(m.getSubject()).contains("THE COMRADES");
        String raw = text(m.getContent());
        assertThat(raw).contains("odile.umuhoza").contains("Abc2-Def3-Ghi4").contains("https://wealthcommunity.ousuite.com/login");
    }

    private static String text(Object content) throws Exception {
        if (content instanceof String s) {
            return s;
        }
        StringBuilder b = new StringBuilder();
        if (content instanceof jakarta.mail.Multipart mp) {
            for (int i = 0; i < mp.getCount(); i++) {
                b.append(text(mp.getBodyPart(i).getContent())).append('\n');
            }
        }
        return b.toString();
    }

    @Test
    void temporaryPasswordsAreReadable() {
        for (int i = 0; i < 50; i++) {
            assertThat(AccountEmailService.temporaryPassword()).matches("[A-HJ-NP-Z][a-km-np-z]{2}[2-9](-[A-HJ-NP-Z][a-km-np-z]{2}[2-9]){2}");
        }
    }
}
