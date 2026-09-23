---
id: T6006
title: R 会话 R3 Space-Saving 频繁项的验证裁决
type: task
status: closed
assignee: zcode-r
blocked-by: [T6005]
created: 2026-09-23
---

## Question

R3 合同怎么逐一验绿？（spec 4002 / effort #4002 / R3）

## Resolution

**验证通过**：SpaceSavingTopKTest 四测全绿——少容量三键精确
（7/3/1 直读 + 降序榜 + 守恒 11 + minCount 1）；k=2 淘汰继承链
（a=5 精确幸存/e=4 ≥ 真值 1/b 读零/minCount 误差累积 4/守恒 9）；
搅局流热键三席全保恒在榜精确；畸形三型 fail-fast（capacity 0、
observe/estimate null）。
