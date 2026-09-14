---
id: T1577
title: Dashboard HTTP 状态分布读面（DashboardHttpStats）的形状裁决
type: task
status: closed
assignee: zcode-j
blocked-by: T1575
created: 2026-09-15
---

## Question

J 会话第 61 轮：dashboard 域的读面增量选什么形状？

## Resolution

**用户常设授权 AFK（可推翻）**

选题（跨模块轮换入 observe-dashboard 域，J 系首次）：DashboardHttpServer.route() 按异常类型分派七个状态码结局（200/400/401/404/413/500/501），全部只进日志或响应体——**HTTP 状态分布不可见**（401 高发=探测扫描、500 高发=内部缺陷、404 高发=客户端版本错位）。nginx status zone（按状态码分桶对账）思想。

形状裁决：`DashboardHttpServer`（internal 包——不属公共 API 面，静态计数无兼容承诺负担）内静态 `AtomicLong` 八计数——requests（入口）/ ok（dispatch 正常完成）/ authRejects（401）/ badRequests（IllegalArgument 与 JsonProcessing 合并 400 桶）/ notFounds（404）/ tooLarges（413）/ unimplemented（501）/ serverErrors（500）；嵌套 `record DashboardHttpStats` + `stats()` + `resetForTest()`。守恒 `requests = ok + 七结局桶之和`（每请求恰落一桶）。route() 响应行为逐位不变。

Out of scope：按 path 分桶（URL 含会话标识敏感面）；时延直方图（另轴）。
