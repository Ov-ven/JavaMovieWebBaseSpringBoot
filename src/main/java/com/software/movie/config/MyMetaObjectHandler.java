package com.software.movie.config;

import com.baomidou.mybatisplus.core.handlers.MetaObjectHandler;
import org.apache.ibatis.reflection.MetaObject;
import org.springframework.stereotype.Component;
import java.util.Date;

/**
 * MyBatis-Plus 自动填充处理器。
 * <p>在执行 INSERT 或 UPDATE 操作时，自动为实体的 {@code createTime} 和 {@code updateTime}
 * 字段填充当前时间，无需在业务代码中手动赋值。</p>
 *
 * <p>使用前提：实体类对应字段需添加 {@code @TableField(fill = FieldFill.INSERT)} 等注解。</p>
 */
@Component
public class MyMetaObjectHandler implements MetaObjectHandler {

    /**
     * 插入操作自动填充。
     * <p>自动为 {@code createTime} 和 {@code updateTime} 字段设置为当前时间。</p>
     *
     * @param metaObject MyBatis-Plus 元对象，包含实体字段信息
     */
    @Override
    public void insertFill(MetaObject metaObject) {
        // 插入时自动填充 createTime 和 updateTime（使用Date类型）
        setFieldValByName("createTime", new Date(), metaObject);
        setFieldValByName("updateTime", new Date(), metaObject);
    }

    /**
     * 更新操作自动填充。
     * <p>自动为 {@code updateTime} 字段设置为当前时间。</p>
     *
     * @param metaObject MyBatis-Plus 元对象，包含实体字段信息
     */
    @Override
    public void updateFill(MetaObject metaObject) {
        // 更新时自动填充 updateTime
        setFieldValByName("updateTime", new Date(), metaObject);
    }
}
