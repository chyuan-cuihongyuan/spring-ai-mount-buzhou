# 520 — 工具调用图谱环检测

**What to build:** ToolGraphAnalyzer.cycles——初等环 DFS 枚举（锚去重 + visited + MAX_CYCLES=16 有界 + 稳定排序），自环识别，纯函数零 IO。

**Blocked by:** None — can start immediately.

**Status:** done

- [x] cycles 纯函数（锚去重 DFS + 封顶 + 稳定排序 + 自环）
- [x] 双环/自环/链/菱形/封顶/空图六组用例
- [x] spec 717 + README 行
- [x] 模块测试绿

## Done

验证：`mvn -pl buzhou-observability -am test` 绿。commit 见本轮 `feat(observability)` 提交。
