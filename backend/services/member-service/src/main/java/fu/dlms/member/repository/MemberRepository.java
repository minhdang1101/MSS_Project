package fu.dlms.member.repository;

import fu.dlms.member.entity.Member;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface MemberRepository extends JpaRepository<Member, Long> {

    boolean existsByEmailIgnoreCase(String email);

    boolean existsByEmailIgnoreCaseAndIdNot(String email, Long id);

    boolean existsByPhone(String phone);

    boolean existsByPhoneAndIdNot(String phone, Long id);

    /**
     * Takes a transaction-scoped lock so two concurrent requests cannot both read the same highest card number.
     * Must run in its own statement, before {@link #findHighestCardNumber()}, so that one sees the other's commit.
     */
    @Query(value = "select cast(pg_advisory_xact_lock(7001) as text)", nativeQuery = true)
    String lockCardNumbering();

    /** Highest numeric part among cards shaped like TV-0412; 0 when there are none. */
    @Query(value = "select coalesce(max(cast(substring(library_card_no from 4) as bigint)), 0) "
            + "from member where library_card_no ~ '^TV-[0-9]{1,15}$'", nativeQuery = true)
    long findHighestCardNumber();
}
