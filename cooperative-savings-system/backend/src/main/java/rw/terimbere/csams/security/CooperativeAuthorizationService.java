package rw.terimbere.csams.security;

import java.util.Arrays;
import java.util.UUID;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import rw.terimbere.csams.shared.exceptions.ForbiddenException;
import rw.terimbere.csams.shared.exceptions.UnauthorizedException;

@Service
public class CooperativeAuthorizationService {

    public static final String SUPER_ADMIN = "SUPER_ADMIN";

    public UserPrincipal currentPrincipal() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null
                || !authentication.isAuthenticated()
                || !(authentication.getPrincipal() instanceof UserPrincipal principal)) {
            throw new UnauthorizedException("Authentication required");
        }
        return principal;
    }

    public void requireMembership(UUID cooperativeId) {
        UserPrincipal principal = currentPrincipal();
        if (principal.hasRole(SUPER_ADMIN)) {
            return;
        }
        if (cooperativeId == null || !principal.isMemberOf(cooperativeId)) {
            throw new ForbiddenException("Not a member of this cooperative");
        }
    }

    /** Membership plus officer access to member-identifying dashboard insights. */
    public void requireMemberInsightsAccess(UUID cooperativeId) {
        requireMembership(cooperativeId);
        CooperativeOfficerRoles.requireMemberInsightsAccess(currentPrincipal());
    }

    public void requireSuperAdmin() {
        if (!currentPrincipal().hasRole(SUPER_ADMIN)) {
            throw new ForbiddenException("Super admin access is required");
        }
    }

    public boolean hasRole(String... roleCodes) {
        UserPrincipal principal = currentPrincipal();
        return Arrays.stream(roleCodes).anyMatch(principal::hasRole);
    }

    public boolean hasAuthority(String... authorities) {
        UserPrincipal principal = currentPrincipal();
        return Arrays.stream(authorities).anyMatch(principal::hasAuthority);
    }
}
