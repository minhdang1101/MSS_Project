package fu.dlms.member.service;

import fu.dlms.member.dto.MemberImportRow;
import fu.dlms.member.exception.InvalidFileException;
import fu.dlms.member.service.impl.MemberExcelParser;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class MemberExcelParserTest {

    private static final String[] HEADER = {"Họ tên", "Email", "Điện thoại"};

    private final MemberExcelParser parser = new MemberExcelParser();

    private static byte[] workbook(String[][] rows) throws IOException {
        try (XSSFWorkbook wb = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Sheet sheet = wb.createSheet("Thành viên");
            for (int r = 0; r < rows.length; r++) {
                Row row = sheet.createRow(r);
                for (int c = 0; c < rows[r].length; c++) {
                    if (rows[r][c] != null) row.createCell(c).setCellValue(rows[r][c]);
                }
            }
            wb.write(out);
            return out.toByteArray();
        }
    }

    private List<MemberImportRow> parse(byte[] bytes) {
        return parser.parse(new ByteArrayInputStream(bytes));
    }

    @Test
    void parsesDataRowsSkippingHeaderAndBlankLinesKeepingExcelRowNumbers() throws IOException {
        byte[] file = workbook(new String[][]{
                HEADER,
                {"Nguyễn Văn A", "a@example.com", "0900000001"},
                {},
                {"  Trần Thị B ", " b@example.com ", null},
        });

        assertThat(parse(file)).containsExactly(
                new MemberImportRow(2, "Nguyễn Văn A", "a@example.com", "0900000001"),
                new MemberImportRow(4, "Trần Thị B", "b@example.com", null));
    }

    @Test
    void numericPhoneCellIsFlaggedBecauseTheLeadingZeroMayAlreadyBeLost() throws IOException {
        try (XSSFWorkbook wb = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Sheet sheet = wb.createSheet();
            Row header = sheet.createRow(0);
            for (int c = 0; c < HEADER.length; c++) header.createCell(c).setCellValue(HEADER[c]);
            Row row = sheet.createRow(1);
            row.createCell(0).setCellValue("Lê C");
            row.createCell(1).setCellValue("c@example.com");
            row.createCell(2).setCellValue(901000003d);
            Row text = sheet.createRow(2);
            text.createCell(0).setCellValue("Phạm D");
            text.createCell(1).setCellValue("d@example.com");
            text.createCell(2).setCellValue("0901000004");
            wb.write(out);

            List<MemberImportRow> rows = parse(out.toByteArray());

            assertThat(rows.get(0).phone()).isEqualTo("901000003");
            assertThat(rows.get(0).phoneIsNumeric()).isTrue();
            assertThat(rows.get(1).phone()).isEqualTo("0901000004");
            assertThat(rows.get(1).phoneIsNumeric()).isFalse();
        }
    }

    @Test
    void legacyXlsContentRenamedToXlsxIsRejected() throws IOException {
        try (HSSFWorkbook wb = new HSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            wb.createSheet().createRow(0).createCell(0).setCellValue("Họ tên");
            wb.write(out);

            assertThatThrownBy(() -> parse(out.toByteArray())).isInstanceOf(InvalidFileException.class);
        }
    }

    @Test
    void fileWithoutTheExpectedHeaderRowIsRejectedSoNoDataRowIsSilentlyDropped() throws IOException {
        byte[] noHeader = workbook(new String[][]{{"Nguyễn Văn A", "a@example.com", "0900000001"}});

        assertThatThrownBy(() -> parse(noHeader))
                .isInstanceOf(InvalidFileException.class)
                .hasMessageContaining("Họ tên")
                .hasMessageContaining("file mẫu");
    }

    @Test
    void headerIsMatchedIgnoringCaseAndSurroundingSpaces() throws IOException {
        byte[] file = workbook(new String[][]{
                {" HỌ TÊN ", "email", " điện thoại"},
                {"Nguyễn Văn A", "a@example.com", "0900000001"}});

        assertThat(parse(file)).hasSize(1);
    }

    @Test
    void emptyWorkbookIsRejected() throws IOException {
        assertThatThrownBy(() -> parse(workbook(new String[][]{})))
                .isInstanceOf(InvalidFileException.class);
    }

    @Test
    void fileThatIsNotAnXlsxIsRejectedWithAClearMessage() {
        assertThatThrownBy(() -> parse("id,name\n1,a".getBytes()))
                .isInstanceOf(InvalidFileException.class)
                .hasMessageContaining(".xlsx");
    }

    @Test
    void moreThanOneThousandDataRowsIsRejected() throws IOException {
        String[][] rows = new String[1002][];
        rows[0] = HEADER;
        for (int i = 1; i < rows.length; i++) {
            rows[i] = new String[]{"Tên " + i, "u" + i + "@example.com", "0900000000"};
        }

        assertThatThrownBy(() -> parse(workbook(rows)))
                .isInstanceOf(InvalidFileException.class)
                .hasMessageContaining("1000");
    }

    @Test
    void workbookWithOnlyAHeaderYieldsNoRows() throws IOException {
        assertThat(parse(workbook(new String[][]{HEADER}))).isEmpty();
    }
}
