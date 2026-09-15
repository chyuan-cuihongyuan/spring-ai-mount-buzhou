# impl 1417 — LoadShedLadder 负载脱落阶梯（R17 = effort #1816 / spec 1816 / T2833-T2834）

**What**：`LoadShedLadder`（core/backpressure 静态纯函数）——Level 阶梯级
（低阈值先掉）+ decide(loadFactor) 越阈即甩（含边界）→ ShedDecision
（shedLevels/keptLevels + shedRatio(-1 哨兵) + escalating()）；负/NaN 因子、
空白名、负阈值 fail-fast。

**Why**：Envoy overload manager / Akka 断路器组思想——一刀切全拒让高价值
请求与批量杂活同死；按优先级阶梯逐级甩（batch 先掉、critical 最后），
过载时「谁先死」显式声明而非运气。

**Verify**：`LoadShedLadderTest` 4 用例全绿。

**Status**：done（2026-09-16）
