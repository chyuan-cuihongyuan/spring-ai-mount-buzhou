---
Type: task
Status: closed
---
## Question

事务性并行批 A 侧通用原语：完成序感知快速失败 + 每任务去向 + 全有或全无可见性。

## Resolution

done（2026-08-30）：impl-272；`concurrent/SuperstepBatch.runAll` +
`SUPERSTEP_FAILED` 错误码 + 红队 4 例（提交序返回/首败中断在途/空批零提交/
参数 fail-fast）。与 B 侧 harness 前检同轮分工（MAP 登记）。
