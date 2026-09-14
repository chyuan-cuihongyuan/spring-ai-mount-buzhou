---
id: T1587
title: Deno 沙箱探测读面（DenoProbeStats）的形状裁决
type: task
status: closed
assignee: zcode-j
blocked-by: T1585
created: 2026-09-15
---

## Question

J 会话第 66 轮：guard/sandbox 域的读面增量选什么形状？

## Resolution

**用户常设授权 AFK（可推翻）**

选题：DenoSandbox.available()（impl-40 TTL 探测缓存）三分支全静默——缓存命中、重探测成功、重探测失败。「run_command 沙箱档为什么不可用/为何每次都在探测」（probeTtl 误配 0 或探测持续失败）不可见。Envoy health check 统计思想（健康检查本身的成败分布是第一信号）。

形状裁决：`DenoSandbox` 内静态 `AtomicLong` 五计数双守恒——availableCalls（入口）/ probeCacheHits（TTL 内缓存命中短路径）/ probes（重探测执行）/ probeSuccesses / probeUnavailables（探测命令不成功或异常）；守恒 **availableCalls = probeCacheHits + probes** 且 **probes = probeSuccesses + probeUnavailables**。静态面理由同族先例（进程级探测面跨实例聚合）。available()/invalidateProbeCache 行为逐位不变。

Out of scope：探测耗时直方图（5s 超时上界既有）；per-binary 分桶（binary 名配置面）。
