# 803 — 检索多路改写融合

> 来源：H 会话第 4 轮 = effort #803 / [T1107](../../.wayfinder/tickets/T1107-multi-query-retriever.md) / [T1108](../../.wayfinder/tickets/T1108-multi-query-retriever-verify.md) / impl 556。
> 借鉴：LangChain MultiQueryRetriever（≈105K star）。

## Problem

单一查询词形脆弱：换个说法/词序/同义词就漏召。既有融合面（HybridSkillRanker、RecallSearch HYBRID）融合的是单查询的文本×向量双信号——「查询本身只问了一个角度」无解。

## Solution

`MultiQueryRetriever`（memory.recall）：

- **变体展开**：`variants` 函数注入（原查询 → N 个改写；LLM/规则/词典皆可作生成器，测试用确定性函数）。封顶 8 个变体（防生成器失控）。
- **逐路检索**：每变体独立过 `base` 检索（(文本, limit) → 排名降序），单路故障隔离跳过。
- **跨变体 RRF 融合**：k=60（与 605/ES 同款口径），名次 r 贡献 `1/(60+r+1)` 累加；去重键=消息 id（多变体命中同一消息=分数累加，是奖励不是重复）。
- **口径留痕**：Result 带 variantsExecuted / uniqueHits；Hit.score=RRF 值、mode=multi-rrf。
- **fail-open**：生成器返回空/null/抛异常 → 回退单路原查询（检索可用性优先）。

## 兼容性

纯新增类（需显式组装 variants+base 两函数——默认装配零变化）；与 RecallSearch 组合使用（base 可为 RecallSearch::search 的适配）。

## 诚实边界

变体质量归生成器（不做内置同义词典）；RRF 值非语义相关度（跨变体可比、跨检索器不可比）；base 契约=列表序即名次；不缓存（记忆化族已有先例可叠加）。
