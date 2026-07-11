package com.dabai.easy_lowcode.etl.engine.source;

import com.dabai.easy_lowcode.etl.engine.NodeExecutor;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.batch.item.ItemReader;
import org.springframework.stereotype.Component;

import java.io.*;
import java.util.*;

/**
 * Excel 文件源节点执行器
 */
@Slf4j
@Component
public class ExcelSourceExecutor implements NodeExecutor {

    @Override
    public String getNodeType() { return "excel"; }

    @Override
    public String getCategory() { return "SOURCE"; }

    @Override
    public ItemReader<Map<String, Object>> createReader(Map<String, Object> config) {
        String filePath = (String) config.get("filePath");
        Integer sheetIndex = (Integer) config.getOrDefault("sheetIndex", 0);
        Boolean hasHeader = (Boolean) config.getOrDefault("hasHeader", true);

        log.info("Excel Source: file={}, sheetIndex={}", filePath, sheetIndex);

        return new ExcelReaderWrapper(filePath, sheetIndex, hasHeader);
    }

    private static class ExcelReaderWrapper implements ItemReader<Map<String, Object>> {
        private final Workbook workbook;
        private final Sheet sheet;
        private final boolean hasHeader;
        private String[] headers;
        private int currentRow;
        private final int lastRow;

        ExcelReaderWrapper(String filePath, int sheetIndex, boolean hasHeader) {
            try {
                this.workbook = WorkbookFactory.create(new FileInputStream(filePath));
                this.sheet = workbook.getSheetAt(sheetIndex);
                this.hasHeader = hasHeader;
                this.lastRow = sheet.getLastRowNum();
                this.currentRow = 0;

                if (hasHeader && sheet.getRow(0) != null) {
                    Row headerRow = sheet.getRow(0);
                    headers = new String[headerRow.getLastCellNum()];
                    for (int i = 0; i < headers.length; i++) {
                        Cell cell = headerRow.getCell(i);
                        headers[i] = cell != null ? cell.toString() : "col_" + (i + 1);
                    }
                    currentRow = 1;
                }
            } catch (Exception e) {
                throw new RuntimeException("Excel 文件打开失败: " + e.getMessage(), e);
            }
        }

        @Override
        public Map<String, Object> read() {
            if (currentRow > lastRow) return null;

            Row row = sheet.getRow(currentRow++);
            if (row == null) return read(); // 跳过空行

            Map<String, Object> data = new LinkedHashMap<>();
            for (int i = 0; i < row.getLastCellNum(); i++) {
                Cell cell = row.getCell(i);
                String key = headers != null && i < headers.length ? headers[i] : "col_" + (i + 1);
                data.put(key, cell != null ? getCellValue(cell) : null);
            }
            return data;
        }

        private Object getCellValue(Cell cell) {
            return switch (cell.getCellType()) {
                case STRING -> cell.getStringCellValue();
                case NUMERIC -> DateUtil.isCellDateFormatted(cell) ? cell.getDateCellValue() : cell.getNumericCellValue();
                case BOOLEAN -> cell.getBooleanCellValue();
                case FORMULA -> cell.getCellFormula();
                default -> null;
            };
        }
    }

    @Override
    public String getConfigSchema() {
        return """
                {
                  "filePath": { "type": "string", "label": "文件路径" },
                  "sheetIndex": { "type": "number", "label": "Sheet索引", "default": 0 },
                  "hasHeader": { "type": "boolean", "label": "首行为表头", "default": true }
                }""";
    }
}
