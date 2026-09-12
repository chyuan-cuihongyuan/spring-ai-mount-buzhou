---
id: T984
title: 告警规则 dry-run 的验证
type: task
status: closed
assignee: zcode-g
blocked-by: T983
created: 2026-09-13
---

## Question

三不承诺真成立（状态机/通知/指标零副作用）？wouldFire/wouldRecover/pending 三分类正确？实弹语义不受污染？

## Resolution

**用户常设授权 AFK（可推翻）**

验证（G 会话第 17 轮）：① DOWN+for=0 → wouldFire 命中且 firingView 仍空、listener 未被调、CapturingMetrics 零事件；② 实弹 firing 后置 UP → dryRun wouldRecover 含该规则且真实状态机不动（evaluate 前不自行恢复）；③ DOWN+for=5m 刚开始 → pending 含剩余时长；④ dryRun 后紧跟 evaluate → 实弹行为与无 dryRun 时一致（状态零污染实证）。`mvn -pl buzhou-core -am test` 全绿。
