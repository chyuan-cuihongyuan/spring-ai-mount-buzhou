# 493 — fork 谱系键公共常量收口

**What to build:** `core.session.SessionForkKeys`（SOURCE/TURN/PRODUCER）公共常量类；写入口（DefaultAgentRuntime）与读入口（BuzhouSessionsEndpoint）改引用常量，main 零字面量残留；api-surface 补行；常量钉值用例。

**Blocked by:** None — can start immediately.

**Status:** done

- [x] SessionForkKeys 常量类（core.session 公共面）
- [x] 写读两侧同源引用（import 化、清残留注释）
- [x] SessionForkLineageTest 常量一致性断言（3 键钉值）
- [x] api-surface.md 主段补行
- [x] spec 640 + README 行
- [x] 全模块测试绿

## Done

验证：`mvn -pl buzhou-core -am test` 绿。commit 见本轮 `refactor(core)` 提交。
