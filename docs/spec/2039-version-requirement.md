# Spec 2039 — 版本要求判定（effort #2039，R40）

> wayfinder map：`.wayfinder/maps/effort-2000.md`（T3181–T3182，impl 1590）。
> 借鉴：npm semver range——^/~/>=/精确/* 六算子（0.x 锁定惯例）。

## Problem Statement

技能声明运行时要求（requires）散落自由文本：装载时靠手写 if 比较版本
串（字典序错、"1.10" < "1.9" 经典坑）、兼容范围语义（^ 同主兼容）
无处表达——技能与运行时版本错配只能炸在运行深处。

## Solution

`VersionRequirement`（buzhou-skills，不可变线程安全，纯函数）：

- `parse(spec)` 六算子：`^1.2.3`（CARET——同主兼容 ≥1.2.3 <2.0.0；
  **0.x 特例**：^0.2.3 锁次 <0.3.0、^0.0.3 锁补丁 <0.0.4——semver 0.x
  惯例）、`~1.2.3`（TILDE 次锁定 <1.3.0）、`>=`/`>`（含/不含下界）、
  `1.4.2`（EXACT）、`*`（ANY）；畸形 fail-fast；
- `satisfies(version)`：点分逐段（短补 0——"1.10" 正确 > "1.9"）、
  prerelease 后缀低于同基段（与 SkillChannelResolver 同口径）。

## User Stories

1. 作为技能作者，requires: "^1.2.0" 声明兼容范围——装载即判，错配
   显形在门口。
2. 作为运行时，0.x 期 breaking 频繁——^0.2.3 自动锁次，不误放 0.3。

## Testing Decisions

- CARET 稳定主/0.2 锁次/0.0.3 锁补丁三型；TILDE 次锁；GTE/GT 边含；
  EXACT/ANY；prerelease 恰界低于（>=1.2.3 不认 1.2.3-rc1）+ 高于界
  认 + EXACT 不认预发；短版本补 0（~1.2 匹 1.2/1.2.5 拒 1.3）；畸形
  五型 fail-fast。

## Out of Scope

- 不做复合范围（"||" 或 空格 AND——单算子口径）；不做 x 通配段
 （"1.x"——ANY/算子已覆盖主流）；不接技能装载链（接线归后续轮）。

## Further Notes

- 与 SkillChannelResolver（发布通道解析）同模块互补：那选通道，这判
  版本门。
