# 749 — G 会话 700 系收口

> 来源：G 会话第 50 轮 = effort #749（D/E/F 会话收口模式）/ [T1049](../../.wayfinder/tickets/T1049-session-g-closing.md) / [T1050](../../.wayfinder/tickets/T1050-session-g-closing-verify.md) / impl 552。

## 背景

50 轮自迭代完成（第 1–49 轮台账见 effort-700 map）——终验三件事：① 全仓 `mvn -B -ntp clean verify` 终验；② 地图 Destination 达成标记 + 雾区清理；③ 票/impl/spec 三台账闭环核查（第 49 轮预检已过）。

## 目标

- 全仓 verify 绿（16 模块 + JaCoCo ≥70% + enforcer + 快照门 + SpecCoverage）；
- effort-700 map：Destination 达成 + 50 轮台账全 ✅ + 票 T951–T1050 全闭环；
- MAP.md 总索引：#700 标记已收口；
- 收口提交（Conventional Commits）推送 GitHub。

## 兼容性

收口轮纯文档/验证域 + 全仓终验。
