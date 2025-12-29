package com.aifuzzer.context;

import burp.api.montoya.MontoyaApi;
import burp.api.montoya.http.message.HttpRequestResponse;
import burp.api.montoya.http.message.requests.HttpRequest;
import burp.api.montoya.ui.contextmenu.ContextMenuEvent;
import burp.api.montoya.ui.contextmenu.ContextMenuItemsProvider;
import burp.api.montoya.ui.contextmenu.MessageEditorHttpRequestResponse;
import com.aifuzzer.AiFuzzerExtension;
import com.aifuzzer.ai.AiClient;
import com.aifuzzer.ui.MainPanel;

import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;

/**
 * 右键菜单提供者，负责捕获用户选中的请求并发送到 AI Fuzzer
 */
public class AiFuzzerContextMenu implements ContextMenuItemsProvider {
    private final MontoyaApi api;
    private final AiFuzzerExtension extension;
    private final AiClient aiClient;

    public AiFuzzerContextMenu(MontoyaApi api, AiFuzzerExtension extension) {
        this.api = api;
        this.extension = extension;
        this.aiClient = new AiClient(api);
    }

    @Override
    public List<Component> provideMenuItems(ContextMenuEvent event) {
        List<Component> menuItems = new ArrayList<>();

        JMenuItem sendToAiFuzzer = new JMenuItem("Send to AI Fuzzer");
        sendToAiFuzzer.addActionListener(e -> {
            if (!event.selectedRequestResponses().isEmpty()) {
                for (HttpRequestResponse requestResponse : event.selectedRequestResponses()) {
                    handleRequest(requestResponse);
                }
            } else if (event.messageEditorRequestResponse().isPresent()) {
                handleRequest(event.messageEditorRequestResponse().get());
            }
        });

        menuItems.add(sendToAiFuzzer);
        return menuItems;
    }

    private void handleRequest(HttpRequestResponse requestResponse) {
        HttpRequest request = requestResponse.request();
        SwingUtilities.invokeLater(() -> {
            extension.getMainPanel().setRequestText(request);
        });
    }

    private void handleRequest(MessageEditorHttpRequestResponse messageEditorRequestResponse) {
        HttpRequest request = messageEditorRequestResponse.requestResponse().request();
        SwingUtilities.invokeLater(() -> {
            extension.getMainPanel().setRequestText(request);
        });
    }

    // 移除不再需要的旧方法 extractAndLog
}
