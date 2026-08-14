---
Type: grilling
Status: closed
blocked-by: T69
---

## Question

buzhou-tools 的生产级收口范围：run_command 取消时杀进程树（InterruptedException 路径补 kill）、子进程环境变量白名单/黑名单脱敏、read_file/http_request 读入上限（超限报错而非 OOM）、http_request timeoutSeconds 上限校验与危险头过滤、SSRF 补 IPv6 link-local（fe80::/10）、write_file temp+rename 原子写、core DEFAULT_TOOL_TIMEOUT 60s 硬编码提升为可配并与模块超时语义对齐、与 guard CommandSandbox 的合流程度（本轮是否接线）。

## Resolution

进本轮（采纳 T69 §4 OpenHands 三要素）：
1. **run_command 取消杀进程树**：InterruptedException 路径与超时路径统一走 killProcessTree，finally 收口；harness 取消（future.cancel(true)）不再产生孤儿进程。e2e 用例：启动长命子进程→取消 turn→断言进程死亡。
2. **环境变量白名单**：默认最小集（PATH/HOME/LANG/LC_ALL/TZ/TERM）+ `envAllowlist` 显式追加；其余不透传。配置项注记在 ToolsProperties。
3. **读入上限**：read_file 按 Files.size 预检（默认上限 8MB，超限返回错误文本指引走 spill）；http_request 响应按 Content-Length 预检 + 流式截断兜底（同 8MB）。不 OOM。
4. http_request timeoutSeconds 上限校验（默认 max 300s）；危险头过滤（默认拒 Host/Authorization 值回显类——保留自定义 Authorization 但黑名单 Connection-specific 头：Host/Content-Length/Transfer-Encoding/Connection）。
5. SSRF 补 fe80::/10 链路本地段。
6. write_file temp+atomic move（同目录 .tmp 前缀 + ATOMIC_MOVE + fallback REPLACE_EXISTING）；content 上限 8MB。
7. core DEFAULT_TOOL_TIMEOUT 60s → 可配（BuzhouCoreProperties.toolTimeout，默认维持 60s），语义注记：run_command maxTimeout 可超 60s 但需同步调 toolTimeout（README/元数据 hint）。
8. guard CommandSandbox 完整合流：本轮不做（黑名单+kill+env 白名单+deadline 已收主要面），注记开放问题。（可推翻）
