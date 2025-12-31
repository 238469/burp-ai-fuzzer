package com.aifuzzer.ai;

import burp.api.montoya.MontoyaApi;
import okhttp3.*;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * AI 客户端，负责与 OpenAI/Claude 进行通信
 */
public class AiClient {
    private final MontoyaApi api;
    private final OkHttpClient httpClient;

    public AiClient(MontoyaApi api) {
        this.api = api;
        this.httpClient = new OkHttpClient.Builder()
                .connectTimeout(60, TimeUnit.SECONDS)
                .readTimeout(180, TimeUnit.SECONDS)
                .writeTimeout(60, TimeUnit.SECONDS)
                .build();
    }

    /**
     * 根据请求上下文生成 Fuzz Payload 列表
     */
    public void generatePayloads(String apiKey, String baseUrl, String model, String customPrompt, String context, PayloadCallback callback) {
        // 构建带有强制约束的完整 Prompt
        String systemConstraints = "\n\n### 强制执行规则 ###\n" +
                "1. 如果请求中存在 '§' 符号（例如：param=§value§），说明这是用户指定的测试位置。请根据该位置的参数名、当前值和上下文环境，生成Payload。\n" +
                "2. 仅输出 Payload 列表，每行一个。\n" +
                "3. 严禁输出任何解释性文字、代码块标记或序号。";

        String prompt = customPrompt + systemConstraints + "\n\n### 待分析的 HTTP 接口上下文 ###\n" + context;

        // 根据模型名称决定调用逻辑
        if (model.toLowerCase().contains("claude")) {
            callClaude(apiKey, baseUrl, model, prompt, callback);
        } else {
            // 默认使用 OpenAI 兼容模式（支持 GPT 及其他自定义模型）
            callOpenAI(apiKey, baseUrl, model, prompt, callback);
        }
    }

    private void callOpenAI(String apiKey, String baseUrl, String model, String prompt, PayloadCallback callback) {
        JSONObject json = new JSONObject();
        json.put("model", model);
        JSONArray messages = new JSONArray();
        messages.put(new JSONObject().put("role", "user").put("content", prompt));
        json.put("messages", messages);
        json.put("temperature", 0.7);

        Request request = new Request.Builder()
                .url(baseUrl.endsWith("/") ? baseUrl + "chat/completions" : baseUrl + "/chat/completions")
                .addHeader("Authorization", "Bearer " + apiKey)
                .post(RequestBody.create(json.toString(), MediaType.parse("application/json")))
                .build();

        executeRequest(request, callback);
    }

    private void callClaude(String apiKey, String baseUrl, String model, String prompt, PayloadCallback callback) {
        // Claude API 的实现逻辑（略有不同，需要 x-api-key header 等）
        JSONObject json = new JSONObject();
        json.put("model", model);
        json.put("max_tokens", 1024);
        JSONArray messages = new JSONArray();
        messages.put(new JSONObject().put("role", "user").put("content", prompt));
        json.put("messages", messages);

        Request request = new Request.Builder()
                .url(baseUrl) // Claude 通常使用单一 endpoint
                .addHeader("x-api-key", apiKey)
                .addHeader("anthropic-version", "2023-06-01")
                .post(RequestBody.create(json.toString(), MediaType.parse("application/json")))
                .build();

        executeRequest(request, callback);
    }

    private void executeRequest(Request request, PayloadCallback callback) {
        api.logging().logToOutput("发送请求到: " + request.url());
        httpClient.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                api.logging().logToError("网络请求失败: " + e.getMessage());
                callback.onError("网络请求失败: " + e.getMessage());
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                api.logging().logToOutput("收到响应: " + response.code() + " " + response.message());
                try (ResponseBody responseBody = response.body()) {
                    if (!response.isSuccessful()) {
                        String errorBody = responseBody != null ? responseBody.string() : "无响应体";
                        api.logging().logToError("API 返回错误: " + response.code() + "\n响应体: " + errorBody);
                        callback.onError("API 返回错误: " + response.code() + " " + response.message());
                        return;
                    }

                    if (responseBody == null) {
                        api.logging().logToError("API 响应体为空");
                        callback.onError("API 响应体为空");
                        return;
                    }

                    String body = responseBody.string();
                    api.logging().logToOutput("响应体内容: " + (body.length() > 500 ? body.substring(0, 500) + "..." : body));
                    try {
                        List<String> payloads = parseResponse(body);
                        api.logging().logToOutput("解析成功，生成了 " + payloads.size() + " 个 Payload");
                        callback.onSuccess(payloads);
                    } catch (Exception e) {
                        api.logging().logToError("解析 Payload 失败: " + e.getMessage());
                        callback.onError(e.getMessage());
                    }
                } catch (Exception e) {
                    api.logging().logToError("处理响应时发生异常: " + e.getMessage());
                    callback.onError("处理响应时发生异常: " + e.getMessage());
                }
            }
        });
    }

    private List<String> parseResponse(String body) throws Exception {
        List<String> payloads = new ArrayList<>();
        JSONObject json = new JSONObject(body);
        
        // 检查 OpenAI 错误格式
        if (json.has("error")) {
            JSONObject error = json.getJSONObject("error");
            throw new Exception("AI 服务返回错误: " + error.optString("message", "未知错误"));
        }

        // 检查 Claude 错误格式
        if (json.has("type") && "error".equals(json.getString("type"))) {
            JSONObject error = json.getJSONObject("error");
            throw new Exception("Claude 返回错误: " + error.optString("message", "未知错误"));
        }

        // 解析 OpenAI 格式
        if (json.has("choices")) {
            String content = json.getJSONArray("choices")
                    .getJSONObject(0)
                    .getJSONObject("message")
                    .getString("content");
            
            extractPayloads(content, payloads);
        } 
        // 解析 Claude 格式
        else if (json.has("content") && json.get("content") instanceof JSONArray) {
            JSONArray contentArray = json.getJSONArray("content");
            for (int i = 0; i < contentArray.length(); i++) {
                JSONObject item = contentArray.getJSONObject(i);
                if ("text".equals(item.optString("type"))) {
                    extractPayloads(item.getString("text"), payloads);
                }
            }
        }
        
        if (payloads.isEmpty()) {
            throw new Exception("未能从响应中解析到有效的 Payload。原始响应: " + (body.length() > 100 ? body.substring(0, 100) + "..." : body));
        }

        return payloads;
    }

    private void extractPayloads(String content, List<String> payloads) {
        for (String line : content.split("\n")) {
            String trimmed = line.trim();
            // 过滤掉可能的 Markdown 代码块标记或空行
            if (!trimmed.isEmpty() && !trimmed.startsWith("```")) {
                payloads.add(trimmed);
            }
        }
    }

    /**
     * 测试 API 连接是否正常
     */
    public void testConnection(String apiKey, String baseUrl, String model, ConnectionCallback callback) {
        String testPrompt = "hi";
        PayloadCallback wrapper = new PayloadCallback() {
            @Override
            public void onSuccess(List<String> payloads) {
                callback.onResult(true, "连接成功！");
            }

            @Override
            public void onError(String message) {
                callback.onResult(false, "连接失败: " + message);
            }
        };

        if (model.toLowerCase().contains("claude")) {
            callClaude(apiKey, baseUrl, model, testPrompt, wrapper);
        } else {
            callOpenAI(apiKey, baseUrl, model, testPrompt, wrapper);
        }
    }

    public interface ConnectionCallback {
        void onResult(boolean success, String message);
    }

    public interface PayloadCallback {
        void onSuccess(List<String> payloads);
        void onError(String message);
    }
}
