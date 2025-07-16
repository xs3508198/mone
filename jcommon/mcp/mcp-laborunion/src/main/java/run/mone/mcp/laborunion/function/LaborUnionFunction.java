package run.mone.mcp.laborunion.function;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.reflect.TypeToken;
import lombok.extern.slf4j.Slf4j;
import okhttp3.Headers;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.springframework.stereotype.Service;
import run.mone.mcp.laborunion.model.SignDTO;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Slf4j
@Service
public class LaborUnionFunction {

    private static final String SIGN_URL = "https://union.hr.mioffice.cn/labourunion/member/getSignIn";
    private static String AUTH = "";

    private static final Gson gson = new Gson();



    public String checkTodaySign(){
        String signInfo = getSignInfo();
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
            return "true";
        }
        return "false";
    }

    public String setAuth(String auth){
        AUTH = auth;
        if (AUTH.isEmpty()){
            return "设置登录信息失败";
        }else {
            return "登录信息成功";
        }
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
                .url(SIGN_URL)
                .headers(headers)
                .get()
                .build();

        OkHttpClient client = new OkHttpClient();
        String responseBodyStr = "";
        try (Response response = client.newCall(request).execute()) {
            if (response.isSuccessful()) {
                responseBodyStr = response.body().string();
            }else {
                System.out.println("请求失败：" + response.code() + ": "+ response.message());
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
            if (code == 200 && msg.equals("success")){
                JsonObject dataJson = jsonObject.get("data").getAsJsonObject();
                //TODO
                String entityList = dataJson.get("inIntegralEntityList").toString();
                return entityList;
            }else {
                return "请求失败：code: " + code + ", msg: " + msg + ", data: " + responseBodyStr;
            }
        }else {
            return responseBodyStr;
        }

    }

}
