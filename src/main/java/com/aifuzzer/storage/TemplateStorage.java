package com.aifuzzer.storage;

import com.aifuzzer.model.PromptTemplate;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;

/**
 * 模板持久化管理，保存至本地 JSON 文件
 */
public class TemplateStorage {
    private static final String FILE_NAME = "ai_fuzzer_templates.json";
    private final File storageFile;

    public TemplateStorage() {
        // 获取用户主目录下的配置文件夹
        String userHome = System.getProperty("user.home");
        File configDir = new File(userHome, ".burp_ai_fuzzer");
        if (!configDir.exists()) {
            configDir.mkdirs();
        }
        this.storageFile = new File(configDir, FILE_NAME);
    }

    public void saveTemplates(List<PromptTemplate> templates) throws IOException {
        JSONArray array = new JSONArray();
        for (PromptTemplate template : templates) {
            array.put(template.toJson());
        }
        Files.writeString(storageFile.toPath(), array.toString(4), StandardCharsets.UTF_8);
    }

    public List<PromptTemplate> loadTemplates() {
        List<PromptTemplate> templates = new ArrayList<>();
        if (!storageFile.exists()) {
            return getDefaultTemplates();
        }

        try {
            String content = Files.readString(storageFile.toPath(), StandardCharsets.UTF_8);
            JSONArray array = new JSONArray(content);
            for (int i = 0; i < array.length(); i++) {
                templates.add(PromptTemplate.fromJson(array.getJSONObject(i)));
            }
        } catch (Exception e) {
            return getDefaultTemplates();
        }
        return templates;
    }

    private List<PromptTemplate> getDefaultTemplates() {
        List<PromptTemplate> defaults = new ArrayList<>();
        defaults.add(new PromptTemplate("通用 Fuzz", "你是一个专业的渗透测试专家。根据以下 HTTP 请求的上下文，生成 20 个用于 Fuzz 测试该接口参数的恶意或异常 Payload。请根据参数名、当前值和上下文环境，生成最可能导致安全漏洞（如注入、绕过、逻辑错误等）的针对性 Payload。"));
        
        defaults.add(new PromptTemplate("SQL 注入专项", "你是一个渗透测试专家。针对提供的 HTTP 请求，请生成 20 个专门用于探测 SQL 注入漏洞的 Payload。包含各种类型的注入尝试：报错注入、盲注（时间/布尔）、联合查询等，针对不同的数据库系统（MySQL, PostgreSQL, Oracle, SQL Server）。"));

        defaults.add(new PromptTemplate("XSS 攻击专项", "你是一个前端安全专家。请为该 HTTP 请求生成 20 个绕过各种过滤机制的 XSS Payload。包含多种标签：<script>, <img>, <svg>, <iframe> 等，尝试不同的绕过技巧（大小写混合、编码转换等）。"));
        defaults.add(new PromptTemplate("参数/值发现", "你是一个经验丰富的 Web 开发和安全专家。请分析以下 HTTP 请求的上下文（包括 URL 路径、现有参数名、业务逻辑等），生成 20 个该接口可能潜在支持的其他参数名，或者针对现有参数生成可能导致业务逻辑漏洞、未授权访问或信息泄露的特殊测试值。"));
        return defaults;
    }
}
