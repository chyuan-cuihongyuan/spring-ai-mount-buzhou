# effort #845 — Prompt 回滚使用读数

- 会话：H 会话 800 系第 46 轮 ｜ spec [845](../../../docs/spec/845-rollback-usage-stats.md) ｜ 票 [T1191](../tickets/T1191-rollback-usage-stats.md)/[T1192](../tickets/T1192-rollback-usage-stats-verify.md) ｜ impl598
- 借鉴：S6 备选池（Langfuse rollback 借鉴的观测面——回滚机制的使用统计）

## 勘察（排重）

- PromptVersion/PromptRegistry：版本与解析——回滚使用统计缺位。
- PromptVersionDiff：版本差异对比——使用频次不同面。
- grep -i `rollback.*count|rollback.*stat`：无命中。

## 决定

`RollbackUsageStats`（core.prompt，synchronized 记账）：record(name, fromV, toV, atMs)——prompt 名开集封顶 64（超限并入 overflow 桶）；rollbacks/lastFrom/lastTo/lastSeen(max)+total；snapshot 次数降序典序破平；null/空白/负版本忽略；空真。喂点=回滚执行处装配侧。

## 测试

计数降序+最近版本对+lastSeen/溢出桶 64+1+total/脏入参三形态+空真——3 例绿（嵌套 Report 漏定义编译错误修正）。

## 诚实边界

回滚执行归 PromptRegistry（本类只统计）；overflow 桶不可回溯具体名；喂点手动。
