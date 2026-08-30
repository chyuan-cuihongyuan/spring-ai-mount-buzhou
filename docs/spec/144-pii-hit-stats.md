# Spec 144 — PII 命中统计（effort #117）

> wayfinder map：`.wayfinder117/MAP.md`（T469–T470）。借鉴：Presidio
> anonymizer 统计口径（合规面以「哪类敏感数据最常出现」说话）。

## Problem Statement

PII 脱敏钩子有 per-type 的 metrics 计数（tag 有界），但没有可查询的进程内
排行面；自定义规则（spec 118）命中完全零计数——「自定义规则值不值」与
「哪类 PII 是大头」都答不上来。

## Solution

`guard/pii/PiiHitStats`：内置 `PiiType` + 自定义规则名统一命中的进程内有界
计数表。`PiiRedactionHook` 双点接线：内置命中随既有 matches 循环打点；自定义
命中从脱敏产物的 `[PII:NAME]` 占位符提取（内置类型名剔除防双计）。`top(n)`
排行（count 降序 + 名字典序稳定）+ `countOf` + `reset()` 窗口清零（export →
reset 循环）。自定义名封顶 64 折 `__overflow__`（内置枚举不受封顶挤占）。

## User Stories

1. 作为合规审计员，我按窗口导出命中排行，所以脱敏策略调优（类型开关/规则
   维护）有数据依据。
2. 作为安全工程师，自定义规则的命中数可查，所以「这条规则抓没抓到东西」
   不再是盲区。

## Testing Decisions

- 红队：内置+自定义统一排行稳定序；自定义名封顶折入且内置不受挤占 + 既有
  继续细分；reset 窗口清零；recordAll 批量 + 参数 fail-fast。guard 模块全量
  回归兜钩子接线面。

## Out of Scope

- 输入侧钩子接线（下轮）；报表 JSONL 导出；命中样本留存（只有计数——原文
  不落表是纪律）；跨实例聚合。

## Further Notes

- 与 spec 118（自定义规则）/spec 86（PII 三件套）组合成完整合规观测面。
