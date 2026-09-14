# 1206 — R7：T1819 治本 + 两雾区裁决（BRANCH 读面化 / report-aggregate 不引入）

> 来源：K 会话第 7 轮 = effort #1206（[T1820](../../.wayfinder/tickets/T1820-fog-adjudication-r7-shape.md) / [T1821](../../.wayfinder/tickets/T1821-fog-adjudication-r7-verify.md) / impl 909）。方法论：**ADR 式雾区裁决**——决策即产物：证据、选项、理由、后果四段入档；「不引入」与「引入」同为正经决策，不许悬置。

## Problem Statement

R6 对账显形 T1819（WebhookDeadReplayAuditTest 不 close 自启 dispatcher 的 forwarder，泄漏后台重试线程向后续测试窗口的全局指标捕获注入外来条目——顺序扰动型 flake），需治本修复。两个雾区悬置多轮：BRANCH 维度（曾以「缺校准数据」为由搁置）与 report-aggregate 聚合报告（曾以「可行性未研究」为由搁置）——本轮以实测数据一次裁决。

## 目标

- **T1819 治本**：WebhookDeadReplayAuditTest 登记 + @AfterEach close 全部 forwarder（构造即自启 dispatcher 虚拟线程，「谁启动谁收尾」）。
- **BRANCH 裁决**：13 模块实测分布 52.1%–81.6%（中位 ~71%）入台账；纳入对账轮报告读面，硬门暂缓（≥60% 即红 store-redis/observe-otel——缺口属容器门控，非补测纪律可达；50% 无判别力），待缺口批次后另票开启。
- **report-aggregate 裁决**：不引入——聚合模块 = 09 模块工程档结构变更（spec 级）+ BOM/enforcer 对齐成本；其价值已被「本模块直测纪律」实质覆盖（统计盲区的门禁风险已消除，缺的只是视图）。

## 实现决策

- T1819 修复为测试侧最小变更：登记列表 + @AfterEach forEach close；不改被测断言（精确合同是语义）、不动主代码。
- BRANCH 数据来自隔离 worktree `jacoco.xml` BRANCH counter 聚合（LINE 同时留痕：71.2%–91.8%）。
- 雾区清单收敛：BRANCH（裁决=读面化+有条件暂缓）、report-aggregate（裁决=不引入）、收紧判据跨模块复核（R5 已收敛除名）——**K 线雾区清零**。

## 测试决策

- 泄漏修复验证 = 泄漏源与受害者测试同 JVM 群组定向跑（WebhookDeadReplayAuditTest + ToolDurationTimerTest + DeadLetterCapTest + WebhookFanoutTest）全绿；竞态类修复的结构正确性由 close 语义保证（dispatcher 对 closing 标志有界退出），不靠重试次数堆验证。

## 兼容性

纯测试增量 + 决策入档：主代码零变化、公共 API 面零变化、构建配置零变化（BRANCH 不进 jacoco check rule）。

## Out of Scope

- store-redis / observe-otel 分支缺口补批（硬门开启的前置，另票）。
- CI 失败消息聚合视图（report-aggregate 的替代品也不做）。
- 不触碰 I/J/M/N 会话在跑主题。

## Further Notes

- 「不引入」决策的成本写明：跨模块归因视图缺位——每当需要回答「哪个模块的测试执行了本类」仍靠人工 grep；接受此成本换取模块工程档稳定。
- K 线雾区自此清零：后续轮次 = 周期对账轮（R9 起）+ 他线委托/显形议题。
