# 811 — 断路器 crash-loop 检测

> 来源：H 会话第 12 轮 = effort #811 / [T1123](../../.wayfinder/tickets/T1123-circuit-crash-loop.md) / [T1124](../../.wayfinder/tickets/T1124-circuit-crash-loop-verify.md) / impl 564。
> 借鉴：k8s CrashLoopBackOff（≈115K star）。

## Problem

断路器单次跳闸有留痕（702），但「恢复→立刻再跳」的循环（坏凭据/超载模型）混在普通跳闸里：半开探测反复失败白白烧轮次，系统性故障与偶发抖动不可辨。

## Solution

`CircuitCrashLoopDetector`（resilience 根包）：

- **判定**：windowMillis 内 OPEN 次数 ≥ minOpens（≥2）→ looping 闩锁态。
- **闩锁语义（k8s 对齐）**：窗口滑过不自动解除；唯 recordRecovery（恢复事件）清除+清窗。
- **计数**：loopsDetected 只在进入闩锁时 +1（循环轮次可见）。
- **有界**：模型封顶 32 + truncated；旁路喂点（702 journal 同挂点）。

## 兼容性

纯新增旁路类；ModelCircuitBreaker 零变更。

## 诚实边界

不阻止试探（停止试探是路由策略域——后续可挂 dampener）；窗口参数由调用方定；记录点由装配侧接。
