---
Type: task
Status: open
blocked-by:
---
## Question

MCP 工具集漂移检测怎么做？现状：refresh 仅由本地配置变更触发（replaceAll diff），server 端工具变更（协议 tools/list_changed 通知）完全不可见。决策点：list_changed 订阅的 SDK 能力核实（Spring AI MCP client 能力边界）、不可用时的轮询快照比对兜底（间隔/抖动/有界）、漂移事件与指标（mcp.drift detected/span）、处置策略（仅告警 vs 自动 refresh 重建 spec）、测试方案（进程内 MCP server 真协议，沿用 impl50 范式）。产出 spec 18 + impl 61。
