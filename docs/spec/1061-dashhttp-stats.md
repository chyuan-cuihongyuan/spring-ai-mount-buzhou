# 1061 — Dashboard HTTP 状态分布读面

> 来源：J 会话第 61 轮 = effort #1061（[T1577](../../.wayfinder/tickets/T1577-dashhttp-stats-shape.md) / [T1578](../../.wayfinder/tickets/T1578-dashhttp-stats-verify.md) / impl 813）。借鉴：nginx status zone（按状态码分桶对账是 HTTP 服务健康的第一读面）。J 会话首个 observe-dashboard 域轮。

## Problem Statement

`DashboardHttpServer.route()`（impl-48 鉴权硬化）按异常类型分派七个状态码结局，全部只进日志行或响应体：**HTTP 状态分布不可见**。宿主无法回答「dashboard 请求成功率多少、401 是否在升高（探测扫描）、500 是否集中（内部缺陷）」——运维面健康无从对账。

## 目标

- `DashboardHttpServer` 增量（dashboard/internal，静态面）：八 `AtomicLong`。
  - `requests`：route 入口计数（总桶）；`ok`：dispatch 正常完成（无异常穿透）；
  - `authRejects`（401）/ `badRequests`（IllegalArgument 与 JsonProcessing 合并 400 桶）/ `notFounds`（404）/ `tooLarges`（413）/ `unimplemented`（501）/ `serverErrors`（500）六个结局桶。
- 嵌套 `record DashboardHttpStats(...)` + `stats()` + `resetForTest()`。
- 守恒恒等式：**requests = ok + 六结局桶之和**（每请求恰落一桶）。

## 兼容性

纯增量读面：route() 响应状态码、错误 JSON、鉴权行为逐位不变；类属 internal 包（不属公共 API 面），静态计数无兼容承诺负担；无新配置项。

## Out of Scope

- 按 path 分桶（URL 含会话标识敏感面——红线纪律）。
- 时延直方图（另轴）。
