package io.github.chyuan_cuihongyuan.buzhou.core.crypto;

import java.util.List;

/**
 * 密钥轮换重叠窗（spec 1838 / T2877 / impl 1439）——TLS 证书轮换 /
 * Vault 密钥 grace 思想：轮换不是切换瞬间——旧钥在**重叠窗**内仍有效
 *（在飞请求用旧钥加密的数据还能解、旧签名还能验），窗尽才真失效。
 * 无重叠窗的轮换 = 用旧钥的数据瞬间全废（蓝绿不接）。epoch 单调递增，
 * 未来 epoch 视为时钟/协议违和 fail-fast。
 *
 * <p>纯函数零状态、只判态不执行（轮换动作归宿主）。
 */
public final class RotationOverlapWindow {

    private RotationOverlapWindow() {
    }

    /** 凭据三态：CURRENT 当前代 / GRACE 重叠窗内旧代 / EXPIRED 窗尽失效。 */
    public enum EpochValidity {

        /** 当前代凭据。 */
        CURRENT,

        /** 旧代但在重叠窗内——仍有效（在飞数据兼容）。 */
        GRACE,

        /** 窗尽——旧代凭据失效。 */
        EXPIRED
    }

    /**
     * 单凭据判态。契约：tokenEpoch/currentEpoch ≥ 0、graceEpochs ≥ 0、
     * tokenEpoch ≤ currentEpoch（未来代 fail-fast——单调性违和）；语义：
     * 同代 CURRENT；相差 ≤ graceEpochs 为 GRACE（含边界）；更旧 EXPIRED。
     */
    public static EpochValidity validity(long tokenEpoch, long currentEpoch,
                                         long graceEpochs) {
        validate("tokenEpoch", tokenEpoch);
        validate("currentEpoch", currentEpoch);
        validate("graceEpochs", graceEpochs);
        if (tokenEpoch > currentEpoch) {
            throw new IllegalArgumentException(String.format(
                    "未来代凭据（tokenEpoch=%d > currentEpoch=%d）——单调性违和",
                    tokenEpoch, currentEpoch));
        }
        long lag = currentEpoch - tokenEpoch;
        if (lag == 0) {
            return EpochValidity.CURRENT;
        }
        return lag <= graceEpochs ? EpochValidity.GRACE : EpochValidity.EXPIRED;
    }

    /**
     * 凭据普查。null 按空表；逐凭据核契约。
     *
     * @param current/grace/expired 三态计数（合计 = credentials）
     */
    public record Census(int credentials, long current, long grace, long expired) {

        /** 失效占比（无凭据 -1 哨兵）——轮换清扫进度读数。 */
        public double expiredRatio() {
            return credentials == 0 ? -1d : (double) expired / credentials;
        }
    }

    /** 普查入口。 */
    public static Census census(long currentEpoch, long graceEpochs,
                                List<Long> tokenEpochs) {
        List<Long> window = tokenEpochs == null ? List.of() : tokenEpochs;
        long current = 0;
        long grace = 0;
        long expired = 0;
        for (Long epoch : window) {
            if (epoch == null) {
                throw new IllegalArgumentException("凭据 epoch 不能为 null");
            }
            switch (validity(epoch, currentEpoch, graceEpochs)) {
                case CURRENT -> current++;
                case GRACE -> grace++;
                case EXPIRED -> expired++;
            }
        }
        return new Census(window.size(), current, grace, expired);
    }

    private static void validate(String name, long value) {
        if (value < 0) {
            throw new IllegalArgumentException(name + " 不能为负：" + value);
        }
    }
}
