# 1060 — PII 检测器合成探针自查

**What to build:** PiiProbeSelfCheck 纯函数（合成正负例池 + probe → ProbeReport 逐类召回+误报哨兵）+ 四测。

**Blocked by:** None — can start immediately.

**Status:** done

- [x] PiiProbeSelfCheck（guard/pii，private 构造静态面，身份证红线不入池）
- [x] PiiProbeSelfCheckTest（结构典序/基线全召回/误报哨兵/只读可重复）
- [x] spec 1407 + README 行 + api-surface.md L 段 + 快照再生

## Done

验证：`mvn -pl buzhou-guard -am test -Dtest='PiiProbeSelfCheckTest'` 4/4 绿。
