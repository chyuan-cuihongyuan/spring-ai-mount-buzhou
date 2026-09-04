# Spec 335 — 错误预算政策·烧穿自动降级（effort #335）

> wayfinder map：`.wayfinder335/MAP.md`（T661–T662）。C 会话第 36 轮。

## Problem Statement

错误预算燃尽率（321）烧穿了只是「有人知道」——观察与告警之后没有
动作。Google SRE 的 error budget policy 实践：预算烧穿 → 冻结低优先级
工作（用可靠性换稳定），预算回充才解冻。buzhou 已有三级优先 spawn
排队（123），缺的是「烧穿时准入地板自动抬到 HIGH」的那只手。

## Solution

- **`SpawnAdmissionFloor`**（core.backpressure）：共享地板槽——volatile
  优先级，默认 LOW（零变化）；政策驱动、gate 读取（解耦两者装配顺序——
  gate 在 runtime 装配期构造，政策是独立 SmartLifecycle bean）。
- **SpawnGate 地板判定**：`acquireSlotOrThrow` 入口先查地板——低于地板
  的优先级立即拒（`admission-floor` 原因 + 拒绝事件 + 容量异常），先于
  容量与排队判定（FAIL_FAST 档同样生效）；未配地板恒 LOW——存量行为
  零变化（既有 SpawnGate 测试不改动全绿即证明）。
- **`ErrorBudgetPolicy`**（SmartLifecycle 自管虚拟线程，周期评估
  `ErrorBudget.anyBreaching`）：任一 scope 烧穿 → 地板抬 HIGH（NORMAL/
  LOW spawn 即拒——SRE 冻结：烧穿期只保关键租户/运维接管通道）；连续
  两轮评估清明 → 解冻回 LOW（防抖——单轮清明不立即解冻）；冻结/解冻
  WARN + `buzhou.errorbudget.freeze/unfreeze` 计数；观测面 `view()`
  （frozen/since/evaluations）。
- **装配**：`buzhou.backpressure.error-budget-freeze.{enabled,interval}`
  （默认关——enabled=true 且 ErrorBudget bean 在场才装配；无 ErrorBudget
  喂数启动红——无观察面的政策是盲动）。

## User Stories

1. 作为 SRE，我想错误预算烧穿时自动冻结低优先级 spawn，所以 恶化期
   有限容量全部让给关键租户，而不是被批处理挤占。
2. 作为 SRE，我想冻结在预算回充（连续清明）后自动解除，所以 不需要
   人工值守解冻。
3. 作为运维，我想单轮清明不立即解冻（防抖），所以 边界抖动不会造成
   冻结/解冻振荡。
4. 作为审计者，我想冻结/解冻都留 WARN 与计数，所以 事后能回答「昨晚
   为什么批任务全拒了」。
5. 作为运维接管者，我想 HIGH 优先级在冻结期照常准入，所以 事故处理
   通道不被自己的保护机制锁死。
6. 作为使用者，我不想启用该政策时 spawn 行为与现状完全一致，所以
   升级零风险。

## Implementation Decisions

- 地板判定在 gate 入口最前（低于地板先于容量/排队/drain 判定——语义
  顺序即保护优先级）。
- 全局冻结（任一 scope 烧穿即抬地板）——分域冻结留待 scope→priority
  映射需求出现。
- 新公共类型：`SpawnAdmissionFloor` + `ErrorBudgetPolicy` +
  `ErrorBudgetFreezeProperties`（独立属性 record，不动 BuzhouBackpressureProperties）。

## Testing Decisions

- Gate：地板 HIGH 拒 NORMAL/LOW（异常+事件原因）/HIGH 放行/无地板零
  变化；FAIL_FAST 档同样拒。
- 政策：烧穿→HIGH（计数）/单轮清明不解冻/连续两轮清明解冻/stop 停
  轮询。
- 装配：enabled=true+ErrorBudget 在场 → 政策+地板 bean；enabled 缺省
  → 两者皆无；无 ErrorBudget 启动红。
- 先例：SpawnGate 既有测试（零变化证明）、AlertRuleAssemblyTest。

## Out of Scope

- 按 scope 分域冻结；冻结时处置在途会话（只拒新 spawn 不动在途——
  诚实边界）；熔断/降级联动编排（各机制独立正交）。

## Further Notes

- 与 321/322 关系：321 观察烧穿、322 演练韧性、335 烧穿自动让路——
  预算治理从「知道」走到「行动」。
- 新公共类型随轮 regenerate API 快照。
