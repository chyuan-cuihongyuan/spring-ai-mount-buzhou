---
id: T1801
title: 全模块测试缺口审计与 R1 补测形态裁决
type: task
status: closed
assignee: zcode-k
blocked-by:
created: 2026-09-14
---

## Question

K 会话第 1 轮：用户目标是「完全覆盖到所有模块和细枝末节的完整完善测试集」。全仓 16 模块的测试缺口证据是什么？「细枝末节」的落点与形态如何裁决？

## Resolution

**用户常设授权 AFK（可推翻）**

决策（K 会话第 1 轮 = effort #1200 / spec 1200 / impl 903）：

1. **证据源唯一**：各模块 `target/site/jacoco/jacoco.csv` 逐类 LINE_MISSED/LINE_COVERED。零覆盖定义 `LINE_COVERED=0 && LINE_MISSED>=5`。扫描结论（2026-09-13 报告）：core 16 靶点 + guard 1（BuzhouGuardHealthAutoConfiguration）+ spill 1（BuzhouSpillHealthAutoConfiguration）+ resilience 1（ResilienceHealth 内部类）+ mcp 1（JdbcToolSetSpecStore）；memory/tools/observability/observe-otel/observe-dashboard/store-jdbc/store-redis **无零覆盖类**——审计覆盖全部 16 模块，故「所有模块」成立。
2. **「细枝末节」落点**：零覆盖类逐类直测（18 个测试文件），聚焦行为语义而非行数凑数——如 TableContextWindowResolver 的 override 精确键不走前缀、SessionStateStore 5 个 default 体（现被 InMemory 覆写遮蔽成死路径）、RecoverySupport attach 装配面与 onClose→COMPLETED 分支。
3. **豁免诚实入档不硬凑**：AgentSession 残余 11 行（内部匿名片段）、BuzhouCoreAutoConfiguration SmartLifecycle 匿名类 9 行（装配期样板）。
4. **跨模块执行盲区**：Spotlighting/TableContextWindowResolver 已被 guard/memory 测试真实执行但 core 报告为 0——JaCoCo 按模块独立统计的盲区；本模块直测仍补（回归防线第一层），report-aggregate 聚合留 R2+ fog。
5. **依赖裁决**：mcp 模块补 `h2` test scope（同仓 guard pom 先例，版本走仓库既有管理——非新引第三方）；JdbcToolSetSpecStore DDL 的 CLOB 类型在真实 PG 的方言隐患以 H2 行为测试 + （可行则）PG Testcontainers 显形，实测裁定。
6. **边界**：不改被测主代码行为；测试显形缺陷须单列票独立修复，不在补测轮夹带。
