# Spec 116 — skill_search 遥测（effort #78）

> wayfinder map：`.wayfinder78/MAP.md`（T417–T418）。

## Problem Statement

技能检索（spec 73）零指标：命中率（技能可发现性——模型能不能找到该用的技能）、
语义提示救回率不可观测；命名/描述与实际问法脱节只能靠人工抽检。

## Solution

SkillSearchTool 三出口计数 `buzhou.skills.search`（tag outcome 有界三值）：
- `hit`——子串命中清单；
- `miss`——无匹配且无 ranker（纯失败）；
- `miss-semantic`——零子串命中但语义近邻提示救回。
返回文案与排序行为零变化。

## User Stories

1. 作为技能作者，我要 miss-semantic 率，所以命名/描述与问法脱节可量化修正。

## Testing Decisions

- 命中 + 语义救回两态各计一次；无 ranker 纯 miss 恰一次。

## Out of Scope

- 检索 timer；零结果 query 榜。

## Further Notes

- 与 spec 110 组成技能双遥测：注入面（目录）+ 检索面（search）。
