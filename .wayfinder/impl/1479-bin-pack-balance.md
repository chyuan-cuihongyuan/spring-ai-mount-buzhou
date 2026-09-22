# impl 1479 — BinPackBalance 装箱平衡（R79 = effort #1878 / spec 1878 / T2957-T2958）

**What**：`BinPackBalance`（core/policy 静态纯函数 + 嵌套
PackResult）——pack（FFD 降序保序稳定、首个可容箱、放不下开新箱）
+ wasteRatio（1−Σ载荷/(箱数×容量)）；容量≥1/体积≥0/单件超容
fail-fast，空表 0 箱哨兵。

**Why**：K8s/Mesos bin-packing 语义——批任务落节点的装箱数与碎片率
事前可算（最优 NP-hard，FFD ≤ 11/9 OPT），扩容预算与缩容收益有账。
落轮前 grep 复核：TwoChoiceSelector（Q-3022）占坑原选题「两次随机」，
换静脉为离线装箱不撞。

**Verify**：`BinPackBalanceTest` 5 用例全绿（经典 3 箱载荷/完美装
0 浪费/空表哨兵/浪费率 2/9 精确/畸形四型 fail-fast）。

**Status**：done（2026-09-23）
