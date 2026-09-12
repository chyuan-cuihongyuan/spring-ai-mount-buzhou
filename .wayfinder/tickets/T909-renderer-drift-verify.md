---
id: T909
title: 渲染节拍巡查验证口径
type: task
status: closed
assignee: zcode-f
blocked-by: T908
created: 2026-09-12
---

## Question

巡查语义如何钉住？

## Resolution

**用户常设授权 AFK（可推翻）**

验证口径（RendererDriftWatchTest 2/2 + skills 全模块零回归；真实渲染路径——绑定索引+registry 替身）：

- 两轮渲染建基线静默 → 改描述再渲染即漂移事件（changed=[audit]）。
- 无 watcher 构造正常渲染零变化。
