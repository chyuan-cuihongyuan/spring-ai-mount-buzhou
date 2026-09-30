# impl 2430 — X 会话 X28 PCG-XSH-RR 感知置换随机数（spec 10027 / X10055–X10056 / X28）

纵切片：PcgXshRr——LCG 状态+XSH-RR 输出置换+流分离（O'Neill 思想）（core/metrics）。

- 验证：`mvn -pl buzhou-core test -Dtest='PcgXshRrTest'` 全绿。
