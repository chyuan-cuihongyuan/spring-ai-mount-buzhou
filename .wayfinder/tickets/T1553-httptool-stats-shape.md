---
id: T1553
title: http_request 请求量水位与结果分布读面（HttpToolStats）的形状裁决
type: task
status: closed
assignee: zcode-j
blocked-by: T1551
created: 2026-09-14
---

## Question

J 会话第 49 轮：tools/http 域请求侧的读面增量选什么形状？

## Resolution

**用户常设授权 AFK（可推翻）**

选题（http 域第三轴收官）：HttpRequestTool.call() 的全部拒绝路径——不支持 method / 非法 URL 与非 http(s) scheme / SSRF 拒绝 / timeoutSeconds 越界 / 响应体超 8MB / 异常兜底（网络失败·超时）——当前全部静默返回字符串，请求成功率与拒绝原因分布不可见。借鉴：Envoy upstream statistics（请求按结局分桶 rq_success/rq_error，circuit breaking 与健康洞察的第一信号）。

形状裁决：`HttpRequestTool` 内静态 `AtomicLong` 八计数——attempts（入口）/ successes（拿到 HTTP 状态码返回）/ methodRejects / urlRejects（非法 URL 与非法 scheme 合并语法桶）/ ssrfRejects（守卫拒绝）/ timeoutParamRejects（越界）/ oversizeRejects（响应超限）/ failures（catch 兜底）；嵌套 `record HttpToolStats`（totalRejects 派生）+ `stats()` + `resetForTest()`。守恒 `attempts = successes + totalRejects()`。静态面理由同域内先例（R46–R48）。call() 返回语义逐位不变。

Out of scope：按 URL/host 分桶（敏感面红线）；状态码分布直方图（2xx/4xx/5xx 细分留后续轮——当前 successes 单桶已回答"送达率"）。
