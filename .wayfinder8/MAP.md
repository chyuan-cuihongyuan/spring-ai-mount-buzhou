# Wayfinder Map — Buzhou 生产级纵深（effort #8）

> effort #8，延续 #1–#7（#5：22 轮；#6：9 轮；#7：20 轮）。主线：**能力补全与社区/对抗面**——
> skill 检索、死信重放、保留策略、跨 store 迁移、新能力攻击面的红队覆盖、健康端点、社区文件。
> 到达 = 20 轮自迭代落地、全仓 verify 绿、MAP 闭合。

## Notes

- 领域/测试哲学/10K★ 政策/AFK 授权：沿用 effort #7 MAP Notes。
- 图前勘察（2026-08-15）：LoadSkillTool 为 ToolCallback 直实现（skill_search 同款）；
  effort #7 收口 fog：skill_search / 索引 CLOSED 保留 / 压缩梯子事件化等。
- 新面识别：新能力（多模态/结果限幅/摄取）无红队对抗覆盖；.github 社区文件（ISSUE/PR 模板）缺失；
  health details 未含 outbox/索引新维度。

## Decisions so far

- [T132 skill_search 检索工具](tickets/T132-skill-search.md) — query 子串检索不截断全集（listAllFor 新 default + 覆写）；上限 20 + load_skill 指引；绑定可见性沿用；spec 37 §A。
- [T133 死信重放 API](tickets/T133-deadletter-replay.md) — replayDeadLetters() 一键迁回 outbox（attempts 清零/容量满部分重放/损坏丢弃）；spec 37 §B。
- [T134 索引 CLOSED 行保留](tickets/T134-index-retention.md) — purgeOlderThan（三实现覆写，ACTIVE 永不扫）+ 观察者 1/64 惰性清扫 + closed-retention 可配；spec 37 §C。

## Not yet specified

- LLM 响应缓存（语义边界未清，长期 fog）。
- dashboard 前端工程化（#4 边界）。
- outbox Redis 服务端 SCAN 下推（当前键集合侧过滤已够用，量级不抵复杂度）。

## Out of scope

- 沿用 effort #7 Out of scope 全部条目。

## Tickets

初始 19 张（T132–T150，均含 AFK 决议，按轮逐张闭合）：

- [T132 skill_search 检索工具](tickets/T132-skill-search.md)
- [T133 死信重放 API](tickets/T133-deadletter-replay.md)
- [T134 索引 CLOSED 行保留](tickets/T134-index-retention.md)
- [T135 压缩梯子事件化](tickets/T135-ladder-compaction-events.md)
- [T136 跨 store 迁移器](tickets/T136-store-migrator.md)
- [T137 黄金轨迹 C](tickets/T137-golden-c.md)（blocked-by T132/133/134）
- [T138 红队新攻击面](tickets/T138-redteam-new-surface.md)
- [T139 观测背压审计](tickets/T139-observability-backpressure-audit.md)
- [T140 health 新维度](tickets/T140-health-new-dimensions.md)
- [T141 社区文件](tickets/T141-community-files.md)
- [T142 javadoc @since 审计](tickets/T142-javadoc-since-audit.md)
- [T143 黄金轨迹 D](tickets/T143-golden-d.md)（blocked-by T136）
- [T144 perf 哨兵增补](tickets/T144-perf-sentinels-2.md)
- [T145 examples 演示](tickets/T145-examples-demo-2.md)（blocked-by T132/133/136）
- [T146 runbook 增补](tickets/T146-runbook-4.md)（blocked-by T133/134/136）
- [T147 CONTEXT/api-surface 增补](tickets/T147-context-api-addendum.md)
- [T148 配置元数据增补](tickets/T148-config-metadata-2.md)（blocked-by T134）
- [T149 里程碑 verify](tickets/T149-milestone-verify.md)（blocked-by T132–T148）
- [T150 收口](tickets/T150-effort8-closing.md)（blocked-by T149）
