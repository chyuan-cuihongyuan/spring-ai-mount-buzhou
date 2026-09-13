# effort #829 — 事实合并决策分布

- 会话：H 会话 800 系第 30 轮 ｜ spec [829](../../../docs/spec/829-fact-merge-decisions.md) ｜ 票 [T1159](../tickets/T1159-fact-merge-decisions.md)/[T1160](../tickets/T1160-fact-merge-decisions-verify.md) ｜ impl582
- 借鉴：mem0 冲突解决统计扩散（mem0ai/mem0 ≈30K+；828 对账管线的分布面——G 会话 R35 思想族深化）

## 勘察（排重）

- SummaryFactReconciler：reconcile 执行面——无决策分布读数。
- BiTemporalFactLedger：supersede 留痕（双时态账）——非统计聚合。
- grep -i `merge.*decision|decision.*distribution`：无命中。

## 决定

`FactMergeDecisionDistribution`（memory，synchronized 记账）：record(section, decision)——三分决策 CREATED/KEPT/SUPERSEDED×9 段闭集（27 格天然有界）；Report：total/created/kept/superseded/supersededRatio+段行（声明序、只含触碰段）；null 双参忽略；空真。喂点=对账管线装配侧（EVENT_RECONCILED 消费者）——不改 reconcile 行为。

## 测试

三分计数+替换率 0.5+段行三分对账/未触碰段不出现+声明序保持/null 双参忽略+空真——3 例绿（SummarySection 在 memory.summary 子包——import 修正后绿）。

## 诚实边界

记账脑不接 reconcile 主路径（读数纪律）；「改写激进/保守」是决策者（LLM）行为画像——本类只计数不评价；跨会话聚合由调用方分桶。
