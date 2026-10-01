---
id: Y11040
title: Y 会话 20 BridgeFinder 桥检测 的验证裁决
type: task
status: closed
assignee: zcode-y
blocked-by: [Y11039]
created: 2026-10-01
---

## Question

20 合同怎么逐一验绿？

## Resolution

验证通过：七测全绿（链图全桥+三角零桥+双三角共点单桥+重边非桥+随机图暴力删边连通性交叉互证圣像+确定性+fail-fast）。
