# 516 — PII 格式保持假名化

**What to build:** PiiDetector.pseudonymize——逐命中同长度同形态替身（数字/字母/分隔符分层），(seed,type,text) 哈希播种确定性，保形状不保校验位诚实划界。

**Blocked by:** None — can start immediately.

**Status:** done

- [x] pseudonymize（形状保持 + 确定性 + 线程安全）
- [x] 三类命中形状/确定性/不留痕/非 PII 原样用例
- [x] 既有 redact/scan 零回归
- [x] spec 713 + README 行
- [x] 模块测试绿

## Done

验证：`mvn -pl buzhou-guard -am test` 绿。commit 见本轮 `feat(guard)` 提交。
