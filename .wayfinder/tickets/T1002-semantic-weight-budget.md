---
id: T1002
title: 语义缓存权重预算驱逐的裁决
type: task
status: closed
assignee: zcode-g
blocked-by:
created: 2026-09-12
---

## Question

语义缓存按条数驱逐——大响应与小 FAQ 同权，长文几次注入就能挤出整批高频短条目；内存也无上界口径。加权重预算吗？

## Resolution

**用户常设授权 AFK（可推翻）**

决策（G 会话第 2 轮 = effort #701 / spec 701 / impl 601）：加 `maxWeightChars`（默认 0=关）——条目权重=响应字符数；put 后腾挪至预算内（eldest 先出）；`weightEvictions()` 独立口径（不混 evictedCount）；超预算单条拒存同计；maxEntries 保留硬顶。Caffeine weigher 思想。record 5 组件规范构造+@ConstructorBinding，4 参兼容零破坏；yml `semantic-cache.max-weight-chars`+metadata 登记。
