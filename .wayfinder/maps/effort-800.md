# effort #800 — 工具自动封禁

- 会话：H 会话 800 系第 1 轮 ｜ spec [800](../../../docs/spec/800-tool-auto-ban.md) ｜ 票 [T1101](../tickets/T1101-tool-auto-ban.md)/[T1102](../tickets/T1102-tool-auto-ban-verify.md) ｜ impl553
- 借鉴：fail2ban（fail2ban/fail2ban ≈14K star）maxretry+findtime+bantime——连续失败达阈值自动封禁一段时长

## 勘察（排重）

- ToolDenialLog（spec 709）：角色权限拒绝<b>留痕读数</b>——无执行性封禁。
- ToolKillSwitchHook：全局/手动杀开关——无自动触发。
- ToolQuotaHook（spec 185）：次数配额——按调用计数而非失败计数，无时间窗。
- grep -i `ban|jail`：仅 FirecrackerSandbox 的 jailer（Firecracker 自带术语，不同族）。

## 决定

`ToolAutoBanHook`（guard.hook，order 255 紧随 quota）：watch 集内工具 afterTool 观测失败（error≠null）按 (session,tool) 键滑窗累计，窗内达 maxViolations 自动封禁 banSeconds——beforeTool 拦截并报剩余秒。fail2ban 语义忠实：成功调用不重置、窗口过期自然滑出、到期惰性解除。键封顶 256（truncated 纪律）+ snapshot() 只读快照 + banned/blocked 双计数。Clock 注入可测。空 watch 集=零行为（fail-safe）。

## 测试

达阈值封禁→到期解除/滑窗滑出/成功不重置/会话隔离/未监视+空集零行为+null fail-safe/snapshot 三口径/键封顶 truncated/参数 fail-fast——8 例全绿。

## 诚实边界

进程内存有界（重启清零；跨实例共享封禁归 Redis 后端族留位）；(session,tool) 粒度而非全局（单会话失控不锁全租户——与 fail2ban per-IP 的差异是有意的多租户正确性取舍）；封禁期内失败不累计。
