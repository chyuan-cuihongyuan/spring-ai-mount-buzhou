package io.github.chyuan_cuihongyuan.buzhou.core.error;

import io.github.chyuan_cuihongyuan.buzhou.core.fs.SandboxViolationException;
import io.github.chyuan_cuihongyuan.buzhou.core.session.LeaseLostException;
import io.github.chyuan_cuihongyuan.buzhou.core.session.SessionAlreadyActiveException;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * ticket 29：异常分类体系单元测试——每个领域异常的 RetryCategory 判定正确、
 * ErrorCode 全量携带类别与默认消息模板、既有异常的消息与 RuntimeException 兼容性不变。
 */
class ErrorTaxonomyTest {

    @Test
    void shouldClassifyNonRetryable_whenSandboxViolationThrown() {
        SandboxViolationException exception = new SandboxViolationException("路径越出沙箱：../etc");
        assertThat(exception.errorCode()).isEqualTo(ErrorCode.SANDBOX_VIOLATION);
        assertThat(exception.retryCategory()).isEqualTo(RetryCategory.NON_RETRYABLE);
    }

    @Test
    void shouldClassifyNonRetryable_whenLeaseLostThrown() {
        LeaseLostException exception = new LeaseLostException("sess-1");
        assertThat(exception.errorCode()).isEqualTo(ErrorCode.LEASE_LOST);
        assertThat(exception.retryCategory()).isEqualTo(RetryCategory.NON_RETRYABLE);
    }

    @Test
    void shouldClassifyNonRetryable_whenSessionAlreadyActiveThrown() {
        SessionAlreadyActiveException exception = new SessionAlreadyActiveException("sess-1");
        assertThat(exception.errorCode()).isEqualTo(ErrorCode.SESSION_ALREADY_ACTIVE);
        assertThat(exception.retryCategory()).isEqualTo(RetryCategory.NON_RETRYABLE);
    }

    @Test
    void shouldClassifyNonRetryable_whenQuotaExceededThrown() {
        QuotaExceededException exception = new QuotaExceededException("per-session 消息数达到上限 5000");
        assertThat(exception.errorCode()).isEqualTo(ErrorCode.QUOTA_EXCEEDED);
        assertThat(exception.retryCategory()).isEqualTo(RetryCategory.NON_RETRYABLE);
    }

    @Test
    void shouldClassifyFatal_whenDataCorruptionThrown() {
        BuzhouDataCorruptionException exception =
                new BuzhouDataCorruptionException("记录解析失败：sess-1#msg-9", new IllegalStateException("bad json"));
        assertThat(exception.errorCode()).isEqualTo(ErrorCode.DATA_CORRUPTION);
        assertThat(exception.retryCategory()).isEqualTo(RetryCategory.FATAL);
        assertThat(exception.getCause()).isInstanceOf(IllegalStateException.class);
    }

    @Test
    void shouldClassifyByCarriedErrorCode_whenBuzhouExceptionConstructedDirectly() {
        assertThat(new BuzhouException(ErrorCode.TIMEOUT, "模型调用超时").retryCategory())
                .isEqualTo(RetryCategory.RETRYABLE);
        assertThat(new BuzhouException(ErrorCode.STORE_WRITE_FAILED, "写失败").retryCategory())
                .isEqualTo(RetryCategory.RETRYABLE);
        assertThat(new BuzhouException(ErrorCode.SHUTDOWN_INTERRUPTED, "停机中断").retryCategory())
                .isEqualTo(RetryCategory.RETRYABLE);
        assertThat(new BuzhouException(ErrorCode.TOOL_EXECUTION_FAILED, "工具失败").retryCategory())
                .isEqualTo(RetryCategory.RETRYABLE);
        assertThat(new BuzhouException(ErrorCode.ARGS_VALIDATION_FAILED, "校验失败").retryCategory())
                .isEqualTo(RetryCategory.NON_RETRYABLE);
        assertThat(new BuzhouException(ErrorCode.SESSION_CLOSED, "已关闭").retryCategory())
                .isEqualTo(RetryCategory.NON_RETRYABLE);
        assertThat(new BuzhouException(ErrorCode.CONFIG_INVALID, "配置非法").retryCategory())
                .isEqualTo(RetryCategory.FATAL);
    }

    @Test
    void shouldCarryCategoryAndTemplate_whenErrorCodeEnumIterated() {
        for (ErrorCode code : ErrorCode.values()) {
            assertThat(code.retryCategory()).as("错误码 %s 必须有重试分类", code).isNotNull();
            assertThat(code.defaultMessageTemplate()).as("错误码 %s 必须有默认消息模板", code).isNotBlank();
        }
    }

    @Test
    void shouldRemainRuntimeException_whenBuzhouExceptionThrown() {
        assertThat(new BuzhouException(ErrorCode.TIMEOUT, "t")).isInstanceOf(RuntimeException.class);
        assertThat(new SandboxViolationException("m")).isInstanceOf(RuntimeException.class);
        assertThat(new LeaseLostException("s")).isInstanceOf(RuntimeException.class);
        assertThat(new SessionAlreadyActiveException("s")).isInstanceOf(RuntimeException.class);
    }

    @Test
    void shouldPreserveLegacyMessages_whenExistingDomainExceptionsConstructed() {
        assertThat(new LeaseLostException("sess-9")).hasMessage("Session lease lost: sess-9");
        assertThat(new SessionAlreadyActiveException("sess-9")).hasMessage("Session already active: sess-9");
        assertThat(new SandboxViolationException("路径为空")).hasMessage("路径为空");
    }
}
