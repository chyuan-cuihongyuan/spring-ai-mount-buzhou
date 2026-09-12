# 704 — 最小可用水位闸（归档 PDB）

> 来源：G 会话第 5 轮 = effort #704（借鉴 k8s PodDisruptionBudget）/ [T959](../../.wayfinder/tickets/T959-session-pdb-shape.md) / [T960](../../.wayfinder/tickets/T960-session-pdb-verify.md) / impl 507。

## 背景

会话归档/空闲压缩（spec 310/58）在故障或流量高峰期仍照常摘会话——在线容量被运维动作进一步削薄，与「先保容量、再谈治理」的运维常识相悖。K8s 的洞察：自愿驱逐（drain/evict）必须尊重 PodDisruptionBudget——低于 minAvailable 的驱逐直接拒绝。

## 目标

- `SessionAvailabilityFloor`（core/cleanup）：`minAvailable` + `IntSupplier liveSessions`；`allowsArchive()` = 存活数 > minAvailable；供应商返回负数（未知）= **fail-open 放行**（保底闸失明时不误伤运维动作）。
- 挂点 = `SessionArchiver.archive()`（自愿驱逐唯一入口）：floor 非空且不放行 → 计数 `buzhou.archive.pdb-rejected` + 返回 false；null（默认两参构造）逐字节不变。
- restore / purgeExpired 不受闸（还原是恢复容量；purge 是冷层 TTL 治理）。
- 装配接线（liveSessions ← SessionIndexStore 计数）归本会话装配轮（spec 738 计划位）。

## 非目标

不做 spawn 准入联动（SpawnAdmissionFloor 是优先级语义，正交不混）；不做动态 minAvailable。

## 测试

水位恰在 floor 拒绝 + 计数 + 归档未发生；之上放行真归档；供应商未知 fail-open；null floor 零回归；restore 不受闸。

## 兼容性

opt-in 纯增量；默认构造逐字节不变。
