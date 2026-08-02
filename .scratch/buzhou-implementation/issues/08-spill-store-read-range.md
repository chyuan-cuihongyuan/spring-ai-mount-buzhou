# 08 — Spill 存储与 read_range

**What to build:** SpillStore SPI+本地磁盘实现（根目录可配、原子写）；`spill://agentName/sessionId/toolCallId` URI；read_range 工具三模式（bytes/json/page）+JSON List 智能预览（前 20 项+totalCount+truncated）+递归 spill 防护；占位符文案自含回读指引；生命周期：会话注册表成套清理+evidence 引用保留+TTL 兜底。

**Blocked by:** 02

**Status:** ready-for-agent

- [ ] 超阈值工具返回落盘、上下文只留预览+路径+指引（端到端）
- [ ] 三模式回读与二次 spill 递归防护有测试
- [ ] 会话 close 后未被引用的 spill 被清理、被 evidence 引用的保留
- [ ] 并发 spill 命名不冲突（toolCallId 唯一性）有测试
