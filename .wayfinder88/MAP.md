# Wayfinder Map — Buzhou 虚拟 key 配额（effort #88，A 会话）

> fog 种子⑨「虚拟 key 配额」收口（.wayfinder85 台账）。与 B 会话并行推进：
> 轮次按「先建 `.wayfinder<N>/MAP.md` 者得 N」互斥（#86 MAP 分工登记）。

## Destination

每 key token 硬顶的进程内配额注册表：花超即拒（结构化 QUOTA_EXCEEDED）+
窗口清零 + 用量 top-N——多租户/多下游 key 分账限流的事实源。

## Notes

- 借鉴 LiteLLM virtual-key budgets；口径：未注册 key 直通（不设预算 = 不拦，
  默认关哲学）；表满 256 fail-fast；不自装调度（export → reset 循环）。
- 票号对齐：B 会话 #87 用 T445-T446 与本会话 #86 A 侧已提交票号撞号（其建票
  先于本侧提交）——收口轮台账归一，后续轮次票号以 git 提交顺序自然递增。

## Decisions so far

- [虚拟 key 配额注册表](tickets/T449-virtual-keys.md) — per-key AtomicLong CAS
  原子扣减 + 越限整体拒绝不留部分扣减 + 拒绝计数无 tag（key 名无界纪律）。

## Not yet specified

- TokenBudgetHook 接线（key 解析来自会话/请求面）；成本面（micro-USD）配额。

## Out of scope

- 远端 key 仓同步；per-key 限速（与配额正交）。

## Tickets

- [x] [T449 虚拟 key 配额注册表](tickets/T449-virtual-keys.md)（impl-274）
- [x] [T450 收口提交](tickets/T450-virtual-keys-close.md)（impl-274）
