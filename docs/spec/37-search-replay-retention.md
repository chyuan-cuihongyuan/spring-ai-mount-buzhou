# Spec 37 — 技能检索 / 死信重放 / 索引保留

> effort #8（T132–T134 / impl-105–107）。

## §A skill_search 检索工具（T132 / impl-105）

- skills 模块 `SkillSearchTool`（ToolCallback 直实现，LoadSkillTool 同款）：query 子串按
  名称/描述**不分大小写**匹配；数据源 = `SkillRegistry.listAllFor`（**不截断**可见全集——
  不受 catalog-max-entries 注入上限限制）；命中上限 20 条 + `load_skill(name)` 指引；
  无命中给可操作提示（换关键词/查绑定）。
- 绑定可见性沿用 `BindingVisibility`（会话不可见技能不出结果）；
  SkillModule.configure() 自动注册（与 load_skill 同列）。

## §B webhook 死信重放（T133 / impl-106）

- `WebhookEventForwarder.replayDeadLetters()`：死信迁回 outbox（attempts 清零、seq 续新、
  立即可投递）并 nudge 分发器；返回重放条数（outbox 容量满则部分重放）。
- 投递语义回到常规 at-least-once（可能再死信——消费端幂等键契约内）；损坏死信在重放时丢弃。
- runbook §2 处置更新：死信处置从「按 eventId 消费端补录」升级为一键重放。

## §C 索引 CLOSED 行保留（T134 / impl-107）

- `SessionIndexStore.purgeOlderThan(cutoff, limit)` default no-op；三实现覆写
  （内存遍历删 / JDBC 单语句 `DELETE … WHERE status<>'ACTIVE' AND last_active_at_ms<? LIMIT n` /
  Redis ZSET score 升序区段翻页删）。ACTIVE 永不扫；limit 截断（增量清扫）。
- **惰性触发**：SessionIndexObserver.onOpen 时 1/64 概率清扫、单次 ≤256 条（免热路径开销）；
  保留期 `buzhou.index.closed-retention`（默认 30d；-1/0 = 永久）经
  `SessionIndexObserver.configureRetention` 装配期注入。
