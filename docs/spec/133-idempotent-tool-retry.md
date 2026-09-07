# Spec 133 — 幂等工具重试（effort #95）

> wayfinder map：`.wayfinder/maps/effort-95.md`（T479–T480）。借鉴：Temporal Activity
> retry policy（幂等 Activity 框架级重试）+ Failsafe 指数退避。

## Problem Statement

外部工具的瞬时故障（连接抖动、下游 503、DNS 一闪）会让一次本可成功的调用变成
错误反馈回喂——模型只能 REASK 整轮重试，多耗一次模型调用与 Turn 预算；而这类
故障对幂等工具（查询类/只读类）而言，框架静默重试一次就救回来了。

## Solution

`RetryingToolCallback`（core/exec，装饰器——`wrap(callback, policy)` 即启用）：

- **重试面**：仅<b>异常</b>（callback.call 抛出）触发重试——指数退避
  （initial×2^n，封顶 maxBackoff），最多 maxAttempts 次；耗尽后原样上抛最后异常
  （既有 harness 兜底把它转为错误反馈——词汇不变）。
- **不重试面**：工具正常返回的<b>错误反馈文案</b>（结构化标记）是语义结局
  （业务失败/校验失败），不重试；返回值一律不重试。
- **幂等性契约**：宿主只包幂等工具（javadoc 显性告知——非幂等工具重试有重复
  副作用风险，责任归声明方）。
- **策略**：`RetryPolicy(maxAttempts, initialBackoff, maxBackoff)`——默认
  3 次 / 50ms / 500ms；maxAttempts=1 = 零重试（行为与裸工具一致）。
- 计数：`buzhou.tool-retry.retries`（per 工具名 tag）。

## User Stories

1. 作为宿主，我把只读查询工具包一层 wrap，瞬时抖动被静默消化——模型零感知、
   Turn 零浪费。
2. 作为宿主，非幂等工具我不包（契约清晰），或显式 maxAttempts=1 保持裸行为。
3. 作为运维，重试计数告诉我哪些工具在「带病抖动」（高重试率 = 下游劣化前兆）。

## Implementation Decisions

- 装饰 ToolCallback（getToolDefinition 原样透传——名称/schema/description 不变，
  装配面零感知）。
- Thread.sleep 退避（虚拟线程时代阻塞可接受；工具执行本就在专用线程）。
- 不与 ToolCircuitBreakerHook 联动（正交——熔断在 hook 层看最终结局）。

## Testing Decisions

- 抖动工具（前 N 次抛异常后成功）：重试后成功且尝试数正确 / 耗尽上抛最后异常 /
  maxAttempts=1 零重试等价裸工具 / 错误文案返回不重试 / 计数与退避时长下限。
- 先例：FaultInjectingToolCallback（testsupport 抖动注入）。

## Out of Scope

- yml 配置面；重试事件 webhook；断路器联动；谓词化异常过滤（后续按需）。

## Further Notes

- 与 spec 131 工具熔断互补：重试管瞬时抖动（毫秒级），熔断管持续故障（冷却级）。
