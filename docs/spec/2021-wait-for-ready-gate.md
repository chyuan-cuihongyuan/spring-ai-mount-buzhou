# Spec 2021 — 就绪等待门（effort #2021，R22）

> wayfinder map：`.wayfinder/maps/effort-2000.md`（T3143–T3144，impl 1572）。
> 借鉴：gRPC wait_for_ready——依赖未就绪时排队等待 vs 立即 fail-fast。

## Problem Statement

依赖（MCP 建连 / 模型端点 / 工具装载）未就绪时调用方只有一种姿态：
立即失败。冷启动窗口的请求被白白弹掉；而无限排队又会积压成灾——
「等一等」与「立刻失败」之间缺一个有预算的中间态。

## Solution

`WaitForReadyGate`（core/concurrent，非阻塞询问式——Outcome 即指令）：

- `tryAcquire(waitForReady)` 四态：就绪 → PASS；未就绪且等待姿态且
  队列预算内 → QUEUED（深度 +1）；预算满 → QUEUE_FULL（防无限积压）；
  fail-fast 姿态 → FAIL_FAST；
- `setReady(true)` **批量排空**队列（drained + drainBatches 计数——
  在队请求全放行）；setReady(false) 清零深度（新队重计，排空账不
  重复）；同值翻转 no-op；
- 读数：queueDepth（积压水位）/ isReady / stats()（passes/queued/
  failFasts/queueFullRejections/drained/drainBatches）；
- 契约：maxQueued ≥ 0（0 = 禁排队）fail-fast。

## User Stories

1. 作为调用方，wait_for_ready=true 的请求在冷启动窗口排队不弹掉；
   预算满改走换道——积压有界。
2. 作为 SRE，drainBatches 与 queueFullRejections 对账——排队纪律
   （放行率 vs 拒绝率）显形。

## Testing Decisions

- 双姿态两态；就绪直通；预算满拒绝（深度不超预算）；零预算禁排；
  批量排空（3 入队 1 批次）；再失就绪重计（排空账不重复）；同值
  no-op；畸形 fail-fast。

## Out of Scope

- 不做真实阻塞/唤醒（询问式——动作归调用方）；不接 MCP 建连链
  （接线归后续轮）。

## Further Notes

- 与 MaintenanceCordon（维护门）正交：cordon 拒新请求，本件管依赖
  就绪前的请求姿态。
