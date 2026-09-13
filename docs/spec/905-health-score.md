# 905 — 健康加权评分读面

> 来源：I 会话第 6 轮 = effort #905（[T1261](../../.wayfinder/tickets/T1261-health-score-shape.md) / [T1262](../../.wayfinder/tickets/T1262-health-score-verify.md) / impl 658）。借鉴：K8s probe aggregate / HTTP health score 面板直觉——多探针聚合成单一分数+分档，告警与 dashboard 一读即知。

## Problem Statement

`/actuator/buzhou` 逐机制罗列 status（16+ 机制），无聚合视图：值班要人眼扫全部条目才能回答"系统现在整体几分"。K8s probe aggregate 的直觉是探针集 → 单一分数 + 分档（healthy/degraded/unhealthy），dashboard 一格、告警一条。

## 目标

- 新公共纯函数类 `BuzhouHealthScore`（core.health）：
  - 评分口径（与 `BuzhouHealth` 严格 DOWN 语义对齐）：UP=100、UNKNOWN=50（未启用中性）、DOWN=0；总分=算术平均取整（0–100）；
  - 分档常量：`HEALTHY_FLOOR=90`、`DEGRADED_FLOOR=70`（static final）——`score ≥ 90` healthy、`≥ 70` degraded、否则 unhealthy；
  - 返回公共 record `ScoreReport(int score, String tier, int upCount, int downCount, int unknownCount, List<String> downMechanisms)`；
  - 空机制清单约定 100/healthy（无机制=无故障，与空集合法状态纪律一致）；
- 纯函数不触 store、不改 `BuzhouHealthEndpoint`（端点接线留装配轮）。

## 兼容性

纯增量新类型，零既有行为变化。
