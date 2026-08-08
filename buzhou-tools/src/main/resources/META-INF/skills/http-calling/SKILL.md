---
name: http-calling
description: http_request 深度指引：SSRF 拦截范围、写方法守卫、长请求体与响应治理
allowed-tools: http_request, read_range
---

# HTTP 调用指引

## SSRF 防护（默认开）

以下地址一律拦截（DNS 解析后对目标 IP 校验）：

- 回环与本机：`127.0.0.0/8`、`0.0.0.0/8`、`::1`
- 内网段：`10.0.0.0/8`、`172.16.0.0/12`、`192.168.0.0/16`、`fc00::/7`
- 链路本地/云元数据：`169.254.0.0/16`（含 `169.254.169.254`）

被拦时确认目标确为公网服务；确需访问内网地址，由运维在
`buzhou.tools.http-request.ssrf.allowlist` 放行该主机/网段。

## 写方法守卫

POST/PUT/DELETE/PATCH 默认挂人工确认（HITL）：首次调用被 BLOCK 并透出确认请求，
用户批准后重发同一调用即放行。GET/HEAD 不强制确认。

## 长请求体与大响应

- 长 body 不要直接拼入参：先 `write_file` 写到文件，用 `bodyPath=文件路径` 让框架加载。
- 响应体超阈值自动 Spill 落盘，返回预览 + 回读指针；按指针 `read_range`（json/page 模式）
  取所需片段。
- 请求不自动跟随重定向（逐跳校验）；收到 3xx 时对新 Location 重新发起请求。
