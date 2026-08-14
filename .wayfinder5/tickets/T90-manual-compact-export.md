---
Type: task
Status: open
blocked-by:
---
## Question

手动 compact 与摘要导出怎么做？现状：压缩全自动（预算触发），无手动触发 API；摘要无导出接口。借鉴：Claude Code /compact 手动压缩。决策点：API 形态（AgentRuntime.compact(sessionId) 或 session.compactNow()）、与在途 turn 的互斥、导出形态（summary Markdown/JSON 导出接口）、配置与幂等。产出 spec 20 增量 + impl 65。
