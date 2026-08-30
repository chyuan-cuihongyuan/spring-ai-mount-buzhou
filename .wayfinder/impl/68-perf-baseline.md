# 68 — 轻量性能基准 harness（T93 决策落地）

**What to build:** examples `PerfBaselineTest`（@Tag("perf") 三场景哨兵）+ root pom 默认排除 + perf-nightly workflow + docs/perf/baseline.md 首轮基线。

**Blocked by:** None.

**Status:** done

## Done

验证：`./mvnw -pl examples clean test -Dtest=PerfBaselineTest -Dsurefire.excluded.groups=` 3/3 绿（实测每轮 P50 0.29ms / P95 0.55ms、微压缩 ~1.8M msgs/s、存储 round-trip P95 0.01ms）；默认全量 62/62 绿且 perf 正确排除。
落地：`PerfBaselineTest` 三哨兵（微压缩吞吐/100 轮会话开销/存储 round-trip，10 倍宽幅硬顶）+ root pom `surefire.excluded.groups=perf` 属性默认排除 + `.github/workflows/perf-nightly.yml`（weekly+manual，报告工件归档）+ `docs/perf/baseline.md` 首轮基线与解读规则。
