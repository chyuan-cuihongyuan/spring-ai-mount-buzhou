# Spec 172 — 归档清理键矩阵登记（effort #130）

> wayfinder map：`.wayfinder130/MAP.md`（T526–T527）。spec 130 fog：三键落了
> properties 与 autoconfig，但 metadata/矩阵未登记——T187 类静默失防线缺口。

## Problem Statement

spec 130 的 `buzhou.session-archive.purge-*` 三键在 additional-metadata 与
ConfigBindingsMatrix 都缺席：拼错键时 config-doctor 的键宇宙看不到它们、
矩阵不保证可绑——登记防线的漏面。

## Solution

additional-spring-configuration-metadata 补三键（含默认值 7d/1h/false）；
矩阵 PLAIN 键登记 + PREFIX_TO_BEAN 补 `buzhou.session-archive →
BuzhouArchiveProperties` 归属——Binder 真实装配路径断言覆盖。

## Testing Decisions

- 矩阵两测（宇宙覆盖 + 真实装配路径）全绿。

## Out of Scope

- 键语义变化；SKIPPED 面。

## Further Notes

- 登记纪律三件套（metadata/归属/样例）自此对全量 yml 键闭环。
