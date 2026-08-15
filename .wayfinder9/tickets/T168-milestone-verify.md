---
Type: task
Status: closed
---
## Question

里程碑：全仓 mvn clean verify（18 模块全绿、0 失败）；失败则回修再验。

## Resolution

- 首验（2026-08-16，`mvn clean verify -T 1C`）在 buzhou-mcp 失败：`McpToolsDriftTest.lateNotificationAfterRemovalDropped`
  期望 0 漂移实得 1。
- **根因（真实竞态 bug，非测试问题）**：条目摘除是异步的——`tryClose` 在虚拟线程完成物理 close 后才
  `entries.remove`；`handleToolsChanged` 只查 map 不查状态，下线后立即到达的 tools/list_changed 通知撞上
  「仍在 map、状态已 CLOSED」窗口即误报一次漂移。
- 修复：`handleToolsChanged` 增加 `status != ACTIVE` 丢弃闸（与规格语义「条目下线，漂移无从归属」一致）。
- 复验：全仓 `mvn clean verify -T 1C` **BUILD SUCCESS——18 模块全绿；1178 测试 0 失败 0 错误**
  （50 skipped = docker/真实 LLM 门控，与 #8 口径一致）。

