---
Type: task
Status: closed
blocked-by: T173-turn-feedback.md
---
## Question

SessionExport 扩展段带 feedback 记录（memory.facts 同款 extensions 机制）；导出/导入往返保真；评估集口径：负反馈（boolean=false 或 numeric 低分）turn 在导出文档中带标记，供离线评估筛选用。验证：往返单测 + 标记断言。

## Resolution

spec 48 §A / impl-143 落地：FeedbackExporter（SessionExportExtension，段名 core.feedback）——
导出 scanByPrefix 解码为结构化行 + negative 极性标记（boolean false / numeric 负值）+
negativeTurnSeqs 去重升序汇总（评估集筛选用）；导入按原键回放（producer/createdTurn 保留）；
空反馈段缺席。FEEDBACK_PREFIX 收敛单一事实源；Spring 装配 @ConditionalOnBean(BuzhouStores)
（实现期纠偏：无条件 bean 在 store.type 拼错场景抢跑既有 fail-fast 引导文案，条件化后回归绿）。
往返保真测试（导出→导入→再导出 JSON 等价）钉住。core 全绿。
