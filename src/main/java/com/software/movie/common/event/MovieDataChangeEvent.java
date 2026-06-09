package com.software.movie.common.event;

/**
 * 电影数据变更事件。
 * <p>当电影数据发生增删改时发布此事件，触发向量库增量同步。</p>
 */
public class MovieDataChangeEvent implements java.io.Serializable {

    private static final long serialVersionUID = 1L;

    /** 操作类型：新增或更新 */
    public static final String UPSERT = "UPSERT";
    /** 操作类型：删除 */
    public static final String DELETE = "DELETE";

    /** 变更的电影ID */
    private Long movieId;

    /** 操作类型（UPSERT 或 DELETE） */
    private String operation;

    /** 无参构造函数（Jackson 反序列化需要） */
    public MovieDataChangeEvent() {
    }

    public MovieDataChangeEvent(Long movieId, String operation) {
        this.movieId = movieId;
        this.operation = operation;
    }

    public Long getMovieId() {
        return movieId;
    }

    public void setMovieId(Long movieId) {
        this.movieId = movieId;
    }

    public String getOperation() {
        return operation;
    }

    public void setOperation(String operation) {
        this.operation = operation;
    }
}
