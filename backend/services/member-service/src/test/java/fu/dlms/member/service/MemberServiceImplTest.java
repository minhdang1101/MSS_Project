package fu.dlms.member.service;

import fu.dlms.member.dto.MemberImportResult;
import fu.dlms.member.dto.MemberImportRow;
import fu.dlms.member.dto.MemberRequest;
import fu.dlms.member.dto.MemberResponse;
import fu.dlms.member.entity.Member;
import fu.dlms.member.entity.MemberStatus;
import fu.dlms.member.exception.DuplicateValueException;
import fu.dlms.member.exception.ResourceNotFoundException;
import fu.dlms.member.repository.MemberRepository;
import fu.dlms.member.service.impl.CardNumberGenerator;
import fu.dlms.member.service.impl.MemberExcelParser;
import fu.dlms.member.service.impl.MemberServiceImpl;
import jakarta.validation.Validation;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.io.ByteArrayInputStream;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class MemberServiceImplTest {

    private final MemberRepository repository = mock(MemberRepository.class);
    private final MemberExcelParser parser = mock(MemberExcelParser.class);
    private final CardNumberGenerator cardNumbers = mock(CardNumberGenerator.class);
    private MemberServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new MemberServiceImpl(repository, parser, cardNumbers,
                Validation.buildDefaultValidatorFactory().getValidator());
        when(repository.save(any(Member.class))).thenAnswer(inv -> inv.getArgument(0));
    }

    private static Member member(long id, String card, String email) {
        Member m = new Member();
        m.setId(id);
        m.setLibraryCardNo(card);
        m.setFullName("Tên " + id);
        m.setEmail(email);
        m.setJoinedAt(LocalDate.of(2025, 1, 1));
        return m;
    }

    // ---------- create ----------

    @Test
    void createIssuesTheNextCardNumberAndStartsActiveToday() {
        when(cardNumbers.next(1)).thenReturn(List.of("TV-0413"));

        MemberResponse response = service.create(new MemberRequest("Nguyễn A", "a@example.com", "0900000001"));

        assertThat(response.libraryCardNo()).isEqualTo("TV-0413");
        assertThat(response.status()).isEqualTo(MemberStatus.ACTIVE);
        assertThat(response.joinedAt()).isEqualTo(LocalDate.now());
    }

    @Test
    void createRejectsAnEmailAlreadyRegisteredIgnoringCaseAndPointsAtTheEmailField() {
        when(repository.existsByEmailIgnoreCase("A@Example.com")).thenReturn(true);

        assertThatThrownBy(() -> service.create(new MemberRequest("Nguyễn A", "A@Example.com", "0900")))
                .isInstanceOfSatisfying(DuplicateValueException.class, e -> {
                    assertThat(e.getField()).isEqualTo("email");
                    assertThat(e.getMessage()).isEqualTo("Email này đã được dùng cho thành viên khác");
                });
        verify(cardNumbers, never()).next(anyInt());
    }

    @Test
    void createRejectsAPhoneAlreadyRegisteredAndPointsAtThePhoneField() {
        when(repository.existsByPhone("0900")).thenReturn(true);

        assertThatThrownBy(() -> service.create(new MemberRequest("Nguyễn A", "a@example.com", "0900")))
                .isInstanceOfSatisfying(DuplicateValueException.class, e -> {
                    assertThat(e.getField()).isEqualTo("phone");
                    assertThat(e.getMessage()).isEqualTo("Số điện thoại này đã được dùng cho thành viên khác");
                });
        verify(cardNumbers, never()).next(anyInt());
    }

    // ---------- update ----------

    @Test
    void updateRejectsEmailAlreadyUsedByAnotherMember() {
        when(repository.findById(1L)).thenReturn(Optional.of(member(1, "TV-1", "one@example.com")));
        when(repository.existsByEmailIgnoreCaseAndIdNot("two@example.com", 1L)).thenReturn(true);

        assertThatThrownBy(() -> service.update(1L, new MemberRequest("Tên mới", "two@example.com", "0900")))
                .isInstanceOfSatisfying(DuplicateValueException.class, e -> assertThat(e.getField()).isEqualTo("email"));
    }

    @Test
    void updateRejectsPhoneAlreadyUsedByAnotherMember() {
        when(repository.findById(1L)).thenReturn(Optional.of(member(1, "TV-1", "one@example.com")));
        when(repository.existsByPhoneAndIdNot("0911", 1L)).thenReturn(true);

        assertThatThrownBy(() -> service.update(1L, new MemberRequest("Tên mới", "one@example.com", "0911")))
                .isInstanceOfSatisfying(DuplicateValueException.class, e -> assertThat(e.getField()).isEqualTo("phone"));
    }

    @Test
    void updateChangesNameEmailAndPhoneButNeverTheCardNumber() {
        Member existing = member(1, "TV-1", "one@example.com");
        when(repository.findById(1L)).thenReturn(Optional.of(existing));

        MemberResponse response = service.update(1L, new MemberRequest("Tên mới", "new@example.com", "0900"));

        assertThat(response.fullName()).isEqualTo("Tên mới");
        assertThat(response.email()).isEqualTo("new@example.com");
        assertThat(response.phone()).isEqualTo("0900");
        assertThat(response.libraryCardNo()).isEqualTo("TV-1");
    }

    // ---------- status ----------

    @Test
    void changeStatusPersistsTheNewStatus() {
        when(repository.findById(1L)).thenReturn(Optional.of(member(1, "TV-1", "one@example.com")));

        assertThat(service.changeStatus(1L, MemberStatus.SUSPENDED).status()).isEqualTo(MemberStatus.SUSPENDED);
    }

    @Test
    void changeStatusOfUnknownMemberIsNotFound() {
        when(repository.findById(9L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.changeStatus(9L, MemberStatus.LOCKED))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    // ---------- import ----------

    @Test
    @SuppressWarnings("unchecked")
    void importSavesValidRowsWithConsecutiveCardNumbersAndReportsEachInvalidRowByExcelRow() {
        when(parser.parse(any())).thenReturn(List.of(
                new MemberImportRow(2, "Hợp lệ một", "ok1@example.com", "0900000001"),
                new MemberImportRow(3, "Email hỏng", "not-an-email", "0900000003"),
                new MemberImportRow(4, "Trùng email DB", "taken@example.com", "0900000004"),
                new MemberImportRow(5, "Thiếu email", null, "0900000005"),
                new MemberImportRow(6, "Trùng email trong file", "OK1@example.com", "0900000006"),
                new MemberImportRow(7, "Thiếu điện thoại", "nophone@example.com", null),
                new MemberImportRow(8, "x".repeat(101), "long@example.com", "0900000008"),
                new MemberImportRow(9, "Trùng điện thoại DB", "p1@example.com", "0911111111"),
                new MemberImportRow(10, "Trùng điện thoại trong file", "p2@example.com", "0900000001"),
                new MemberImportRow(11, "Hợp lệ hai", "ok2@example.com", "0900000011")));
        when(repository.existsByEmailIgnoreCase("taken@example.com")).thenReturn(true);
        when(repository.existsByPhone("0911111111")).thenReturn(true);
        when(cardNumbers.next(2)).thenReturn(List.of("TV-0413", "TV-0414"));

        MemberImportResult result = service.importFromExcel(new ByteArrayInputStream(new byte[0]));

        ArgumentCaptor<List<Member>> saved = ArgumentCaptor.forClass(List.class);
        verify(repository).saveAll(saved.capture());
        assertThat(saved.getValue()).extracting(Member::getLibraryCardNo).containsExactly("TV-0413", "TV-0414");
        assertThat(saved.getValue()).extracting(Member::getFullName).containsExactly("Hợp lệ một", "Hợp lệ hai");
        assertThat(saved.getValue()).allSatisfy(m -> {
            assertThat(m.getStatus()).isEqualTo(MemberStatus.ACTIVE);
            assertThat(m.getJoinedAt()).isEqualTo(LocalDate.now());
        });

        assertThat(result.imported()).isEqualTo(2);
        assertThat(result.errors()).extracting(MemberImportResult.RowError::row).containsExactly(3, 4, 5, 6, 7, 8, 9, 10);
        assertThat(result.errors().get(0).message()).contains("Email");
        assertThat(result.errors().get(1).message()).contains("taken@example.com").contains("đã có trong hệ thống");
        assertThat(result.errors().get(2).message()).isEqualTo("Email: Không được để trống");
        assertThat(result.errors().get(3).message()).contains("OK1@example.com").contains("bị lặp trong file");
        assertThat(result.errors().get(4).message()).isEqualTo("Điện thoại: Không được để trống");
        assertThat(result.errors().get(5).message()).isEqualTo("Họ tên: Tối đa 100 ký tự");
        assertThat(result.errors().get(6).message()).contains("0911111111").contains("đã có trong hệ thống");
        assertThat(result.errors().get(7).message()).contains("0900000001").contains("bị lặp trong file");
    }

    @Test
    void importRejectsAPhoneTypedAsANumberBecauseTheLeadingZeroMayBeLost() {
        when(parser.parse(any())).thenReturn(List.of(
                new MemberImportRow(2, "Số hoá", "num@example.com", "912345678", true)));

        MemberImportResult result = service.importFromExcel(new ByteArrayInputStream(new byte[0]));

        assertThat(result.imported()).isZero();
        assertThat(result.errors()).singleElement().satisfies(e -> {
            assertThat(e.row()).isEqualTo(2);
            assertThat(e.message()).startsWith("Điện thoại:").contains("Text");
        });
        verify(cardNumbers, never()).next(anyInt());
    }

    @Test
    void importNormalisesPhoneAndTrimsBeforeCheckingDuplicates() {
        when(parser.parse(any())).thenReturn(List.of(
                new MemberImportRow(2, "  Một ", " one@example.com ", "0912 345 678"),
                new MemberImportRow(3, "Hai", "two@example.com", "0912.345.678")));
        when(cardNumbers.next(1)).thenReturn(List.of("TV-0413"));

        MemberImportResult result = service.importFromExcel(new ByteArrayInputStream(new byte[0]));

        assertThat(result.imported()).isEqualTo(1);
        assertThat(result.errors()).singleElement().satisfies(e -> {
            assertThat(e.row()).isEqualTo(3);
            assertThat(e.message()).contains("0912345678").contains("bị lặp trong file");
        });
    }

    @Test
    void importOfAFileWithNoDataRowsSavesNothingAndConsumesNoCardNumbers() {
        when(parser.parse(any())).thenReturn(List.of());

        MemberImportResult result = service.importFromExcel(new ByteArrayInputStream(new byte[0]));

        assertThat(result.imported()).isZero();
        assertThat(result.errors()).isEmpty();
        verify(repository, never()).saveAll(anyList());
        verify(cardNumbers, never()).next(anyInt());
    }

    @Test
    void importWhereEveryRowIsInvalidConsumesNoCardNumbers() {
        when(parser.parse(any())).thenReturn(List.of(new MemberImportRow(2, "Tên", "bad", "0900")));

        MemberImportResult result = service.importFromExcel(new ByteArrayInputStream(new byte[0]));

        assertThat(result.imported()).isZero();
        assertThat(result.errors()).hasSize(1);
        verify(cardNumbers, never()).next(anyInt());
    }
}
