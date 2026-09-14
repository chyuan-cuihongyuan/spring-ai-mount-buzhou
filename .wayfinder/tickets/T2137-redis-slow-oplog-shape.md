---
id: T2137
title: Redis 慢操作榜（RedisSlowOpLog）的形状裁决
type: task
status: closed
assignee: zcode-l
blocked-by: 
created: 2026-09-14
---

## Question

L 会话第 19 轮（换题轮）：Redis 客户端侧慢操作榜的形状选什么？

## Resolution

**用户常设授权 AFK（可推翻）**

勘察换题：R33 与 mcp breaker 域（811/814）临界——换入 S4 备选题（store-redis 无 slowlog 类读面；J T1453 ToolSlowLog 为工具侧同型可扩散）。

形状裁决：RedisSlowOpLog 进程级静态面（store-redis）——record 严格大于阈值入榜（volatile 热路径零成本）+有界 FIFO 32 新→旧+totalSlowOps 水位+configureThresholdMillis 动态调整+resetForTest 全复位；埋点 RedisMessageStore append/load/findById 三操作 finally（deleteSession 混叠另轴显式出域）。

Out of scope：deleteSession 计时；键名分桶；服务端 SLOWLOG 代理。
