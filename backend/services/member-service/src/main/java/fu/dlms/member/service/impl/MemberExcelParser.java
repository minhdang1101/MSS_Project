package fu.dlms.member.service.impl;

import fu.dlms.member.dto.MemberImportRow;
import fu.dlms.member.exception.InvalidFileException;
import org.apache.poi.openxml4j.util.ZipSecureFile;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Reads the first sheet of an .xlsx file (OOXML only: a legacy .xls renamed to .xlsx is rejected).
 * Row 1 must be the header Họ tên | Email | Điện thoại; columns are fixed: A = full name, B = email, C = phone
 * (the card number is issued automatically). Completely blank rows are skipped.
 */
@Component
public class MemberExcelParser {

    static final int MAX_DATA_ROWS = 1000;
    private static final String[] HEADERS = {"Họ tên", "Email", "Điện thoại"};
    private static final long MAX_UNCOMPRESSED_ENTRY_BYTES = 20L * 1024 * 1024;

    static {
        // The 2 MB upload cap limits the compressed size only: refuse any part that would inflate beyond 20 MB.
        ZipSecureFile.setMaxEntrySize(MAX_UNCOMPRESSED_ENTRY_BYTES);
    }

    public List<MemberImportRow> parse(InputStream in) {
        // DataFormatter is not thread-safe and this bean is a singleton, so each call gets its own.
        DataFormatter formatter = new DataFormatter();
        try (XSSFWorkbook workbook = new XSSFWorkbook(in)) {
            Sheet sheet = workbook.getSheetAt(0);
            requireHeader(sheet.getRow(0), formatter);
            List<MemberImportRow> rows = new ArrayList<>();
            for (int index = 1; index <= sheet.getLastRowNum(); index++) {
                Row row = sheet.getRow(index);
                if (row == null) continue;
                MemberImportRow parsed = new MemberImportRow(index + 1, text(row, 0, formatter), text(row, 1, formatter),
                        text(row, 2, formatter), isNumeric(row.getCell(2)));
                if (isBlank(parsed)) continue;
                rows.add(parsed);
                if (rows.size() > MAX_DATA_ROWS) {
                    throw new InvalidFileException("File có hơn " + MAX_DATA_ROWS + " dòng. Chia nhỏ file rồi nhập lại.");
                }
            }
            return rows;
        } catch (InvalidFileException e) {
            throw e;
        } catch (IOException | RuntimeException e) {
            throw new InvalidFileException("Không đọc được file. Hãy dùng file Excel .xlsx.");
        }
    }

    /** Without this a file lacking a header would silently lose its first data row. */
    private void requireHeader(Row header, DataFormatter formatter) {
        for (int column = 0; column < HEADERS.length; column++) {
            String actual = header == null ? null : text(header, column, formatter);
            if (actual == null || !actual.toLowerCase(Locale.ROOT).equals(HEADERS[column].toLowerCase(Locale.ROOT))) {
                throw new InvalidFileException(
                        "Dòng 1 phải là tiêu đề: Họ tên, Email, Điện thoại. Hãy dùng file mẫu.");
            }
        }
    }

    /** Displayed text, trimmed; null when empty. DataFormatter keeps numbers as the user sees them. */
    private String text(Row row, int column, DataFormatter formatter) {
        Cell cell = row.getCell(column);
        if (cell == null) return null;
        String value = formatter.formatCellValue(cell).trim();
        return value.isEmpty() ? null : value;
    }

    private boolean isNumeric(Cell cell) {
        return cell != null && cell.getCellType() == CellType.NUMERIC;
    }

    private boolean isBlank(MemberImportRow r) {
        return r.fullName() == null && r.email() == null && r.phone() == null;
    }
}
