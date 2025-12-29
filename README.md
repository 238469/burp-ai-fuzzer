# Burp AI Fuzzer

一个基于 AI 驱动的 Burp Suite 渗透测试辅助插件，旨在利用大语言模型（LLM）的上下文理解能力，为复杂的 HTTP 请求自动生成针对性的 Fuzz 字典。

## 🌟 核心特性

- **AI 智能生成**：根据 HTTP 请求的上下文（参数名、值、接口逻辑）自动生成恶意或异常 Payload。
- **§ 标记支持**：支持像 Intruder 一样使用 `§` 符号手动标记需要 Fuzz 的位置，AI 将针对该位置进行深度优化。
- **Intruder 深度集成**：一键将带标记的请求发送至 Burp Intruder，并支持在 Intruder 中直接引用 AI 生成的字典。
- **多模型支持**：支持 OpenAI (GPT-4/GPT-3.5) 和 Anthropic (Claude 3) 等兼容 API 格式的模型。
- **配置持久化**：所有 API 配置（Key, Base URL, Prompt 等）均自动保存至 Burp 的全局设置。

## 🚀 快速开始

### 1. 编译项目
项目使用 Maven 管理依赖，你可以直接运行：
```bash
mvn clean package
```
编译完成后，在 `target/` 目录下会生成 `ai-fuzzer-1.0-SNAPSHOT-jar-with-dependencies.jar`。

### 2. 安装插件
1. 打开 Burp Suite。
2. 进入 `Extensions` -> `Installed` -> `Add`。
3. 选择 `Java` 类型，并加载上述编译好的 JAR 文件。

### 3. 配置 AI
1. 切换到 `AI Fuzzer` 标签页。
2. 填写你的 API Key、Base URL（如 `https://api.openai.com/v1`）以及模型名称。
3. 点击 `保存配置` 并点击 `测试连接` 确保 API 正常。

## 🛠 使用说明

1. **同步请求**：在 Burp 的 Proxy 或 Repeater 历史记录中，右键点击请求，选择 `Send to AI Fuzzer`。
2. **标记位置**（可选）：在插件编辑框中，使用 `§` 包裹你想测试的参数值，例如 `id=§1001§`。
3. **生成字典**：点击 `生成 AI 字典`，稍等片刻即可看到针对性生成的 Payload 列表。
4. **开始爆破**：
   - 点击 `发送至 Intruder`。
   - 在 Intruder 的 `Payloads` 选项卡中，`Payload type` 选择 `Extension-generated`。
   - 在下方下拉框中选择 `AI Fuzzer Generated`。
   - 点击 `Start attack`。

## 📝 许可证

[MIT License](LICENSE)
