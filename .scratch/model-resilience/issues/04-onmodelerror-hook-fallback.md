# 04 — onModelError Hook 切面 + 兜底/吞错语义

**What to build:** `BuzhouHook` 新增 `onModelError` 切面（default no-op，兼容既有 Hook）；`HookAdvisor` 在模型调用**终态失败**（重试耗尽 / 不可重试类别 / 超时）后、决定兜底或放行前触发；允许 Hook 吞错并返回兜底响应，或放行让异常按底座原语义抛出。从用户视角：模型失败时用户得到受控兜底回复而非裸异常（或按策略放行）。

**Blocked by:** 01, 02, 03

**Status:** done

## 范围

- **`BuzhouHook.onModelError`**（default no-op）—— 公共 API 变更，PR 说明兼容性影响（default 方法，源码 + 二进制兼容）。
- **`HookAdvisor` 触发**：在 `ResilienceAdvisor` 判定终态失败之后、兜底/放行决策之前，调用链 `onModelError`。
- **两态语义**：Hook 可**吞错 + 回填兜底响应**（复用既有 `HookResult.Replace` / beforeModel-Block 回填响应的同构能力），或**放行**让异常按底座原语义抛出。
- **触发源覆盖**：onModelError 对三类终态源均触发——重试耗尽（01）、不可重试类别（02）、超时（03）。

## 验收

- [ ] `onModelError` 切面在终态失败（重试耗尽 / 不可重试 / 超时）后触发
- [ ] Hook 返回兜底时用户得到兜底回复、无裸异常抛到调用方
- [ ] Hook 放行时异常按底座原语义抛出（行为与未接 Hook 一致）
- [ ] 既有 Hook 实现不受影响（default no-op，兼容）
- [ ] e2e（兜底 / 放行两态，分别以重试耗尽与超时为触发源）通过

## 备注

- 管辖 Spec：`.scratch/model-resilience/spec.md`「onModelError 切面」。
- 借鉴：Google ADK `on_model_error_callback`（错误回调吞错兜底 / 改写控制流 → onModelError 蓝本）。
- 本票是 01/02/03 的整合票：把三类终态失败汇到统一切面。是 05（流式）的前置之一。
