package fu.dlms.member.service.impl;

import fu.dlms.member.repository.MemberRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.LongStream;

/** Issues library card numbers: TV- plus a zero-padded sequence, continuing from the highest one in use. */
@Component
@RequiredArgsConstructor
public class CardNumberGenerator {

    private static final String PREFIX = "TV-";

    private final MemberRepository memberRepository;

    /**
     * Returns the next {@code count} consecutive card numbers. Call it inside the transaction that saves the
     * members (MANDATORY: it refuses to run without one, because outside a transaction the lock would be released
     * immediately). The lock is held until that transaction ends, so concurrent callers queue up.
     */
    @Transactional(propagation = Propagation.MANDATORY)
    public List<String> next(int count) {
        if (count <= 0) {
            return List.of();
        }
        memberRepository.lockCardNumbering();
        long highest = memberRepository.findHighestCardNumber();
        return LongStream.rangeClosed(highest + 1, highest + count)
                .mapToObj(n -> String.format("%s%04d", PREFIX, n))
                .toList();
    }
}
