# 1120 — spill 默认值单一事实源（M 系 R19）

**What to build:** SpillProperties 三常量 + 三处引用收口。

**Blocked by:** T2285 / T2286（同轮 shape+verify）。

**Status:** done

- [x] DEFAULT_PREVIEW_CHARS/DEFAULT_LIST_PREVIEW_ITEMS/DEFAULT_THRESHOLD_CHARS（引用 SpillOffloadHook）
- [x] SpillModule.withDefaults / MediaIntake 便捷构造改引用
- [x] spill 180 用例零回归；六-6/六-9 裁定入档

## Done

验证：定向测试绿。commit 见本轮 refactor 提交。
