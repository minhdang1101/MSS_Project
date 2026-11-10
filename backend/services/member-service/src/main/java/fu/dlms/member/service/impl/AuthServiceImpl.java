package fu.dlms.member.service.impl;

import fu.dlms.member.dto.MemberRequest;
import fu.dlms.member.dto.MemberResponse;
import fu.dlms.member.dto.RegisterRequest;
import fu.dlms.member.service.AuthService;
import fu.dlms.member.service.MemberService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * Sign-in, forgot-password and tokens belong to Keycloak. What is left here is signing up: it issues the card number
 * and creates the account in Keycloak with the password the reader chose.
 */
@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final MemberService memberService;

    @Override
    public MemberResponse register(RegisterRequest request) {
        // MemberRequest cleans the fields the same way the librarian's form does; a self-registered reader has no phone yet
        return memberService.register(new MemberRequest(request.fullName(), request.email(), null), request.password());
    }
}
