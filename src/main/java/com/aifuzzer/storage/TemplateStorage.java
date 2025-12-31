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
        defaults.add(new PromptTemplate("通用 Fuzz", "你是一个专业的渗透测试专家。根据以下 HTTP 请求的上下文，生成 20 个用于 Fuzz 测试该接口参数的恶意或异常 Payload。\n\n" +
                "特别指令：\n" +
                "1. 如果请求中存在 '§' 符号（例如：param=§value§），说明这是用户指定的测试位置。请根据该位置的参数名、当前值和上下文环境，生成最可能导致安全漏洞（如注入、绕过、逻辑错误等）的针对性 Payload。\n" +
                "2. 仅输出 Payload 列表，每行一个。\n" +
                "3. 严禁输出任何解释性文字、代码块标记或序号。"));
        
        defaults.add(new PromptTemplate("SQL 注入专项", "你是一个 Web 安全审计专家。针对提供的 HTTP 请求，请生成 20 个专门用于探测 SQL 注入漏洞的 Payload。\n\n" +
                "要求：\n" +
                "1. 包含各种类型的注入尝试：报错注入、盲注（时间/布尔）、联合查询等。\n" +
                "2. 针对不同的数据库系统（MySQL, PostgreSQL, Oracle, SQL Server）。\n" +
                "3. 仅输出 Payload 列表，每行一个，无解释。"));

        defaults.add(new PromptTemplate("XSS 攻击专项", "你是一个前端安全专家。请为该 HTTP 请求生成 20 个绕过各种过滤机制的 XSS Payload。\n\n" +
                "要求：\n" +
                "1. 包含多种标签：<script>, <img>, <svg>, <iframe> 等。\n" +
                "2. 尝试不同的绕过技巧（大小写混合、编码转换等）。\n" +
                "3. 仅输出 Payload 列表，每行一个。"));
        
        return defaults;
    }
}
