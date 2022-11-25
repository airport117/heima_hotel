package cn.itcast.hotel.pojo.vo;

import lombok.Data;

/**
 * 分页请求实体类
 */
@Data
public class RequestParams {
    /**
     * 搜索关键字
     */
    private String key;
    private Integer page;
    private Integer size;
    /**
     * 排序方式
     */
    private String sortBy;

    // 下面是新增的过滤条件参数
    private String city;
    private String brand;
    private String starName;
    private Integer minPrice;
    private Integer maxPrice;

    /**
     * 我当前的地理坐标
     */
    private String location;

}