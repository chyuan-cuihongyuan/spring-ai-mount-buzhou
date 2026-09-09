# Wayfinder Map — Buzhou 跨实例原子配额扣减（effort #16）

> effort #16（已闭合 2026-08-29），延续 #5–#15（累计 160 轮 / T1–T248 / impl 1–195）；本 effort 收口后累计 162 轮 / impl 1–198。
> 本 effort 主线：**共享配额原子扣减**——effort #14 勘察遗留的 fog 毕业生：session 日配额
> 计数已在共享 state store（Redis/JDBC），但「读—改—写」非原子，多实例并发下丢计数。
> 借鉴 LiteLLM（~26K★）Redis 限流器的原子扣减思想（pipeline/Lua 单脚本原子窗口判定）；
> 本仓先例 `deleteIfValueMatches`（Redis Lua / JDBC 条件语句 / 内存 CAS）已验证三栈 CAS 通路。

## Destination

共享 state store（Redis/JDBC/内存）之上的日配额三维度（turns / tool-calls / tokens）扣减
原子化：N 并发递增最终计数 = N（红队钉住）；日翻越竞争只重置一次；单条新 SPI
`compareAndSwap` 由三 store 覆写提供真原子（默认实现诚实标注非原子）；配额拦截语义
（事件/文案/stats）零变化；无新配置键。

## Notes

- 领域/测试哲学/10K★ 政策/AFK 授权：沿用 effort #6–#15 MAP Notes。
- 外部事实源：LiteLLM redis rate limiter 用单 Lua 脚本原子完成「读窗口—判定—扣减—重置」；
  本仓适配为 store 层 CAS 原语 + Hook 层有界重试循环（store 可移植，免每种业务各自写脚本）。
- 本地勘察（2026-08-29）：`SessionStateStore.deleteIfValueMatches` 三实现先例齐备
  （内存 computeIfPresent / JDBC 条件 DELETE 影响行数 / Redis Lua）；`SessionQuotaHook`
  三处 read-then-put（turns 增量 / tool-calls 增量 / tokens 累计）即竞差点；
  `HookEnvironment.stateHandle` 是 Hook 面唯一 store 门面。
- 诚实边界：CAS 耗尽（16 次）回退 last-write 覆写并计数——极端竞争下计数可能少记
  （宁可少记不误拦截硬化语义），stats 暴露回退次数；RunawayHook/TokenBudgetHook 的
  同型计数竞态不在本 effort（fog 留位，独立 effort 推广）。

## Decisions so far

- **CAS 原语放 store 层而非 Hook 自带锁**：跨实例原子性只能由存储侧单语句/脚本提供；
  Hook 层 per-JVM 锁（既有 sessionLocks）对跨实例无效——保留但降级为默认实现兜底。
- **expected=null 语义 = 键不存在才写**：日翻越首写竞态（两实例同时见 stale 值）由
  「expect stale 原串」收敛——只有一方 CAS 成功，另一方重读后以新值续算。
- **JDBC 原子 = 条件 UPDATE / 条件 INSERT 各单语句**（影响行数判定），不引入
  方言 MERGE/ON CONFLICT（H2/MySQL/PG 可移植性优先）。
- **Redis CAS = 单 Lua**：DEL+HSET+SADD 原子（先 DEL 清残字段再写全量字段）。

## Not yet specified

- RunawayHook / TokenBudgetHook 计数写路径同型原子化（本 effort 只改配额——推广独立议）。
- 共享熔断状态跨实例（fog 沿用）。
- outbox SCAN 下推 / 观测 OLAP / skill 语义排序（fog 沿用）。

## Out of scope

- 沿用 effort #7–#15 Out of scope 全部条目。
- 分布式全局限流之外的配额语义变化（拦截点/事件口径不动）。
- 新配置键（本 effort 零新键——绑定矩阵不动）。

## Tickets

初始 5 张（T249–T253，按轮逐张闭合）：

- [x] [T249 SPI compareAndSwap + 三 store 原子覆写](../tickets/T249-cas-spi.md)（impl-196；Redis 走 WATCH/MULTI/EXEC——平台扫描禁新增 eval 调用点，Lua 等价改道）
- [x] [T250 SessionStateHandle 透出 + SessionQuotaHook 三维度 CAS 化](../tickets/T250-quota-cas.md)（impl-197；进度检测重试：值停滞 16 次才回退，运行中不丢计数）
- [x] [T251 并发红队 + 三 store CAS 单测](../tickets/T251-cas-redteam.md)（impl-197；契约用例三栈同测 + 4 用例并发红队 8/8 绿）
- [x] [T252 文档面（runbook §6 / CONTEXT 术语 / 快照说明）](../tickets/T252-cas-docs.md)（impl-198；方法级增补不入类型快照）
- [x] [T253 里程碑 verify + 收口](../tickets/T253-effort16-closing.md)（全仓 verify 绿；累计 162 轮）
