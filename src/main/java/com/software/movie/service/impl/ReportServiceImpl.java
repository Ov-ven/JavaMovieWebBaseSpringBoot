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

@Service
public class ReportServiceImpl implements ReportService {

    @Autowired
    private MovieService movieService; // 注入 MovieService 以获取电影数据

    @Override
    public ByteArrayOutputStream generateMovieRankReport() throws IOException {
        // 1. 创建一个新的 Excel 工作簿 (.xlsx 格式)
        Workbook workbook = new XSSFWorkbook();
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

        // 5. 获取电影数据（这里我们获取Top N电影，可以根据需求调整）
        // 假设我们获取所有电影并按播放量排序，或者直接调用 getHotMovies 获取 Top N
        List<Movie> movies = movieService.getHotMovies(20); // 获取播放量最高的20部电影作为榜单

        // 6. 填充数据行
        int rowNum = 1; // 从第二行开始填充数据 (索引为1)
        for (int i = 0; i < movies.size(); i++) {
            Movie movie = movies.get(i);
            Row row = sheet.createRow(rowNum++); // 创建新行，行号递增

            row.createCell(0).setCellValue(i + 1); // 排名
            row.createCell(1).setCellValue(movie.getTitle()); // 电影名称
            row.createCell(2).setCellValue(movie.getType()); // 类型
            row.createCell(3).setCellValue(movie.getRegion()); // 地区
            row.createCell(4).setCellValue(movie.getScore()); // 评分
            row.createCell(5).setCellValue(movie.getViews()); // 播放量

            // 上映日期可能为LocalDate，需要特殊处理
            Cell dateCell = row.createCell(6);
            if (movie.getReleaseDate() != null) {
                // 创建一个日期样式，如果需要的话
                CellStyle dateCellStyle = workbook.createCellStyle();
                CreationHelper createHelper = workbook.getCreationHelper();
                dateCellStyle.setDataFormat(createHelper.createDataFormat().getFormat("yyyy-mm-dd"));

                dateCell.setCellValue(movie.getReleaseDate()); // LocalDate可以直接设置
                dateCell.setCellStyle(dateCellStyle);
            } else {
                dateCell.setCellValue(""); // 如果日期为空，设置为空字符串
            }
        }

        // 7. 自动调整列宽以适应内容
        for (int i = 0; i < headers.length; i++) {
            sheet.autoSizeColumn(i);
        }

        // 8. 将工作簿写入 ByteArrayOutputStream
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        workbook.write(outputStream);
        workbook.close(); // 关闭工作簿

        return outputStream;
    }
}