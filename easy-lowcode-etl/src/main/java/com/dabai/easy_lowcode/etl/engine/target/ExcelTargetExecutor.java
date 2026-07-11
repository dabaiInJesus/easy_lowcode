package com.dabai.easy_lowcode.etl.engine.target;

import com.dabai.easy_lowcode.etl.engine.NodeExecutor;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.batch.item.ItemWriter;
import org.springframework.stereotype.Component;

import java.io.*;
import java.util.*;

/**
 * Excel 文件目标节点执行器
 */
@Slf4j
@Component
public class ExcelTargetExecutor implements NodeExecutor {

    @Override
    public String getNodeType() { return "excel"; }

    @Override
    public String getCategory() { return "TARGET"; }

    @Override
    public ItemWriter<Map<String, Object>> createWriter(Map<String, Object> config) {
        String filePath = (String) config.get("filePath");
        Boolean writeHeader = (Boolean) config.getOrDefault("writeHeader", true);

        log.info("Excel Target: file={}", filePath);

        return chunk -> {
            List<Map<String, Object>> items = new ArrayList<>(chunk.getItems());
            if (items.isEmpty()) return;

            try (Workbook workbook = new XSSFWorkbook()) {
                Sheet sheet = workbook.createSheet("Data");
                int rowIndex = 0;

                if (writeHeader && !items.isEmpty()) {
                    Row headerRow = sheet.createRow(rowIndex++);
                    List<String> headers = new ArrayList<>(items.get(0).keySet());
                    for (int i = 0; i < headers.size(); i++) {
                        headerRow.createCell(i).setCellValue(headers.get(i));
                    }
                }

                List<String> columns = items.isEmpty() ? List.of() : new ArrayList<>(items.get(0).keySet());
                for (Map<String, Object> row : items) {
                    Row dataRow = sheet.createRow(rowIndex++);
                    for (int i = 0; i < columns.size(); i++) {
                        Cell cell = dataRow.createCell(i);
                        Object value = row.get(columns.get(i));
                        if (value == null) {
                            cell.setBlank();
                        } else if (value instanceof Number) {
                            cell.setCellValue(((Number) value).doubleValue());
                        } else if (value instanceof Boolean) {
                            cell.setCellValue((Boolean) value);
                        } else {
                            cell.setCellValue(value.toString());
                        }
                    }
                }

                for (int i = 0; i < columns.size(); i++) {
                    sheet.autoSizeColumn(i);
                }

                try (FileOutputStream fos = new FileOutputStream(filePath)) {
                    workbook.write(fos);
                }
                log.info("Excel Target 写入 {} 条记录到 {}", items.size(), filePath);
            }
        };
    }

    @Override
    public String getConfigSchema() {
        return """
                {
                  "filePath": { "type": "string", "label": "输出文件路径" },
                  "writeHeader": { "type": "boolean", "label": "写入表头", "default": true }
                }""";
    }
}
