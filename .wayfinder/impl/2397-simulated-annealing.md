# impl 2397 — W 会话 W45 Simulated Annealing 模拟退火（spec 9044 / W9089–W9090 / W45）

纵切片：Simulated Annealing 模拟退火——SimulatedAnnealing（core/policy）：exp(−Δ/T) 接受+几何降温+best-ever 最优化器。

- 验证：`mvn -pl buzhou-core test -Dtest='SimulatedAnnealingTest'` 全绿。
