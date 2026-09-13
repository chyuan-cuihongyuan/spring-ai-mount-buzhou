# effort #717 — 共享事实冲突审计

- 会话：G 会话 700 系第 18 轮 ｜ spec [717](../../../docs/spec/717-fact-conflict-audit.md) ｜ 票 [T1034](../tickets/T1034-fact-conflict-audit.md)/[T1035](../tickets/T1035-fact-conflict-audit-verify.md) ｜ impl617
- 借鉴：mem0（mem0ai/mem0 ≈30K star）冲突事实治理——同键异值是记忆库腐化第一信号

## 勘察（排重）

- SharedFact（410）所有权模型：非 owner 发布已存在键 fail-fast——**单 store 内**有写防护；但导出/合并/多实例聚合场景（FactsExporter 导出集、跨实例恢复）冲突可静默进入。
- 604 置信度衰减是时间维度——值冲突维度无面。
- grep Conflict：fact 族零命中。

## 决定

`FactConflictAudit`（core/fact 纯函数）：audit(List<SharedFact>)→Report——按键分组：CONFLICT（同键 ≥2 个互异值—— owners+values 全列）/DUPLICATE（同键同值多 owner——重复发布信号）/keysScanned+conflictKeys 计数；value 比较用 Objects.equals。纯读数（裁决/清理归宿主——所有权冲突解决语义复杂度不抵收益）。

## 测试

同键异值 CONFLICT/同键同值异 owner DUPLICATE/健康单键零发现/null fail-fast。

## 诚实边界

值等价用 Objects.equals（对象语义等价需宿主投影后比较）；不裁决谁对（合并策略是业务语义）；只审计提交的快照（不盯 store 实时变更）。
