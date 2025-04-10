package run.mone.mcp.weibo.function;

import lombok.extern.slf4j.Slf4j;
import run.mone.hive.mcp.spec.McpSchema;
import run.mone.mcp.weibo.http.HttpClientUtil;

import java.awt.*;
import java.io.IOException;
import java.net.URI;
import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;
import java.util.function.Function;

@Slf4j
public class WeiboFunction implements Function<Map<String, Object>, McpSchema.CallToolResult> {

    @Override
    public McpSchema.CallToolResult apply(Map<String, Object> stringObjectMap) {
        return null;
    }

    private static final String CLIENT_ID = "1500473794";
    private static final String CLIENT_SECRET = "111111";
    private static final String AUTHORIZATION_URL = "https://api.weibo.com/oauth2/authorize";
    private static final String TOKEN_URL = "https://api.weibo.com/oauth2/access_token";
    public static final String REDIRECT_URI = "https://api.weibo.com/oauth2/default.html";

    public void login() {
        String finalUrl = AUTHORIZATION_URL + "?client_id=" + CLIENT_ID + "&response_type=code&redirect_uri=" + REDIRECT_URI;
        if (Desktop.isDesktopSupported()) {
            Desktop desktop = Desktop.getDesktop();
            if (desktop.isSupported(Desktop.Action.BROWSE)) {
                try {
                    desktop.browse(new URI(finalUrl));
                    for (int i = 0; i < 4; i++) {
                        if (i == 0) {
                            System.out.println("输入授权后获取的code：");
                        } else {
                            System.out.println("请核验您的code，重新输入，重试次数：" + i + "，剩余重试次数：" + (3 - i));
                        }
                        System.out.print("输入授权后获取的code：");
                        Scanner scanner = new Scanner(System.in);
                        String code = scanner.nextLine();
                        String res = getAccessToken(code);
                        log.info("get access token result is: " + res);
                    }


                } catch (Exception e) {
                    log.error(e.getMessage());
                }
            }
        }
    }


    private String getAccessToken(String code) throws IOException {
        Map<String, String> params = new HashMap<>();
        params.put("client_id", CLIENT_ID);
        params.put("client_secret", CLIENT_SECRET);
        params.put("grant_type", "authorization_code");
        params.put("code", code);
        params.put("redirect_uri", REDIRECT_URI);
        String res = HttpClientUtil.postForm(TOKEN_URL, params);
        return res;
    }
}
