# Spec 9022 — Morse Codec 莫尔斯编解码（effort #9022，W23）

> wayfinder map：`.wayfinder/maps/effort-9000.md`（W9045–W9046，impl 2375）。
> 借鉴：International Morse 1838/ITU-R M.1677（电报时代信令——SOS 同源）

## Problem Statement

位级编码人眼不可校——**莫尔斯**：定长字符→
变长点划，A-Z/0-9 全表、可读可听可手拍的
信号形态。

## Solution

MorseCodec（core/message，静态工具面）：encode
（大小写不敏感，单空格分字母、" / "分词）/
decode 精确逆（大写输出）。

## Testing Decisions

SOS/HELLO WORLD 锚；数字全覆盖；60 随机
文本往返+大小写归一；fail-fast 九面
（含 "."=E 与 "----" 非法辨析）。

## Out of Scope

不做标点/扩展表（A-Z/0-9 明示）；不做
音频/WPM 时长面；不做缩写（ITU 缩语）。

## Further Notes

与 Base58Codec（8017）同包不同面。
Wave 4 收束件（电报独件）。
