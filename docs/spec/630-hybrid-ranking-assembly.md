# 630 — 混合排序装配

> 来源：F 会话第 31 轮 = effort #600（spec 605 原语装配扩散；含 SkillRanker 接口抽取）/ [T910](../../.wayfinder/tickets/T910-hybrid-assembly-shape.md) / [T911](../../.wayfinder/tickets/T911-hybrid-assembly-verify.md) / impl 483。

## 背景

Hybrid/Lexical ranker（605）无公共接口无装配面——精确词检索增强无法经 yml 启用。

## 目标

`SkillRanker` 接口（双实现互换）+ `hybrid-ranking.{enabled,lexical-weight}` yml 装配。

## 非目标

- 不改 605 融合语义。

## 设计

渲染器/检索工具参数面改接口（源兼容）；hybrid 需 EmbeddingModel fail-fast；与 semantic 同开 hybrid 胜。

## 测试

3 用例：fail-fast / 缺省 / yml 装配。

## 兼容性

构造参数具体类→接口（源兼容；二进制窄改 0.x 语义内）；快照随轮再生。
