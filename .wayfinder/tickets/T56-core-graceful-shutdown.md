---
id: T56
title: core · 优雅停机与生命周期语义
type: grilling
status: closed
assignee: ""
blocked-by: [T55]
created: 2026-08-14
---

## Question

停机语义如何定？需裁决：① 每机制 SmartLifecycle bean 的 phase 排布（停机顺序）；② stop(Runnable) 排空契约与超时预算（对齐 spring.lifecycle.timeout-per-shutdown-phase 默认 30s？）；③ DefaultAgentRuntime 是否追踪 spawn 会话（停机时等待在途 Turn 完成到什么程度——CANCELMode 联动？）；④ stream() 取消后的 doFinally 收尾语义；⑤ executor shutdown+awaitTermination 的 drain 形状与 @Bean destroyMethod 双重触发防护；⑥ close() 路径的异常隔离（dispatchEvent 失败不得跳过 listeners.clear）。

## Resolution

**ratify 研究推荐（用户常设授权，可推翻）→ spec 13 §core-1**：每机制 SmartLifecycle bean；`BuzhouLifecyclePhases` 常量——core phase 最大（最先 stop：拒新 Turn → 对在途发 AFTER_CURRENT_TURN 取消 → 排空），memory/spill/guard 后停（只关后台任务/缓存）；stop(Runnable) 完成后必回调、容忍无 stop 直接 destroy；排空超时 `buzhou.lifecycle.timeout-per-shutdown-phase` 默认 30s；Runtime 追踪活跃会话；executor 走 shutdown+awaitTermination + 显式 destroyMethod 防双触发；stream() 补 doFinally 同路收尾；close/事件分发逐 listener try/catch 异常隔离。
