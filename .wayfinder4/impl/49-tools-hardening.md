# 49 — tools 取消穿透与输入上限

**What to build:** run_command 在 turn 取消时杀整棵进程树且子进程只拿白名单环境变量；read_file/http_request/write_file 超限返回可读错误而非 OOM；http 头过滤与超时校验；SSRF 补链路本地段；写文件原子化；toolTimeout 可配。

**Blocked by:** None — can start immediately.

**Status:** ready-for-agent

- [ ] killProcessTree 在 InterruptedException/finally 统一收口；e2e：spawn 长 sleep → cancel turn → join 断言进程死
- [ ] env 白名单（PATH/HOME/LANG/LC_ALL/TZ/TERM + envAllowlist）；测试断言子进程看不到未列入变量
- [ ] read_file Files.size 预检 8MB；http_request Content-Length+流式截断；write_file content 上限+temp+ATOMIC_MOVE
- [ ] http_request timeoutSeconds ≤300 校验；头黑名单（Host/Content-Length/Transfer-Encoding/Connection）；fe80::/10
- [ ] buzhou.core.tool-timeout 可配（默认 60s）+ 元数据 + README 注记
