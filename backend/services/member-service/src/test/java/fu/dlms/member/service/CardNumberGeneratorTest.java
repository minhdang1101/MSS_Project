package fu.dlms.member.service;

import fu.dlms.member.repository.MemberRepository;
import fu.dlms.member.service.impl.CardNumberGenerator;
import org.junit.jupiter.api.Test;
import org.mockito.InOrder;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class CardNumberGeneratorTest {

    private final MemberRepository repository = mock(MemberRepository.class);
    private final CardNumberGenerator generator = new CardNumberGenerator(repository);

    @Test
    void continuesFromTheHighestExistingNumberWithFourDigitPadding() {
        when(repository.findHighestCardNumber()).thenReturn(412L);

        assertThat(generator.next(3)).containsExactly("TV-0413", "TV-0414", "TV-0415");
    }

    @Test
    void startsAtOneWhenThereAreNoMembersYet() {
        when(repository.findHighestCardNumber()).thenReturn(0L);

        assertThat(generator.next(1)).containsExactly("TV-0001");
    }

    @Test
    void numbersBeyondFourDigitsAreNotTruncated() {
        when(repository.findHighestCardNumber()).thenReturn(9999L);

        assertThat(generator.next(2)).containsExactly("TV-10000", "TV-10001");
    }

    @Test
    void takesTheNumberingLockBeforeReadingTheHighestNumberSoConcurrentCreatesCannotClash() {
        when(repository.findHighestCardNumber()).thenReturn(5L);

        generator.next(1);

        InOrder order = inOrder(repository);
        order.verify(repository).lockCardNumbering();
        order.verify(repository).findHighestCardNumber();
    }

    @Test
    void asksForNothingAndDoesNotTouchTheDatabaseWhenCountIsZero() {
        assertThat(generator.next(0)).isEqualTo(List.of());
        org.mockito.Mockito.verifyNoInteractions(repository);
    }
}
