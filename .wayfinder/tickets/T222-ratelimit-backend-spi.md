---
Type: task
Status: done
---
## Question

RateLimitBackend SPI（resilience：tryAcquire/consume/available/waitHint + 容量）+
InMemoryRateLimitBackend（TokenBucket 逻辑平移）；ModelRateLimiter 改造走 backend；
默认行为零变化（全量既有测试绿即证明）。
