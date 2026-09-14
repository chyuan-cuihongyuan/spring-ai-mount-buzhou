---
id: T2420
title: R35 dashboard gzip 的验证裁决
type: task
status: closed
assignee: zcode-n
blocked-by: T2419
created: 2026-09-15
---

## Question

N 会话第 35 轮：如何验收？

## Resolution

DashboardGzipTest 两断言（真实 HttpServer）：80 session 列表（15KB）协商 gzip
→ Content-Encoding=gzip + GZIPInputStream 解压回含数据的 JSON 且解压体大于
压缩体；无 Accept-Encoding 恒明文无 Content-Encoding。dashboard 34 用例零回归。
