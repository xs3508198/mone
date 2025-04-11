package run.mone.mcp.weibo.function;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import lombok.extern.slf4j.Slf4j;
import run.mone.hive.mcp.spec.McpSchema;
import run.mone.mcp.weibo.http.HttpClientUtil;
import run.mone.mcp.weibo.model.WeiboContent;

import java.awt.*;
import java.io.IOException;
import java.net.URI;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.function.Function;

import static run.mone.hive.common.JsonUtils.gson;

@Slf4j
public class WeiboFunction implements Function<Map<String, Object>, McpSchema.CallToolResult> {

    @Override
    public McpSchema.CallToolResult apply(Map<String, Object> stringObjectMap) {
        return null;
    }


    private static String ACCESS_KEY = "2.ss" ;

    private static final String CLIENT_ID = "1500473794";
    private static final String CLIENT_SECRET = "s";
    private static final String AUTHORIZATION_URL = "https://api.weibo.com/oauth2/authorize";
    private static final String TOKEN_URL = "https://api.weibo.com/oauth2/access_token";
    public static final String REDIRECT_URI = "https://api.weibo.com/oauth2/default.html";

    private static final String HOME_TIMELINE = "https://api.weibo.com/2/statuses/home_timeline.json";
    private static final String USER_TIMELINE = "https://api.weibo.com/1/statuses/user_timeline.json";
    private static final String FRIENDS_TIMELINE = "https://api.weibo.com/2/statuses/friends_timeline.json";

    public String loginAuthorization() {
        String finalUrl = AUTHORIZATION_URL + "?client_id=" + CLIENT_ID + "&response_type=code&redirect_uri=" + REDIRECT_URI + "&scope=all";
        if (Desktop.isDesktopSupported()) {
            Desktop desktop = Desktop.getDesktop();
            if (desktop.isSupported(Desktop.Action.BROWSE)) {
                try {
                    desktop.browse(new URI(finalUrl));
                    return "授权成功后请输入code：";
                } catch (Exception e) {
                    log.error(e.getMessage());
                }
            }
        }
        return "授权出现问题，请重试！";
    }


    public String loginGetAccessToken(String code) throws IOException {
        Map<String, String> params = new HashMap<>();
        params.put("client_id", CLIENT_ID);
        params.put("client_secret", CLIENT_SECRET);
        params.put("grant_type", "authorization_code");
        params.put("code", code);
        params.put("redirect_uri", REDIRECT_URI);
        String res = HttpClientUtil.postForm(TOKEN_URL, params);
        if (res == null) {
            return "登录失败，请核验参数重新登录！";
        }
        JsonObject jsonObject = gson.fromJson(res, JsonObject.class);
        String accessKey = jsonObject.get("access_token").getAsString();
        if (accessKey != null && accessKey.isEmpty()) {
            ACCESS_KEY = accessKey;
            return "登录成功！";
        }
        return "登录失败，请核验参数重新登录！";
    }

    public WeiboContent homeTimeline(String page) throws IOException {
        if (page == null || page.isEmpty()) {
            page = "1";
        }
        Map<String, String> params = new HashMap<>();
        params.put("access_token", ACCESS_KEY);
        params.put("page", page);
        String res = HttpClientUtil.get(HOME_TIMELINE, params);
        WeiboContent weiboContent = gson.fromJson(res, WeiboContent.class);
        return weiboContent;
    }


    public WeiboContent userTimeline() throws IOException {
        Map<String, String> params = new HashMap<>();
        params.put("access_token", ACCESS_KEY);
        String res = HttpClientUtil.get(USER_TIMELINE, params);
        System.out.println(res);
        WeiboContent weiboContent = gson.fromJson(res, WeiboContent.class);
        return weiboContent;
    }

    public WeiboContent firendsTimeline() throws IOException {
        Map<String, String> params = new HashMap<>();
        params.put("access_token", ACCESS_KEY);
        String res = HttpClientUtil.get(FRIENDS_TIMELINE, params);
        WeiboContent weiboContent = gson.fromJson(res, WeiboContent.class);
        return weiboContent;
    }


}
