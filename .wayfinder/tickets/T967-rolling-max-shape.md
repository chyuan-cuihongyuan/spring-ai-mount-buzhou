---
id: T967
title: HookTiming 滚动 max 读面的形态裁决
type: task
status: closed
assignee: zcode-g
blocked-by:
created: 2026-09-13
---

## Question

HookTimingAggregator 的 maxNanos 是进程生命周期 max——永不衰减。修好慢 hook 后 max 仍顶着历史峰值，「现在还慢不慢」不可答。Micrometer Timer max 的发布衰减窗口怎么映射？

## Resolution

**用户常设授权 AFK（可推翻）**

决策（G 会话第 9 轮 = effort #708 / spec 708 / impl 511）：新公共类 `RollingMaxCounter`（core/metrics，可复用）——时间桶滚动 max：固定 BUCKETS×BUCKET_MILLIS 环（默认 8×10s=80s 窗，常量 + 可注入构造），`record(v)` 按 `clockMillis()/BUCKET_MILLIS` 定位桶（过期桶先重置）CAS 更新桶内 max；`max()` = 活跃桶 max（全过期=0，诚实口径：窗口内无样本即无 max）。接入：HookTimingAggregator.Timing 增 windowedMax 成员（record 路径同发——既有生命周期 max 不动）；`windowedMax()` 快照 Map<hook, nanos>；HookTimingHealth 行增 `rollingMaxMicros`（details 是 Map——加键非破坏）。时钟 `LongSupplier` 注入（测试可推进）。借鉴 micrometer `Timer` max 发布衰减窗。
