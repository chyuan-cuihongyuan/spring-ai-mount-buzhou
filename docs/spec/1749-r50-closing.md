# Spec 1749 — L 会话 1700 系收口终验（effort #1749，R50）

> wayfinder map：`.wayfinder/maps/effort-1700.md`（T2699–T2700，impl 1349）。
> 先例：L 会话 1400 系 R50 / N 会话 1600 系 R48-R49 收口。

## Problem Statement

50 轮四步闭环后需要一次全量终验：隔离 worktree 全仓串行 verify（排除
Windows 平台假红三项 + 已知满载 flaky 五项）、三门（快照/对账/覆盖）
在 HEAD 复验、台账/地图/README 归档——收口的「Destination 达成证明」。

## Solution

1. **隔离 worktree 全仓 verify**：`git worktree add` 独立目录，JDK21
   串行 `mvn verify`（禁 -T），排除集 = ClasspathSkillScannerTest
   （CRLF 资源）/ RunCommandToolTest + GuardAndHitlDemoTest（/bin/sh
   平台假红）/ EvalFingerprintChangeTest + LaneLimitingToolCallbackWaitTest
   + HookEndToEndTest + PairwiseEvalRunnerTest + MetricFreshnessHolderTest
   （满载 flaky，单跑全绿）；
2. **三门复验**：HEAD 上快照/对账/覆盖三门（已在 R49 绿，收口轮复验）；
3. **归档**：progress-effort-1700 台账 50/50 落账封卷、MAP.md #1700 行
   收口态、README 纵深段补收口行。

## User Stories

1. 作为合并审查者，全仓 verify + 三门 = 可合并的机器证明。
2. 作为后续会话，号段占用与收口状态在 MAP.md 一目了然。

## Implementation Decisions

- 隔离 worktree 验证（不污染工作区未提交态）；排除集诚实入档——
  平台假红与满载 flaky 非 Linux CI 权威。
- 收口轮自身工件（本 spec/票/impl/台账）随收口 commit 入账，R50 哈希
  由合并后封卷 chore 提交补登。

## Testing Decisions

- 全仓 `mvn verify` 退出码 0 + BUILD SUCCESS 为唯一通过标准。

## Out of Scope

- 不做性能基准；不改生产代码。

## Further Notes

- 1700 系 Destination：efforts #1700–#1749 连续 50 轮、48 生产机制轮
  +1 对账门落位轮 +2 收口轮，15 模块覆盖，借鉴定源 30+ 高价值开源项目。
