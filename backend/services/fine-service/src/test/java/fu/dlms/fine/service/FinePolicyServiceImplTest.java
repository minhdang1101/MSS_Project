package fu.dlms.fine.service;

import fu.dlms.fine.dto.req.FinePolicyRequest;
import fu.dlms.fine.dto.res.FinePolicyResponse;
import fu.dlms.fine.entity.FinePolicy;
import fu.dlms.fine.exception.ResourceNotFoundException;
import fu.dlms.fine.repository.FinePolicyRepository;
import fu.dlms.fine.service.impl.FinePolicyServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class FinePolicyServiceImplTest {

    /** 27/10/2026 01:00 in Hà Nội, still 26/10 in UTC. */
    private static final Instant NOW = Instant.parse("2026-10-26T18:00:00Z");
    private static final LocalDate TODAY = LocalDate.of(2026, 10, 27);

    private final FinePolicyRepository policyRepository = mock(FinePolicyRepository.class);
    private FinePolicyServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new FinePolicyServiceImpl(policyRepository, Clock.fixed(NOW, ZoneOffset.UTC));
        when(policyRepository.save(any(FinePolicy.class))).thenAnswer(inv -> inv.getArgument(0));
    }

    @Test
    void currentPolicyIsLookedUpForTodayInTheLibraryTimeZone() {
        FinePolicy policy = FineCalculatorTest.policy(3000, 1, 150000);
        when(policyRepository.findFirstByEffectiveFromLessThanEqualOrderByEffectiveFromDescIdDesc(TODAY))
                .thenReturn(Optional.of(policy));

        assertThat(service.findCurrent().dailyRate()).isEqualByComparingTo("3000");
    }

    @Test
    void noCurrentPolicyIsReportedAsNotFound() {
        when(policyRepository.findFirstByEffectiveFromLessThanEqualOrderByEffectiveFromDescIdDesc(TODAY))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.findCurrent()).isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void updateSavesANewActiveVersionAndSwitchesTheOldOneOff() {
        FinePolicy old = FineCalculatorTest.policy(3000, 1, 150000);
        when(policyRepository.findByActiveTrue()).thenReturn(List.of(old));

        FinePolicyResponse updated = service.update(new FinePolicyRequest(BigDecimal.valueOf(5000), 2, null,
                TODAY.plusDays(5)));

        assertThat(old.isActive()).isFalse();
        assertThat(updated.active()).isTrue();
        assertThat(updated.dailyRate()).isEqualByComparingTo("5000");
        assertThat(updated.maxAmount()).isNull();
        assertThat(updated.effectiveFrom()).isEqualTo(TODAY.plusDays(5));
    }
}
