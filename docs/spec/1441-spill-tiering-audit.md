# 1441 — Spill 冷热分层访问审计

> 来源：L 会话第 41 轮 = effort #1441（票 T2183 / T2184 / impl 1092）。借鉴：MinIO tiering / S3 lifecycle（访问频率决定分层——热数据留快层、冷数据下沉清理）。

## Problem Statement

`ReadAuditTrail`（窗口 128 读事件）记录了「谁被读过」，但**冷热分层水位**无审计面：多少 spill handle 从未被回读（写完即死——TTL/清理可以更激进）、多少是多次回读热点（值得预热）——纯单次写读模式的占比决定 spill 容量策略。

## 目标

- `SpillTieringAudit`（spill，纯函数静态面，private 构造）：
  - `analyze(totalHandleUris, List<ReadRecord>)` → `record TieringReport(totalHandles, neverReadCount, singleReadCount, multiReadCount)`；
  - 精确口径：never = 存量全集 − 读事件覆盖过的 distinct uri（trail 窗口含已删 uri 的历史读时钳 0 不为负）；
  - 热点阈值 `HOT_READ_THRESHOLD=2`（≥2 次回读记热点）；派生 `hotRatio()`/`coldRatio()`（空库 -1 哨兵）；
  - 只读不裁决（分层动作归宿主）。
- 与 843（证据引用失效率）/815（写放大）辨义：那两轴是引用有效性与写侧放大，本轴是**读侧访问频率分层**。

## 兼容性

纯函数零 IO；不触 DiskSpillStore/ReadAuditTrail 状态。

## Out of Scope

- 自动分层执行（读面不裁决）。
- 时间衰减热度（窗口内等权——口径显式）。
- 按会话分桶（基数红线）。
