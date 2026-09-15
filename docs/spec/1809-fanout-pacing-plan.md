# Spec 1809 — 扇出 pacing 计划（effort #1809，R10）

> wayfinder map：`.wayfinder/maps/effort-1800.md`（T2819–T2820，impl 1410）。借鉴：
> TCP 拥塞控制 pacing + 初始拥塞窗口（IW）——头部 IW 段立即发（小流零节流），
> 其余按 pacing 间隔匀速放行（大流不惊群）。

## Problem Statement

并行工具扇出一股脑全发是惊群：N 个调用同瞬间砸向下游（瞬时过载、限流
集体触发、自己排队挨饿）；但全量匀速又让小扇出白付延迟——「多小的扇出
免节流、多大的扇出要摊平」没有排程语义。

## Solution

`FanoutPacingPlan`（core/exec，静态纯函数）：

- `plan(fanout, intervalMillis, headStart)` → `Plan(fanout, intervalMillis,
  headStart, starts)`：前 headStart 个任务延迟 0（IW 语义），第 i（≥headStart）
  个延迟 (i − headStart + 1) × interval；
- `totalSpanMillis()` 计划跨度 + `pacedRatio()` 被节流占比（零扇出 -1 哨兵）；
- 契约 fail-fast：负扇出 / 间隔 < 1 / 名额越界。

## User Stories

1. 作为工具编排者，5 并发 headStart=2、interval=100ms → 前 2 个立即发、
   余 3 个按 100/200/300ms 摊平——小流零延迟、大流不惊群。
2. 作为下游守护者，pacedRatio 读数直接对应我承受的到达曲线摊平程度。
3. 作为框架宿主，间隔与名额口径自声明，纯排程零状态可回放。

## Implementation Decisions

- 纯排程不执行（派发归宿主）；core/exec 与工具执行脊柱同包。
- headStart=fanout 退化为全立即（等于现状），headStart=0 全量 pacing——
  两极语义连续可调。

## Testing Decisions

- 头部立即+尾部匀速；两极（全 pacing/全立即）；零扇出空计划哨兵；畸形
  四型 fail-fast；任务序连续无缺号。

## Out of Scope

- 不接 HarnessToolCallingManager 热路径（接线归后续轮）；不做动态间隔
  （BBR 式带宽估计归未来静脉）。

## Further Notes

- 与 TurnDeadlineBudget 正交：那是预算裁决，这是起发节奏排程。
