---
Type: task
Status: closed
blocked-by: T190-eval-dataset-store.md, T191-feedback-import.md, T192-evaluator-spi.md, T193-eval-runner.md, T194-eval-query.md, T195-eval-events.md
---
## Question

新键入档 + 绑定验证（如无新键则钉住零键入档）；多构造器 record 预防性检查（T187 教训）。

## Resolution

impl-167 落地：零新键钉住（评估面全 API 驱动构造注入，元数据零改动即正确）；新增 7 record
逐一核对单构造器——无 @ConstructorBinding 盲区；行为面已由 19 测试覆盖。T201 关闭。
