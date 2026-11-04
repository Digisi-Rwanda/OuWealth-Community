package rw.terimbere.csams.modules.notification.mail;

import jakarta.mail.internet.MimeMessage;
import java.security.SecureRandom;
import java.util.concurrent.Executor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.util.StringUtils;

/**
 * Account emails: the sign-in details of a new member (username and a temporary password they may change later) and the
 * password-reset link. Sent through the configured SMTP relay (Brevo) after the transaction commits, with the platform's
 * name as the visible sender. A failure is logged, never thrown: the account exists either way.
 */
@Service
public class AccountEmailService {

    private static final Logger log = LoggerFactory.getLogger(AccountEmailService.class);
    /** No look-alike characters (0/O, 1/l/I), so a password read from an email is typed right the first time. */
    private static final String UPPER = "ABCDEFGHJKLMNPQRSTUVWXYZ";
    private static final String LOWER = "abcdefghijkmnpqrstuvwxyz";
    private static final String DIGITS = "23456789";
    private static final SecureRandom RANDOM = new SecureRandom();

    private final JavaMailSender mailSender;
    private final MailProperties mail;
    private final Executor executor;

    public AccountEmailService(
            ObjectProvider<JavaMailSender> mailSender, MailProperties mail, @Qualifier("taskExecutor") Executor executor) {
        this.mailSender = mailSender.getIfAvailable();
        this.mail = mail;
        this.executor = executor;
    }

    /** True when emails can actually leave: SMTP host, sender address and the switch are set. */
    public boolean isReady() {
        return mailSender != null && mail.isConfigured();
    }

    @EventListener(ApplicationReadyEvent.class)
    public void reportStatus() {
        if (isReady()) {
            log.info("Account emails: enabled through {} as \"{}\" <{}>", mail.getHost(), mail.getFromName(), mail.getFrom());
        } else {
            log.warn("Account emails: DISABLED (set MAIL_HOST, MAIL_PORT, MAIL_USERNAME, MAIL_PASSWORD and MAIL_FROM); "
                    + "new members will not receive their sign-in details");
        }
    }

    /** A temporary password: 12 characters, upper and lower case letters and digits, easy to read and type. */
    public static String temporaryPassword() {
        StringBuilder b = new StringBuilder();
        for (int group = 0; group < 3; group++) {
            if (group > 0) {
                b.append('-');
            }
            b.append(UPPER.charAt(RANDOM.nextInt(UPPER.length())));
            b.append(LOWER.charAt(RANDOM.nextInt(LOWER.length())));
            b.append(LOWER.charAt(RANDOM.nextInt(LOWER.length())));
            b.append(DIGITS.charAt(RANDOM.nextInt(DIGITS.length())));
        }
        return b.toString();
    }

    public void sendLoginDetails(String email, String firstName, String username, String password, String schemeName) {
        if (!StringUtils.hasText(email)) {
            return;
        }
        String signIn = url("/login");
        String subject = "Your OuWealth Community account" + (StringUtils.hasText(schemeName) ? " for " + schemeName.trim() : "");
        String text = "Hello " + name(firstName) + ",\n\n"
                + (StringUtils.hasText(schemeName) ? schemeName.trim() + " now keeps its records on OuWealth Community. " : "")
                + "An account has been created for you.\n\n"
                + "Sign in at: " + signIn + "\n"
                + "Username: " + username + "\n"
                + "Temporary password: " + password + "\n\n"
                + "You can keep this password or change it at any time from your profile (Change password).\n"
                + "If you did not expect this email, contact your scheme's officers.";
        later(() -> send(email, subject, text));
    }

    public void sendPasswordReset(String email, String firstName, String token) {
        if (!StringUtils.hasText(email)) {
            return;
        }
        String link = url("/reset-password?token=" + token);
        String text = "Hello " + name(firstName) + ",\n\n"
                + "We received a request to reset your OuWealth Community password.\n\n"
                + "Choose a new password here (the link works for one hour): " + link + "\n\n"
                + "If you did not ask for this, ignore this email; your password stays as it is.";
        later(() -> send(email, "Reset your OuWealth Community password", text));
    }

    private void send(String to, String subject, String text) {
        if (!isReady()) {
            log.warn("Account email to {} not sent: outgoing mail is not configured", mask(to));
            return;
        }
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setFrom(NotificationEmailCopy.fromAddress(mail.getFrom(), mail.getFromName()));
            helper.setTo(to.trim());
            helper.setSubject(subject);
            helper.setText(NotificationEmailCopy.plainText(subject, text), NotificationEmailCopy.html(subject, text));
            mailSender.send(message);
            log.info("Account email sent to {}: {}", mask(to), subject);
        } catch (Exception e) {
            log.warn("Account email to {} failed: {} {}", mask(to), e.getClass().getSimpleName(), e.getMessage());
        }
    }

    private void later(Runnable task) {
        if (TransactionSynchronizationManager.isSynchronizationActive() && TransactionSynchronizationManager.isActualTransactionActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    executor.execute(task);
                }
            });
            return;
        }
        executor.execute(task);
    }

    private String url(String path) {
        String base = StringUtils.hasText(mail.getAppUrl()) ? mail.getAppUrl().trim() : "https://wealthcommunity.ousuite.com";
        return (base.endsWith("/") ? base.substring(0, base.length() - 1) : base) + path;
    }

    private static String name(String firstName) {
        return StringUtils.hasText(firstName) ? firstName.trim() : "there";
    }

    static String mask(String email) {
        int at = email == null ? -1 : email.indexOf('@');
        return at <= 1 ? "***" : email.charAt(0) + "***" + email.substring(at);
    }
}
