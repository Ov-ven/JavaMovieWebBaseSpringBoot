package com.software.movie.service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

public interface ReportService {
    /**
     * 生成电影播放量榜单的 Excel 报表。
     *
     * @return 包含 Excel 文件内容的字节数组输出流
     * @throws IOException 如果在生成过程中发生I/O错误
     */
    ByteArrayOutputStream generateMovieRankReport() throws IOException;
}
