# 1601 · MCP 连接最大寿命（HikariCP maxLifetime 思想）

> 来源：N 会话 R2（effort #1601 / T2353–T2354 / impl 1154）。借鉴对象：HikariCP
> `maxLifetime`（>10K star 项目）——连接到寿即退役重建，防长连接的状态腐化、内存漂移与
> 服务端隐性配额累积；在飞请求等归还时退役，不硬切。

## Problem Statement

MCP 注册表（spec 04）的连接一经建立即长期复用（仅探活失败/spec 变更才重建）。长期存活的
stdio/HTTP 连接会累积真实腐化：server 端句柄/内存漂移、TLS 会话过期、代理层静默半开、
工具目录漂移基线失真——这些都以「偶发慢/偶发断」的形式在数天后才显形，且只能靠重启进程
恢复。连接池世界（HikariCP）的成熟答案是给连接一个最大寿命，到寿退役、原子换新。

## Solution

注册表新增 opt-in 连接寿命策略 `LifetimePolicy(maxLifetime, checkInterval, clock)`：

- 定期扫描（单线程调度器，与 keepalive 探活共用）ACTIVE 条目，`createdAt + maxLifetime`
  到期且 **inFlight = 0** 的连接走既有 `rebuildEntry` 口径（DRAINING 排水 + 原样 spec
  重建，与探活失败路径完全同构）——新条目 createdAt 自然重置。
- 在飞连接**推迟到下一轮**（HikariCP「归还时退役」语义——绝不硬切在飞请求），
  `deferredRetireCount` 观测推迟发生。
- 默认 null = 关，零行为零线程零开销。
- 观测：`retiredCount()` 到寿退役累计 + `buzhou.mcp.lifetime.retired` 指标 + INFO 留痕。

## User Stories

1. 作为运维者，我想让 MCP 长连接到寿自动换新，所以 server 端漂移/腐化不会累积到事故级才暴露。
2. 作为运维者，我想让在飞工具调用绝不被寿命机制打断，所以退役只发生在连接空闲时。
3. 作为运维者，我想保持默认行为零变化，所以不配置 max-lifetime 时一切照旧。
4. 作为运维者，我想看到退役与推迟的读数，所以寿命机制的效果与推迟频率可观测。

## Implementation Decisions

- `DefaultMcpClientRegistry` 嵌套 public record `LifetimePolicy`（校验两时长为正）；
  主构造器追加末位参数（既有 5 层委托构造器全部不动——向后兼容）。
- `Entry` 增 `createdAt`（policy 开启时取 policy 时钟，测试可控；关时为 null 不参与任何逻辑）。
- `retireExpiredOnce()` 公开（与 `probeOnce` 同模式——测试可直接手动驱动一轮扫描），
  调度线程经 `safeRetire` 保护。
- Builder/yml：`buzhou.mcp` 下 `max-lifetime`（声明即启用，扫描间隔 = maxLifetime——每寿
  命周期扫一轮，语义直白无第二参数）。

## Testing Decisions

- 新测试类 `McpLifetimeRetireTest`（伪连接/伪工厂沿用 `McpKeepaliveProbeTest` 模式 +
  可变时钟）：
  ① 未到寿：时钟未过寿命 → 扫描零退役、零重建；
  ② 到寿空闲：时钟过寿命 → retiredCount=1、工厂重建一次、新条目可用；
  ③ 在飞推迟：阻塞中的工具调用持有引用 → 扫描 deferredRetireCount=1 不重建；调用完成后
  再扫 → 重建发生（Latch 控制时序，全程超时保护）；
  ④ 关（null policy）：时钟任意推进也零动作（零行为钉死）。
- Prior art：`McpKeepaliveProbeTest`（rebuildEntry 口径）、`DefaultMcpClientRegistryTest`。

## Out of Scope

- 连接池化/多连接并发（当前注册表 = 每 server 单连接，池化是另一个量级的机制）。
- 按连接健康度动态调寿命（到寿时间是静态策略，健康面已有 keepalive 探活）。

## Further Notes

- > 【推演】HikariCP 对到寿连接做「最多 30s 抖动」避免齐步退役；本注册表 server 数量级
  个位到几十、且重建走排水宽限（30s grace），天然错峰，不引入抖动参数。
