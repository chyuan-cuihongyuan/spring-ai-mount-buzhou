# 1629 · http_request 输入边界四护栏（Envoy SETTINGS_MAX_* 思想）

> 来源：N 会话 R30（effort #1629 / T2409–T2410 / impl 1182）。

## Problem Statement

http_request 的模型自报输入（url/body/headers）此前只有 timeoutSeconds 与响应体
上限有界——超长 URL、超长 body 直传、海量/巨值请求头都能穿透到执行层（内存
与连接资源被单次调用霸占）。Envoy/H2 的 SETTINGS_MAX_* 族是同型护栏先例。

## Solution

四护栏（call() 前段、SSRF 校验后）：
- `MAX_BODY_CHARS=64K`：超限拒 + 「长内容请用 bodyPath 走框架加载」修法指引；
- `MAX_URL_CHARS=8K`；`MAX_HEADERS=64`；`MAX_HEADER_VALUE_CHARS=8K`（超限走
  HeaderTooLargeException 专项分支——不计 FAILURES 桶，守恒式语义准确）。
拒绝计入既有观测桶（urlRejects/oversizeRejects）；合规输入零影响。

## Testing Decisions

- `HttpRequestInputBoundsTest` 五断言：四护栏各自拒绝入桶（文案含上限值；
  body 拒绝含 bodyPath 指引；单头超限 failures=0）+ 合规输入不受影响。
- 回归：tools 全量 118 用例。

## Out of Scope

- 护栏阈值的 yml 配置化（静态常量先行——真实需求出现再配）。
- 响应体上限（已有 spec 1049/impl-49 的 8MB 界）。
