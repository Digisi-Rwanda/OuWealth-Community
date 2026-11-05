package rw.terimbere.csams.modules.member.service;

import jakarta.servlet.http.HttpServletRequest;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import rw.terimbere.csams.modules.audit.service.AuditService;
import rw.terimbere.csams.modules.cooperative.repository.CooperativeRepository;
import rw.terimbere.csams.modules.membership.entity.CooperativeMembership;
import rw.terimbere.csams.modules.membership.repository.CooperativeMembershipRepository;
import rw.terimbere.csams.modules.notification.mail.AccountEmailService;
import rw.terimbere.csams.modules.user.entity.User;
import rw.terimbere.csams.modules.user.repository.UserRepository;
import rw.terimbere.csams.security.CooperativeAuthorizationService;
import rw.terimbere.csams.security.UserPrincipal;
import rw.terimbere.csams.shared.auditing.AuditableAction;
import rw.terimbere.csams.shared.exceptions.BusinessException;

/**
 * Emails members their sign-in details: a fresh temporary password and their username. By default only members who
 * have never signed in (for example those created by the opening-balance import) are included, so nobody who already
 * uses the app loses their password; an officer can include the others explicitly, or pick members.
 */
@Service
@RequiredArgsConstructor
public class MemberLoginDetailsService {

    private final CooperativeMembershipRepository memberships;
    private final UserRepository users;
    private final CooperativeRepository cooperatives;
    private final PasswordEncoder passwordEncoder;
    private final AccountEmailService accountEmails;
    private final CooperativeAuthorizationService authorization;
    private final AuditService auditService;

    public record Preview(boolean emailReady, int neverSignedIn, int signedIn, int withoutEmail) {}

    public record Result(int sent, int skippedSignedIn, int skippedNoEmail, List<String> sentTo) {}

    @Transactional(readOnly = true)
    public Preview preview(UUID cooperativeId) {
        authorization.requireMembership(cooperativeId);
        int never = 0;
        int signed = 0;
        int noEmail = 0;
        for (User u : members(cooperativeId, null)) {
            if (!StringUtils.hasText(u.getEmail())) {
                noEmail++;
            } else if (u.getLastLoginAt() == null) {
                never++;
            } else {
                signed++;
            }
        }
        return new Preview(accountEmails.isReady(), never, signed, noEmail);
    }

    @Transactional
    public Result send(UUID cooperativeId, List<UUID> memberUserIds, boolean includeSignedIn, HttpServletRequest http) {
        authorization.requireMembership(cooperativeId);
        UserPrincipal me = authorization.currentPrincipal();
        if (!accountEmails.isReady()) {
            throw new BusinessException("EMAIL_NOT_CONFIGURED",
                    "Outgoing email is not set up on the server, so sign-in details cannot be sent");
        }
        String scheme = cooperatives.findById(cooperativeId).map(c -> c.getName()).orElse(null);
        Set<UUID> chosen = memberUserIds == null || memberUserIds.isEmpty() ? null : new HashSet<>(memberUserIds);
        int sent = 0;
        int skippedSignedIn = 0;
        int skippedNoEmail = 0;
        List<String> sentTo = new ArrayList<>();
        for (User u : members(cooperativeId, chosen)) {
            if (u.getId().equals(me.getId())) {
                continue;
            }
            if (!StringUtils.hasText(u.getEmail())) {
                skippedNoEmail++;
                continue;
            }
            // a member picked by name is sent even if they signed in before (the officer chose them)
            if (u.getLastLoginAt() != null && !includeSignedIn && chosen == null) {
                skippedSignedIn++;
                continue;
            }
            String temporary = AccountEmailService.temporaryPassword();
            u.setPasswordHash(passwordEncoder.encode(temporary));
            u.setFailedLoginAttempts(0);
            u.setLockedUntil(null);
            users.save(u);
            accountEmails.sendLoginDetails(u.getEmail(), u.getFirstName(), u.getUsername(), temporary, scheme);
            auditService.record(me.getId(), cooperativeId, AuditableAction.LOGIN_DETAILS_SENT, "User", u.getId(), null,
                    "{\"username\":\"" + u.getUsername().replace("\"", "") + "\"}", ip(http), http == null ? null : http.getHeader("User-Agent"));
            sentTo.add(u.getUsername());
            sent++;
        }
        return new Result(sent, skippedSignedIn, skippedNoEmail, sentTo);
    }

    private List<User> members(UUID cooperativeId, Set<UUID> chosen) {
        List<User> out = new ArrayList<>();
        for (CooperativeMembership m : memberships.findByCooperativeIdAndMembershipStatus(cooperativeId, "ACTIVE")) {
            if (chosen != null && !chosen.contains(m.getUserId())) {
                continue;
            }
            users.findByIdAndDeletedFalse(m.getUserId()).ifPresent(out::add);
        }
        return out;
    }

    private static String ip(HttpServletRequest r) {
        if (r == null) return null;
        String f = r.getHeader("X-Forwarded-For");
        return f != null && !f.isBlank() ? f.split(",")[0].trim() : r.getRemoteAddr();
    }
}
