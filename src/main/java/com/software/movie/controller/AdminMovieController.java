package com.software.movie.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.software.movie.common.Result;
import com.software.movie.entity.Movie;
import com.software.movie.service.MovieService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 管理员电影管理控制器（无鉴权）。
 * <p>提供电影的增删改查 RESTful 接口，用于测试 RocketMQ 向量同步链路。
 * 所有写操作通过 MovieService 触发 MovieDataChangeEvent → RocketMQ → 向量同步。</p>
 */
@RestController
@RequestMapping("/admin/movie")
public class AdminMovieController {

    private static final Logger log = LoggerFactory.getLogger(AdminMovieController.class);

    @Autowired
    private MovieService movieService;

    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    private static final String STOCK_KEY_PREFIX = "seckill:stock:movie:";

    /**
     * 分页查询电影（支持按名称模糊搜索）。
     *
     * @param pageNum  页码（默认 1）
     * @param pageSize 每页条数（默认 10）
     * @param title    电影名称关键字（可选）
     * @return 分页结果
     */
    @GetMapping("/list")
    public Result list(@RequestParam(defaultValue = "1") Integer pageNum,
                       @RequestParam(defaultValue = "10") Integer pageSize,
                       @RequestParam(required = false) String title) {
        Page<Movie> page = new Page<>(pageNum, pageSize);
        QueryWrapper<Movie> wrapper = new QueryWrapper<>();
        if (title != null && !title.trim().isEmpty()) {
            wrapper.like("title", title.trim());
        }
        wrapper.orderByDesc("create_time");
        Page<Movie> result = movieService.page(page, wrapper);

        // 补充 Redis 实时库存（覆盖 MySQL 的配置值）
        for (Movie movie : result.getRecords()) {
            if (movie.getSeckillStock() != null) {
                String key = STOCK_KEY_PREFIX + movie.getId();
                String val = stringRedisTemplate.opsForValue().get(key);
                if (val != null) {
                    movie.setSeckillStock(Integer.parseInt(val));
                }
            }
        }

        return Result.success(result);
    }

    /**
     * 获取单部电影详情。
     *
     * @param id 电影ID
     * @return 电影信息
     */
    @GetMapping("/{id}")
    public Result getById(@PathVariable Long id) {
        Movie movie = movieService.getById(id);
        if (movie == null) {
            return Result.error("电影不存在");
        }
        return Result.success(movie);
    }

    /**
     * 新增电影。
     * <p>保存成功后自动发布 MovieDataChangeEvent(UPSERT)，
     * 触发 RocketMQ → 向量同步链路。</p>
     *
     * @param movie 电影数据（JSON）
     * @return 操作结果
     */
    @PostMapping("/add")
    public Result add(@RequestBody Movie movie) {
        log.info("管理员新增电影: {}", movie.getTitle());
        // 清空 ID，确保自增生成
        movie.setId(null);
        boolean saved = movieService.save(movie);
        if (saved) {
            log.info("电影新增成功，已触发向量同步事件: id={}", movie.getId());
            return Result.success("新增成功", movie);
        }
        return Result.error("新增失败");
    }

    /**
     * 修改电影。
     * <p>保存成功后自动发布 MovieDataChangeEvent(UPSERT)，
     * 触发 RocketMQ → 向量同步链路。</p>
     *
     * @param movie 电影数据（JSON，必须包含 id）
     * @return 操作结果
     */
    @PutMapping("/update")
    public Result update(@RequestBody Movie movie) {
        if (movie.getId() == null) {
            return Result.error("电影ID不能为空");
        }
        log.info("管理员修改电影: id={}", movie.getId());
        boolean updated = movieService.updateById(movie);
        if (updated) {
            log.info("电影修改成功，已触发向量同步事件: id={}", movie.getId());
            return Result.success("修改成功", movie);
        }
        return Result.error("修改失败");
    }

    /**
     * 删除电影。
     * <p>删除成功后自动发布 MovieDataChangeEvent(DELETE)，
     * 触发 RocketMQ → 向量删除链路。</p>
     *
     * @param id 电影ID
     * @return 操作结果
     */
    @DeleteMapping("/delete/{id}")
    public Result delete(@PathVariable Long id) {
        log.info("管理员删除电影: id={}", id);
        boolean removed = movieService.removeById(id);
        if (removed) {
            log.info("电影删除成功，已触发向量同步事件: id={}", id);
            return Result.success("删除成功");
        }
        return Result.error("删除失败");
    }

    /**
     * 上架秒杀：设置秒杀价 + 初始化 Redis 库存。
     *
     * @param movieId      电影ID
     * @param seckillPrice 秒杀价
     * @param seckillStock 秒杀库存
     * @return 操作结果
     */
    @PostMapping("/seckill")
    public Result setupSeckill(@RequestParam Long movieId,
                               @RequestParam Double seckillPrice,
                               @RequestParam Integer seckillStock) {
        Movie movie = movieService.getById(movieId);
        if (movie == null) {
            return Result.error("电影不存在");
        }
        // 更新电影秒杀字段
        movie.setSeckillPrice(seckillPrice);
        movie.setSeckillStock(seckillStock);
        movieService.updateById(movie);

        // 写入 Redis 库存
        String key = STOCK_KEY_PREFIX + movieId;
        stringRedisTemplate.opsForValue().set(key, String.valueOf(seckillStock));

        log.info("上架秒杀: movieId={}, price={}, stock={}", movieId, seckillPrice, seckillStock);
        return Result.success("秒杀上架成功", Map.of(
                "movieId", movieId,
                "seckillPrice", seckillPrice,
                "seckillStock", seckillStock
        ));
    }
}
