package fu.dlms.member.keycloak;

/** Keycloak refused or could not be reached while creating or changing an account. */
public class KeycloakException extends RuntimeException {

    public KeycloakException(String message) {
        super(message);
    }

    public KeycloakException(String message, Throwable cause) {
        super(message, cause);
    }
}
