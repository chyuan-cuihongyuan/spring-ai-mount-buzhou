# 800 — 工具自动封禁

> 来源：H 会话第 1 轮 = effort #800 / [T1101](../../.wayfinder/tickets/T1101-tool-auto-ban.md) / [T1102](../../.wayfinder/tickets/T1102-tool-auto-ban-verify.md) / impl 553。
> 借鉴：fail2ban maxretry + findtime + bantime（≈14K star）。

## Problem

失效工具（坏凭据/网络故障/参数持续非法）会反复失败浪费轮次预算：ToolKillSwitchHook 是全局手动开关、ToolQuotaHook 按调用数计数（失败也在内、无时间窗）、ToolDenialLog 只是拒绝留痕——「连续失败到一定程度自动止损」缺位，运维要盯梢后手动拉闸。

## Solution

`ToolAutoBanHook`（guard.hook，order 255）：

- **watch 集**：显式登记可被自动封禁的工具名；空集 = 零行为（fail-safe，不会误伤未登记工具）。
- **滑窗失败计数**：afterTool 观测 `error != null`，按 (sessionId, tool) 键记时间戳环；窗内达 `maxViolations` → 封禁 `banSeconds`。成功不重置、窗口过期自然滑出（fail2ban 滑窗语义）。
- **beforeTool 拦截**：封禁期内 block，理由含工具名、阈值与剩余秒。
- **到期惰性解除**：不设定时器，读数与拦截时即时判定。
- **读数**：`snapshot()` → 生效封禁列表 + totalViolations/totalBans + truncated 标记；banned/blocked 双 Micrometer 计数。
- **有界**：跟踪键封顶 256（超限不再记、truncated 如实）；Clock 注入可测。

## 兼容性

新 hook 需显式注册进 hook 链（同 ToolQuotaHook 模式）——默认装配零行为。无既有签名变更。

## 诚实边界

进程内存有界（重启清零，跨实例共享封禁归 Redis 后端族留位）；(session, tool) 粒度而非全局——单会话失控不锁全租户（与 fail2ban per-IP 的差异是刻意的多租户取舍）；封禁期内失败不累计（都进不来）。
