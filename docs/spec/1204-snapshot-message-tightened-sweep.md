# 1204 — SnapshotMessage 补测与收紧判据跨模块复核（R5）

> 来源：K 会话第 5 轮 = effort #1204（[T1813](../../.wayfinder/tickets/T1813-snapshot-message-and-tightened-sweep-shape.md) / [T1814](../../.wayfinder/tickets/T1814-snapshot-message-and-tightened-sweep-verify.md) / impl 907）。方法论：判据收紧后的**残留归口**——每个新浮出项要么补测、要么显式豁免，不许静默（「判据是过滤噪音的工具，不是豁免义务」的延续）。

## Problem Statement

R4 把 zero 判据从 miss≥5 收紧到 miss≥1，复核随即浮出 `SnapshotMessage`（mis=2）——compact 构造的 null 防御分支从未执行。该 record 是注入快照还原「模型当时实际看到什么」的格式合同（微压缩占位符 / spill 引用句柄），metadata 缺省化行为是其读面合同的一部分。同时，收紧判据从未在旧判据期（9/13–9/14）生成报告的小模块上复核过——存在同类浮出项的系统性可能。

## 目标

- **SnapshotMessageTest**（compact 构造防御合同）：
  - null metadata → 空 Map（不可 null 的读面合同）；
  - 传入 metadata 防御拷贝（`Map.copyOf` 语义：构造后外部 mutation 不透传）；
  - spillUri / evidenceId 字段透传（微压缩占位符与 spill 句柄双路径的字段面）。
- **跨模块收紧复扫**：tools / observability / observe-otel / observe-dashboard / spill / resilience 六模块在隔离 worktree 以 miss≥1 口径重扫，浮出项清单入 map 台账归 R6+（不在本轮夹带实现）。

## 实现决策

- 纯测试增量：SnapshotMessageTest 与被测类同包（spi），record 直接构造，主代码零变化。
- 跨模块复扫在 `.scratch/k-wt` 隔离 worktree 执行（R4 确立的证据基建口径），小模块逐个 `test jacoco:report` 后扫 csv。
- store-jdbc / store-redis（H2/fake 本地实测 96–99%）、memory / guard / skills（9/15 新鲜报告）不在复核列。

## 测试决策

- 好测试标准：断言 compact 构造的输入→字段合同（null 防御 + 拷贝语义），不测 record 的 equals/hashCode 编译合成。
- seam：构造器单点。
- 先例：ToolSetSpecTest（R1，record 紧凑构造校验 + 防御拷贝同型断言）。
- 验收门：SnapshotMessage 清零 + 复扫清单完整入档 + core 定向测试绿。

## 兼容性

纯测试增量：主代码零变化、公共 API 面零变化、既有测试零改动。

## Out of Scope

- 浮出项的补测实现归 R6+（本轮只清单化，单轮单scope）。
- 不做 record equals/hashCode/toString 编译合成行断言。
- 不触碰 I/J 会话在跑主题与号段产物。

## Further Notes

- 收敛信号入台账：miss≥1 口径下 core 浮出量 R4=2 → R5=1，呈收敛态；R6 起该口径的增量复核并入周期性对账轮，不再单开专轮——「判据收紧 → 清扫 → 归口常规化」三步收官。
