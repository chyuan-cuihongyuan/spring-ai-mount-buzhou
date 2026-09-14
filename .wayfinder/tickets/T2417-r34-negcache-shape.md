---
id: T2417
title: R34 负缓存装配面的形状裁决
type: task
status: closed
assignee: zcode-n
blocked-by: T2416
created: 2026-09-15
---

## Question

N 会话第 34 轮：负缓存接装配链还是保持宿主 wrap？

## Resolution

选 **Holder 开关装配**。装饰器族「宿主 wrap」在多会话场景意味着每个宿主
重复装配代码；进程级开关（默认关）让启用成本一行，且未启用透传保持零开销。
包装在 CatalogDrift 快照前——负缓存不改 ToolDefinition，指纹不受影响。
