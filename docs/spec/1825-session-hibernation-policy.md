# Spec 1825 — 会话休眠分级（effort #1825，R26）

> wayfinder map：`.wayfinder/maps/effort-1800.md`（T2851–T2852，impl 1426）。借鉴：
> k8s scale-to-zero / duty-cycling——空闲负载分级回收足迹，唤醒税显式化。

## Problem Statement`

空闲会话常驻全热足迹（内存/句柄/索引槽）——全热与销毁之间缺分级：预降级
（轻税可逆）与降冷（重税重建）是两种不同的省法，「省多少 vs 醒多慢」没有
可算的档位面。

## Solution

`SessionHibernationPolicy`（core/session，静态纯函数）：

- `Policy(drowsyAfterMillis, hibernateAfterMillis)`（契约 0 ≤ 软阈 ≤ 硬阈）；
- `band(idle, policy)` 三档 ACTIVE/DROWSY/HIBERNATED（边界含上：达软阈进
  DROWSY、达硬阈进 HIBERNATED）；
- `profile(idle, policy)` 档位画像：唤醒税（ACTIVE 0 / DROWSY 50ms /
  HIBERNATED 2000ms 常量）+ 足迹比（1.0 / 0.5 / 0.1 常量）；
- `census(policy, idles)` 普查（三档计数 + footprintReduction 足迹节省率，
  无会话 -1 哨兵）。

## User Stories

1. 作为容量治理者，混合负载节省率 46% 直接读出——分级回收的实时收益。
2. 作为延迟治理者，DROWSY 唤醒税 50ms：高频回归会话停在预降级即回；冷会话
   降到 HIBERNATED 吃 2s 税也值。
3. 作为框架宿主，闲置口径（末次轮次/末次读）自声明，纯判档零执行。

## Implementation Decisions

- 纯判档不执行（降级/换页归宿主）；税与足迹为公开常量（禁魔法数，调用方
  可引用断言）。
- fail-fast：负闲置、阈值倒挂、null 元素；null 按空表。

## Testing Decisions

- 三档+双边界；画像单调（省得多醒得慢）；普查计数+节省率（全活 0/全冷
  0.9/空 -1）；畸形三型 fail-fast。节省率浮点直等假红一次，容差断言修正。

## Out of Scope

- 不执行降级；不做自适应阈值（idle 分布学习归未来静脉）。

## Further Notes

- 与 SessionArchiver（97）正交：那是终态归档，这是可逆分级。
