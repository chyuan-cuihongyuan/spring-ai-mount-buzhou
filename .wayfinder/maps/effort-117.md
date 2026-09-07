# Wayfinder Map — Buzhou PII 命中统计（effort #117，A 会话第 12 轮）

> A 侧编号策略沿用 #111 声明。借鉴 Presidio anonymizer 统计口径。

## Destination

合规报表的事实表：内置类型 + 自定义规则名统一命中排行——「哪类 PII 最常
出现」供脱敏策略调优与审计共用；自定义规则命中此前零计数（补盲）。

## Notes

- PiiRedactionHook 双点接线（内置 matches 循环 + 自定义占位符提取——内置
  类型名剔除防双计）；自定义名 64 封顶折 __overflow__（内置枚举不受挤占）；
  reset 窗口清零 export→reset 循环同纪律。

## Decisions so far

- [PiiHitStats](../tickets/T469-pii-hit-stats.md) — record/recordCustom/top/
  countOf/reset + global 旋钮。

## Not yet specified

- 输入侧钩子（PiiInputRedactionHook）同款接线；报表 JSONL 导出。

## Out of scope

- 跨实例聚合；命中样本留存（只有计数——原文不落表是纪律）。

## Tickets

- [x] [T469 PII 命中统计](../tickets/T469-pii-hit-stats.md)（impl-284）
- [x] [T470 收口提交](../tickets/T470-pii-hit-stats-close.md)（impl-284）
