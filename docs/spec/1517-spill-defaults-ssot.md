# 1517 — spill 默认值单一事实源（五-6 收口）

> 来源：M 会话第 19 轮 = effort #1517（impl 1120）。design-incompleteness 五-6 收口 + 六-6/六-9 裁定入档。

## 背景

spill 默认值 2048/20/32000 散落四处（SpillProperties compact constructor / SpillModule.withDefaults / MediaIntake 便捷构造 / SpillOffloadHook 常量）——改默认值需散弹多文件。

## 目标

- SpillProperties 落三个公共常量（previewChars/listPreviewItems/thresholdChars——threshold 引用 SpillOffloadHook 既有常量），其余三处引用收口；
- 六-6（load 前两参）与六-9（单线程调度器工厂）裁定入档（见票 T2285）。

## 兼容性

等值重构零行为变化。
