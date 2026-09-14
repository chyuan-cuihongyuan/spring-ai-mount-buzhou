---
id: T1817
title: K 会话周期对账轮 R6 验证
type: task
status: closed
assignee: zcode-k
blocked-by:
  - T1816
created: 2026-09-15
---

## Question

R6 对账执行结果：全仓 verify 是否绿？工件链五项对账是否全 OK？发现的不一致是否入档？

## Resolution

**用户常设授权 AFK（可推翻）**

对账结论（2026-09-15，隔离 worktree 固定提交点两次全仓 verify + 脚本清单）：

1. **工件链五项全 OK**：spec 1200–1205 存在且 README 各一行；票 T1801–T1818 全 closed（T1819 open 已排下轮）；impl 903–908 全 done；map Decisions 全登记；K 线增量 33 用例前轮隔离证据全绿。
2. **全仓 verify 两次执行均为「15/16 绿 + starter 显形红」**：
   - 第一次红 = API 快照过期（T1818，非破坏 added 1 项）——已修，reactor 联编复验绿；
   - 第二次红 = ToolDurationTimerTest 顺序扰动（T1819，Webhook 测试泄漏后台重试线程污染全局指标捕获）——单列票下轮修，本轮诚实入档；其余 15 模块（core 除该 1 用例外 2652/2653 绿、memory、spill、skills、mcp、guard、resilience、tools、observability、observe-otel、observe-dashboard、store-jdbc、store-redis）全绿，examples 因 starter 失败连带跳过。
3. **过程风险入档（对账轮的元收获）**：多会话共享工作区下验证基座频遭扰动——worktree 目录被并行清理删除、主工作区曾被切到 detached HEAD、未提交工件两度被他线全量 add 卷入（T1815→cc084a62、T1818 文本→64da66cf 后续 sweep，均路径追认）；「固定提交点 + 隔离 worktree + 显式路径 add」为本线既定对策。
4. **R7 议程**：T1819 修复（治本 = forwarder 测试显式收尾其调度线程）+ 雾区裁决轮（BRANCH 门限校准数据已在手 / report-aggregate 可行性）。
