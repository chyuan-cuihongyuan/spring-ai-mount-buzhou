# 1131 — load 已序快路径（M 系 R32）

**What to build:** isSorted 检查 + copyOf 快路径 + 乱序回退。

**Blocked by:** T2307 / T2308（同轮 shape+verify）。

**Status:** done

- [x] 快路径实现 + 乱序回退
- [x] 契约 14 + 专属 4 用例（新增乱序例）绿

## Done

验证：定向测试绿。commit 见本轮 perf 提交。
