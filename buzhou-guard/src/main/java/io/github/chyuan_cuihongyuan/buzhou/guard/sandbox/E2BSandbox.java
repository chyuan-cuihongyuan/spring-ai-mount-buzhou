package io.github.chyuan_cuihongyuan.buzhou.guard.sandbox;

import java.nio.file.Path;
import java.time.Duration;
import java.util.List;
import java.util.Map;

/**
 * E2B 托管档（wayfinder2 impl-25 / T51：<b>接口预留</b>）：Firecracker 云沙箱
 * （13,383★）REST（ApiKeyAuth；出网 allowOut/denyOut CIDR/域名、per-domain header 注入）；
 * 适合无本地 KVM 的托管场景（凭据/端点由部署配置注入后实现）。
 */
public final class E2BSandbox implements CommandSandbox {

    @Override
    public String name() {
        return "e2b";
    }

    @Override
    public boolean available() {
        // 无凭据即不可用（探测式；实现接入时检查配置端点连通）
        return System.getenv("E2B_API_KEY") != null;
    }

    @Override
    public String unavailableHint() {
        return "E2B 托管档需 E2B_API_KEY（https://e2b.dev）；本地开发建议 DenoSandbox。";
    }

    @Override
    public CommandResult run(List<String> command, Map<String, String> allowedEnv, Path workDir,
                             Duration timeout) {
        throw new UnsupportedOperationException(
                "E2BSandbox 为接口预留档（实现按部署需求提供）；" + unavailableHint());
    }
}
