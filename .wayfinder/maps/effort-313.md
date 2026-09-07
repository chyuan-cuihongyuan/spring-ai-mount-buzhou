# Wayfinder Map — Buzhou PII 命中分侧（effort #313，C 会话第 14 轮）

> C 会话第 14 轮。PiiHitStats（144/164）只记总量——「哪类 PII 出现在<b>用户
> 输入</b> vs <b>工具输出</b>」是两个不同策略面（输入侧重预防提示、输出侧
> 重脱敏规则调优），混在一列里调不出分别的动作（fog 152「PII 命中分侧列
> 拆分」项）。

## Destination

`PiiHitStats.Side{INPUT,OUTPUT,UNSPECIFIED}` 维度：双钩各记各侧、topBySide
分侧排行、countOf(name, side) 点查、JSONL 导出补 input/output 两列；既有
总量口径与 API 逐位兼容（1 参 record = UNSPECIFIED）。

## Notes

- 号段：spec 313 / T617–T618 / impl-336。
- 借鉴：Presidio anonymizer 统计（144 原始来源的分侧深化）。

## Decisions so far

- 总量列保留（count 不变——既有报表零破坏）；分侧为增量列。
- UNSPECIFIED 兜底兼容旧调用方（未升级的宿主自定义打点）。

## Out of scope

- 会话级分组（合规报表按窗口聚合即可）；侧级 overflow 独立封顶（沿用名级封顶）。

## Tickets

- [x] [T617 Side 维度 + 双钩接线（输入/输出各记各侧）](../tickets/T617-pii-side.md)（impl-336）
- [x] [T618 分侧排行/点查/JSONL 列回归](../tickets/T618-pii-side-close.md)（impl-336）
