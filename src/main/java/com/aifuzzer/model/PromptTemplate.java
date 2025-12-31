package com.aifuzzer.model;

import org.json.JSONObject;

/**
 * 提示词模板类
 */
public class PromptTemplate {
    private String name;
    private String content;

    public PromptTemplate(String name, String content) {
        this.name = name;
        this.content = content;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    @Override
    public String toString() {
        return name;
    }

    public JSONObject toJson() {
        JSONObject json = new JSONObject();
        json.put("name", name);
        json.put("content", content);
        return json;
    }

    public static PromptTemplate fromJson(JSONObject json) {
        return new PromptTemplate(
                json.getString("name"),
                json.getString("content")
        );
    }
}
