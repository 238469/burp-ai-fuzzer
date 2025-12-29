package com.aifuzzer.ui;

import burp.api.montoya.MontoyaApi;
import burp.api.montoya.persistence.Preferences;
import com.aifuzzer.ai.AiClient;

import burp.api.montoya.http.message.requests.HttpRequest;
import com.aifuzzer.ai.AiClient;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;

/**
 * 主配置面板，负责 UI 布局和配置持久化
 */
public class MainPanel extends JPanel {
    private final MontoyaApi api;
    private final Preferences preferences;
    private final AiClient aiClient;

    private JTextField apiKeyField;
    private JTextField baseUrlField;
    private JComboBox<String> modelSelector;
    private JSpinner threadSpinner;
    private JTextArea promptArea;
    private JTextArea resultArea;
    private JTextArea requestEditor;
    private JButton genDictButton;
    private JButton sendToIntruderBtn;
    private JLabel statusLabel;
    private JProgressBar progressBar;
    private burp.api.montoya.http.HttpService currentService;

    private static final String PREF_API_KEY = "ai_fuzzer_api_key";
    private static final String PREF_BASE_URL = "ai_fuzzer_base_url";
    private static final String PREF_MODEL = "ai_fuzzer_model";
    private static final String PREF_THREADS = "ai_fuzzer_threads";
    private static final String PREF_PROMPT = "ai_fuzzer_prompt";

    private static final String DEFAULT_PROMPT = "你是一个专业的渗透测试专家。根据以下 HTTP 请求的上下文，生成 20 个用于 Fuzz 测试该接口参数的恶意或异常 Payload。\n\n" +
            "特别指令：\n" +
            "1. 如果请求中存在 '§' 符号（例如：param=§value§），说明这是用户指定的测试位置。请根据该位置的参数名、当前值和上下文环境，生成最可能导致安全漏洞（如注入、绕过、逻辑错误等）的针对性 Payload。\n" +
            "2. 仅输出 Payload 列表，每行一个。\n" +
            "3. 严禁输出任何解释性文字、代码块标记或序号。";

    public MainPanel(MontoyaApi api) {
        this.api = api;
        this.preferences = api.persistence().preferences();
        this.aiClient = new AiClient(api);
        
        setLayout(new BorderLayout());
        setBorder(new EmptyBorder(10, 10, 10, 10));

        // 顶部配置面板
        add(createConfigPanel(), BorderLayout.NORTH);
        
        // 中间：编辑器和字典展示
        JPanel centerPanel = new JPanel(new BorderLayout());
        centerPanel.add(createControlPanel(), BorderLayout.NORTH);
        centerPanel.add(createEditorPanel(), BorderLayout.CENTER);
        
        add(centerPanel, BorderLayout.CENTER);

        // 加载现有配置
        loadConfig();
    }

    private JPanel createControlPanel() {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        panel.setBorder(BorderFactory.createTitledBorder("字典生成与工具"));

        genDictButton = new JButton("生成 AI 字典");
        genDictButton.addActionListener(e -> generateAiDictionary());
        panel.add(genDictButton);

        sendToIntruderBtn = new JButton("发送至 Intruder");
        sendToIntruderBtn.addActionListener(e -> sendToIntruder());
        panel.add(sendToIntruderBtn);

        progressBar = new JProgressBar();
        progressBar.setIndeterminate(false);
        progressBar.setVisible(false);
        progressBar.setPreferredSize(new Dimension(150, 20));
        panel.add(progressBar);

        statusLabel = new JLabel("状态: 等待中");
        statusLabel.setBorder(new EmptyBorder(0, 10, 0, 0));
        panel.add(statusLabel);

        return panel;
    }

    private void generateAiDictionary() {
        String requestText = getRequestText();
        if (requestText.isEmpty()) {
            JOptionPane.showMessageDialog(this, "请先在左侧编辑器中填入 HTTP 请求内容", "提示", JOptionPane.WARNING_MESSAGE);
            return;
        }

        String apiKey = getApiKey();
        if (apiKey.isEmpty()) {
            JOptionPane.showMessageDialog(this, "请先配置 API Key", "错误", JOptionPane.ERROR_MESSAGE);
            return;
        }

        setGeneratingState(true);
        api.logging().logToOutput("正在调用 AI 生成字典...");

        aiClient.generatePayloads(apiKey, getBaseUrl(), getModel(), getCustomPrompt(), requestText, new AiClient.PayloadCallback() {
            @Override
            public void onSuccess(List<String> payloads) {
                SwingUtilities.invokeLater(() -> {
                    try {
                        updatePayloads(payloads);
                        statusLabel.setText("状态: 生成成功 (" + payloads.size() + " 个)");
                    } catch (Exception e) {
                        api.logging().logToError("更新 UI 失败: " + e.getMessage());
                    } finally {
                        setGeneratingState(false);
                    }
                });
            }

            @Override
            public void onError(String message) {
                SwingUtilities.invokeLater(() -> {
                    try {
                        statusLabel.setText("状态: 生成失败");
                        JOptionPane.showMessageDialog(MainPanel.this, message, "AI 生成失败", JOptionPane.ERROR_MESSAGE);
                    } catch (Exception e) {
                        api.logging().logToError("显示错误对话框失败: " + e.getMessage());
                    } finally {
                        setGeneratingState(false);
                    }
                });
            }
        });
    }

    private void setGeneratingState(boolean isGenerating) {
        genDictButton.setEnabled(!isGenerating);
        sendToIntruderBtn.setEnabled(!isGenerating);
        progressBar.setVisible(isGenerating);
        progressBar.setIndeterminate(isGenerating);
        if (isGenerating) {
            statusLabel.setText("状态: 正在生成...");
        }
    }

    private JPanel createConfigPanel() {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBorder(BorderFactory.createTitledBorder("AI 配置"));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.anchor = GridBagConstraints.WEST;

        // API Key
        gbc.gridx = 0; gbc.gridy = 0;
        panel.add(new JLabel("API Key:"), gbc);
        gbc.gridx = 1; gbc.fill = GridBagConstraints.HORIZONTAL; gbc.weightx = 1.0;
        apiKeyField = new JTextField(30);
        panel.add(apiKeyField, gbc);

        // Base URL
        gbc.gridx = 0; gbc.gridy = 1; gbc.fill = GridBagConstraints.NONE; gbc.weightx = 0;
        panel.add(new JLabel("Base URL:"), gbc);
        gbc.gridx = 1; gbc.fill = GridBagConstraints.HORIZONTAL; gbc.weightx = 1.0;
        baseUrlField = new JTextField("https://api.openai.com/v1", 30);
        panel.add(baseUrlField, gbc);

        // Model
        gbc.gridx = 0; gbc.gridy = 2; gbc.fill = GridBagConstraints.NONE; gbc.weightx = 0;
        panel.add(new JLabel("Model:"), gbc);
        gbc.gridx = 1; gbc.fill = GridBagConstraints.HORIZONTAL; gbc.weightx = 1.0;
        modelSelector = new JComboBox<>(new String[]{"gpt-3.5-turbo", "gpt-4", "claude-3-opus", "claude-3-sonnet"});
        modelSelector.setEditable(true);
        panel.add(modelSelector, gbc);

        // Threads
        gbc.gridx = 0; gbc.gridy = 3; gbc.fill = GridBagConstraints.NONE; gbc.weightx = 0;
        panel.add(new JLabel("并发线程数:"), gbc);
        gbc.gridx = 1; gbc.fill = GridBagConstraints.HORIZONTAL; gbc.weightx = 1.0;
        threadSpinner = new JSpinner(new SpinnerNumberModel(5, 1, 50, 1));
        panel.add(threadSpinner, gbc);

        // Custom Prompt
        gbc.gridx = 0; gbc.gridy = 4; gbc.fill = GridBagConstraints.NONE; gbc.weightx = 0;
        panel.add(new JLabel("AI Prompt:"), gbc);
        gbc.gridx = 1; gbc.fill = GridBagConstraints.BOTH; gbc.weightx = 1.0; gbc.weighty = 1.0;
        promptArea = new JTextArea(5, 30);
        promptArea.setLineWrap(true);
        promptArea.setWrapStyleWord(true);
        promptArea.setText(DEFAULT_PROMPT);
        panel.add(new JScrollPane(promptArea), gbc);

        // Save Button
        gbc.gridx = 1; gbc.gridy = 5; gbc.fill = GridBagConstraints.NONE; gbc.anchor = GridBagConstraints.EAST; gbc.weighty = 0;
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        
        JButton testButton = new JButton("测试连接");
        testButton.addActionListener(e -> testConnection());
        buttonPanel.add(testButton);

        JButton saveButton = new JButton("保存配置");
        saveButton.addActionListener(e -> saveConfig());
        buttonPanel.add(saveButton);
        
        panel.add(buttonPanel, gbc);

        return panel;
    }

    private void testConnection() {
        String apiKey = getApiKey();
        String baseUrl = getBaseUrl();
        String model = getModel();

        if (apiKey.isEmpty()) {
            JOptionPane.showMessageDialog(this, "请输入 API Key", "错误", JOptionPane.ERROR_MESSAGE);
            return;
        }

        api.logging().logToOutput("正在测试 API 连接: " + baseUrl + " [" + model + "]");
        
        aiClient.testConnection(apiKey, baseUrl, model, (success, message) -> {
            SwingUtilities.invokeLater(() -> {
                if (success) {
                    JOptionPane.showMessageDialog(this, message, "成功", JOptionPane.INFORMATION_MESSAGE);
                } else {
                    JOptionPane.showMessageDialog(this, message, "失败", JOptionPane.ERROR_MESSAGE);
                }
            });
        });
    }

    private JPanel createEditorPanel() {
        JPanel panel = new JPanel(new GridLayout(1, 2, 10, 0));

        // 左侧：请求编辑器（支持标记 §）
        JPanel requestPanel = new JPanel(new BorderLayout());
        requestPanel.setBorder(BorderFactory.createTitledBorder("HTTP 请求 (使用 §标记 Fuzz 位置§)"));
        requestEditor = new JTextArea();
        requestEditor.setFont(new Font("Monospaced", Font.PLAIN, 12));
        requestPanel.add(new JScrollPane(requestEditor), BorderLayout.CENTER);
        panel.add(requestPanel);

        // 右侧：字典生成结果
        JPanel dictPanel = new JPanel(new BorderLayout());
        dictPanel.setBorder(BorderFactory.createTitledBorder("Fuzz 字典 (AI 生成或手动输入)"));
        
        resultArea = new JTextArea();
        resultArea.setEditable(true);
        resultArea.setFont(new Font("Monospaced", Font.PLAIN, 12));
        dictPanel.add(new JScrollPane(resultArea), BorderLayout.CENTER);

        // 字典操作按钮
        JPanel dictTools = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JButton clearBtn = new JButton("清空字典");
        clearBtn.addActionListener(e -> resultArea.setText(""));
        dictTools.add(clearBtn);
        dictPanel.add(dictTools, BorderLayout.SOUTH);

        panel.add(dictPanel);
        
        return panel;
    }

    public void updatePayloads(java.util.List<String> payloads) {
        StringBuilder sb = new StringBuilder();
        for (String p : payloads) {
            sb.append(p).append("\n");
        }
        resultArea.setText(sb.toString());
    }

    private void sendToIntruder() {
        String requestTemplate = getRequestText();
        if (requestTemplate.isEmpty()) {
            JOptionPane.showMessageDialog(this, "请求内容为空", "错误", JOptionPane.ERROR_MESSAGE);
            return;
        }

        HttpRequest request;
        if (currentService != null) {
            // 使用保存的服务信息创建请求，这会保留 Host/Port 信息
            request = HttpRequest.httpRequest(currentService, requestTemplate);
        } else {
            // 如果是手动粘贴的，尝试从文本解析
            request = HttpRequest.httpRequest(requestTemplate);
        }

        if (request.httpService() == null) {
            api.logging().logToError("警告: 无法识别目标服务器信息。Intruder 可能无法直接运行。");
        }

        api.intruder().sendToIntruder(request);
        
        api.logging().logToOutput("请求已发送至 Intruder。目标: " + (request.httpService() != null ? request.httpService().toString() : "未知"));
        JOptionPane.showMessageDialog(this, "已发送至 Intruder。\n提示：在 Intruder 的 Payload 设置中选择 'Extension-generated' 即可使用 AI 生成的字典。", "成功", JOptionPane.INFORMATION_MESSAGE);
    }

    public void setRequestText(HttpRequest request) {
        this.currentService = request.httpService();
        this.requestEditor.setText(request.toString());
        String hostInfo = (currentService != null) ? " [" + currentService.host() + "]" : "";
        statusLabel.setText("状态: 请求已同步" + hostInfo + "，等待生成字典");
    }

    public String getRequestText() {
        return requestEditor.getText();
    }

    public List<String> getPayloads() {
        List<String> payloads = new ArrayList<>();
        for (String line : resultArea.getText().split("\n")) {
            if (!line.trim().isEmpty()) {
                payloads.add(line.trim());
            }
        }
        return payloads;
    }

    public String getApiKey() { return apiKeyField.getText().trim(); }
    public String getBaseUrl() { return baseUrlField.getText().trim(); }
    public String getModel() { return (String) modelSelector.getSelectedItem(); }
    public String getCustomPrompt() { return promptArea.getText().trim(); }

    private void saveConfig() {
        preferences.setString(PREF_API_KEY, apiKeyField.getText().trim());
        preferences.setString(PREF_BASE_URL, baseUrlField.getText().trim());
        preferences.setString(PREF_MODEL, (String) modelSelector.getSelectedItem());
        preferences.setString(PREF_THREADS, threadSpinner.getValue().toString());
        preferences.setString(PREF_PROMPT, promptArea.getText().trim());
        
        api.logging().logToOutput("配置已成功保存到 Burp 全局设置。");
        JOptionPane.showMessageDialog(this, "配置已保存", "成功", JOptionPane.INFORMATION_MESSAGE);
    }

    private void loadConfig() {
        String apiKey = preferences.getString(PREF_API_KEY);
        String baseUrl = preferences.getString(PREF_BASE_URL);
        String model = preferences.getString(PREF_MODEL);
        String threads = preferences.getString(PREF_THREADS);
        String prompt = preferences.getString(PREF_PROMPT);

        if (apiKey != null) apiKeyField.setText(apiKey);
        if (baseUrl != null) baseUrlField.setText(baseUrl);
        if (model != null) modelSelector.setSelectedItem(model);
        if (threads != null) {
            try {
                threadSpinner.setValue(Integer.parseInt(threads));
            } catch (NumberFormatException ignored) {}
        }
        if (prompt != null) promptArea.setText(prompt);
    }
}
