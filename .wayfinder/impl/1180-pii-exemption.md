# 1180 — PII 脱敏豁免双粒度

**What to build:** PiiRedactionHook +exemptions（工具级短路 + 类型级剔除）。

**Blocked by:** None — can start immediately.

**Status:** done

- [x] PiiRedactionHook：4 参构造 + afterTool 双粒度征询
- [x] GuardModule 装配传入同 registry
- [x] 三断言 + guard 359 用例零回归

## Done

验证：`mvn -pl buzhou-guard test` 全绿。
