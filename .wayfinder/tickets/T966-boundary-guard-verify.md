---
id: T966
title: 模块边界守卫测试 + 存量违规清零的验证
type: task
status: closed
assignee: zcode-g
blocked-by: T965
created: 2026-09-13
---

## Question

守卫真能抓违规（不是恒绿）？迁移后全量测试绿？快照门与 SpecCoverage 门同步过？

## Resolution

**用户常设授权 AFK（可推翻）**

验证（G 会话第 8 轮）：① 守卫首轮跑红——抓到 import 扫描漏掉的 guard 内联 FQN 违规（自证有效非恒绿）；② 清零后守卫绿；③ core/memory/guard/resilience/observability/starter/examples 全模块测试绿；④ API 快照再生（全量 reactor 口径，spec 615 合规触发）恰 11 行新增零意外（6 个新公共类 + 5 个迁出类入面）；⑤ 全仓 verify 绿。
