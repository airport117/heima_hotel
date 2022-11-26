package cn.itcast.hotel.pojo;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;


@Data
@NoArgsConstructor
public class HotelDoc {

    private Long id;
    private String name;
    private String address;
    private Integer price;
    private Integer score;
    private String brand;
    private String city;
    private String starName;
    private String business;
    private String location;
    private String pic;

    /**
     * 排序时的 距离值
     */
    private Object distance;

    /**
     * 广告标记
     */
    private Boolean isAD;

    /**
     * 自动补全
     */
    private List<String> suggestion;

    public HotelDoc(Hotel hotel) {
        this.id = hotel.getId();
        this.name = hotel.getName();
        this.address = hotel.getAddress();
        this.price = hotel.getPrice();
        this.score = hotel.getScore();
        this.brand = hotel.getBrand();
        this.city = hotel.getCity();
        this.starName = hotel.getStarName();
        this.business = hotel.getBusiness();
        this.location = hotel.getLatitude() + ", " + hotel.getLongitude();
        this.pic = hotel.getPic();


        // 组装suggestion
        if (this.business.contains("/")) {
            String[] split = this.business.split("/");
            this.suggestion = new ArrayList();
            this.suggestion.add(this.brand);
            Collections.addAll(this.suggestion, split);
        }
        if (this.business.contains("、")) {
            String[] split = this.business.split("、");
            this.suggestion = new ArrayList();
            this.suggestion.add(this.brand);
            Collections.addAll(this.suggestion, split);
        } else {
            this.suggestion = Arrays.asList(this.brand, this.business);
        }

    }
}