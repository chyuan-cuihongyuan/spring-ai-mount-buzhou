---
id: T3143
title: 就绪等待门的形状裁决
type: task
status: closed
assignee: zcode-p
blocked-by: []
created: 2026-09-17
---

## Question

依赖未就绪时的请求姿态怎么有预算地中间态化？（spec 2021 / effort #2021 / R22）

## Resolution

**gRPC wait_for_ready 非阻塞询问门 `WaitForReadyGate`
（core/concurrent）**：tryAcquire 四态（PASS/QUEUED 预算内排队/
FAIL_FAST 立即失败/QUEUE_FULL 预算满拒绝防积压）+setReady(true)
批量排空（drained+drainBatches 计数）+再失就绪重计+同值 no-op+
stats 四计数排空面——冷启动请求不弹掉且积压有界。
