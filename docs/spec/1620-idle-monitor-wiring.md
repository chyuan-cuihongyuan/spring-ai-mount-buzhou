# 1620 · 空闲监控全链接线（spec 161/179/841 三孤类救活）

> 来源：N 会话 R21（effort #1620 / T2391–T2392 / impl 1173）。spec 1611 普查修复
> 第七弹——且比普查发现的更深：IdleSessionMonitor 与 IdleDurationHistogram 的
> 喂数面 SessionFeaturesHook（spec 161）本身也未装配，三件构成断链。

## Solution

- `IdleMonitorHolder`：进程级 SessionFeatureStore（15 分钟阈值监控器 + 直方）。
- `SessionFeaturesHook`：构造 null store → Holder store；新增 afterTurn 覆写——
  每 32 轮节拍 sweepAndRecord（sweep 判定 + 翻转通知 + 空闲时长喂直方；观测
  旁路失败静默不伤轮次）。
- 装配：`buzhou.session.features.enabled`（默认开——纯记账旁路；显式 false 关），
  BuzhouHook bean 经 List<BuzhouHook> 自动收集。

## Testing Decisions

- `IdleMonitorHolderTest` 三断言：全链（recordTurnStart → 16 分钟后 sweep 含
  两会话 + 翻转通知 + 直方 total=2）；Holder 便捷面（sweepAndRecord 喂静态
  直方）；hook 喂数（null 构造走 Holder store，turns=1）。
- 回归：IdleSessionMonitorTest / SessionFeaturesTest / IdleDurationHistogramTest。

## Out of Scope

- 空闲清单的动作面（压缩/归档/排水以清单为候选——分层诚实，后续轮）。
- sweep 的定时调度（轮次节拍已够——零会话期无 sweep 语义上正确）。
