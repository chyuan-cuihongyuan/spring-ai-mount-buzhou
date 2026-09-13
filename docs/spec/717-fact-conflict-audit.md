# 717 — 共享事实冲突审计

> 来源：G 会话第 18 轮 = effort #717（410 共享事实的治理面）/ [T1034](../../.wayfinder/tickets/T1034-fact-conflict-audit.md) / [T1035](../../.wayfinder/tickets/T1035-fact-conflict-audit-verify.md) / impl 617。

## Problem

SharedFact 的所有权模型（非 owner 发布已存在键 fail-fast）守住单 store 写入，但**聚合场景**防不住：FactsExporter 导出的集合、跨实例备份合并、主从复制缝隙——同一个 key 带着不同 value 静默共存。下游注入提示词时读到哪个全凭顺序，事实库「精神分裂」。mem0/Zep 族的冲突治理是记忆库运维标配。

## Solution

- `FactConflictAudit`（core/fact，纯函数静态原语）：
  - `audit(List<SharedFact>)` → `Report(rows, keysScanned, conflictKeys, duplicateKeys)`；
  - 按键分组后逐键判定：
    - **CONFLICT**——同键 ≥2 个互异值（Objects.equals）：`Row(key, CONFLICT, entries)`，entries 列全 owner=value（裁决证据齐备）；
    - **DUPLICATE**——同键同值但多 owner：重复发布信号（冗余无害但暴露流程问题）；
  - rows 按 key 字典序；`conflictKeys`/`duplicateKeys` 计数。
- 纯读数：谁对谁错的合并裁决是业务语义——本面只把「精神分裂」变可见。

## User Stories

1. 备份合并后：audit(合并集) 发现 `user.language` 同时是 "zh"（owner=a1）与 "en"（owner=a2）——合并策略有据可依。
2. 健康巡检：DUPLICATE 持续增长 = 多实例重复发布同一事实——流程冗余信号。

## Implementation Decisions

- value 等价用 Objects.equals（值多为 JSON 解码产物——字符串/数字自然可比；对象语义等价由宿主投影后比较）。
- 不依赖 store SPI（调用方给快照 List——导出/合并产物直接可审）。
- entries 全列不截断（冲突键数量天然少；截断会丢裁决证据）。

## Testing Decisions

- 同键异值（双 owner）CONFLICT + entries 全列；同键同值异 owner DUPLICATE；健康集（键唯一）零发现；null 表 fail-fast；空表零发现。

## Out of Scope

- 自动合并/裁决。
- store 实时盯防（快照审计口径）。
- 数值近似等价（1 与 1.0 视为不同——保守口径）。

## Further Notes

与 711（序列审计）/712（状态分布）同主线：**fsck 族从资源层到数据层到语义层推进**。
