---
id: T5068
title: Q 会话 R34 2Q 缓存的验证裁决
type: task
status: closed
assignee: zcode-q
blocked-by: [T5067]
created: 2026-09-18
---

## Question

R34 合同怎么逐一验绿？（spec 3033 / effort #3033 / R34）

## Resolution

**验证通过**：TwoQueueCacheTest 六测全绿——命中/未命中/入口更新
即晋升、50 写容量恒 ≤4、二触晋升手迹（a 存活 b 亡）、扫描抗性
（主区 6..10 终存且 20 键扫描全打转 5 键全存活）、10 写入口逐出
恰 8 主区零、容量三路 fail-fast。首版 mainCapacity 与 size 耦合
写崩（final 初始化块依赖构造参数不成立）整文件重写教训入档。
