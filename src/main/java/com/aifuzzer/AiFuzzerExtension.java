package com.aifuzzer;

import burp.api.montoya.BurpExtension;
import burp.api.montoya.MontoyaApi;
import burp.api.montoya.logging.Logging;
import burp.api.montoya.intruder.*;
import com.aifuzzer.context.AiFuzzerContextMenu;
import com.aifuzzer.ui.MainPanel;

import java.util.List;

/**
 * AI Fuzzer 插件入口类
 * 实现了 Burp Montoya API 的 Burp Extension 接口
 */
public class AiFuzzerExtension implements BurpExtension {
    private MainPanel mainPanel;
    private MontoyaApi api;

    @Override
    public void initialize(MontoyaApi api) {
        this.api = api;
        // 设置插件名称
        api.extension().setName("AI Fuzzer");

        Logging logging = api.logging();

        // 注册 UI Tab
        mainPanel = new MainPanel(api);
        api.userInterface().registerSuiteTab("AI Fuzzer", mainPanel);

        // 注册右键菜单
        api.userInterface().registerContextMenuItemsProvider(new AiFuzzerContextMenu(api, this));

        // 注册 Intruder Payload 生成器
        api.intruder().registerPayloadGeneratorProvider(new PayloadGeneratorProvider() {
            @Override
            public String displayName() {
                return "AI Fuzzer Generated";
            }

            @Override
            public PayloadGenerator providePayloadGenerator(AttackConfiguration attackConfiguration) {
                return new AiPayloadGenerator();
            }
        });

        // 在 Burp 控制台输出启动成功日志
        logging.logToOutput("--------------------------------------------------");
        logging.logToOutput("AI Fuzzer 插件加载成功！");
        logging.logToOutput("版本: 1.0.0");
        logging.logToOutput("作者: lcy");
        logging.logToOutput("技术栈: Java 17, Montoya API, AI-Driven");
        logging.logToOutput("--------------------------------------------------");
    }

    public MainPanel getMainPanel() {
        return mainPanel;
    }

    /**
     * 自定义 Payload 生成器，直接从 MainPanel 的结果区域读取数据
     */
    private class AiPayloadGenerator implements PayloadGenerator {
        private List<String> payloads;
        private int index = 0;

        public AiPayloadGenerator() {
            this.payloads = mainPanel.getPayloads();
        }

        @Override
        public GeneratedPayload generatePayloadFor(IntruderInsertionPoint insertionPoint) {
            if (index < payloads.size()) {
                return GeneratedPayload.payload(payloads.get(index++));
            }
            return null;
        }
    }
}
