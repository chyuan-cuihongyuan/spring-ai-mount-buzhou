# effort #833 — 危险工具命中分布

- 会话：H 会话 800 系第 34 轮 ｜ spec [833](../../../docs/spec/833-dangerous-tool-hit-stats.md) ｜ 票 [T1167](../tickets/T1167-dangerous-tool-hit-stats.md)/[T1168](../tickets/T1168-dangerous-tool-hit-stats-verify.md) ｜ impl586
- 借鉴：WAF top-rules 观测思想（Cloudflare/Radware WAF 报告惯例——命中热力排行）

## 勘察（排重）

- DangerousToolMatcher/DangerousToolGuardHook：匹配+拦截执行——无命中统计。
- ToolDenialLog（709）：角色拒绝留痕（不同触发域）。
- grep -i `hit.*stats`：SecretHitStats/PiiHitStats（扫描域）——危险工具域缺位。

## 决定

`DangerousToolHitStats`（guard.config，纯读数）：record(toolName, requiredState, atMillis)——工具封顶 64 超限并入 __overflow__ 桶（跨域口径一致）；per-tool hits/requiredState/lastSeen(max)+totalHits/distinctTools+top(n) 降序（典序破平）；null/空白工具名忽略不计 total。喂点=GuardHook 命中处装配侧。

## 测试

top 降序+lastSeen max+requiredState 留存+totalHits=4 全入账/溢出桶入账 distinct=64+1/脏入参不计 total+top 边界（0/-1 空真）——3 例绿（totalHits/distinct 两处账误修正：全入账口径+溢出桶占位）。

## 诚实边界

命中计数不改拦截行为（喂点手动）；requiredState 留最近一次非空值（不存历史——画像口径）；溢出桶聚合不可回溯具体工具（有界取舍如实）。
