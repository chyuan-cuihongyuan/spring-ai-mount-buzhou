# 1634 · dashboard 响应 gzip（客户端协商）

> 来源：N 会话 R35（effort #1634 / T2419–T2420 / impl 1187）。

## Solution

`writeJson`：请求头 `Accept-Encoding` 含 gzip 且响应 ≥ `GZIP_MIN_BYTES`(512)
→ GZIP 压缩体 + `Content-Encoding: gzip`（阈值下不压——压缩头开销倒挂）；
无协商头恒明文（HTTP 语义正确——服务器不得对未协商客户端单方面压缩）。

## Testing Decisions

- `DashboardGzipTest` 两断言（真实 HttpServer 端到端）：多 session 大列表
  （~15KB）协商 gzip → Content-Encoding=gzip + 体 GZIP 解压回 JSON 含数据；
  无 Accept-Encoding 恒明文。
- 回归：dashboard 全量 34 用例。

## Out of Scope

- 静态资源（index.html）的 gzip（单页小文件）。
- 压缩级别配置（默认档够用）。
