package run.mone.mcp.laborunion.function;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.reflect.TypeToken;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import okhttp3.*;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;

import run.mone.hive.mcp.spec.McpSchema;
import run.mone.mcp.laborunion.model.SignDTO;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Function;

@Slf4j
@Data
@Component
public class LaborUnionFunction implements Function<Map<String, Object>, Flux<McpSchema.CallToolResult>> {
    private String name = "labor union bot";

    private String desc = "The automatic service of the trade union mini-program including checkTodaySign, getAuth, setAuth, signIn and getRandomExecutionTime";

    private String toolScheme = """
            {
                "type": "object",
                "properties": {
                    "operation": {
                        "type": "string",
                        "enum": ["checkTodaySign", "setAuth", "getAuth", "signIn", "getRandomExecutionTime"],
                        "description":"The operation of the trade union mini-program, only 'setAuth' requires auth as the input parameter，'checkTodaySign' 'getAuth' 'signIn' 'getRandomExecutionTime' do not require the input parameter auth"
                    },
                    "auth": {
                        "type": "string",
                        "description":"The authorization key of the trade union mini-program, only 'setAuth' need this parameter"
                    }
                },
                "required": ["operation", "auth"]
            }
            """;


    private static final String GET_SIGN_URL = "https://union.hr.mioffice.cn/labourunion/member/getSignIn";

    private static final String SIGN_URL = "https://union.hr.mioffice.cn/labourunion/member/signIn";

    private static final String USER_INFO = "https://union.hr.mioffice.cn/labourunion/member/getUserInfo";

    private static String AUTH = "";

    private static final Gson gson = new Gson();

    private static final boolean start = false;

    private static final OkHttpClient client = new OkHttpClient();

    private LocalTime randomExecutionTime;

    public LaborUnionFunction() {
        LocalTime now = LocalTime.now();
        if ((now.isAfter(LocalTime.of(9, 0)) || now.equals(LocalTime.of(9, 0)))
                && now.isBefore(LocalTime.of(12, 0))
                && randomExecutionTime == null) {
            randomExecutionTime = generateRandomTime(now.plusMinutes(5), LocalTime.of(12, 0));
            System.out.println("启动时生成随机时间: " + randomExecutionTime);
        }
    }

    @Override
    public Flux<McpSchema.CallToolResult> apply(Map<String, Object> arguments) {
        String operation = (String) arguments.get("operation");
        try {
            String result = switch (operation) {
                case "checkTodaySign" -> checkTodaySign();
                case "setAuth" -> setAuth((String) arguments.get("auth"));
                case "getAuth" -> getAuth();
                case "getRandomExecutionTime" -> getRandomExecutionTime();
                case "signIn" -> signIn();
                default -> throw new IllegalArgumentException("Unknown operation: " + operation);
            };

            return Flux.just(new McpSchema.CallToolResult(
                    List.of(new McpSchema.TextContent(result)),
                    false));
        } catch (Exception e) {
            return Flux.just(new McpSchema.CallToolResult(List.of(new McpSchema.TextContent("Error: " + e.getMessage())), true));
        }
    }
    public String checkTodaySign() {
        String signInfo = getSignInfo();
        if (signInfo != null && signInfo.startsWith("请求失败")) {
            return signInfo;
        }
        List<SignDTO> signDTOList = gson.fromJson(signInfo, new TypeToken<List<SignDTO>>() {
        }.getType());
        //最近的一次的签到信息
        SignDTO latestSign = signDTOList.get(0);
        String signTime = latestSign.getCreateDate();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        LocalDateTime signDateTime = LocalDateTime.parse(signTime, formatter);
        LocalDate localDate = signDateTime.toLocalDate();
        LocalDate today = LocalDate.now();
        if (localDate.isEqual(today)) {
            return "今天已签到";
        }
        return "今天未签到";
    }

    public String setAuth(String auth) {
        AUTH = auth;
        if (AUTH.isEmpty()) {
            return "设置登录信息失败";
        } else {
            return "登录信息成功";
        }
    }

    public String getAuth() {
        return AUTH;
    }

    public String getRandomExecutionTime() {
        if (randomExecutionTime == null) {
            return "当前随机执行时间为空";
        }
        return randomExecutionTime.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
    }

    private String getSignInfo() {
        Headers headers = new Headers.Builder()
                .add("accept", "application/json, text/plain, */*")
                .add("accept-language", "zh-Hans")
                .add("authorization", AUTH)
                .add("cache-control", "no-cache")
                .add("pragma", "no-cache")
                .add("sec-fetch-dest", "empty")
                .add("sec-fetch-mode", "cors")
                .add("sec-fetch-site", "cross-site")
                .add("user-agent", "Mozilla/5.0 (Macintosh; Intel Mac OS X 14_5_0) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/109.0.5414.128 Safari/537.36 Lark/7.18.5 LarkLocale/zh_CN EEMicroApp/1.9.28.6 miniprogram/window-semi SuperApp")
                .add("x-neotix-chromium", "neotix")
                .add("x-request-id", "0217526542971709170215bdd03cbfdde43279e8ad0802c00000c")
                .add("x-tt-logid", "0217526542971709170215bdd03cbfdde43279e8ad0802c00000c")
                .build();
        Request request = new Request.Builder()
                .url(GET_SIGN_URL)
                .headers(headers)
                .get()
                .build();


        String responseBodyStr = "";
        try (Response response = client.newCall(request).execute()) {
            if (response.isSuccessful()) {
                responseBodyStr = response.body().string();
            } else {
                System.out.println("请求失败：" + response.code() + ": " + response.message());
                responseBodyStr = "请求失败：" + response.code() + ":" + response.message();
            }
        } catch (Exception e) {
            log.error("请求签到列表出错！错误信息：{}", e.getMessage());
            responseBodyStr = "请求失败：" + e.getMessage();
        }
        if (!responseBodyStr.isBlank() && !responseBodyStr.startsWith("请求失败")) {
            JsonObject jsonObject = gson.fromJson(responseBodyStr, JsonObject.class);
            int code = jsonObject.get("code").getAsInt();
            String msg = jsonObject.get("msg").getAsString();
            if (code == 200 && msg.equals("success")) {
                JsonObject dataJson = jsonObject.get("data").getAsJsonObject();
                String entityList = dataJson.get("inIntegralEntityList").toString();
                return entityList;
            } else {
                return "请求失败：code: " + code + ", msg: " + msg + ", data: " + responseBodyStr;
            }
        } else {
            return responseBodyStr;
        }

    }

    public String signIn() {
        Headers headers = new Headers.Builder()
                .add("accept", "application/json, text/plain, */*")
                .add("accept-language", "zh-Hans")
                .add("authorization", AUTH)
                .add("cache-control", "no-cache")
                .add("pragma", "no-cache")
                .add("sec-fetch-dest", "empty")
                .add("sec-fetch-mode", "cors")
                .add("sec-fetch-site", "cross-site")
                .add("user-agent", "Mozilla/5.0 (Macintosh; Intel Mac OS X 14_5_0) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/109.0.5414.128 Safari/537.36 Lark/7.18.5 LarkLocale/zh_CN EEMicroApp/1.9.28.6 miniprogram/window-semi SuperApp")
                .add("x-neotix-chromium", "neotix")
                .add("x-request-id", "0217526542971709170215bdd03cbfdde43279e8ad0802c00000c")
                .add("x-tt-logid", "0217526542971709170215bdd03cbfdde43279e8ad0802c00000c")
                .build();
        Request request = new Request.Builder()
                .url(SIGN_URL)
                .headers(headers)
                .get()
                .build();

        String responseBodyStr = "";
        try (Response response = client.newCall(request).execute()) {
            if (response.isSuccessful()) {
                responseBodyStr = response.body().string();
            } else {
                System.out.println("请求失败：" + response.code() + ": " + response.message());
                responseBodyStr = "请求失败：" + response.code() + ":" + response.message();
            }
        } catch (Exception e) {
            log.error("请求签到出错！错误信息：{}", e.getMessage());
            responseBodyStr = "请求签到失败：" + e.getMessage();
        }
        if (!responseBodyStr.isBlank() && !responseBodyStr.startsWith("请求失败")) {
            JsonObject jsonObject = gson.fromJson(responseBodyStr, JsonObject.class);
            int code = jsonObject.get("code").getAsInt();
            String msg = jsonObject.get("msg").getAsString();
            if (code == 200 && msg.equals("success")) {
                JsonObject dataJson = jsonObject.get("data").getAsJsonObject();
                responseBodyStr = dataJson.toString();
                return responseBodyStr;
            } else {
                return "请求签到失败：code: " + code + ", msg: " + msg + ", data: " + responseBodyStr;
            }
        } else {
            return responseBodyStr;
        }
    }

    @Scheduled(cron = "5 * 9-12 * * * ", zone = "Asia/Shanghai")
    public void checkAndExecute() {
        LocalTime now = LocalTime.now().withSecond(0).withNano(0);
        //启动时
        if (now.equals(randomExecutionTime)){
            try {
                //随机睡几毫秒
                Thread.sleep(ThreadLocalRandom.current().nextInt(1000));
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            signIn();
        }
    }

    @Scheduled(cron = "0 0 1 * * *")
    public void resetRandomTime() {
        log.info("定时任务启动");
        Random random = new Random();
        int totalMinutes = 570 + random.nextInt(151); // 540~719分钟
        int hour = totalMinutes / 60;
        int min = totalMinutes % 60; // 转换为当前小时的分钟数
        randomExecutionTime = LocalTime.of(hour, min);
    }

    // 4. 生成指定时间范围内的随机时间
    private LocalTime generateRandomTime(LocalTime start, LocalTime end) {
        int startMinutes = start.toSecondOfDay() / 60; // 转为分钟数
        int endMinutes = end.toSecondOfDay() / 60;
        int randomMinutes = startMinutes + ThreadLocalRandom.current().nextInt(endMinutes - startMinutes + 1);

        int hour = randomMinutes / 60;
        int minute = randomMinutes % 60;
        return LocalTime.of(hour, minute);
    }


}
