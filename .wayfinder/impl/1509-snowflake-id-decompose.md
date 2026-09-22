# impl 1509 — SnowflakeIdDecompose 雪花 ID 分解（R109 = effort #1908 / spec 1908 / T3017-T3018）

**What**：`SnowflakeIdDecompose`（core/concurrent 静态纯函数 + 嵌套
Decomposed）——decompose（41 位时间戳+10 位机器+12 位序列按位拆解）
+ compose 逆组装；worker≤1023/seq≤4095/时间≥epoch/符号位 0
fail-fast。

**Why**：Twitter Snowflake 布局——时间有序 ID 自描述信息（何时/
哪台/第几个）被黑盒浪费；编解码面让排障不查库、roundtrip 自洽。
与 DeterministicHash 互补（无序指纹 vs 有序可分解 ID）。

**Verify**：`SnowflakeIdDecomposeTest` 4 用例全绿（roundtrip 三例/
字段精确/时间有序/越界四型 fail-fast）。

**Status**：done（2026-09-23）
