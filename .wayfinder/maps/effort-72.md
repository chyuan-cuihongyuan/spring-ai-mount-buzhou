# Wayfinder Map — Buzhou 技能目录注入遥测（effort #72，50 轮自迭代第 37 轮）

> effort #72，延续 #71（T401–T402 / impl-256）。主线：技能面零指标——目录是否
> 在注入、注入是否频繁被预算截断（catalog-max-entries 过小的信号）不可观测。

## Destination

SkillCatalogRendererImpl.renderEntries 双 counter（BuzhouMetricsHolder）：
`buzhou.skills.catalog-injected`（每成功注入 +1）+
`buzhou.skills.catalog-overflow`（tag outcome=truncated|fit 两值——截断频率是
预算调优信号）；空目录零计数（无注入无信号）。注入行为零变化。

## Notes

- 借鉴：Micrometer 有界 tag 纪律（LangSmith skill-usage 观测面的最小内核）。

## Decisions so far

- 计数点在 renderEntries（两条注入路径——注册序/语义排序——同源计数）。

## Not yet specified

- per-skill 选择计数（技能名不可进 tag——进程内表另议）；skill_search 命中率指标。

## Out of scope

- 沿用 #7–#71。

## Tickets

- [x] [T403 目录注入双 counter](../tickets/T405-catalog-telemetry.md)（impl-257）
- [x] [T404 2 例红队（truncated/fit 双态 + 空目录零计数）+ 收口](../tickets/T406-telemetry-close.md)
