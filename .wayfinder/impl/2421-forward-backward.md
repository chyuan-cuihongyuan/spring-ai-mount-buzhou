# impl 2421 — X 会话 X19 Forward Backward HMM 前向后向（spec 10018 / X10037–X10038 / X19）

纵切片：ForwardBackward——缩放 α/β 联合递推全路径似然（Rabiner 思想——Viterbi 已占镜像面）（core/eval）。

- 验证：`mvn -pl buzhou-core test -Dtest='ForwardBackwardTest'` 全绿。
