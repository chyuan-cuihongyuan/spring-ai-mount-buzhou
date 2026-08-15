---
Type: task
Status: closed
blocked-by: T170-ttft-tpot-metrics.md, T173-turn-feedback.md, T177-model-pool-quota.md
---
## Question

TTFT 打点开销（打点前后流式吞吐 delta）/ 反馈写入开销 / 池配额闸开销 / shadow 并行开销四哨兵（10 倍宽幅硬顶）+ baseline 落档。

## Resolution

spec 51 §C / impl-152 落地：四哨兵（TTFT 打点/rateTurn 写入/候选限流闸/shadow 提交）——
10 倍宽幅硬顶（50/20/5/20ms），首轮实测全部 <5ms（4 测试 0.52s 总耗时）。baseline 第四批落档。
nightly 口径钉住：-Dgroups=perf 须配 -Dsurefire.excluded.groups=（默认排除反转）。4/4 绿。
