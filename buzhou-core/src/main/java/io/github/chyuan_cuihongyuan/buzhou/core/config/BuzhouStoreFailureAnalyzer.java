package io.github.chyuan_cuihongyuan.buzhou.core.config;

import org.springframework.boot.diagnostics.FailureAnalysis;
import org.springframework.boot.diagnostics.FailureAnalyzer;

/**
 * 启动失败翻译器（impl-42 / spec 13 §T68）：把 {@link BuzhouConfigurationException}
 * （store.type 拼错 / 方言未知 / 配置约束冲突）翻译成「description + action」的
 * 人类可读诊断（Boot FAILURE ANALYSIS 面板）；未知失败返回 null 走默认输出。
 * 经 {@code META-INF/spring.factories} 注册。
 */
public class BuzhouStoreFailureAnalyzer implements FailureAnalyzer {

    @Override
    public FailureAnalysis analyze(Throwable failure) {
        BuzhouConfigurationException exception = findBuzhouConfigurationException(failure);
        if (exception == null) {
            return null;
        }
        return new FailureAnalysis(
                "buzhou 配置错误：" + exception.getMessage(),
                exception.action(),
                exception);
    }

    private BuzhouConfigurationException findBuzhouConfigurationException(Throwable failure) {
        Throwable current = failure;
        for (int depth = 0; current != null && depth < 10; depth++) {
            if (current instanceof BuzhouConfigurationException configuration) {
                return configuration;
            }
            current = current.getCause();
        }
        return null;
    }
}
