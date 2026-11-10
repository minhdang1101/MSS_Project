package fu.dlms.member.service;

import fu.dlms.member.dto.MemberResponse;
import fu.dlms.member.dto.RegisterRequest;

public interface AuthService {

    /**
     * A reader signs up: the card number is issued automatically and the login account is created in Keycloak with
     * the password they chose. They sign in afterwards on Keycloak's login page.
     *
     * @throws fu.dlms.member.exception.DuplicateValueException when the e-mail is already used
     * @throws fu.dlms.member.keycloak.KeycloakException when Keycloak cannot create the account (503)
     */
    MemberResponse register(RegisterRequest request);
}
