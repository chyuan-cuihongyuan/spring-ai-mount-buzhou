# 1049 — http_request 请求量水位与结果分布读面

> 来源：J 会话第 49 轮 = effort #1049（[T1553](../../.wayfinder/tickets/T1553-httptool-stats-shape.md) / [T1554](../../.wayfinder/tickets/T1554-httptool-stats-verify.md) / impl 801）。借鉴：Envoy upstream statistics（请求按结局分桶 rq_success/rq_error——熔断与健康洞察的第一信号）。http 域三轴收官：R47 读文件 / R48 出网守卫 / R49 请求本身。

## Problem Statement

`HttpRequestTool.call()`（spec 06 推演 #6 / impl-49 硬化）的全部拒绝路径——不支持 method、非法 URL 与非 http(s) scheme、SSRF 拒绝、timeoutSeconds 越界、响应体超 8MB、异常兜底（网络失败/超时）——当前只返回失败字符串：**请求送达率与拒绝原因分布不可见**。宿主无法回答"出网请求成功率多少、失败集中在模型参数错误（method/URL/timeout）还是环境故障（网络异常）"；系统性故障（DNS 全挂）与偶发笔误无法区分。

## 目标

- `HttpRequestTool` 增量（tools/http，静态面）：八 `AtomicLong`。
  - `attempts`：call() 入口计数（总桶）；`successes`：拿到 HTTP 状态码并返回（含 4xx/5xx——HTTP 层有响应即送达，状态码语义归模型判读）；
  - `methodRejects` / `urlRejects`（非法 URL 与非法 scheme 合并语法桶）/ `ssrfRejects`（守卫拒绝）/ `timeoutParamRejects`（越界）/ `oversizeRejects`（响应超限）/ `failures`（catch 兜底）六个拒绝分桶。
- 嵌套 `record HttpToolStats(long attempts, long successes, long methodRejects, long urlRejects, long ssrfRejects, long timeoutParamRejects, long oversizeRejects, long failures)`（`totalRejects()` 派生）+ `stats()` + `resetForTest()`。
- 守恒恒等式：**attempts = successes + totalRejects()**（每入口恰落一桶）。
- 拒绝桶语义分轴：参数错误桶（method/url/timeout）指向模型行为，环境桶（ssrf/failures）指向环境与守卫。

## 兼容性

纯增量读面：call() 返回语义、SSRF/上限/黑名单头判定逐位不变；静态面理由同 R46–R48 域内先例；无新配置项。

## Out of Scope

- 按 URL/host 分桶（敏感面——红线纪律）。
- 状态码分布直方图（successes 单桶已回答送达率，细分留后续轮）。
- 重定向跟随（ NEVER 语义维持——SSRF 逐跳校验开放问题）。
