---
id: T989
title: 生效配置 diff 读面的形态裁决
type: task
status: closed
assignee: zcode-g
blocked-by:
created: 2026-09-13
---

## Question

生效配置快照端点（spec 343）只有全量视图——「这次热重载/部署到底改了什么」需要人肉对比两份快照。kubectl diff / git diff 语义怎么映射？

## Resolution

**用户常设授权 AFK（可推翻）**

决策（G 会话第 20 轮 = effort #719 / spec 719 / impl 522）：`ConfigDiff`（core/health 纯函数）——`static List<Entry> diff(before, after)`：Entry{key, before, after, kind}，kind ∈ ADDED（仅 after）/REMOVED（仅 before）/CHANGED（两有不同值）；按 key 字典序稳定输出。**掩码语义**：diff 消费方传入的是快照端点同源 Map（敏感值已 `***` 掩码）——掩码值相等 = 未变（诚实注记：掩码底变化不可见，欲见底需授权通道）；纯函数不做二次掩码（不猜哪些键敏感——上游已定）。null map 拒绝。借鉴 kubectl diff（声明的变化即清单）。
