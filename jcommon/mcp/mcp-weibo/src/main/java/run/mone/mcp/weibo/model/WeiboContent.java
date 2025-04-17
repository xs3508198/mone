package run.mone.mcp.weibo.model;

import lombok.Data;

import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Data
public class WeiboContent {
    private List<Status> statuses;

    @Data
    public static class Status {
        private String created_at;
        private String idstr;
        private String text;
        private List<PictureUrl> pic_urls;
        private User user;
        private Map<String, Object> geo;
        private String reposts_count;
        private String comments_count;
        private String attitudes_count;
    }

    @Data
    public static class User {
        private String id;
        private String name;
        private String followers_count_str;
    }

    @Data
    public static class StatusTotalCounter {
        private String repost_cnt;
        private String comment_cnt;
        private String like_cnt;
    }

    @Data
    public static class PictureUrl {
        private String thumbnail_pic;
    }


    public List<WeiboContentDisplay> toContentDisplay() {
        List<WeiboContentDisplay> res = new ArrayList<>();
        statuses.forEach(status -> {
            WeiboContentDisplay weiboContentDisplay = new WeiboContentDisplay();
            weiboContentDisplay.setWeiboUrl(status.getIdstr());
            weiboContentDisplay.setUsername(status.getUser().getName());
            weiboContentDisplay.setUserId(status.getUser().getId());
            weiboContentDisplay.setContent(status.getText());
            if (status.getCreated_at() != null && !status.getCreated_at().isEmpty()) {
                String time = formatTime(status.getCreated_at());
                weiboContentDisplay.setTime(time);
            }
            if (status.getPic_urls() != null && !status.getPic_urls().isEmpty()) {
                List<String> pictureList = status.getPic_urls().stream().map(PictureUrl::getThumbnail_pic).toList();
                weiboContentDisplay.setPictureUrl(pictureList);
            }
            weiboContentDisplay.setComment(status.getComments_count());
            weiboContentDisplay.setLike(status.getAttitudes_count());
            weiboContentDisplay.setRepost(status.getReposts_count());
            res.add(weiboContentDisplay);
        });
        return res;
    }

    //例如 Wed Apr 16 14:10:04 +0800 2025 转为2025年04月16日 14:10:04
    private String formatTime(String originTime) {
        DateTimeFormatter inputFormatter = new DateTimeFormatterBuilder()
                .parseCaseInsensitive()
                .appendPattern("EEE MMM dd HH:mm:ss Z yyyy")
                .toFormatter(Locale.US);

        ZonedDateTime zonedDateTime = ZonedDateTime.parse(originTime, inputFormatter);
        DateTimeFormatter outputFormatter = DateTimeFormatter.ofPattern("yyyy年MM月dd日 HH:mm:ss");
        String output = zonedDateTime.format(outputFormatter);
        return output;
    }


}
