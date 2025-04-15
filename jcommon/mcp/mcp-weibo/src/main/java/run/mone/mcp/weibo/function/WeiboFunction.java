package run.mone.mcp.weibo.function;

import com.google.gson.JsonObject;
import lombok.extern.slf4j.Slf4j;
import org.json.JSONArray;
import org.json.JSONObject;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import run.mone.hive.mcp.spec.McpSchema;
import run.mone.mcp.weibo.http.HttpClientUtil;
import run.mone.mcp.weibo.model.WeiboContent;

import java.awt.*;
import java.io.IOException;
import java.net.URI;
import java.nio.charset.StandardCharsets;
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


    private static String ACCESS_KEY = "2.009t_itFOmpXdB7ef68576b70JvGR_" ;
    private static final String ALAPI_TOKEN  = "kz9rbidzldo4ukzxwbexrgguzndqh4";



    private static final String CLIENT_ID = "1500473794";
    private static final String CLIENT_SECRET = "1e8d2ba34025a53aa78ca9b9286419cf";
    private static final String AUTHORIZATION_URL = "https://api.weibo.com/oauth2/authorize";
    private static final String TOKEN_URL = "https://api.weibo.com/oauth2/access_token";
    public static final String REDIRECT_URI = "https://api.weibo.com/oauth2/default.html";

    private static final String HOME_TIMELINE = "https://api.weibo.com/2/statuses/home_timeline.json";
    private static final String USER_TIMELINE = "https://api.weibo.com/2/statuses/user_timeline.json";

    private static final String WEIBO_HOT = "https://v3.alapi.cn/api/new/wbtop";

    private static final String SEARCH = "https://s.weibo.com/weibo?q=";

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
        if (res == null || res.isEmpty()) {
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

    public String weiboHot() throws IOException {
        Map<String, String> params = new HashMap<>();
        params.put("token", ALAPI_TOKEN);
        String res = HttpClientUtil.postForm(WEIBO_HOT, params);
        return res;
    }

    public String searchWeibo(String keyword) throws IOException {
        Map<String, String> params = new HashMap<>();
        params.put("q", keyword);
        Map<String, String> headers = new HashMap<>();
        headers.put("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/130.0.0.0 Safari/537.36");
        String html = HttpClientUtil.getHtml(SEARCH, params, headers);
        Document doc = Jsoup.parse(html);
        Elements posts = doc.select("div.card-wrap");
        JSONArray results = new JSONArray();
        for (Element post : posts) {
            try {
                // 提取用户名
                String username = post.select("a.name").text();
                // 提取微博内容
                String content = post.select("p.txt").text();
                // 提取发布时间
                String time = post.select("p.from a").first() != null ? post.select("p.from a").first().text() : "未知";

                // 过滤空数据
                if (!username.isEmpty() && !content.isEmpty()) {
                    JSONObject postJson = new JSONObject();
                    postJson.put("username", username);
                    postJson.put("content", content);
                    postJson.put("time", time);
                    results.put(postJson);
                }
            } catch (Exception e) {
                System.out.println("解析单条微博失败: " + e.getMessage());
            }
        }

        // 6. 输出结果
        if (results.length() > 0) {
            System.out.println("搜索结果（JSON 格式）：");
            return results.toString(2);

        } else {
            return "未找到有效搜索结果，可能是页面结构变更或数据需动态加载。";
        }

    }


}
