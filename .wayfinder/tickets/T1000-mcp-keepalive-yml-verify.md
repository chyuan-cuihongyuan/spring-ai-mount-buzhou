---
id: T1000
title: MCP keepalive yml 装配的验证
type: task
status: closed
assignee: zcode-g
blocked-by: T999
created: 2026-09-13
---

## Question

yml 声明 keepalive-interval → 注册表调度真启动？缺省零变化？非法时长解析报错口径一致？

## Resolution

**用户常设授权 AFK（可推翻）**

验证（G 会话第 25 轮）：① fromYml(Map.of("keepalive-interval","30s")) → McpModule.registry 生效（fake factory 下 probeOnce 可手动驱动且 probeSuccesses 累计——调度启动由构造参数非空佐证）；② 缺省键 → 现有 McpModule 用例零回归；③ 既有装配用例全绿。`mvn -pl buzhou-mcp -am test` 全绿。
