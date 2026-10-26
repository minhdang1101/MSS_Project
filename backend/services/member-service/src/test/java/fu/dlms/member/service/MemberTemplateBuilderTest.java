package fu.dlms.member.service;

import fu.dlms.member.service.impl.MemberExcelParser;
import fu.dlms.member.service.impl.MemberTemplateBuilder;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;

class MemberTemplateBuilderTest {

    private final byte[] file = new MemberTemplateBuilder().build();

    private Workbook open() throws IOException {
        return new XSSFWorkbook(new ByteArrayInputStream(file));
    }

    @Test
    void firstSheetHasTheThreeHeadersInTheOrderTheImportExpects() throws IOException {
        try (Workbook wb = open()) {
            Sheet sheet = wb.getSheetAt(0);

            assertThat(sheet.getRow(0).getCell(0).getStringCellValue()).isEqualTo("Họ tên");
            assertThat(sheet.getRow(0).getCell(1).getStringCellValue()).isEqualTo("Email");
            assertThat(sheet.getRow(0).getCell(2).getStringCellValue()).isEqualTo("Điện thoại");
        }
    }

    @Test
    void importingTheUntouchedTemplateYieldsNoMembersSoNothingIsCreatedByAccident() {
        assertThat(new MemberExcelParser().parse(new ByteArrayInputStream(file))).isEmpty();
    }

    @Test
    void phoneColumnIsTextFormattedSoLeadingZerosSurviveTyping() throws IOException {
        try (Workbook wb = open()) {
            Sheet sheet = wb.getSheetAt(0);
            Cell typed = sheet.createRow(1).createCell(2);
            typed.setCellStyle(wb.getCellStyleAt(sheet.getColumnStyle(2).getIndex()));

            assertThat(sheet.getColumnStyle(2).getDataFormatString()).isEqualTo("@");
        }
    }

    @Test
    void secondSheetHoldsInstructionsWithAnExampleRowAndTheLimits() throws IOException {
        try (Workbook wb = open()) {
            assertThat(wb.getNumberOfSheets()).isEqualTo(2);
            Sheet help = wb.getSheetAt(1);
            StringBuilder text = new StringBuilder();
            help.forEach(row -> row.forEach(cell -> text.append(cell.toString()).append('\n')));

            assertThat(text).contains("1000").contains("100").contains("20").contains("không được trùng");
        }
    }
}
