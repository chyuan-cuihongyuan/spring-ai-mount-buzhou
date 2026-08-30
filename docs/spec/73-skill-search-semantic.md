# Spec 73 — skill_search 语义面（effort #33）

> wayfinder map：`.wayfinder33/MAP.md`（T297–T298）。复用 #19 SemanticSkillRanker。

## Problem Statement

目录注入有了语义排序（spec 59），但检索工具没有：命中集按注册序展示（相关技能可能
沉底）；零命中只有「换关键词」文案——模型无处继续。

## Solution

共享同一 ranker：命中集按 query cosine 排序（相关在前，20 条上限内保最相关）；零
子串命中给语义最近 3 条提示。无 ranker（默认关）行为与历史完全一致；零新键。

## User Stories

1. 作为模型，我要检索命中相关在前，所以 20 条上限内先看到最可能要用的技能。
2. 作为模型，我要零命中时有近邻提示，所以能直接 load_skill 而非放弃。
3. 作为既有用户，我要默认关零变化，所以升级零风险。

## Implementation Decisions

- SkillSearchTool 3 参构造（ranker 可空）；与目录注入共享实例（向量缓存互通）。

## Testing Decisions

- 命中排序断言（stub 向量）；零命中近邻；无 ranker 回归。

## Out of Scope

- 阈值化；混合加权；正文检索；新键。

## Further Notes

- 近邻无相似度阈值——排序质量归嵌入模型（诚实边界同 spec 59）。
