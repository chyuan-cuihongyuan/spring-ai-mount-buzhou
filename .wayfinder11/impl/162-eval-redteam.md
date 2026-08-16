# 162 — 评估面红队对抗

**Parent:** spec 52 对抗面 / [T196](../tickets/T196-eval-redteam.md)

**Status:** done

- [x] 注入①：伪造超范围 turnSeq 负反馈（直写 state）→ skippedMissingReply 挡（不入集不崩）
- [x] 注入②：dataset 名键结构注入（点/斜杠/空格/冒号）→ NAME_PATTERN 拒绝（键布局不可逃逸）
- [x] 篡改：run 记录 JSON 损坏 → 查询面 DATA_CORRUPTION 快速失败（不静默半解析）
- [x] 隔离：业务会话在途 + 评估 run 并存互不干扰（命名空间独立 + 历史无混入）
- [x] 4 用例绿
