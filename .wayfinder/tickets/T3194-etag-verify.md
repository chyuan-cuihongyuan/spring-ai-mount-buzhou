---
id: T3194
title: ETag 条件请求匹配的验证裁决
type: task
status: closed
assignee: zcode-p
blocked-by: [T3193]
created: 2026-09-17
---

## Question

EntityTagMatcher 合同（强弱语义/304/412/无头/畸形）怎么钉住？（spec 2046 / effort #2046 / R47）

## Resolution

**七用例一次全绿**（buzhou-core）：强比较真/W 弱不配强/异假 / 弱比较
剥前缀真×2 异假 / 304 判定精确+通配+列表弱等命中与未命中 / If-Match
强门满足+通配+W 不满足→412+已改拒 / 无头 null 与空白恒 false / 逗号
列表空格容忍 / 畸形四型（null ×4 入口）fail-fast。
