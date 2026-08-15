---
Type: task
Status: open
---
## Question

多模态输入透传（新缺口，2026-08-15 勘察）：`AgentSession.chat(String)` 与 `SnapshotMessage(content:String)` 全链路纯文本；Spring AI `UserMessage` 原生支持 `Media`（图片/PDF）。生产 agent（OpenHands 读截图、aider 附图改码、Dify 多模态节点）是标配能力。需要决策：API 形态（重载 `chat(ChatInput)` vs `chat(String, List<MediaRef>)` vs 泛型 input 对象）、媒体传递方式（URL 引用 vs 字节上传 spill 化）、存储表示（SnapshotMessage 扩字段 vs metadata 约定）、内存视图与压缩对媒体的处理（媒体占 token 估算口径）、流式/chatForEntity 兼容。产出 spec 27 + impl 切片。

## Resolution

AFK 自决（授权同 effort #5，可推翻）：

1. **API 形态：`chat(String input, List<MediaRef> media)` 重载 + `MediaRef` 值对象**（mimeType + uri URL 或 bytes）——不引入泛型 input 对象（破坏既有 API 简洁性）；default 方法委托 `chat(String)` 保持二进制兼容。MediaRef 转 Spring AI `Media`（MimeType + Url/Resource）在 core 内部完成，不泄漏 Spring AI 类型到公共 API。
2. **媒体传递：URL 引用直传 + 字节走 spill 化 URL**——URL 直接给 Spring AI（模型侧拉取）；字节先落 spill（复用既有 spill 写路径）取临时 URL 再引用，避免 base64 塞进消息体膨胀存储。
3. **存储表示：`SnapshotMessage` 增 `List<MediaRef>` 字段（record 组件追加，构造器兼容 null→空表）**——metadata 约定无 schema 保障，正式字段才能进 store 契约测试；JDBC/Redis 序列化增列/增键。
4. **token 估算口径：媒体固定计数（图片按尺寸分档：≤512² ≈ 160 tok、≤1024² ≈ 320、更大 ≈ 640；PDF 按页 ×150）**——CharHeuristicTokenEstimator 增媒体档位；口径写入 spec（估算非精确，预算闸按此累计）。
5. **兼容矩阵**：stream 同签名重载；chatForEntity 委托带媒体重载；压缩管线对媒体消息「不可压缩标记」（带媒体的用户消息不进摘要合并，原文保留——摘要器只处理文本）。
