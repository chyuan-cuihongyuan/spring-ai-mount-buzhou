# Spec 4009 — Crockford Base32（effort #4009，R10）

> wayfinder map：`.wayfinder/maps/effort-4000.md`（T6019–T6020，impl 2110）。
> 借鉴：Douglas Crockford 2001；ULID 底层字母表同源。

## Problem Statement

电话/工单口传 ID 的「念错写错」病：十六进制 I/1、O/0、B/8 形近，
Base64 大小写敏 + 符号噪——**人类可转录**编码件缺失。

## Solution

`CrockfordBase32`（core/message，纯静态）：

- 32 符号字母表剔除 I/L/O/U；最小位数编码（无前导零）；
- 解码宽容归一：O→0、I/L→1、大小写不敏感、连字符忽略；
- 可选 mod-37 校验符号（37 素数 >32，单字符检错——转写错一位
  必被捕获）；非法符/溢出 fail-fast。纯静态。

## User Stories

1. 作为运维作者，口传 run ID 不再纠结婚头是 0 还是 O。
2. 作为工单作者，校验符兜住单字符转写错（事前拦住错单）。

## Testing Decisions

- 锚点（0→"0"/31→"Z"/32→"10"/"C1S"→12345）+ Long.MAX_VALUE 13 位
  往返；形近归一五型（o/i/l/小写/连字符/混写）；校验符号往返 +
  尾符篡改必捕；畸形七型 fail-fast（负值/null/空/U/#/溢出/不足两位）。

## Out of Scope

- 不做定长（固定 13 位 ULID 面归 UuidV7 轮）；不做十六进制变体
 （JDK 已有）；不做人名纠错（Norvig 系归后续候选）。

## Further Notes

- 与 SnowflakeIdDecompose（位布局）正交：本件管「人面转录」。
- 里程碑：10/50。
