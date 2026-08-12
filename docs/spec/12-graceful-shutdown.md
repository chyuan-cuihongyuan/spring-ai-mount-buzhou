# 12 优雅停机与会话 drain

> 生产就绪缺口 06（M1 稳定基线）。设计 Spec 立项：core + autoconfig 扩展（不新增模块 / SPI）。
> 蓝本：Spring Boot 4 graceful shutdown（只管 web 请求——底座留白）、LangGraph Platform 线程存活语义（会话跨实例续接天然性的参照）。
> 忠实度：drain 三步协议 / SmartLifecycle 触发器形态由缺口决策定案；具体编排（轮次粒度等待、取消传播强杀、事件 fan-out）由本项目自主推演，标注 `> 【推演】`。

## 设计目标

让单个 Agent 实例**受控停机**：停机时不丢在途轮次数据、不孤儿会话、可被另一实例续接，且停机过程对调用方有明确的路由信号。

- **drain 粒度 = 当前轮次完结**（与微压缩「完结轮次」原子单位对齐；不等整个会话）。
- **拒新即路由信号**：drain 开始后 `spawn()` 立即抛 `RuntimeDrainingException`——不排队、不缓冲，调用方向另一实例续接 / 重试 / 退避自行决策。
- **可接管性靠五 SPI + 租约天然供给**：drain 关闭会话即释放租约（既有 `SessionResourceRegistry.closeAll()` 谢幕链），同 sessionId 可立即在另一 runtime spawn 续接——**不做显式跨实例迁移**。
- **三步协议**：拒新 → 等完在途轮次（预算内）→ 超时强杀（取消传播）→ 正常 close。
- **safe-by-default**：`buzhou.shutdown.enabled` 默认开；默认超时派生自 `spring.lifecycle.timeout-per-shutdown-phase`。
- **诚实边界**：模型调用阶段无取消句柄（探查事实），强杀只能到工具层——此边界如实表达，不伪造能力。

## 术语

| 术语 | 含义 |
|---|---|
| drain | 运行时受控停机编排：拒新 → 等完在途轮次 / 超时强杀 → close 全部活跃会话 |
| 拒新（refuse-new） | drain 开始后 `spawn()` 立即抛 `RuntimeDrainingException`，不排队不缓冲 |
| 等完（waited） | 预算内等完当前轮次后正常 close 的处置方式 |
| 强杀（force-killed） | 预算耗尽仍在轮次中，经取消传播强杀当前轮次后 close 的处置方式 |
| 活跃会话台账 | `DefaultAgentRuntime` 内的 `ConcurrentHashMap<sessionId, LiveSession>`：spawn 注册 / close 注销 |
| 轮次在途门闸 | `TurnGate`（`SessionObserver` 实现）：经 onTurnStart/End/Error 维护在途计数，供 drain 有界等待 |
| drain 事件 | `drain.started` / `drain.session.completed` / `drain.timeout-force-kill` / `drain.finished`，经各在途会话的事件通道 fan-out |

## API

### 编程式入口

```java
public interface AgentRuntime {
    // ... 既有 spawn 重载不变 ...

    /** 优雅停机（drain）：拒新 + 等完/强杀在途轮次 + close 全部活跃会话。additive default（二进制兼容）。 */
    default DrainResult drain(Duration timeout) {
        throw new UnsupportedOperationException("drain not supported by this AgentRuntime: " + getClass().getName());
    }
}
```

- `AgentRuntime.drain(Duration)` 为 **additive default 方法**：未实现的运行时抛 `UnsupportedOperationException`；`DefaultAgentRuntime` 已实现完整 drain 协议。既有实现与集成源码/二进制兼容。
- 返回 `DrainResult`（record）：`drainedCount`（等完数）/ `forceKilledCount`（强杀数）/ `totalDuration`（总耗时）。

### 拒新异常

```java
public class RuntimeDrainingException extends RuntimeException {
    public RuntimeDrainingException(String sessionId) {
        super("Runtime is draining, cannot spawn session: " + sessionId);
    }
}
```

- drain 开始后 `spawn()` 立即抛出；message 带 sessionId 与「实例正在 drain」上下文，便于排障。

### drain 事件

| 事件类型 | payload | 语义 |
|---|---|---|
| `drain.started` | `activeCount` | drain 开始，fan-out 到每个在途会话的事件通道 |
| `drain.session.completed` | `sessionId` / `disposition`（`waited` \| `force-killed`）/ `durationMs` | 单会话 drain 处置完成（close 前） |
| `drain.timeout-force-kill` | `sessionIds`（List）/ `count` | 预算耗尽强杀的会话列表（仅当有强杀时发射） |
| `drain.finished` | `drainedCount` / `forceKilledCount` / `totalDurationMs` | drain 全部完成（close 前 fan-out） |

> 【推演】drain 事件经各在途会话的事件通道 fan-out（每会话均见 drain 全周期），而非新增运行时级事件总线——复用既有 `DefaultAgentSession.emit()` 通道，观测管线按会话归因。无在途会话时无事件发射（空载 drain 直接返回）。

## 配置项

| 属性 | 默认 | 语义 |
|---|---|---|
| `buzhou.shutdown.enabled` | `true`（safe-by-default） | drain 生命周期 bean 总开关；关则不装配 `BuzhouDrainLifecycle` |
| `buzhou.shutdown.drain-timeout` | 派生（见下） | drain 总预算；显式配置优先 |
| `spring.lifecycle.timeout-per-shutdown-phase` | `30s`（Boot 4 内建默认） | `buzhou.shutdown.drain-timeout` 未配置时派生源 |

**超时派生优先级**：`buzhou.shutdown.drain-timeout`（显式）> `spring.lifecycle.timeout-per-shutdown-phase`（Boot 4 内建）> `@Value` 默认 `30s`。
编程式入口 `drain(Duration)` 未给 timeout 时取保守默认常量 `DEFAULT_DRAIN_TIMEOUT = Duration.ofSeconds(30)`。

> 装配层不裸读 `Environment`：`BuzhouShutdownProperties`（`@ConfigurationProperties(prefix="buzhou.shutdown")`）承载显式配置；`spring.lifecycle.timeout-per-shutdown-phase` 经 `@Value` 注入 String 后用 `DurationStyle` 解析（ApplicationContextRunner 无 `ApplicationConversionService`，`@Value` 不支持 Duration 直转）。

## 时序

### drain 三步协议

```
drain(timeout) 调用
  │
  ├─ ① 拒新：drainFuture CAS 设值 → 后续 spawn() 抛 RuntimeDrainingException（幂等：重复调用等待首次结果）
  │
  ├─ ② 快照活跃会话（写锁内，确保不孤儿刚 assemble 完的会话）
  │
  ├─ ③ fan-out drain.started（activeCount）→ 每会话事件通道
  │
  ├─ ④ 等完在途轮次（虚拟线程 fan-out，每会话 TurnGate.awaitIdle(budget)）
  │     ├─ idle=true  → disposition=waited
  │     └─ idle=false → 预算耗尽 → session.cancel()（cancelInFlight → future.cancel(true) 中断）→ disposition=force-killed
  │                    → fan-out drain.timeout-force-kill（强杀 sessionIds）
  │
  ├─ ⑤ fan-out drain.session.completed（sessionId + disposition + durationMs）
  ├─ ⑥ fan-out drain.finished（drainedCount / forceKilledCount / totalDurationMs）
  └─ ⑦ close 全部会话（触发既有谢幕链：EXIT flush 同步落盘 → 停心跳 → 关执行器 → 释租约）
        单会话 close 异常不阻塞其他会话（首异常收集后汇总抛出）
```

- **轮次在途信号**复用既有 `SessionObserver` 轮次边界（onTurnStart/onTurnEnd/onTurnError），覆盖 chat / stream / AUTO_RESUME 全部轮次形态——三者均在 `DefaultAgentSession` 内回调。
- **等待用虚拟线程 + 计数门闸**（`TurnGate` monitor wait/notify，latch 等价语义），禁止轮询 sleep；单会话等待失败不阻塞其他会话。
- **EXIT 档联动**：drain close 会话触发既有 `DurabilityTieredStores.flush` 钩子（同步执行，不依赖后台虚拟线程），缓冲写落盘——与 05 崩溃恢复 EXIT flush 同一钩子（spec 11「EXIT flush 钩子」）。
- **被强杀会话与正常关闭会话可接管性一致**：均走正常 close 路径（flush → 停心跳 → 释租约），同 sessionId 可立即在另一 runtime spawn 续接。

### SmartLifecycle 装配

`BuzhouDrainLifecycle` 实现 `SmartLifecycle`，Spring 停机时 `stop(Runnable)` 触发与编程式入口**同一** drain 编排实现（Spring 只是触发器）。

- **相位** = `SmartLifecycle.DEFAULT_PHASE`（Boot 4 最高相位）：
  - 先于 web 容器优雅停机（`WebServerGracefulLifecycle` 相位 `DEFAULT_PHASE - 100`）停止——drain 期间 web 仍存活，新 HTTP 请求经 spawn 拒新异常路由，在途请求的当前轮次被 drain 等完或超时强杀。
  - 先于观测异步管线排空停止——drain 事件全部进入观测管线后再排空，不丢事件。
- **幂等**：重复 stop / 停机中再收信号复用 `DefaultAgentRuntime` 的 drain 幂等（首次结果共享）。

### 运维：滚动发布会话续接模式

> 【推演】「不做显式跨实例迁移」决策的标准答案——可接管性靠五 SPI + 租约天然供给。

```
实例 A（旧版本）              实例 B（新版本）
     │                              │
  1. 收到停机信号                    │
  2. SmartLifecycle.stop → drain    │
     ├─ 拒新（spawn 抛拒新异常）     │
     ├─ 等完在途轮次 / 超时强杀      │
     └─ close 全部会话 → 释租约      │
     │                              │
     │ ── 同 sessionId spawn ─────►  3. spawn(appId, agentName, sessionId)
                                    ├─ 租约已释放 → tryAcquire 成功
                                    ├─ 加载历史（MessageStore.load）
                                    ├─ 05 恢复语义：中断轮次按档位处置
                                    └─ 续接完成
```

- 实例 A drain → 释租约 → 实例 B 同 sessionId spawn → 05 恢复语义加载历史续接。
- 网关层在 drain 期间将新请求路由到实例 B（spawn 拒新异常是路由信号）。
- EXIT 档下 drain 的 flush 保证缓冲写落盘，实例 B 加载到完整历史。

## 推演标注

| # | 推演点 | 依据 |
|---|---|---|
| 1 | drain 事件经各在途会话事件通道 fan-out（不新增运行时级事件总线） | 复用既有 `DefaultAgentSession.emit()` 通道；观测按会话归因 |
| 2 | drain 粒度 = 当前轮次（不等整个会话） | 与微压缩「完结轮次」原子单位对齐；长会话不阻塞停机 |
| 3 | `TurnGate` 用 monitor wait/notify（latch 等价） | 避免单用 `CountDownLatch` 的 volatile 重赋值竞争；非轮询 |
| 4 | drain 快照与 spawn 互斥用 `ReentrantReadWriteLock` | spawn 读 / drain 快照写；确保刚 assemble 完的会话不孤儿 |
| 5 | drain.finished 在 close 前 fan-out | close 清空会话监听器；须在 close 前发射才能被观测 |
| 6 | 相位 = `SmartLifecycle.DEFAULT_PHASE` | 先于 web 容器与观测管线停止；drain 期间 web 存活供拒新路由 |

## 开放问题

- **M2 预留 · 15 运行时干预**：15 的「暂停/纠偏注入/kill switch/人工接管」复用 drain 的轮次边界等待原语（`TurnGate`）——挂起-回填原语与 drain 等完同源。
- **M2 预留 · 21 按租户 drain**：drain 当前是运行时级（全部会话）；按租户 drain 需在台账上叠加 tenantId 维度，编排不变。
- **M2 预留 · 07 背压联动**：07 的「快速失败」过载档与 drain 的「拒新即路由信号」语义同源——过载时 spawn 抛 `OverloadException`（与 `RuntimeDrainingException` 同为路由信号族）。
- **模型调用阶段不可强中断**：`session.cancel()` 经 `HarnessToolCallingManager.cancelInFlight()` 仅中断工具层在途调用（`future.cancel(true)`）；模型调用阶段（ChatClient.call）无取消句柄，强杀只能等模型层自身超时返回——此边界在代码注释与 `drain.timeout-force-kill` 事件语义中如实表达，不伪造能力。
- **多实例/集群级 drain 协调**：本机制只管单实例 drain；集群级「逐实例 drain」编排归部署层（网关健康检查 + 滚动发布）。
