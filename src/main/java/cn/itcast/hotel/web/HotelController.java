package cn.itcast.hotel.web;

import cn.itcast.hotel.pojo.vo.PageResult;
import cn.itcast.hotel.pojo.vo.RequestParams;
import cn.itcast.hotel.service.IHotelService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/hotel")
public class HotelController {

    @Autowired
    private IHotelService hotelService;

    /**
     * 搜索酒店数据
     *
     * @param params .key搜索的值
     * @return
     */
    @PostMapping("/list")
    public PageResult search(@RequestBody RequestParams params) {
        return hotelService.search(params);
    }

    /**
     * 聚合搜索，主页标签动态变化
     *
     * @param params .key搜索的值
     * @return
     */
    @PostMapping("filters")
    public Map<String, List<String>> getFilters(@RequestBody RequestParams params) {
        return hotelService.getFilters(params);
    }

    /**
     * 自动补全功能
     *
     * @param prefix 用户输入的搜索值
     * @return
     */
    @GetMapping("suggestion")
    public List<String> getSuggestions(@RequestParam("key") String prefix) {
        return hotelService.getSuggestions(prefix);
    }
}