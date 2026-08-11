# 03 — 模型调用统一超时（deadline）+ session.cancel() 在途模型调用漏网修复

**What to build:** 模型调用纳入统一 deadline（虚拟线程 `Future.get` 兜底 + 中断传播），超时触发 `timeout-fired` 事件并取消在途调用；修复 `session.cancel()` 只中断工具、不中断模型调用的漏网——让运行时干预也能即时停掉在途模型调用。从用户视角：卡死的模型调用不再无限挂住会话；cancel 能真正停掉模型调用。

**Blocked by:** 01

**Status:** done

## 范围

- **模型级 deadline**：把模型调用包进虚拟线程任务、`Future.get(deadline)` 兜底（**对齐执行脊柱 `HarnessToolCallingManager` 既有的工具超时手法**，复用同一思路）；超时 `cancel(true)` 把中断传播进在途模型调用。
- **deadline 配置**：`buzhou.resilience.deadline`，默认取生产合理值，可 yml 覆盖（未来按绑定级覆盖）。
- **session.cancel() 漏网修复**：当前 `cancel()` 只 `cancelInFlight` 工具任务、不触及模型调用（代码盘点确认）；本票让 cancel 也能中断在途模型调用。deadline 与 cancel 共用同一条中断传播路径。
- **`timeout-fired` 事件**进 observability；超时作为终态失败，在 04 的 onModelError 切面落地后接入触发（本票先保证事件 + 取消语义）。

## 验收

- [ ] 慢模型（`CountDownLatch` 阻塞）+ 短 deadline 在预算内返回、发出 `timeout-fired` 事件、在途调用被取消（**不用** wall-clock sleep；对齐 `HarnessToolCallingManagerTest` 超时手法）
- [ ] `session.cancel()` 在模型调用进行中能中断它（当前漏网被修复，对齐 `HarnessToolCallingManagerTest#cancelInFlight` 形态）
- [ ] `buzhou.resilience.deadline` 经 yml 可调且生效
- [ ] 超时事件进 observability 事件流
- [ ] e2e（超时 / cancel 中断模型调用）通过

## 备注

- 管辖 Spec：`.scratch/model-resilience/spec.md`「统一超时（deadline）」+「补 session.cancel() 的缺口」。
- 与 03（底座无统一超时，02 号票盘点结论）对应：底座留白即不周山合法空间。
- 与 01 可并行（均仅依赖 01 的 ResilienceAdvisor 接缝）。
