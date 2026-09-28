---
id: U7226
title: U 会话 U13 ConvexHull 凸包的验证裁决
type: task
status: closed
assignee: zcode-u
blocked-by: [U7225]
created: 2026-09-29
---

## Question

U13 合同怎么逐一验绿？（spec 7012 / effort #7012 / U13）

## Resolution

**验证通过**：四测全绿——方形/菱形/共线手锚；200 随机
点集凸性+包含性质；幂等/退化；越域 fail-fast。
