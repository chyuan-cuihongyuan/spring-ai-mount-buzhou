# 25 — guard · CommandSandbox SPI + Deno 档

**What to build:** `run_command` 爆炸半径可伸缩的进程级硬隔离：CommandSandbox SPI 三档（Deno 轻量档必做、Firecracker/E2B 重载档接口预留），探测式启用、secret 白名单透传。

**Blocked by:** None — can start immediately.

**Status:** ready-for-agent

- [ ] `CommandSandbox` SPI（隔离执行契约 + 能力探测）
- [ ] **DenoSandbox**：deny-by-default、`--allow-read=<路径>/--allow-net=<host:port>/--allow-env=<变量名>/--allow-run=<程序名>` 精细授权、secret 经白名单透传、跨平台
- [ ] FirecrackerSandbox / E2BSandbox：**接口预留**（类型与配置位存在、实现抛明确 Unsupported，文档说明部署前提）
- [ ] 探测式启用：未装 Deno 时给出明确指引（不静默回退）
- [ ] 既有 FileSandbox/黑名单定位为「无沙箱依赖内联档」（文档明确层次）
- [ ] 端到端：Deno 档下命令越权（路径/网络/环境变量）被拒
- [ ] spec 07（Hook 护栏）同步

> spec 12 §guard-23；[T51](../tickets/T51-guard-command-sandbox.md)。源：deno 108,248★（必做档）/ firecracker 36,040★ / E2B 13,383★（预留档）。
