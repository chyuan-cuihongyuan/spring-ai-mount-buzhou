# 1196 — 流式 PII 类型级豁免

**What to build:** PiiStreamRedactionHook 4 参构造 + filter 创建时生效集剔除。

**Blocked by:** None — can start immediately.

**Status:** done

- [x] replyStreamFilter 类型级剔除（每轮窗口视图）
- [x] 两断言（chunk 化同先例型）+ guard 372 用例零回归

## Done

验证：`mvn -pl buzhou-guard test` 全绿。
