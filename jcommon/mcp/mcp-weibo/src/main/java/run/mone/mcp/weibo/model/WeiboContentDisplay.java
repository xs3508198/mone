package run.mone.mcp.weibo.model;

import lombok.Data;

import java.util.List;
import java.util.Map;

@Data
public class WeiboContentDisplay {

    private String username;

    private String content;

    private String time;

    private List<String> pictureUrl;

    private String comment;

    private String repost;

    private String like;

    private String weiboId;

    private String userId;
}
