package fu.dlms.member.repository;

import fu.dlms.member.dto.MemberRequest;
import fu.dlms.member.dto.MemberResponse;
import fu.dlms.member.entity.Member;
import fu.dlms.member.service.impl.CardNumberGenerator;
import fu.dlms.member.service.impl.MemberExcelParser;
import fu.dlms.member.service.impl.MemberServiceImpl;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.validation.ValidationAutoConfiguration;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.transaction.IllegalTransactionStateException;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Runs the parts the mock-based tests cannot prove against a real PostgreSQL: the native card-number queries,
 * the advisory lock under concurrency, and the database-level unique constraints. Skipped when Docker is absent.
 * Not transactional on purpose: each service call must commit on its own, exactly as in production.
 */
@Testcontainers(disabledWithoutDocker = true)
@DataJpaTest(properties = "spring.jpa.hibernate.ddl-auto=create")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import({MemberServiceImpl.class, CardNumberGenerator.class, MemberExcelParser.class, ValidationAutoConfiguration.class})
@Transactional(propagation = Propagation.NOT_SUPPORTED)
class MemberPostgresTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");

    @Autowired
    MemberRepository repository;
    @Autowired
    MemberServiceImpl service;
    @Autowired
    CardNumberGenerator generator;

    @AfterEach
    void clean() {
        repository.deleteAll();
    }

    private static Member member(String card, String email, String phone) {
        Member m = new Member();
        m.setLibraryCardNo(card);
        m.setFullName("Tên " + card);
        m.setEmail(email);
        m.setPhone(phone);
        m.setJoinedAt(LocalDate.of(2025, 1, 1));
        return m;
    }

    @Test
    void highestCardNumberIgnoresCardsThatDoNotLookLikeTvNumbers() {
        repository.saveAll(List.of(
                member("TV-0412", "a@example.com", "0900000001"),
                member("TV-0007", "b@example.com", "0900000002"),
                member("LEGACY-99999", "c@example.com", "0900000003"),
                member("TV-ABC", "d@example.com", "0900000004")));

        assertThat(repository.findHighestCardNumber()).isEqualTo(412L);
    }

    @Test
    void highestCardNumberIsZeroWhenThereAreNoMembers() {
        assertThat(repository.findHighestCardNumber()).isZero();
    }

    @Test
    void concurrentCreatesAllGetDifferentConsecutiveCardNumbers() throws Exception {
        int threads = 8;
        ExecutorService pool = Executors.newFixedThreadPool(threads);
        CountDownLatch go = new CountDownLatch(1);
        List<Future<MemberResponse>> results = new ArrayList<>();
        for (int i = 0; i < threads; i++) {
            int n = i;
            results.add(pool.submit(() -> {
                go.await();
                return service.create(new MemberRequest("Người " + n, "p" + n + "@example.com", "09123450" + n + "0"));
            }));
        }
        go.countDown();

        List<String> cards = new ArrayList<>();
        for (Future<MemberResponse> f : results) {
            cards.add(f.get(30, TimeUnit.SECONDS).libraryCardNo());
        }
        pool.shutdown();

        assertThat(cards).doesNotHaveDuplicates()
                .containsExactlyInAnyOrder("TV-0001", "TV-0002", "TV-0003", "TV-0004",
                        "TV-0005", "TV-0006", "TV-0007", "TV-0008");
    }

    @Test
    void cardNumbersContinueAfterTheHighestExistingOne() {
        repository.save(member("TV-0412", "a@example.com", "0900000001"));

        MemberResponse created = service.create(new MemberRequest("Mới", "new@example.com", "0911111111"));

        assertThat(created.libraryCardNo()).isEqualTo("TV-0413");
    }

    @Test
    void generatorRefusesToRunWithoutATransactionBecauseTheLockWouldBeReleasedAtOnce() {
        assertThatThrownBy(() -> generator.next(1)).isInstanceOf(IllegalTransactionStateException.class);
    }

    @Test
    void databaseRejectsTwoMembersWithTheSamePhone() {
        repository.saveAndFlush(member("TV-0001", "a@example.com", "0900000001"));

        assertThatThrownBy(() -> repository.saveAndFlush(member("TV-0002", "b@example.com", "0900000001")))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void caseInsensitiveEmailLookupFindsTheExistingMember() {
        repository.saveAndFlush(member("TV-0001", "Mixed.Case@Example.com", "0900000001"));

        assertThat(repository.existsByEmailIgnoreCase("mixed.case@example.COM")).isTrue();
        assertThat(repository.existsByEmailIgnoreCase("other@example.com")).isFalse();
    }
}
