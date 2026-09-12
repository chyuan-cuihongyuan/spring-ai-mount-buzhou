# 497 — JSON 行手工拼接收口

**What to build:** PromptUsageJsonl / ExportBundle.manifestJson / FailureTurnSnapshots / WebhookDeadLetterJsonl 四处手工拼 JSON 统一改 Jackson writeValueAsString；删两份重复私有 escape；注入对抗用例。

**Blocked by:** None — can start immediately.

**Status:** done

- [x] 四处 Jackson 化
- [x] 两份私有 escape 删除
- [x] 四处注入回读对抗用例 + 既有零回归
- [x] spec 644 + README 行
- [x] 全模块测试绿

## Done

验证：`mvn -pl buzhou-core -am test` 绿。commit 见本轮 `fix(core)` 提交。
