package cn.itcast.hotel.service;

import cn.itcast.hotel.pojo.Hotel;
import cn.itcast.hotel.pojo.vo.PageResult;
import cn.itcast.hotel.pojo.vo.RequestParams;
import com.baomidou.mybatisplus.extension.service.IService;

import java.util.List;
import java.util.Map;

public interface IHotelService extends IService<Hotel> {

    /**
     * 根据关键字搜索酒店信息
     * @param params 请求参数对象，包含用户输入的关键字
     * @return 酒店文档列表
     */
    PageResult search(RequestParams params);

    /**
     * 聚合搜索，主页标签动态变化
     * @param params
     * @return
     */
    Map<String, List<String>> getFilters(RequestParams params);

    /**
     * 自动补全
     * @param prefix
     * @return
     */
    List<String> getSuggestions(String prefix);
}
