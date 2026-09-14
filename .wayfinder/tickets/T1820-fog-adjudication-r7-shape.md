---
id: T1820
title: R7 形态裁决——T1819 修复形态 + 两雾区（BRANCH 门限 / report-aggregate）处置
type: task
status: closed
assignee: zcode-k
blocked-by:
created: 2026-09-15
---

## Question

K 会话第 7 轮（雾区裁决轮）：T1819 泄漏的治本形态？两个悬置雾区——BRANCH 维度是否入硬门、JaCoCo report-aggregate 是否引入——依据何种证据裁决？

## Resolution

**用户常设授权 AFK（可推翻）**

决策（K 会话第 7 轮 = effort #1206 / spec 1206 / impl 909）：

1. **T1819 治本形态（已实施）**：WebhookDeadReplayAuditTest 补 forwarder 登记列表 + @AfterEach 统一 close——构造即自启的 dispatcher 虚拟线程不得越测试类存活（「谁启动谁收尾」资源纪律）；不改 ToolDurationTimerTest 的严格断言（精确合同是被测语义）；主代码零变化。
2. **BRANCH 维度：纳入报告读面，硬门暂缓**。证据（隔离 worktree 13 模块实测）：BRANCH 分布 52.1%（store-redis）–81.6%（tools），中位 ~71%。硬门两难：门槛 ≥60% 则 store-redis（52.1%，容器门控下本地实测）与 observe-otel（62.9%）即红——涨点属容器测试增强而非本线补测纪律可达；门槛 50% 则形同虚设。裁决 = BRANCH 计数入周期对账轮读面台账（每轮留痕追踪趋势），硬门待 store-redis/otel 分支缺口批次补齐后另票开启。
3. **report-aggregate：不引入（ruled-out 入档）**。理由：需新增聚合报告模块 = 改动 09 模块工程档（spec 级结构决策）+ BOM/enforcer 对齐成本；其价值（跨模块归因视图）已被「本模块直测纪律 + K 线隔离 worktree 复扫口径」实质覆盖——统计盲区的*门禁*风险已由直测消除，聚合报告只是锦上添花的视图。spec 09 结构不动。
4. **边界**：两裁决均为过程决策，零代码；T1819 修复为测试侧（已验）。
