# Spec 309 — 影子对照明细 JSONL 导出（effort #309）

> wayfinder map：`.wayfinder309/MAP.md`（T609–T610）。借鉴：W&B lineage
> （spec 171 同族——对照数据落盘可溯；fog 227「影子读对照明细 JSONL 导出」项）。

## Problem Statement

影子对照明细（primary vs shadow 结果、耗时、token）只在 `shadow.compared`
事件流过路：离线对账（分歧样本复盘、影子模型回归分析）需要宿主自接监听
自写文件——明细导出面缺位。

## Solution

`buzhou.resilience.shadow.detail-path`（文件路径）声明即装配：

- 全局 `SessionEventListener`（resilience 模块贡献 bean）过滤
  `shadow.compared` 事件，逐条追加 JSONL：`at / primaryMs / shadowMs /
  deltaMs / tokens / primary / shadow`（结果节选封顶 1024 字符）。
- 每行 flush（tail -f 可观察）；IO 失败吞 + 计数（旁路语义不放大）；
  容器关闭关闭文件句柄。
- 未配置 detail-path = 不装配（零变化）。

## User Stories

1. 作为算法工程师，JSONL 明细离线 diff 分歧样本——影子模型升级有据可依。
2. 作为运维，tail -f 明细文件实时观察影子分歧率。

## Implementation Decisions

- 消费既有事件流（事件即契约——core 零改动）。

## Testing Decisions

- `ShadowJsonlExportTest`：行格式断言（Jackson 读回）；节选封顶；非目标
  事件不落盘；IO 失败（不可写路径）不抛、计数。

## Out of Scope

- 全事件 JSONL 化与合流打包（导出族轮）。

## Further Notes

- 影子族：采样旁路（49）/ 池预算（shadow.maxConcurrent/dailyBudget）/
  **明细导出（本轮）**。
