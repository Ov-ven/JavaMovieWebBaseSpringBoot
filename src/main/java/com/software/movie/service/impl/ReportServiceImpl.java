package com.software.movie.service.impl;

import com.software.movie.entity.Movie;
import com.software.movie.service.MovieService;
import com.software.movie.service.ReportService;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;

/**
 * 报表服务实现类。
 * 使用 Apache POI 生成 Excel 格式的数据报表，支持电影播放量榜单导出。
 */
@Service
public class ReportServiceImpl implements ReportService {

    @Autowired
    private MovieService movieService;

    /**
     * 生成电影播放量榜单的 Excel 报表（.xlsx 格式）。
     * 获取播放量最高的 20 部电影，生成包含排名、名称、类型、地区、评分、播放量、上映日期的报表。
     *
     * @return 包含 Excel 文件内容的字节数组输出流
     * @throws IOException 如果在生成过程中发生I/O错误
     */
    @Override
    public ByteArrayOutputStream generateMovieRankReport() throws IOException {
        // 1. 创建一个新的 Excel 工作簿 (.xlsx 格式)
        Workbook workbook = new XSSFWorkbook();
        try {
            // 2. 创建一个名为 "电影播放榜单" 的工作表
            Sheet sheet = workbook.createSheet("电影播放榜单");

            // 3. 定义表头样式
            Font headerFont = workbook.createFont();
            headerFont.setBold(true); // 粗体
            headerFont.setColor(IndexedColors.WHITE.getIndex()); // 白色字体

            CellStyle headerCellStyle = workbook.createCellStyle();
            headerCellStyle.setFont(headerFont);
            headerCellStyle.setFillForegroundColor(IndexedColors.DARK_BLUE.getIndex()); // 深蓝色背景
            headerCellStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            headerCellStyle.setAlignment(HorizontalAlignment.CENTER); // 居中

            // 4. 创建表头
            String[] headers = {"排名", "电影名称", "类型", "地区", "评分", "播放量", "上映日期"};
            Row headerRow = sheet.createRow(0); // 第一行 (索引为0) 作为表头

            for (int i = 0; i < headers.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(headers[i]);
                cell.setCellStyle(headerCellStyle); // 应用表头样式
            }

            // 5. 获取电影数据
            List<Movie> movies = movieService.getHotMovies(20);

            // 6. 填充数据行
            int rowNum = 1;
            for (int i = 0; i < movies.size(); i++) {
                Movie movie = movies.get(i);
                Row row = sheet.createRow(rowNum++);

                row.createCell(0).setCellValue(i + 1); // 排名
                row.createCell(1).setCellValue(movie.getTitle()); // 电影名称
                row.createCell(2).setCellValue(movie.getType()); // 类型
                row.createCell(3).setCellValue(movie.getRegion()); // 地区
                row.createCell(4).setCellValue(movie.getScore()); // 评分
                row.createCell(5).setCellValue(movie.getViews()); // 播放量

                // 上映日期可能为LocalDate，需要特殊处理
                Cell dateCell = row.createCell(6);
                if (movie.getReleaseDate() != null) {
                    CellStyle dateCellStyle = workbook.createCellStyle();
                    CreationHelper createHelper = workbook.getCreationHelper();
                    dateCellStyle.setDataFormat(createHelper.createDataFormat().getFormat("yyyy-mm-dd"));

                    dateCell.setCellValue(movie.getReleaseDate());
                    dateCell.setCellStyle(dateCellStyle);
                } else {
                    dateCell.setCellValue("");
                }
            }

            // 7. 自动调整列宽以适应内容
            for (int i = 0; i < headers.length; i++) {
                sheet.autoSizeColumn(i);
            }

            // 8. 将工作簿写入 ByteArrayOutputStream
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            workbook.write(outputStream);
            return outputStream;
        } finally {
            workbook.close();
        }
    }
}
