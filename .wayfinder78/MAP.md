# Wayfinder Map — Buzhou skill_search 遥测（effort #78，50 轮自迭代第 43 轮）

> effort #78，延续 #77（T415–T416 / impl-262）。主线：spec 110 补了目录注入遥测，
> 检索面（skill_search）仍零指标——命中率是技能可发现性的直接信号（高
> miss-semantic 率 = 命名/描述与实际问法脱节）。

## Destination

SkillSearchTool 三出口计数 `buzhou.skills.search`（tag outcome 三值有界）：
hit（命中清单）/ miss（无匹配无 ranker）/ miss-semantic（零子串→语义近邻提示）。
返回文案与排序行为零变化。

## Notes

- 借鉴：搜索系统 hit-rate 惯例（LangSmith skill-usage 观测面同族）。

## Decisions so far

- miss-semantic 单列（不并入 miss——语义提示能救回一部分，两种 miss 语义不同）。

## Not yet specified

- per-query 慢检索（timer）；零结果 query 高频榜（进程内表另议）。

## Out of scope

- 沿用 #7–#77。

## Tickets

- [x] [T417 search 三态计数接线](tickets/T419-search-telemetry.md)（impl-263）
- [x] [T418 2 例红队（hit+miss-semantic / 纯 miss）+ 收口](tickets/T420-search-close.md)
