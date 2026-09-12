---
id: T956
title: 模型端点慢启动权重爬坡的验证
type: task
status: closed
assignee: zcode-g
blocked-by: T955
created: 2026-09-13
---

## Question

ramp 真爬坡（floor 起步、逐步到位、终点恰 target）？ramps() 快照正确？热重载上调走 ramp、下调瞬时？默认 null 零回归？

## Resolution

**用户常设授权 AFK（可推翻）**

验证（G 会话第 3 轮）：① ramp 后 routes() 立即 floor；手动 tick() 逐步递增、STEPS 次后恰 target 且 ramps() 清空；② 进行中 ramp(route, 更高 target) 重算且仍收敛新 target；③ 热重载 slowStart 开启：权重 1→8 上调触发 ramp（routes() 变 floor 而非 8）；下调 8→2 瞬时到位；④ slowStart=null 既有 RoutingWeightsHotReload 用例零回归；⑤ close() 后调度器终止。`mvn -pl buzhou-resilience -am test` 全绿。
