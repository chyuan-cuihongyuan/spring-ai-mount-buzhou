---
id: T2377
title: R14 工具目录漂移看门狗接线的形状裁决
type: task
status: closed
assignee: zcode-n
blocked-by: T2376
created: 2026-09-15
---

## Question

N 会话第 14 轮：spec 201 看门狗接线节拍选哪拍——会话构造还是定时调度？

## Resolution

选 **会话构造节拍**（skills 版渲染节拍同思路——零调度）。HarnessAssembler 组装
完最终目录（autoTools+extraTools+包装后）即拍指纹：基线进程级（Holder 模式）跨
会话收敛；包装层不改 ToolDefinition 不误报。定时调度留给低频会话场景的后续增强。
