# 157 — 负反馈回流 API

**Parent:** spec 52 §B / [T191](../tickets/T191-feedback-import.md)

**Status:** done

- [x] FeedbackImporter.importFromFeedback(sessionId, datasetName)：scan 负反馈（isNegative
  单一事实源口径——FeedbackExporter.isNegative/decode 提 public 供跨包复用）→ 轮内 user 输入 +
  首条非空 assistant 回复 → addItem 带溯源
- [x] 幂等去重（数据集内同 sessionId#turnSeq 跳过）；无回复轮 skippedMissingReply（不造空
  expected 项）；未建 dataset fail-fast
- [x] FeedbackImportResult{imported, skippedDuplicate, skippedMissingReply}
- [x] 2 新测试绿 + FeedbackExporterTest 回归绿
