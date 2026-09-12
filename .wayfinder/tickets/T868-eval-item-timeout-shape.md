---
id: T868
title: 评估项级超时预算的形态裁决
type: task
status: closed
assignee: zcode-f
blocked-by:
created: 2026-09-12
---

## Question

pytest-timeout 的价值：单个挂死测试不拖死整个套件。评估 run 同病——一个挂死项（provider 停滞/工具死锁）让整跑永不完成。形态怎么定？

## Resolution

**用户常设授权 AFK（可推翻）**

决策（F 会话第 10 轮 = effort #600 / spec 609 / impl 462）：

1. `EvalRunner.setPerItemTimeout(Duration)`——**默认 null = 不设（零行为变化）**；零/负拒绝。
2. 执行包装：项跑在独立虚拟线程、`future.get(timeout)`；超时 `shutdownNow` 传播中断（挂死项的可中断阻塞即刻中止、会话随中断关闭——与模型超时兜底同取舍）+ 收敛为该条 error（detail 带预算）+ 指标 `buzhou.eval.item.timeouts`。
3. 串行与并行路径同包装；run 必完成（挂死项不断批）。
