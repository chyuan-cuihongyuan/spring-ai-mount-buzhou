# 534 — PII 假名化模式装配

**What to build:** PiiDetector 模式构造 + 双 hook 3 参构造 + Builder.piiPreserveFormat 直通（三元树收敛）。

**Blocked by:** None — can start immediately.

**Status:** done

- [x] PiiDetector(formatPreserving) + redact 分派
- [x] 双 hook 3 参构造 + Builder 接线（三元树收敛）
- [x] 输入缝两态用例 + 装配摘要
- [x] spec 731 + README 行
- [x] 模块测试绿

## Done

验证：`mvn -pl buzhou-guard -am test` 绿。commit 见本轮 `feat(guard)` 提交。
