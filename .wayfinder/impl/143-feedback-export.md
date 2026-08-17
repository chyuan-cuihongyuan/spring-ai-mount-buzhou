# 143 — 反馈导出与评估衔接

**Parent:** spec 48 §A / [T174](../tickets/T174-feedback-export.md)

**Status:** done

- [x] FeedbackExporter（core.feedback 段；entries 行 + negative 极性 + negativeTurnSeqs 汇总）
- [x] FEEDBACK_PREFIX 常量收敛单一事实源；Spring 装配 @ConditionalOnBean(BuzhouStores) + @ConditionalOnMissingBean
- [x] 导入按原键回放；空反馈段缺席（既有导出零变化）
- [x] 测试：混合反馈导出断言 / 导入回放 + 往返保真 / runtime 端到端含段 / 启动校验回归
