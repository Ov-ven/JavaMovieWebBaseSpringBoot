package com.software.movie.common.event;

/**
 * 电影数据变更事件。
 * <p>当电影数据发生增删改时发布此事件，触发向量库增量同步。</p>
 */
public class MovieDataChangeEvent {

    /** 操作类型：新增或更新 */
    public static final String UPSERT = "UPSERT";
    /** 操作类型：删除 */
    public static final String DELETE = "DELETE";

    /** 变更的电影ID */
    private final Long movieId;

    /** 操作类型（UPSERT 或 DELETE） */
    private final String operation;

    public MovieDataChangeEvent(Long movieId, String operation) {
        this.movieId = movieId;
        this.operation = operation;
    }

    public Long getMovieId() {
        return movieId;
    }

    public String getOperation() {
        return operation;
    }
}
