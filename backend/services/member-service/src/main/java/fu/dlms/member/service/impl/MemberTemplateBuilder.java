package fu.dlms.member.service.impl;

import org.apache.poi.ss.usermodel.BorderStyle;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Component;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;

/**
 * Builds the blank Excel file librarians fill in and upload. Sheet 1 is what the importer reads (header only);
 * sheet 2 explains the rules. The phone column is Text-formatted so a leading 0 is not dropped when typing.
 */
@Component
public class MemberTemplateBuilder {

    public static final String FILE_NAME = "mau-nhap-thanh-vien.xlsx";

    private static final String[] HEADERS = {"Họ tên", "Email", "Điện thoại"};
    private static final int[] WIDTHS = {32, 36, 18};
    private static final short TEXT_FORMAT = 0x31; // built-in "@"

    public byte[] build() {
        try (XSSFWorkbook wb = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            fillDataSheet(wb);
            fillHelpSheet(wb);
            wb.write(out);
            return out.toByteArray();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private void fillDataSheet(XSSFWorkbook wb) {
        Sheet sheet = wb.createSheet("Thành viên");

        Font bold = wb.createFont();
        bold.setBold(true);
        CellStyle header = wb.createCellStyle();
        header.setFont(bold);
        header.setFillForegroundColor(IndexedColors.LIGHT_GREEN.getIndex());
        header.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        header.setBorderBottom(BorderStyle.THIN);

        CellStyle text = wb.createCellStyle();
        text.setDataFormat(TEXT_FORMAT);

        Row row = sheet.createRow(0);
        for (int c = 0; c < HEADERS.length; c++) {
            row.createCell(c).setCellValue(HEADERS[c]);
            row.getCell(c).setCellStyle(header);
            sheet.setColumnWidth(c, WIDTHS[c] * 256);
        }
        sheet.setColumnWidth(2, WIDTHS[2] * 256);
        sheet.setDefaultColumnStyle(2, text);
        sheet.createFreezePane(0, 1);
    }

    private void fillHelpSheet(XSSFWorkbook wb) {
        Sheet sheet = wb.createSheet("Hướng dẫn");
        sheet.setColumnWidth(0, 90 * 256);
        String[] lines = {
                "Cách điền file",
                "1. Điền thông tin từ dòng 2 của sheet \"Thành viên\". Dòng 1 là tiêu đề, giữ nguyên.",
                "2. Ba cột đều bắt buộc: Họ tên (tối đa 100 ký tự), Email (tối đa 100 ký tự), Điện thoại (tối đa 20 ký tự).",
                "3. Email và số điện thoại không được trùng với thành viên khác, và không được lặp lại trong file.",
                "4. Số thẻ thư viện không cần điền, hệ thống tự cấp.",
                "5. Tối đa 1000 dòng mỗi file. Lưu file ở định dạng .xlsx.",
                "6. Dòng nào sai sẽ được bỏ qua và báo lại kèm số dòng; các dòng đúng vẫn được nhập.",
                "",
                "Ví dụ một dòng hợp lệ:",
                "Nguyễn Văn An | an.nguyen@example.com | 0912345678",
        };
        for (int i = 0; i < lines.length; i++) {
            sheet.createRow(i).createCell(0).setCellValue(lines[i]);
        }
    }
}
