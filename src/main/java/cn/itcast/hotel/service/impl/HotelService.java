package cn.itcast.hotel.service.impl;

import cn.itcast.hotel.mapper.HotelMapper;
import cn.itcast.hotel.pojo.Hotel;
import cn.itcast.hotel.pojo.HotelDoc;
import cn.itcast.hotel.pojo.vo.PageResult;
import cn.itcast.hotel.pojo.vo.RequestParams;
import cn.itcast.hotel.service.IHotelService;
import com.alibaba.fastjson.JSON;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.common.geo.GeoPoint;
import org.elasticsearch.common.lucene.search.function.CombineFunction;
import org.elasticsearch.common.unit.DistanceUnit;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.index.query.functionscore.FunctionScoreQueryBuilder;
import org.elasticsearch.index.query.functionscore.ScoreFunctionBuilders;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.SearchHits;
import org.elasticsearch.search.aggregations.AggregationBuilders;
import org.elasticsearch.search.aggregations.Aggregations;
import org.elasticsearch.search.aggregations.bucket.MultiBucketsAggregation;
import org.elasticsearch.search.aggregations.bucket.terms.Terms;
import org.elasticsearch.search.sort.SortBuilders;
import org.elasticsearch.search.sort.SortOrder;
import org.elasticsearch.search.suggest.Suggest;
import org.elasticsearch.search.suggest.SuggestBuilder;
import org.elasticsearch.search.suggest.SuggestBuilders;
import org.elasticsearch.search.suggest.completion.CompletionSuggestion;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class HotelService extends ServiceImpl<HotelMapper, Hotel> implements IHotelService {

    @Autowired
    RestHighLevelClient client;


    /**
     * ?????????????????????????????????
     *
     * @param params ???????????????????????????????????????????????????
     * @return ??????????????????
     */
    @Override
    public PageResult search(RequestParams params) {
        try {
            // 1.??????Request
            SearchRequest request = new SearchRequest("hotel");
            // 2.??????DSL
            // 2.1.query
            buildBasicQuery(params, request);
            // 2.2.??????
            int page = params.getPage();
            int size = params.getSize();
            request.source().from((page - 1) * size).size(size);

            // 2.3.??????
            String location = params.getLocation();
            if (location != null && !location.equals("")) {
                request.source().sort(SortBuilders
                        .geoDistanceSort("location", new GeoPoint(location))
                        .order(SortOrder.ASC)
                        .unit(DistanceUnit.KILOMETERS)
                );
            }
            // 3.????????????
            SearchResponse response = client.search(request, RequestOptions.DEFAULT);
            // 4.????????????
            return handleResponse(response);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * ????????????????????????????????????????????????
     * <pre>{@code
     * GET /hotel/_search
     * {
     *   "query": {
     *     "multi_match": {
     *       "query": "??????",
     *       "fields": "all"
     *     }
     *   },
     *   "size": 0,
     *   "aggs": {
     *     "brandAgg": {
     *       "terms": {
     *         "field": "brand",
     *         "size": 10
     *       }
     *     },
     *     "cityAgg": {
     *       "terms": {
     *         "field": "city",
     *         "size": 10
     *       }
     *     },
     *     "starAgg": {
     *       "terms": {
     *         "field": "starName",
     *         "size": 10
     *       }
     *     }
     *   }
     * }
     * }</pre>
     *
     * @param params
     * @return
     */
    @Override
    public Map<String, List<String>> getFilters(RequestParams params) {
        try {
            // 1.??????Request
            SearchRequest request = new SearchRequest("hotel");
            // 2.??????DSL
            // 2.1.query
            buildBasicQuery(params, request);
            // 2.2.??????size
            request.source().size(0);
            // 2.3.??????
            buildAggregation(request);
            // 3.????????????
            SearchResponse response = client.search(request, RequestOptions.DEFAULT);
            // 4.????????????
            Map<String, List<String>> result = new HashMap<>();
            Aggregations aggregations = response.getAggregations();
            // 4.1.???????????????????????????????????????
            List<String> brandList = getAggByName(aggregations, "brandAgg");
            result.put("brand", brandList);
            // 4.2.???????????????????????????????????????
            List<String> cityList = getAggByName(aggregations, "cityAgg");
            result.put("city", cityList);
            // 4.3.???????????????????????????????????????
            List<String> starList = getAggByName(aggregations, "starAgg");
            result.put("starName", starList);
            return result;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * ????????????
     * <pre>{@code
     * GET /hotel/_search
     * {
     *   "suggest": {
     *     "hotel_Suggest": {
     *       "text": "???",
     *       "completion": {
     *         "field": "suggestion",
     *         "skip_duplicates": true,
     *         "size": 10
     *       }
     *     }
     *   }
     * }
     * }</pre>
     *
     * @param prefix ????????????????????????
     * @return
     */
    @Override
    public List<String> getSuggestions(String prefix) {
        try {
            // 1.??????Request
            SearchRequest request = new SearchRequest("hotel");
            // 2.??????DSL
            request.source().suggest(new SuggestBuilder().addSuggestion(
                    "suggestions",
                    SuggestBuilders.completionSuggestion("suggestion")
                            .prefix(prefix)
                            .skipDuplicates(true)
                            .size(10)
            ));
            // 3.????????????
            SearchResponse response = client.search(request, RequestOptions.DEFAULT);
            // 4.????????????
            Suggest suggest = response.getSuggest();
            // 4.1.?????????????????????????????????????????????
            CompletionSuggestion suggestions = suggest.getSuggestion("suggestions");
            // 4.2.??????options
            List<CompletionSuggestion.Entry.Option> options = suggestions.getOptions();
            // 4.3.??????
            List<String> list = new ArrayList<>(options.size());
            for (CompletionSuggestion.Entry.Option option : options) {
                String text = option.getText().toString();
                list.add(text);
            }
            return list;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * ???????????????????????????
     *
     * @param request
     */
    private void buildAggregation(SearchRequest request) {
        request.source().aggregation(AggregationBuilders
                .terms("brandAgg")
                .field("brand")
                .size(100)
        );
        request.source().aggregation(AggregationBuilders
                .terms("cityAgg")
                .field("city")
                .size(100)
        );
        request.source().aggregation(AggregationBuilders
                .terms("starAgg")
                .field("starName")
                .size(100)
        );
    }

    /**
     * ??????????????????????????????????????????
     *
     * @param aggregations
     * @param aggName
     * @return
     */
    private List<String> getAggByName(Aggregations aggregations, String aggName) {
        // 4.1.????????????????????????????????????
        Terms brandTerms = aggregations.get(aggName);

        return brandTerms.getBuckets().stream()
                .map(MultiBucketsAggregation.Bucket::getKeyAsString)
                .collect(Collectors.toList());
    }


    /**
     * ????????????
     *
     * @param params
     * @param request
     */
    private void buildBasicQuery(RequestParams params, SearchRequest request) {
        // 1.??????BooleanQuery
        BoolQueryBuilder boolQuery = QueryBuilders.boolQuery();
        // 2.???????????????
        String key = params.getKey();
        if (key == null || "".equals(key)) {
            boolQuery.must(QueryBuilders.matchAllQuery());
        } else {
            boolQuery.must(QueryBuilders.matchQuery("all", key));
        }
        // 3.????????????
        if (params.getCity() != null && !params.getCity().equals("")) {
            boolQuery.filter(QueryBuilders.termQuery("city", params.getCity()));
        }
        // 4.????????????
        if (params.getBrand() != null && !params.getBrand().equals("")) {
            boolQuery.filter(QueryBuilders.termQuery("brand", params.getBrand()));
        }
        // 5.????????????
        if (params.getStarName() != null && !params.getStarName().equals("")) {
            boolQuery.filter(QueryBuilders.termQuery("starName", params.getStarName()));
        }
        // 6.??????
        if (params.getMinPrice() != null && params.getMaxPrice() != null) {
            boolQuery.filter(QueryBuilders
                    .rangeQuery("price")
                    .gte(params.getMinPrice())
                    .lte(params.getMaxPrice())
            );
        }

        // ????????????
        FunctionScoreQueryBuilder functionScoreQuery =
                QueryBuilders.functionScoreQuery(
                        // ???????????????????????????????????????
                        boolQuery,
                        // function score?????????
                        new FunctionScoreQueryBuilder.FilterFunctionBuilder[]{
                                // ???????????????function score ??????
                                new FunctionScoreQueryBuilder.FilterFunctionBuilder(
                                        // ????????????
                                        QueryBuilders.termQuery("isAD", true),
                                        // ????????????
                                        ScoreFunctionBuilders.weightFactorFunction(999999)
                                )
                        }).boostMode(CombineFunction.REPLACE);


        // 7.??????source
        request.source().query(functionScoreQuery);
    }

    /**
     * ????????????
     */
    private PageResult handleResponse(SearchResponse response) {
        // 4.????????????
        SearchHits searchHits = response.getHits();
        // 4.1.???????????????
        long total = searchHits.getTotalHits().value;
        // 4.2.????????????
        SearchHit[] hits = searchHits.getHits();
        // 4.3.??????
        List<HotelDoc> hotels = new ArrayList<>();
        for (SearchHit hit : hits) {
            // ????????????source
            String json = hit.getSourceAsString();
            // ????????????
            HotelDoc hotelDoc = JSON.parseObject(json, HotelDoc.class);
            //???????????????
            Object[] sortValues = hit.getSortValues();
            if (sortValues.length > 0) {
                hotelDoc.setDistance(sortValues[0]);
            }
            // ????????????
            hotels.add(hotelDoc);
        }
        // 4.4.????????????
        return new PageResult(total, hotels);
    }
}

















