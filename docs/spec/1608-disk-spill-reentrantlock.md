# 1608 · DiskSpillStore 锁迁移（spec 1606 排队项）

> 来源：N 会话 R9（effort #1608 / T2367–T2368 / impl 1161）。spec 1606 审计高危 #3
> 落地：store() 的 MB 级写盘（writeAtomically ×2 + 配额 walk）与 usage() 的全树
> Files.walk 在 monitor 内。修复与 spec 1607 同款：monitor → ReentrantLock，
> 互斥语义零变（虚拟线程 unmount 不 pin）。

## Problem Statement

spill 写路径由工具执行虚拟线程触发（afterTool offload）；store 的 monitor 内含
存在性检查 + 配额 walk + 双文件原子写（MB 级正文）——载体线程被钉住整个写盘时长，
usage 的全树 walk 同理。

## Solution

`store` / `usage` 两方法的 synchronized → 单一 `ReentrantLock` + try/finally。
「一次调用一次 spill」的存在性互斥与配额计量互斥不变。

## User Stories

1. 作为运维者，我想让 spill 写盘不钉住载体线程，所以工具执行虚拟线程在 spill 高峰仍有弹性。
2. 作为开发者，我想并发语义钉住，所以同 uri 并发 store 恰一成功的测试固化。

## Testing Decisions

- 新 `DiskSpillStoreConcurrencyTest`：同 uri 6 虚拟线程并发 store——恰 1 成功
  5 拒绝（IllegalStateException "Spill already exists"）；异 uri 8 并发全成功 +
  usage().entryCount() 守恒。
- 回归：spill 模块全量（168 用例）零变化。

## Out of Scope

- store 与 usage 的锁拆分（当前共用一把——写路径低频，拆分无收益）。
- 配额 walk 的缓存化（若成为吞吐瓶颈再立项）。
