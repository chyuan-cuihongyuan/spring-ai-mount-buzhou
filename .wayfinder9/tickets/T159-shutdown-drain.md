---
Type: task
Status: open
---
## Question

SleepTimeScheduler.close()=shutdownNow() 丢 pending 整理任务（Lifecycle javadoc 自认「属切片 38」未做）；WebhookEventForwarder.close 排空 5s 硬编码且无直接测试：停机排空补全语义（有界等待/预算可配）如何定？
