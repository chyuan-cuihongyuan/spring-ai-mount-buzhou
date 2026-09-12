---
id: T959
title: 最小可用水位闸（归档 PDB）的形态裁决
type: task
status: closed
assignee: zcode-g
blocked-by:
created: 2026-09-13
---

## Question

会话归档/空闲压缩（spec 310/58）在故障或流量高峰期仍照常摘会话——在线容量被运维动作进一步削薄。K8s PodDisruptionBudget（自愿驱逐不低于 minAvailable 保底）怎么映射到归档入口？

## Resolution

**用户常设授权 AFK（可推翻）**

决策（G 会话第 5 轮 = effort #704 / spec 704 / impl 507）：新类 `SessionAvailabilityFloor`（core/cleanup）：`minAvailable` + `IntSupplier liveSessions`（装配面来自 SessionIndexStore 计数——装配轮接线）；`allowsArchive()` = 存活数 > minAvailable；供应商返回负数（未知）= **fail-open 放行**（与健康 UNKNOWN 诚实语义同款——保底闸失明时宁可放行也不误伤运维动作）。挂点 = `SessionArchiver.archive()`（自愿驱逐唯一入口）：floor 非空且不放行 → 计数 `buzhou.archive.pdb-rejected` + 返回 false（K8s evict API 429 同义）；null（默认两参构造）逐字节不变。restore/purge 不受闸（还原是恢复容量、purge 是冷层治理）。借鉴 k8s PodDisruptionBudget。
