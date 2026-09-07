# Wayfinder Map — Buzhou 批内工具合并接线（effort #300，C 会话第 1 轮）

> C 会话（第三期自迭代）开篇轮。上两会话 A（.wayfinder100 系 50 轮）/ B（.wayfinder200 系
> 50 轮）已收口（impl-322，全仓测试绿）。C 会话目标沿用 /goal：**最少 50 个完整
> wayfinder→spec→tickets→implement 闭环**，全程自主决策，借鉴 GitHub 10K+ stars 项目。

## Destination

spec 139 的在飞合并原语 `ToolCallCoalescer` 从 standalone 接线到执行脊柱
`HarnessToolCallingManager` 批派发路径——批内同工具同参调用执行一次、全部
回喂位共享值（B 会话收口 fog 种子第 1 项）。

## Notes

- **C 会话号段裁决（本 MAP 立账，后续 49 轮沿用）**：
  - 轮次目录 `.wayfinder300`–`.wayfinder349`（一轮一号，不与 A/B 撞）。
  - effort # = 目录号（#300–#349）；spec 号段 **300–349 为 C 专段**
    （A=偶 ≥224、B=奇 的旧裁决继续有效，C 从 300 起独占，后续 D 可用 400+）。
  - 票号 T591 起顺延（上会话止于 T590）；impl 号 impl323 起顺延。
  - README 新段「生产级纵深 IV（C 会话 300 系增量）」逐轮累加（覆盖门 213）。
  - 收口轮（#349）统一：全仓 reactor 测试 + API 快照 regenerate + 台账归档
    + fog 种子（B 先例 da1b125 / #227）。
- C 会话 50 轮总表（执行时逐轮在此追加登记，防迷航）：
  1. #300 批内合并接线（本轮）｜2. #301 对冲装配｜3. #302 RetryBudget 接线
  4. #303 围栏持久纪元｜5. #304 事务批补偿｜6. #305 租户键前缀
  7. #306 租约续期｜8. #307 空闲→归档接线｜9. #308 schema yml 化
  10. #309 嵌入缓存｜11. #310 泳道公平模式｜12. #311 deadline 传播
  13. #312 时间旅行重放｜14. #313 定时任务选主｜15. #314 轮指标预聚合
  16. #315 租约围栏令牌｜17. #316 配置 CAS 版本｜18. #317 告警规则 yml
  19. #318 会话 PDB｜20. #319 排队深度伸缩建议｜21. #322 PII 分侧列
  22. #324 影子明细导出｜23. #325 命名空间 ACL｜24. #327 TimeLimiter 降级
  25. #328 备胎预解析｜26. #329 成本 per-tenant｜27. #330 价目快照
  28. #331 技能漏斗｜29. #333 目录 owner 注记｜30. #335 期望套件持久化
  31. #336 门禁结果随 run｜32. #337 熔断 yml 面｜33. #338 泳道 Redis 共享
  34. #339 key 配额 Redis 共享｜35. #340 舱表跨实例查询面｜36. #341 trace 传播
  37. #342 exemplars｜38. #343 rollup 接线 dashboard｜39. #344 导出合流打包
  40. #345 Doctor 规则 SPI｜41. #346 性质测试 III｜42. #347 租户→key 路由
  43. #348 文档轮｜…（余量按 fog 种子滚动补位至 50）…｜50. #349 收口轮。
- 借鉴：Hystrix request collapsing（spec 139 原始来源——本轮是装配收尾）。

## Decisions so far

- 合并键 = 工具名 + 全参串（零碰撞——正确性优先于键紧凑，不沿用 hash 口径）。
- 共享值、独占 id：合并位回喂时逐位重写 tool_call id（协议每位必须有 id 一致回喂）。
- 事件日志只记首执行位（合并位无独立执行——不虚构日志条目）。
- `ToolCallCoalescer` 补取消桥接（共享 Future cancel → 中断底层任务），
  保住 awaitCompletion 超时语义经合并路径不降级。

## Not yet specified

- 跨轮在飞窗口的合并收益观测（coalesced 计数已有，趋势面归观测轮）。
- **本地（Windows）红名单更新（2026-09-01 stash 验证均为存量，非 C 会话引入）**：
  fs.TenantSandboxTest（路径分隔符断言）/ fs.PropertyInvariantsTwoTest（同）/
  metrics.ErrorSignaturesTest（跨包静态污染顺序敏感——metrics 包单跑绿）/
  internal.session.UnsubscribedStreamTest（负载/顺序敏感单飞闸 flaky——单跑绿）。
  本地全量门排除集由 3 类扩至 7 类；CI（Linux）仍为权威。污染根修入 fog。

## Out of scope

- 跨实例合并（归 Redis 共享族 #338）；结果缓存（TTL 缓存 183 已覆盖——
  合并只在飞、完成即忘，不持历史）。

## Tickets

- [x] [T591 批内合并接线 HarnessToolCallingManager](../tickets/T591-batch-coalescing.md)（impl-323）
- [x] [T592 合并位 id 重写 + 取消桥接回归](../tickets/T592-batch-coalescing-close.md)（impl-323）
