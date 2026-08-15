---
Type: task
Status: closed
---
## Question

会话可移植导出/导入（新缺口）：现仅有 `exportSummaryMarkdown`（摘要导出，T90）。生产环境需要整会话跨环境迁移/备份恢复（灰度切流、灾难恢复、bug 复现包）。需要决策：导出格式（单 JSON 文档结构：messages/summary/state/facts/spill 引用清单）、版本字段与向后兼容、导入语义（新 sessionId 重映射 vs 保留原 Id 冲突策略）、spill 证据是否随包（内嵌 base64 vs 引用清单+单独导出）、API 位置（AgentSession vs AgentRuntime vs 工具类）。产出 spec 28 + impl 切片。

## Resolution

AFK 自决（授权同 effort #5，可推翻）：

1. **导出格式：单 JSON 文档 `SessionExport`**——`format: "buzhou.session-export"`、`version: 1`、会话元数据（appId/agentName/sessionId/createdAt）、messages、summary（最新一版）、state entries、facts、spill 引用清单（evidenceId→spillUri 映射，不含内容）。版本字段供未来演进。
2. **导入语义：默认新 sessionId 重映射**（UUID 重生成，防跨环境 Id 撞车；引用内 sessionId/evidenceId 一致重写）+ `keepIds` 选项（true 时原 Id 冲突即 fail-fast 报 `SessionImportConflict`）。
3. **spill 证据不内嵌**：引用清单形式；证据内容走 spill 侧另行导出（运维操作，runbook 记载）；导入后悬垂引用读路径已由 T105 容错（EVIDENCE_GONE）——两票互为依赖，T105 先行。
4. **API 位置：`AgentRuntime.exportSession(sessionId)` / `importSession(SessionExport)`**（会话级生命周期操作归 runtime，非单会话内动作）；返回导入后的新 sessionId。JSON 序列化用 Jackson（core 已依赖 Spring AI 传递 Jackson，零新依赖）。

### 闭合细化（实现期定稿）

- 序列化用 DTO + epoch millis（不假定 jackson-jsr310 在 classpath）；SessionExport 记录组件 JVM 内直用、toJson/fromJson 为可移植边界。
- appId/agentName 尽力携带（活跃会话可查、历史会话 null，导入不依赖）。
- keepIds 冲突检测 = 目标 messageStore 非空即 fail-fast（SessionImportException）。
- spec 28 落档。
