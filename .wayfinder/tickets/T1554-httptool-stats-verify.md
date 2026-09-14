---
id: T1554
title: http_request 请求量水位与结果分布读面的验证裁决
type: task
status: closed
assignee: zcode-j
blocked-by: T1553
created: 2026-09-14
---

## Question

J 会话第 49 轮：HttpToolStats 读面如何验证？

## Resolution

**用户常设授权 AFK（可推翻）**

验证裁决（HttpToolStatsTest，本地 HttpServer 回环骨架——宿主放行清单含 127.0.0.1，零外网依赖，既有 HttpRequestToolTest 先例）：本地回环 GET 200 → successes=1；坏 method → methodRejects=1；非法 URL 与 ftp:// scheme → urlRejects=2；内网地址无放行 → ssrfRejects=1；timeoutSeconds=0 → timeoutParamRejects=1；响应体 8MB+1 → oversizeRejects=1；混合调用后守恒 attempts = successes + totalRejects；resetForTest 归零。定向 `mvn -pl buzhou-tools -am test -Dtest='HttpToolStatsTest'` 绿 + 既有 HttpRequestToolTest 回归绿。
