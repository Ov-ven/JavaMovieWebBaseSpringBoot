package com.software.movie.controller;

import com.software.movie.service.ReportService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

@RestController // 这是一个 RESTful 控制器，直接返回数据 (文件流)
@RequestMapping("/report") // API 基路径
public class ReportController {

    @Autowired
    private ReportService reportService;

    /**
     * 下载电影播放量榜单 Excel 报表
     * URL: /report/movie-rank
     */
    @GetMapping("/movie-rank")
    public ResponseEntity<Resource> downloadMovieRankReport() {
        ByteArrayOutputStream outputStream = null;
        try {
            outputStream = reportService.generateMovieRankReport(); // 调用服务生成 Excel
            byte[] excelBytes = outputStream.toByteArray();

            // 设置响应头，告诉浏览器这是一个文件下载
            HttpHeaders headers = new HttpHeaders();
            // 文件名进行编码，防止中文乱码
            String fileName = URLEncoder.encode("电影播放榜单.xlsx", StandardCharsets.UTF_8.toString());
            headers.add(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename*=UTF-8''" + fileName);
            //headers.add(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=" + fileName); // 备用，但推荐上面一个

            return ResponseEntity.ok()
                    .headers(headers)
                    .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")) // Excel .xlsx 文件的MIME类型
                    .body(new ByteArrayResource(excelBytes)); // 将字节数组作为资源返回
        } catch (IOException e) {
            // 处理生成 Excel 过程中的异常
            e.printStackTrace();
            return ResponseEntity.status(500).body(null); // 返回 500 错误
        } finally {
            if (outputStream != null) {
                try {
                    outputStream.close(); // 确保流被关闭
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }
}