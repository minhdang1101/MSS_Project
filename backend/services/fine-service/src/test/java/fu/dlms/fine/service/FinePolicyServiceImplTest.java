package fu.dlms.fine.service;

import fu.dlms.fine.dto.ClosedDayRequest;
import fu.dlms.fine.dto.FinePolicyRequest;
import fu.dlms.fine.dto.FinePolicyResponse;
import fu.dlms.fine.entity.FinePolicy;
import fu.dlms.fine.exception.BusinessException;
import fu.dlms.fine.exception.ResourceNotFoundException;
import fu.dlms.fine.repository.ClosedDayRepository;
import fu.dlms.fine.repository.FinePolicyRepository;
import fu.dlms.fine.service.impl.FinePolicyServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class FinePolicyServiceImplTest {

    /** 27/10/2026 01:00 in Hà Nội, still 26/10 in UTC. */
    private static final Instant NOW = Instant.parse("2026-10-26T18:00:00Z");
    private static final LocalDate TODAY = LocalDate.of(2026, 10, 27);

    private final FinePolicyRepository policyRepository = mock(FinePolicyRepository.class);
    private final ClosedDayRepository closedDayRepository = mock(ClosedDayRepository.class);
    private FinePolicyServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new FinePolicyServiceImpl(policyRepository, closedDayRepository, Clock.fixed(NOW, ZoneOffset.UTC));
        when(policyRepository.save(any(FinePolicy.class))).thenAnswer(inv -> inv.getArgument(0));
    }

    @Test
    void currentPolicyIsLookedUpForTodayInTheLibraryTimeZone() {
        FinePolicy policy = FineCalculatorTest.policy(3000, 1, 150000);
        when(policyRepository.findFirstByActiveTrueAndEffectiveFromLessThanEqualOrderByEffectiveFromDescIdDesc(TODAY))
                .thenReturn(Optional.of(policy));

        assertThat(service.findCurrent().dailyRate()).isEqualByComparingTo("3000");
    }

    @Test
    void noCurrentPolicyIsReportedAsNotFound() {
        when(policyRepository.findFirstByActiveTrueAndEffectiveFromLessThanEqualOrderByEffectiveFromDescIdDesc(TODAY))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.findCurrent()).isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void createStoresANewActiveVersion() {
        FinePolicyResponse created = service.create(new FinePolicyRequest(BigDecimal.valueOf(5000), 2, null,
                BigDecimal.valueOf(30000), TODAY.plusDays(5)));

        assertThat(created.active()).isTrue();
        assertThat(created.maxAmount()).isNull();
        assertThat(created.effectiveFrom()).isEqualTo(TODAY.plusDays(5));
    }

    @Test
    void theLastPolicyInEffectCannotBeDeactivated() {
        FinePolicy policy = FineCalculatorTest.policy(3000, 1, 150000);
        when(policyRepository.findById(1L)).thenReturn(Optional.of(policy));
        when(policyRepository.findFirstByActiveTrueAndEffectiveFromLessThanEqualOrderByEffectiveFromDescIdDesc(TODAY))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.deactivate(1L)).isInstanceOf(BusinessException.class);
    }

    @Test
    void aDayAlreadyInTheClosedCalendarIsRejected() {
        when(closedDayRepository.existsById(TODAY)).thenReturn(true);

        assertThatThrownBy(() -> service.addClosedDay(new ClosedDayRequest(TODAY, "Quốc khánh")))
                .isInstanceOf(BusinessException.class)
                .hasMessage("Ngày 27/10/2026 đã có trong lịch nghỉ.");
        verify(closedDayRepository, never()).save(any());
    }
}
