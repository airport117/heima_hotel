package cn.itcast.hotel.query;

import cn.itcast.hotel.pojo.HotelDoc;
import com.alibaba.fastjson.JSON;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.HttpHost;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestClient;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.SearchHits;
import org.elasticsearch.search.fetch.subphase.highlight.HighlightBuilder;
import org.elasticsearch.search.sort.SortOrder;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;

/**
 * day2查询文档
 */
@Slf4j
public class HotelQueryTest {
    private RestHighLevelClient client;

    /**
     * 解析数据
     *
     * @param response
     */
    private void handleResponse(SearchResponse response) {
        //解析响应
        SearchHits searchHits = response.getHits();
        //获取命中总数
        long total = searchHits.getTotalHits().value;
        System.err.println("共搜索到" + total + "条数据");
        //获取请求的命中(文档数组)
        SearchHit[] hits = searchHits.getHits();
        //遍历
        for (SearchHit hit : hits) {
            // 获取文档"_source"(文档的json数据)
            String json = hit.getSourceAsString();
            // 解析JSON。获得文档对应的hotel对象
            HotelDoc hotelDoc = JSON.parseObject(json, HotelDoc.class);
            System.out.println(hotelDoc);
        }
    }

    @BeforeEach
    void setUp() {
        this.client = new RestHighLevelClient(RestClient.builder(
                HttpHost.create("http://192.168.255.128:9200")
        ));
    }

    @AfterEach
    void tearDown() throws IOException {
        this.client.close();
    }

    /**
     * 查询所有Match_all
     */
    @SneakyThrows
    @Test
    void testMatchAll() {
        //发送请求，返回数据
        SearchRequest request = new SearchRequest("hotel");
        //查询所有
        request.source().query(QueryBuilders.matchAllQuery());
        SearchResponse response = client.search(request, RequestOptions.DEFAULT);
        //解析数据
        handleResponse(response);
    }

    /**
     * 模糊查询multi_match
     */
    @SneakyThrows
    @Test
    void testMatch() {
        //发送请求。返回数据
        SearchRequest request = new SearchRequest("hotel");
        //todo 单字段查找: matchQuery(String name, Object text)
//        request.source().query(QueryBuilders
//                .matchQuery("all", "上海"));
        //todo 多字段查找: multiMatchQuery(Object text, String... fieldNames)
        request.source().query(QueryBuilders
                .multiMatchQuery("深圳", "name", "city"));
        SearchResponse response = client.search(request, RequestOptions.DEFAULT);

        handleResponse(response);
    }

    /**
     * 精确查询/范围查询 term/range
     */
    @Test
    @SneakyThrows
    void testTerm() {
        SearchRequest request = new SearchRequest("hotel");
        //todo 精确查询: termQuery(String name, Object value)
//        request.source().query(QueryBuilders
//                .termQuery("city", "杭州"));
        //todo 范围查询: rangeQuery(String name)
        request.source().query(QueryBuilders
                .rangeQuery("price").gt(100).lt(200));

        SearchResponse response = client.search(request, RequestOptions.DEFAULT);

        handleResponse(response);
    }

    /**
     * 布尔查询
     */
    @Test
    @SneakyThrows
    void boolTest() {

        SearchRequest request = new SearchRequest("hotel");
        //todo 布尔查询
        BoolQueryBuilder boolQuery = QueryBuilders.boolQuery();
        boolQuery.must(QueryBuilders.termQuery("city", "上海"));
        boolQuery.should(QueryBuilders.termQuery("brand", "华美达"));
        boolQuery.mustNot(QueryBuilders.rangeQuery("price").lte(500));
        boolQuery.filter(QueryBuilders.rangeQuery("score").lte(45));
        //布尔查询
        request.source().query(boolQuery);
        SearchResponse response = client.search(request, RequestOptions.DEFAULT);

        handleResponse(response);
    }

    /**
     * 分页、排序
     */
    @Test
    @SneakyThrows
    void pageTest() {
        SearchRequest request = new SearchRequest("hotel");
        request.source().query(QueryBuilders.matchAllQuery());
        //todo 排序: sort(String name, SortOrder order)
        request.source().sort("price", SortOrder.ASC);
        //todo 分页:
        // from(int from): 从第from个开始返回。默认为0
        // size(int size): 要返回的命中数。默认为10
        int page = 2;
        int size = 5;
        request.source().from((page - 1) * size).size(5);

        SearchResponse response = client.search(request, RequestOptions.DEFAULT);
        handleResponse(response);
    }

    /**
     * 高亮
     */
    @Test
    @SneakyThrows
    void highlightTest() {
        SearchRequest request = new SearchRequest("hotel");
        //要查询的字段名
        final String FIELD = "name";

        request.source().query(QueryBuilders.matchQuery("all", "如家"));
        request.source().highlighter(new HighlightBuilder()
                .field(FIELD)
                .requireFieldMatch(false));

        SearchResponse response = client.search(request, RequestOptions.DEFAULT);

        //高亮的结果和查询的文档是分离的
        SearchHits hits = response.getHits();
        for (SearchHit hit : hits) {
            String name = hit
                    //获取高亮的结果。Map类型。key：字段名、value：高亮值
                    .getHighlightFields()
                    .get(FIELD)
                    .getFragments()[0]
                    .string();
            System.out.println("name = " + name);
        }
    }


}
