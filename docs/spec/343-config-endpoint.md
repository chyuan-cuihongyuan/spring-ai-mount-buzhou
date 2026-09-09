# Spec 343 — 生效配置自描述端点（effort #343）

> wayfinder map：`.wayfinder/maps/effort-343.md`（T677–T678）。C 会话第 44 轮。

## Problem Statement

40+ 机制、几十个 `buzhou.*` 前缀开关：运维回答「这台实例开了什么、
阈值几何」需要同时翻 yml 声明、环境变量覆盖与代码默认——没有运行时
自描述面。

## Solution

`/actuator/buzhou-config`（BuzhouConfigSnapshotEndpoint，只读）：

- 枚举 Environment 全部 `buzhou.*` 生效属性（含 env/命令行覆盖——
  真实生效值而非声明值）；无属性 → 空 map 诚实。
- **密钥掩码**：键小写含 key/secret/password/token/credential 子串
  → 值掩 `***`（宽匹配宁掩勿漏——333 master-key 等绝不出端点）。
- 挂 actuator 条件配置类（332 probes 同位）；无 actuator 不装配。

## User Stories

1. 作为运维，我想一屏看到本实例全部 buzhou 生效配置，所以 「开了
   什么/阈值几何」不用翻三层来源。
2. 作为安全负责人，我想密钥类值自动掩码，所以 端点不成为泄密面。
3. 作为审计者，我想 env 覆盖也可见（生效值口径），所以 「yml 写 A
   而 env 覆盖成 B」的实况可查。
4. 作为使用者，我不想配任何 buzhou 属性时端点返回空 map，所以
   诚实不臆造。

## Implementation Decisions

- 数据源 Environment 直读（PropertySources iterate，relaxed binding
  键原样呈现）；有序稳定输出（键排序）。
- 掩码在端点层做（不改属性源）。

## Testing Decisions

- 端点：含 yml+env 键 / 掩码命中（master-key/secret 等变 ***）/
  非 buzhou 前缀不出现 / 空环境空 map / 键有序。
- 装配：有 actuator 才有端点。

## Out of Scope

- 写操作；分组视图；漂移对比；默认值推断（未设键不臆造——只呈现
  显式生效的）。

## Further Notes

- 新公共类型 `BuzhouConfigSnapshotEndpoint` 随轮 regenerate 快照。
