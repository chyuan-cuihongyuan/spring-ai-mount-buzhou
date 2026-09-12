# 724 — MCP keepalive yml 装配

> 来源：G 会话第 25 轮 = effort #724（D 会话装配轮模式——spec 504 server-breaker 同法）/ [T999](../../.wayfinder/tickets/T999-mcp-keepalive-yml-shape.md) / [T1000](../../.wayfinder/tickets/T1000-mcp-keepalive-yml-verify.md) / impl 527。

## 背景

spec 703 的 keepaliveInterval 只有编程构造面——声明式部署（yml 用户）不可用。装配三层缝：`McpModule.Builder.keepalive` fluent 面 → `fromYml` 键 `keepalive-interval` → 注册表 9 参构造直通。

## 目标

- `Builder.keepalive(Duration)` + `fromYml` 键 `keepalive-interval`（Durations.fromMap 解析；缺省 = 关）。
- McpModule 构造把 keepaliveInterval 传入 DefaultMcpClientRegistry 9 参构造。
- 既有 yml（无此键）行为逐字节不变。

## 测试

fromYml 声明 → registry keepalive 生效（probe 可驱动）；缺省零回归；既有装配用例全绿。

## 兼容性

纯增量装配面；缺省逐字节不变。
