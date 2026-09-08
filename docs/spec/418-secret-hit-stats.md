# Spec 418 — 秘密命中统计与导出（effort #418）

> wayfinder map：`.wayfinder/maps/effort-418.md`（T727–T728）。D 会话第 19 轮。

## Problem Statement

#400 密钥扫描只打指标计数：命中明细无统计面无导出面——泄漏面趋势
（哪个类型/哪条缝常出）不可分析；PII 有分侧统计与 JSONL 先例（86/313），
秘密面缺席。

## Solution

`guard.secret` 扩散（PiiHitStats 同构镜像）：

- **`SecretHitStats`**：全局持有（install/global/reset）；三侧枚举
  INPUT / OUTBOUND / OUTPUT（对应 hook 三缝）；`record(type, side)` 计数；
  `snapshot()` → 排序报表行（name=type 名，count，side 计数表）。
- **`SecretScanHook`** 三缝记账（构造零改动——直查 global）。
- **`SecretHitStatsJsonl`**：`appendSnapshot(Path)` 把当前 snapshot 行
  追加落盘（{at, name, side, count}；父目录自动创建；打开/写失败上抛
  ——导出是显式动作该红）。宿主用定时任务周期落盘（snapshot 不清零——
  趋势对比用两次快照差）。

## User Stories

1. 作为安全负责人，我想按类型×缝看命中分布，so 哪类凭据管理最松、
   哪条缝泄漏最多可分析。
2. 作为宿主，我想快照落盘 JSONL，so 事后审计与趋势对比有数据。

## Implementation Decisions

- 追加快照（不清零）——与 PiiHitStatsJsonl 同族口径。
- 库级不装配（宿主显式调用——与 PII 同）。

## Testing Decisions

- record 三侧计数 + snapshot 排序行；hook 三缝真实记账；
  JSONL 追加两轮快照行数正确。

## Out of Scope

- 周期落盘装配；面板端点；热路径预算。

## Further Notes

- 新公共类型 `SecretHitStats` / `SecretHitStatsJsonl` 随轮 regenerate 快照。
