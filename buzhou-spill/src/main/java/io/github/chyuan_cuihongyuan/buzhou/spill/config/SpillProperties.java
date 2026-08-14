package io.github.chyuan_cuihongyuan.buzhou.spill.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

/**
 * Spill 长内容治理装配属性（spec 02 / 09 / ticket 22，前缀 {@code buzhou.spill}）。
 *
 * <p>布尔字段用 boxed 类型以区分「未配置」（→ 默认开）与「显式关闭」。数值字段未配置时取规范默认。
 *
 * @param rootDir              Spill 落盘根目录（默认当前工作目录）
 * @param previewChars         上下文留存预览字符数（默认 2048）
 * @param listPreviewItems     列表型返回预览条目数（默认 20）
 * @param thresholdChars       工具返回超阈值自动落盘（默认 32000）
 * @param thresholdTokens      token-aware 溢出阈值（T20；配置后按 token 计、优先于 thresholdChars，×4 折算字符）
 * @param sandboxRoot          SpillGuard 沙箱根（默认当前工作目录）
 * @param onloadEnabled        写侧 Onload（默认开）
 * @param copyOnWriteEnabled   写时复制只读护栏（默认开）
 * @param offloadEnabled       读侧 Offload（默认开）
 * @param editingToolsEnabled  str_replace / copy_file 编辑工具（默认开）
 * @param maxTotalBytes        impl-38 / spec 13 §growth-8：全部 spill 数据字节上限
 *                             （超限拒绝落盘、原文回喂；null/非正 = 不限）
 * @param maxFilesPerSession   impl-38：单会话 spill 文件数上限（null/非正 = 不限）
 * @param retentionTtl         impl-38：未引用（unlinked）spill 的保留 TTL（RetentionSweeper
 *                             每周期调度 deleteExpired；默认 PT24H）
 */
@ConfigurationProperties(prefix = "buzhou.spill")
public record SpillProperties(
        String rootDir,
        Integer previewChars,
        Integer listPreviewItems,
        Integer thresholdChars,
        Integer thresholdTokens,
        String sandboxRoot,
        Boolean onloadEnabled,
        Boolean copyOnWriteEnabled,
        Boolean offloadEnabled,
        Boolean editingToolsEnabled,
        Long maxTotalBytes,
        Integer maxFilesPerSession,
        Duration retentionTtl) {

    public SpillProperties {
        String dir = System.getProperty("user.dir");
        rootDir = (rootDir == null || rootDir.isBlank()) ? dir : rootDir;
        sandboxRoot = (sandboxRoot == null || sandboxRoot.isBlank()) ? dir : sandboxRoot;
        previewChars = (previewChars == null || previewChars <= 0) ? 2048 : previewChars;
        listPreviewItems = (listPreviewItems == null || listPreviewItems <= 0) ? 20 : listPreviewItems;
        thresholdChars = (thresholdChars == null || thresholdChars <= 0) ? 32000 : thresholdChars;
        thresholdTokens = (thresholdTokens == null || thresholdTokens <= 0) ? null : thresholdTokens;
        onloadEnabled = onloadEnabled == null ? true : onloadEnabled;
        copyOnWriteEnabled = copyOnWriteEnabled == null ? true : copyOnWriteEnabled;
        offloadEnabled = offloadEnabled == null ? true : offloadEnabled;
        editingToolsEnabled = editingToolsEnabled == null ? true : editingToolsEnabled;
        maxTotalBytes = (maxTotalBytes == null || maxTotalBytes < 1) ? null : maxTotalBytes;
        maxFilesPerSession = (maxFilesPerSession == null || maxFilesPerSession < 1) ? null : maxFilesPerSession;
        retentionTtl = retentionTtl == null || retentionTtl.isZero() || retentionTtl.isNegative()
                ? Duration.ofHours(24) : retentionTtl;
    }
}
