# Spill 存储抽象与生命周期

Type: grilling
Status: resolved
Blocked by: 06

## Question

Spill 的持久化设计：存储介质抽象（本地磁盘为默认，对象存储/分布式文件系统为可选 SPI？）；`spill://agentName/sessionId/toolId` 路径方案的跨实例问题（A 实例落的盘，B 实例如何回读——开源分布式场景必须回答）；会话生命周期绑定与清理（成功/失败/取消/超时都会释放）的实现机制；并发 spill 的文件命名不冲突规则；阈值（默认 32000 字符）与预览（默认 2048 字符）的按工具策略模型。

## Answer

**定案：SpillStore SPI（磁盘/JDBC 首发）+ toolCallId 命名 + 注册表清理（引用保留 + TTL 兜底）+ 策略并入 05。**

1. **SpillStore SPI**，首发两实现：本地磁盘（默认，根目录可配，单机零依赖）+ JDBC（并入 `buzhou-store-jdbc` 模块复用数据源，大字段 BLOB/bytea，跨实例开箱即用）；S3 兼容对象存储留作后续可选扩展模块。`spill://` URI 形式不变，由实现路由——跨实例回读由「选用 JDBC/对象存储实现」回答，文档明确本地磁盘实现不可跨实例。
2. **命名**：`spill://agentName/sessionId/toolCallId`——toolCallId 取自模型 tool_calls，一次调用一次 spill，天然唯一、并发无冲突、回读与消息直接对上。对蓝本 `toolId` 路径的推演修正，Spec 中标注。
3. **生命周期**：会话资源注册表（ticket 04）登记本会话全部 spill 句柄，close/cancel/idle 超时触发成套清理；**被微压缩/摘要 evidence 指针引用的 spill 保留**至会话删除；全局 TTL（可配，默认 7 天）兜底防漏。
4. **策略**：`spillThresholdChars`（默认 32000）、`spillPreviewChars`（默认 2048）作为工具策略字段并入 ticket 05 的四层覆盖模型（默认<yml<绑定级<工具级，通配匹配），字符口径，与微压缩一致；不引 token 双口径。
