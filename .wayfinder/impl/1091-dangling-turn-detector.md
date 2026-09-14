# 1091 — 悬空轮检测器

**What to build:** DanglingTurnDetector 纯函数（turnSeq 分组：有 USER 无 ASSISTANT 即悬空+样本封顶 8 升序）+ 六测。

**Blocked by:** None.

**Status:** done

- [x] DanglingTurnDetector（core/message，private 构造静态面）
- [x] DanglingTurnDetectorTest 六测
- [x] spec 1439 + README 行 + api-surface.md L 段 + 快照再生

## Done

验证：`mvn -pl buzhou-core -am test -Dtest='DanglingTurnDetectorTest'` 6/6 绿。
