---
Type: task
Status: closed
blocked-by: T190-eval-dataset-store.md
---
## Question

importFromFeedback：从会话负反馈（buzhou.feedback. 前缀 + negativeTurnSeqs 汇总）一键回流
评估项；输入消息为 input、轮次 assistant 回复为 expected（可空）；同轮去重；回流计数返回。

## Resolution

impl-157 落地：isNegative/decode 提 public（单一事实源口径跨包复用）；回流只入负轮（boolean
false / numeric 负值），categorical 无极性不入；幂等去重（sessionId#turnSeq）；无 assistant
回复轮跳过计数（不造空 expected 项——spec 裁定 expected 强制非空，修订票面「可空」表述）；
未建 dataset fail-fast。2 新测试 + 导出器回归绿。T191 关闭。
