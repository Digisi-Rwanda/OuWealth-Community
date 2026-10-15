package rw.terimbere.csams.modules.historicalimport.service;

import java.io.ByteArrayInputStream;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.util.StringUtils;
import rw.terimbere.csams.modules.historicalimport.dto.HistoricalImportError;
import rw.terimbere.csams.modules.historicalimport.entity.HistoricalImportSheet;
import rw.terimbere.csams.modules.historicalimport.entity.HistoricalMigrationSheet;
import rw.terimbere.csams.shared.exceptions.ValidationException;

/** Reads the exact three-sheet migration workbook. Legacy (16-sheet) workbooks are rejected. */
final class MigrationWorkbookParser {

    static final String UNSUPPORTED_FORMAT =
            "This historical import format is no longer supported. Download the latest migration template.";

    MigrationWorkbook parse(byte[] bytes) {
        if (bytes == null || bytes.length == 0) {
            throw new ValidationException("Excel file is required");
        }
        try (Workbook workbook = new XSSFWorkbook(new ByteArrayInputStream(bytes))) {
            if (workbook.getNumberOfSheets() == 0) {
                throw new ValidationException("Workbook has no sheets");
            }
            Set<HistoricalMigrationSheet> found = EnumSet.noneOf(HistoricalMigrationSheet.class);
            boolean legacy = false;
            List<String> unknown = new ArrayList<>();
            for (int s = 0; s < workbook.getNumberOfSheets(); s++) {
                String name = workbook.getSheetAt(s).getSheetName();
                var migration = HistoricalMigrationSheet.fromSheetName(name);
                if (migration.isPresent()) {
                    found.add(migration.get());
                } else if (HistoricalImportSheet.isKnownSheetName(name)) {
                    legacy = true;
                } else {
                    unknown.add(name);
                }
            }
            if (legacy) {
                throw new ValidationException(UNSUPPORTED_FORMAT);
            }

            MigrationWorkbook parsed = new MigrationWorkbook();
            WorkbookCellReader reader = new WorkbookCellReader();
            for (String name : unknown) {
                parsed.workbookErrors()
                        .add(error(
                                name,
                                null,
                                "Sheet",
                                "UNKNOWN_SHEET",
                                "Unknown sheet \"" + name
                                        + "\". The workbook must contain only the three template sheets."));
            }
            for (HistoricalMigrationSheet type : HistoricalMigrationSheet.values()) {
                if (!found.contains(type)) {
                    parsed.workbookErrors()
                            .add(error(
                                    type.getSheetName(),
                                    null,
                                    "Sheet",
                                    "MISSING_SHEET",
                                    "Sheet \"" + type.getSheetName()
                                            + "\" is missing. Download the latest migration template."));
                }
            }
            for (int s = 0; s < workbook.getNumberOfSheets(); s++) {
                Sheet sheet = workbook.getSheetAt(s);
                HistoricalMigrationSheet.fromSheetName(sheet.getSheetName())
                        .ifPresent(type -> parseSheet(sheet, type, reader, parsed));
            }
            return parsed;
        } catch (ValidationException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new ValidationException("Invalid Excel file: " + ex.getMessage());
        }
    }

    private void parseSheet(
            Sheet sheet, HistoricalMigrationSheet type, WorkbookCellReader reader, MigrationWorkbook parsed) {
        Row headerRow = sheet.getRow(sheet.getFirstRowNum());
        if (headerRow == null) {
            parsed.workbookErrors()
                    .add(error(type.getSheetName(), 1, "Header", "MISSING_HEADER", "Sheet is missing the header row."));
            return;
        }
        List<String> expected = type.getHeaders();
        for (int c = 0; c < expected.size(); c++) {
            String actual = reader.stringValue(headerRow.getCell(c));
            if (!HistoricalMigrationSheet.normalizeHeader(expected.get(c))
                    .equals(HistoricalMigrationSheet.normalizeHeader(actual))) {
                parsed.workbookErrors()
                        .add(error(
                                type.getSheetName(),
                                1,
                                "Header",
                                "BAD_HEADER",
                                "Headers must be exactly: " + String.join(", ", expected)
                                        + ". Download the latest migration template."));
                return;
            }
        }
        List<MigrationWorkbook.Row> rows = new ArrayList<>();
        for (int i = sheet.getFirstRowNum() + 1; i <= sheet.getLastRowNum(); i++) {
            Row row = sheet.getRow(i);
            if (row == null || isBlank(row, expected.size(), reader)) {
                continue;
            }
            Map<String, String> cells = new LinkedHashMap<>();
            for (int c = 0; c < expected.size(); c++) {
                String header = expected.get(c);
                String value = header.toLowerCase(Locale.ROOT).contains("date")
                        ? reader.dateString(row.getCell(c))
                        : reader.stringValue(row.getCell(c));
                cells.put(HistoricalMigrationSheet.normalizeHeader(header), value);
            }
            rows.add(new MigrationWorkbook.Row(type, i + 1, cells));
        }
        parsed.sheets().put(type, rows);
    }

    private static boolean isBlank(Row row, int columns, WorkbookCellReader reader) {
        for (int c = 0; c < columns; c++) {
            if (StringUtils.hasText(reader.stringValue(row.getCell(c)))) {
                return false;
            }
        }
        return true;
    }

    private static HistoricalImportError error(String sheet, Integer row, String field, String code, String message) {
        return HistoricalImportError.builder()
                .sheet(sheet)
                .rowNumber(row)
                .field(field)
                .code(code)
                .message(message)
                .build();
    }
}
