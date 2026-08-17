---
Type: task
Status: closed
---
## Question

SleepTimeScheduler.close()=shutdownNow() 丢 pending 整理任务（Lifecycle javadoc 自认「属切片 38」未做）；WebhookEventForwarder.close 排空 5s 硬编码且无直接测试：停机排空补全语义（有界等待/预算可配）如何定？

## Resolution

AFK 自决：双侧补全——(a) SleepTimeScheduler.close() 优雅化（shutdown → 有界 awaitTermination（缺省
5s、四参构造可调）→ 超时 shutdownNow 硬截断）；(b) webhook close 排空预算可配（close-drain-timeout
属性，缺省 5s、非正 fail-fast、7 参 record + 兼容构造 + @ConstructorBinding）+ 确定性测试钉住「等在途
收尾并排空已到期」。跨重启整理任务持久化队列出界（pending 允许丢失，只消除计划内停机的粗放丢弃）。
产 spec 44 §A + impl-130。
