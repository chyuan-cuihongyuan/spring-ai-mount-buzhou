# 1158 — A/B 评估 SPRT 序贯提前终止

**What to build:** PairwiseSprtPolicy 判定器 + runner 序贯停 + summary 扩桶 + 落盘/事件同步。

**Blocked by:** None — can start immediately.

**Status:** done

- [x] PairwiseSprtPolicy（α/β 可配、方向分离 LLR、fail-fast 校验）
- [x] PairwiseEvalRunner 5 参重载 + scored 序贯判定 + skipped 桶 + encode/decode/事件
- [x] PairwiseSprtPolicyTest 四断言 + eval 包回归 235 用例全绿

## Done

验证：`mvn -pl buzhou-core test -Dtest=io.github.chyuan_cuihongyuan.buzhou.core.eval.**`。
