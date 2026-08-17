---
Type: task
Status: closed
blocked-by: T191-feedback-import.md, T193-eval-runner.md
---
## Question

评估面对抗：回流注入非法反馈值拒绝 / dataset 越界 item 拒绝 / run 记录篡改只读面 /
评估会话隔离（评估 spawn 不污染业务会话计数）四用例。

## Resolution

impl-162 落地四用例：伪造超范围 turnSeq 负反馈被 skippedMissingReply 挡；dataset 名键结构
注入（点/斜杠/空格/冒号）被 NAME_PATTERN 拒；篡改 run 记录查询面 DATA_CORRUPTION 快速失败；
业务会话在途与评估 run 并存互不干扰（历史无混入）。4 用例绿。T196 关闭。
