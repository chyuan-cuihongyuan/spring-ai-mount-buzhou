package io.github.chyuan_cuihongyuan.buzhou.core.concurrent;

import io.github.chyuan_cuihongyuan.buzhou.core.error.BuzhouException;
import io.github.chyuan_cuihongyuan.buzhou.core.error.ErrorCode;
import io.github.chyuan_cuihongyuan.buzhou.core.metrics.BuzhouMetricsHolder;
import io.github.chyuan_cuihongyuan.buzhou.core.metrics.ErrorSignatures;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorCompletionService;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;

/**
 * 事务性并行批（spec 122 §A / T443，LangGraph superstep 借鉴）：一批任务并发执行，
 * 「同一步」内全员成功才返回结果——<b>任一失败立即中止在途</b>（完成序感知的
 * 快速失败，不等慢同伴）、已完成结果<b>不可见</b>（不返回部分结果），异常携带每任务
 * 去向（已完成被弃 / 被中止）。
 *
 * <p><b>事务性口径（诚实声明）</b>：无回滚——已执行任务的副作用不撤销；「事务」指
 * 失败快速传播 + 在途中断 + 结果全有或全无的可见性，与 superstep「步内并行、失败
 * 即步失败」的传播语义对齐。
 *
 * <p>执行器由宿主注入（池策略/关闭归宿主管）；空批<b>零提交</b>（不触碰执行器——
 * 空批诚实返回空表）。失败折 {@link ErrorCode#SUPERSTEP_FAILED}（NON_RETRYABLE：
 * 同批重放需整批重来，单任务重试无意义），首败异常记错误签名族（kind=
 * {@value #SIGNATURE_KIND}），批级指标 {@code buzhou.superstep.outcome}
 * （tag outcome=ok|failed 有界二值）。
 */
public final class SuperstepBatch {

    /** 错误签名 kind 前缀（失败入族的分组面）。 */
    public static final String SIGNATURE_KIND = "superstep";

    private SuperstepBatch() {
    }

    /**
     * 并发执行一批任务，全员成功返回按提交序的结果表；任一失败立即中止在途并抛
     * {@link BuzhouException}（{@link ErrorCode#SUPERSTEP_FAILED}，message 含失败
     * 任务 id / 首败异常 / 已完成被弃 / 被中止清单）。
     *
     * @param superstepId 批 id（异常/日志归属；非空白）
     * @param tasks       id → 任务（id 非空白、任务非 null；可空批）
     * @param executor    宿主执行器（非 null；生命周期归宿主）
     * @throws IllegalArgumentException 参数非法（空白 id / null 任务或执行器）
     * @throws BuzhouException          任一任务失败（首败即中止）
     * @throws InterruptedException     等待期被中断（恢复中断标记、中止在途后抛出）
     */
    public static <T> Map<String, T> runAll(String superstepId,
                                            Map<String, Callable<T>> tasks,
                                            ExecutorService executor) throws InterruptedException {
        if (superstepId == null || superstepId.isBlank()) {
            throw new IllegalArgumentException("superstepId must not be blank");
        }
        if (tasks == null || executor == null) {
            throw new IllegalArgumentException("tasks and executor must not be null");
        }
        if (tasks.isEmpty()) {
            return new LinkedHashMap<>();
        }
        List<String> ids = new ArrayList<>(tasks.size());
        for (Map.Entry<String, Callable<T>> entry : tasks.entrySet()) {
            if (entry.getKey() == null || entry.getKey().isBlank()) {
                throw new IllegalArgumentException("task id must not be blank: " + entry.getKey());
            }
            if (entry.getValue() == null) {
                throw new IllegalArgumentException("task must not be null: " + entry.getKey());
            }
            ids.add(entry.getKey());
        }

        // 完成序感知：take() 返回任意先完成任务——首败即刻传播，不等慢同伴
        ExecutorCompletionService<T> completion = new ExecutorCompletionService<>(executor);
        Map<Future<T>, String> idOf = new IdentityHashMap<>();
        for (Map.Entry<String, Callable<T>> entry : tasks.entrySet()) {
            idOf.put(completion.submit(entry.getValue()), entry.getKey());
        }
        Map<String, T> byId = new HashMap<>();
        try {
            for (int done = 0; done < ids.size(); done++) {
                Future<T> finished = completion.take();
                T value;
                try {
                    value = finished.get();
                } catch (ExecutionException e) {
                    throw fail(superstepId, idOf, finished, e.getCause() == null ? e : e.getCause());
                }
                byId.put(idOf.get(finished), value);
            }
        } catch (InterruptedException e) {
            abortInFlight(idOf.keySet());
            Thread.currentThread().interrupt();
            throw e;
        }
        BuzhouMetricsHolder.metrics().counter("buzhou.superstep.outcome", "outcome", "ok");
        LinkedHashMap<String, T> ordered = new LinkedHashMap<>();
        for (String id : ids) {
            ordered.put(id, byId.get(id));
        }
        return ordered;
    }

    /** 首败路径：中止在途、入签名族、计失败数、抛结构化异常（每任务去向入 message）。 */
    private static <T> BuzhouException fail(String superstepId, Map<Future<T>, String> idOf,
                                            Future<T> failed, Throwable cause) {
        abortInFlight(idOf.keySet());
        String failedId = idOf.get(failed);
        ErrorSignatures.global().record(SIGNATURE_KIND, cause);
        BuzhouMetricsHolder.metrics().counter("buzhou.superstep.outcome", "outcome", "failed");
        return new BuzhouException(ErrorCode.SUPERSTEP_FAILED,
                "superstep \"" + superstepId + "\" failed: task <" + failedId
                        + "> threw " + cause.getClass().getSimpleName() + ": "
                        + String.valueOf(cause.getMessage()).lines().findFirst().orElse("")
                        + "; " + fates(idOf, failedId), cause);
    }

    /** 失败/中断传播：中止所有未完成任务（已完成的只好作废——副作用不撤销）。 */
    private static void abortInFlight(Iterable<? extends Future<?>> futures) {
        for (Future<?> future : futures) {
            if (!future.isDone()) {
                future.cancel(true);
            }
        }
    }

    /** 每任务去向单行摘要（已完成被弃 / 被中止；失败者单列于 message 主体）。 */
    private static <T> String fates(Map<Future<T>, String> idOf, String failedId) {
        List<String> discarded = new ArrayList<>();
        List<String> aborted = new ArrayList<>();
        for (Map.Entry<Future<T>, String> entry : idOf.entrySet()) {
            Future<T> future = entry.getKey();
            if (entry.getValue().equals(failedId)) {
                continue;
            }
            if (future.isCancelled() || !future.isDone()) {
                aborted.add(entry.getValue());
            } else {
                try {
                    future.get();
                    discarded.add(entry.getValue());
                } catch (Exception ignored) {
                    // 并发双败极小概率：归入被中止面（首败已单列）
                    aborted.add(entry.getValue());
                }
            }
        }
        return "completed-discarded=" + discarded + ", aborted=" + aborted;
    }
}
