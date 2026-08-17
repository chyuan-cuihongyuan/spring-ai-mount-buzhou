# 163 — 评估面 perf 哨兵

**Parent:** spec 52 / [T197](../tickets/T197-eval-perf.md)

**Status:** done

- [x] 三哨兵：runner 全链路（3 项 run 硬顶 80ms）/ 数据集 scan（50 项 items+allRuns 硬顶 40ms）/
  回流导入（3 轮会话幂等路径硬顶 40ms）
- [x] 首轮实测均 <5ms 量级（PerfEffort11SentinelsTest 3 用例 0.055s）；10 倍宽幅口径
- [x] baseline.md 增 effort#11 哨兵表；perf 组 19 测试全绿
- [x] 过程教训：examples 单跑 perf 组须 `-am` + 清空 excludedGroups（NoClassDefFound 排障记录）
