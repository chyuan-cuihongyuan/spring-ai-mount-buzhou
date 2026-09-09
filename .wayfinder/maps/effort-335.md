# Wayfinder Map — Buzhou 错误预算政策·烧穿自动降级（effort #335，C 会话第 36 轮）

> C 会话第 36 轮。321 立了错误预算燃尽率（观察+告警接线），但烧穿了
> 只是「有人知道」——Google SRE 的 error budget policy 思想：预算烧穿
> = 冻结低优先级工作（feature freeze），用可靠性换稳定，预算回充才解冻。
> 123 的 SpawnGate 已有三级优先排队——缺的是「烧穿时地板自动抬到 HIGH」。

## Destination

`SpawnAdmissionFloor`（共享地板槽——volatile LOW，政策驱动）+
`ErrorBudgetPolicy`（周期评 ErrorBudget.anyBreaching：烧穿→floor=HIGH
（NORMAL/LOW spawn 即拒——SRE 冻结），连续两轮清明→解冻地板回 LOW——
防抖；冻结/解冻 WARN+计数；SmartLifecycle 自管虚拟线程）+
SpawnGate 地板判定（低于地板即拒——admission-floor 原因；无地板恒 LOW
零变化）+ yml `buzhou.backpressure.error-budget-freeze.{enabled,interval}`。

## Notes

- 号段：spec 335 / T661–T662 / impl-358。
- 借鉴源：Google SRE error budget policy（预算耗尽→发布冻结/降级）。
- 纪律：任何 scope 烧穿即全局冻结（保守）；存量 SpawnGate 测试不改动
  全绿 = 零变化证明；未启 enabled = 地板不存在。

## Decisions so far

- 地板走共享槽而非闭包直连——gate 在 runtime 装配期构造，政策是独立
  bean 生命周期（SmartLifecycle），解耦两者装配顺序。
- 冻结在 FAIL_FAST 档同样生效（低于地板先于容量判定——语义一致）。

## Out of scope

- 按 scope 分域冻结（全局保守已覆盖本域）；冻结时的在途会话处置
  （只拒新 spawn 不动在途——诚实边界）；自动扩容联动（319 只建议）。

## Tickets

- [x] [T661 SpawnAdmissionFloor + SpawnGate 地板判定](../tickets/T661-admission-floor.md)
- [x] [T662 ErrorBudgetPolicy + 装配 + 收口](../tickets/T662-budget-freeze-policy.md)
