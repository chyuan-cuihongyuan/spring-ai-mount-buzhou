# 1607 · RollingJsonlWriter 锁迁移（spec 1606 排队项）

> 来源：N 会话 R8（effort #1607 / T2365–T2366 / impl 1160）。spec 1606 审计高危 #2
> 落地：事件级磁盘追加 + 每行 flush + 轮转 gzip 全在 monitor 内——JDK21 虚拟线程
> pinning。修复：monitor → ReentrantLock（j.u.c 锁阻塞时 unmount 虚拟线程而非钉住
> 载体；互斥语义零变——HarnessToolCallingManager 同款迁移先例）。

## Problem Statement

观测明细 JSONL（Webhook/审计/导出族）由虚拟线程路径调用（webhook dispatcher、
会话收尾等），appendLine 的 monitor 内含 write+newLine+flush（每行一次 syscall）
与轮转（close+shift+gzip 可达秒级）——载体线程被钉住整个 IO 时长。

## Solution

`appendLine` / `close` / `bytesWritten` 三方法的 synchronized → 单一
`ReentrantLock` + try/finally。互斥语义不变（行完整性/记账原子性同前）；
阻塞等锁的虚拟线程 unmount（弹性保持）。

## User Stories

1. 作为运维者，我想让观测写入不钉住载体线程，所以虚拟线程池在写盘高峰仍有弹性。
2. 作为开发者，我想锁迁移零语义变化，所以既有测试全部零改动通过。

## Testing Decisions

- 新 `RollingJsonlWriterConcurrencyTest`：8 虚拟线程 × 50 行并发追加——
  计数守恒（400 行零丢失零重复）+ 行完整性（无撕裂）。
- 回归：RollingJsonlWriterTest / GzipTest 全量零变化。

## Out of Scope

- 写路径的专用写线程 + 有界队列（背压面改造——若 flush 成为吞吐瓶颈再立项）。
- DiskSpillStore 同款迁移（下一排队项）。
