# 1633 · 负缓存装配面（spec 1616 装饰器 Holder 化）

> 来源：N 会话 R34（effort #1633 / T2417–T2418 / impl 1186）。

## Solution

`NegativeCachingHolder`（进程级开关默认关 + TTL 可调默认 30s）：
`HarnessAssembler` 工具包装链末段对全部工具 `NegativeCachingHolder.wrap`
（未启用 = 原引用透传零开销；启用后新会话工具带失败缓存，停用后自然过期）。
spec 1616 的装饰器（宿主手动 wrap）自此有了开关装配面。

## Testing Decisions

- `NegativeCachingHolderTest` 两断言：关态 wrap 返回同引用；开态包装生效
  （失败 TTL 拦截真调 1 次）+ 真实会话构造链携带（定义不破坏）。
- 回归：core exec 包 249 用例。

## Out of Scope

- yml 配置面（Holder 编程面先行——BuzhouCoreAutoConfiguration 适配后续按需）。
- per-tool 负 TTL 差异（全局统一——恢复窗口语义一致）。
