# Spec 213 — spec 文档覆盖门（effort #226）

> wayfinder map：`.wayfinder226/MAP.md`（T587–T588）。绑定矩阵先例——
> 把「新能力必须入 README 表」的纪律变成 CI 红/绿。

## Problem Statement

每轮 spec 都写了详设，但 README 用户可见表靠文档轮人工补：新 spec 忘记入表
（能力存在但用户不知道）、链接拼错（点开 404）——两类缺陷都静默，只有认真
的读者发现。

## Solution

`SpecCoverageTest`（buzhou-spring-boot-starter 测试）：

- **spec→README**：docs/spec/ 下每个 `NNN-*.md` 文件名（去 `.md`）必须出现在
  README.md 文本中（按文件名匹配——不苛求精确链接语法）。
- **README→spec**：README 中 `docs/spec/NNN-*.md` 链接必须指向实存文件
  （无死链）。
- **例外清单**：显式 `EXCLUDED` 集合（过渡期文件——当前为空，撞号双文件
  122/124/125/127 均按文件名各自可匹配，无需例外）。

## User Stories

1. 作为贡献者（含两会话并行），新 spec 不入 README 表 CI 即红——纪律机械化。
2. 作为用户，README 链接零死链——公开面可信。
3. 作为文档轮，双向门绿的瞬间就是「表与实存一致」的验收章。

## Implementation Decisions

- 文件名匹配（不解析链接语法——误报最小）；测试读仓库相对路径
  （starter 测试基线 ../../docs/spec——先例 ApiSurfaceSnapshotTest 同款路径策略）。

## Testing Decimals

- 当前库全绿即验收；故意注入假 spec 名断言会红（自检 via 参数化反向用例
  不做——门本身即测试）。

## Out of Scope

- runbook 覆盖门；链接语法严格校验。

## Further Notes

- 治理门家族：配置绑定矩阵（既有）/ API 面快照（既有）/ spec 覆盖（本轮）。
