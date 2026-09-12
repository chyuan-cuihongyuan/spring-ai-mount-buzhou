# 708 — HookTiming 滚动 max 读面

> 来源：G 会话第 9 轮 = effort #708（借鉴 micrometer `Timer` max 发布衰减窗）/ [T967](../../.wayfinder/tickets/T967-rolling-max-shape.md) / [T968](../../.wayfinder/tickets/T968-rolling-max-verify.md) / impl 511。

## 背景

HookTimingAggregator（spec 647）的 maxNanos 是进程生命周期 max——永不衰减。修好慢 hook（或换掉慢工具）后 max 仍顶着历史峰值，读面无法回答「现在还慢不慢」，处理后的验证也被历史峰值掩埋。

## 目标

- `RollingMaxCounter`（core/metrics，可复用公共类）：时间桶滚动 max——固定 8 桶 × 10s（常量默认 + 可注入构造），`record(v)` 按时钟定位桶（过期桶先重置再 CAS max）；`max()` = 活跃桶最大，全过期 = 0（诚实口径：窗口内无样本即无 max）。
- 时钟 `LongSupplier` 注入（默认系统毫秒——测试可推进、零等待）。
- 接入：HookTimingAggregator 的 Timing 增 windowedMax（record 路径同发）；既有生命周期 max 与 `stats()` 口径零变化；`windowedMax()` 返回 Map<hook, nanos> 不可变快照。
- HookTimingHealth 行增 `rollingMaxMicros`（details 是 Map——加键非破坏）。

## 非目标

不改 HookChain 链内私有 stats()（口径冻结）；不做工具侧同构（后续轮顺延）；不做可配置窗长（常量默认，诉求出现再开配置面）。

## 测试

注入时钟：峰值过窗归零、窗内保留、新样本覆盖；桶翻转重置正确；生命周期 max 不变；未开启聚合零镜像零回归。

## 兼容性

纯增量观测；新公共类入快照（下轮随再生入档——本轮无快照门窗口则收口轮兜底）。
