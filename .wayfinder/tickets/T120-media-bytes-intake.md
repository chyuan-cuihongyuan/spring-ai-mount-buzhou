---
Type: task
Status: closed
---
## Question

MediaRef 字节摄取助手（T106 fog）：应用拿到的常是字节（上传/剪贴板），需自建对象存储才能引用。spill 侧公开「字节→spill URI→MediaRef」助手是否做？

## Resolution

AFK 自决：做。spill 模块增 `MediaIntake`（SpillService 之上：`MediaRef intake(byte[] bytes, String mimeType, String sessionId)`——落 spill 文件取 spill:// URI 包 MediaRef 返回；配额/去重沿用 spill 语义）。注意 MediaRef 属 core.session（spill 依赖 core ✓）。生命周期：证据随会话级联删（引用计数语义兼容 T105——intake 不登记 fork 引用，属主即会话）。产 spec 35 §C + impl-95。
