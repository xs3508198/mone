package run.mone.mcp.weibo.model;

import lombok.Data;

import java.util.List;
import java.util.Map;

@Data
public class WeiboContent {
    private List<Status> statuses;

    @Data
    public static class Status{
        private String created_at;
        private String idstr;
        private String text;
        private List<Map<String, String>> pic_urls;
        private User user;
        private Map<String, Object> geo;
        private StatusTotalCounter status_total_counter;
    }

    @Data
    public static class User{
        private String id;
        private String name;
        private String followers_count_str;
    }

    @Data
    public static class StatusTotalCounter{
        private String total_cnt;
        private String repost_cnt;
        private String comment_cnt;
        private String like_cnt;
        private String comment_like_cnt;
    }


}
