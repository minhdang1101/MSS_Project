package fu.dlms.member.service;

import fu.dlms.member.dto.LoginRequest;
import fu.dlms.member.dto.LoginResponse;

public interface AuthService {

    /** @throws fu.dlms.member.exception.InvalidCredentialsException for any failed login */
    LoginResponse login(LoginRequest request);
}
