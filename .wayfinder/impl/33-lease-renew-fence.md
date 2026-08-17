# 33 — core · 租约续租 + LeaseLost + 写路径 fence

**What to build:** 长 Turn 不再静默丢租约：自动续租双路径（轮间 + 后台 TTL/3），续租失败（被 steal）立即以 LeaseLostException 中止 Turn（在飞结果丢弃、不入 Completed-Turn——双主窗口零写入），Turn 提交点校验 fencingToken；过期租约物理移除。

**Blocked by:** 28（取消/收尾机制）、29（LeaseLost 纳入分类体系）

**Status:** done

- [x] 续租双路径：Turn 轮间 renew + 后台调度（间隔 TTL/3 可配）
- [x] renew 失败 → LeaseLostException → Turn 中止语义（在飞丢弃、不入历史、会话标记）
- [x] Turn 提交点 fence 校验（history 落库前 fencingToken 仍持有）
- [x] InMemorySessionLeaseStore 过期租约物理移除；TTL/续租间隔入 properties
- [x] examples 端到端：steal 场景（外部抢走租约）→ 本地 Turn 中止且无写入

> 落地说明（2026-08-14，buzhou-core + examples）：
> 新增 `internal/session/SessionLeaseGuard`（单会话租约哨兵，统一裁决点）：
> `beforeRound()`（轮间 fence+续租）/ `renewQuietly()`（后台静默续租）/ `checkFence()`（提交点 fence）。
> 轮间挂点选 `BoundedToolCallingAdvisor.doBeforeCall/doBeforeStream`——Spring AI 内部工具循环每轮
> 迭代都经此缝且<b>先于</b> BuzhouMemoryAdvisor 落库，是唯一能「在飞工具结果落库前」截断的轮缝
> （DefaultAgentSession 只见整次模型调用，无轮可见性）。后台调度：DefaultAgentRuntime 自管单守护
> 虚拟线程（命名 buzhou-lease-renew，可中断 sleep，TTL/3 节奏无条件 renewQuietly），懒启动 +
> 空闲自熄（全 close 后退出）；close 经 SessionResourceRegistry LIFO 注销（"lease-renew" 条目，
> 先于 lease release）防泄漏。
> LeaseLost 传播：advisor 轮缝 / memory advisor 写 fence 抛 LeaseLostException → 经模型调用链上抛
> → DefaultAgentSession.chat()/stream() 捕获 → markLost + `session.lease.lost` 事件 + onTurnError
> 收口（与 TIMEOUT 兜底同型）→ 上抛调用方；不 markResponded / 不 afterTurn / 不 onTurnEnd（不入
> Completed-Turn，快照类写入不发生）；后续 chat() 在入口 ensureLeaseHeld 即拒（明确错误）。
> fence 校验点共三层：① 轮缝（工具结果落库前）② BuzhouMemoryAdvisor 每次写前（`writeFence`
> 可选 Runnable，含终局 assistant 落库点——补住「最后一次模型调用期间被 steal」窗口）③ 会话提交
> 缝（模型调用返回后、afterTurn/onTurnEnd 前，守 Completed-Turn 快照写入）。
> InMemorySessionLeaseStore：renew/inspect/release 以 compute 原子判定，过期即物理移除；并修复
> 既有缺陷——renew 复活已过期租约（现过期不可再取返回 false）。leaseCount() 为 internal 测试可观测口。
> 配置：BuzhouCoreProperties 增 `leaseTtl`（默认 90s）+ `leaseRenewInterval`（默认 null=TTL/3，
> effectiveLeaseRenewInterval() 解析），compact constructor 默认值、不引 jakarta.validation（切片 42）；
> AutoConfiguration 注入 properties → DefaultAgentRuntime 新构造重载；编程式入口
> `Buzhou.runtime(model, stores, config, leaseTtl, leaseRenewInterval, tools...)`。续租阈值固定 TTL/3。
> 测试（should…_when… 命名，全部实跑通过）：core SessionLeaseGuardTest 8、LeaseRenewFenceTest 7
> （轮间节奏 4s 工具跨 TTL/3、后台节奏/close 冻结、steal 中止/台账零写入、终局窗口写 fence、后台发现
> steal 后拒绝后续 chat）、InMemoryStoresTest 增 2（物理移除+过期不复活）、BuzhouCoreAutoConfigurationTest
> 增 2（默认值+kebab 绑定）；examples LeaseStealEndToEndTest 2（外部 steal → Turn 中止且台账零新写入、
> 健康路径回归）。回归：Spine/BoundedTurnLoop/CancelMode/DeadlineHangImmunity/ErrorTaxonomy/
> TurnDeadline/SessionResourceRegistry/HookEndToEnd 全绿。计时测试沿用契约测试真实时序风格，
> 两侧余量 ≥500ms 防 flake；guard 单测用「阈值>TTL 必续配置」避免时序依赖。
