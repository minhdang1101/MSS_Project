package fu.dlms.member.repository;

import fu.dlms.member.entity.Member;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MemberRepository extends JpaRepository<Member, Long> {

    boolean existsByEmail(String email);

    boolean existsByLibraryCardNo(String libraryCardNo);
}
