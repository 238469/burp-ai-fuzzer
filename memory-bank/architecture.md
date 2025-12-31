# 项目架构说明

## 文件列表及作用
- `fuzz.md`: 插件的详细设计文档，包含功能需求、系统架构、技术栈及实施计划。
- `progress.md`: 记录项目的开发进度和已完成的里程碑。
- `architecture.md`: 维护项目的文件结构说明，解释每个文件的职责。
- `pom.xml`: Maven 项目对象模型文件，定义了项目依赖（Montoya API, OkHttp）和构建插件（Assembly Plugin 用于生成 Fat JAR）。
- `src/main/java/com/aifuzzer/AiFuzzerExtension.java`: 插件的入口类，负责初始化插件、注册 UI、右键菜单以及自定义 Intruder Payload 生成器。
- `src/main/java/com/aifuzzer/ui/MainPanel.java`: 主 UI 面板，集成 AI 配置、请求编辑器（支持 § 标记）、内置 Fuzz 引擎执行器、实时结果表格，以及与 Intruder 的交互逻辑。
- `src/main/java/com/aifuzzer/ai/AiClient.java`: AI 交互客户端，支持多平台模型调用、连接测试、自定义 Prompt 处理及异步响应解析。
- `src/main/java/com/aifuzzer/context/AiFuzzerContextMenu.java`: 右键菜单逻辑，负责捕获 HTTP 请求全文并同步至插件编辑器。
- `src/main/java/com/aifuzzer/model/PromptTemplate.java`: 提示词模板数据模型，支持 JSON 转换。
- `src/main/java/com/aifuzzer/storage/TemplateStorage.java`: 模板持久化层，负责本地 JSON 文件的读写。
- `plan.md`: 详细的实施计划，包含分阶段的指令和验证步骤。
