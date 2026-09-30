package io.github.chyuan_cuihongyuan.buzhou.core.policy;

/**
 * PID 控制器（spec 9043 / W9087 / impl 2396）——PID 思想（Minorsky
 * 1922 船舶自动驾驶起家——工业控制百年默认件，Kubernetes 控制
 * 环/温控/云扩缩容同源）：**输出 = Kp·误差 + Ki·误差积分 +
 * Kd·误差变化率——比例追今、积分消历史稳态差、微分抑未来振荡**
 * ——P-only 稳态余差不散（比例带死区）与手调阈值（无收敛语义）
 * 的病解。抗积分饱和（integral clamping：输出越界即停积）；
 * 输出夹挤 [min,max]；微分对首拍跳变防御（Δt 契约=每拍等步，
 * 明示）；增益非负/界序非法 fail-fast；确定性逐步可回放。
 *
 * <p>与 EwmaEstimator（metrics 域）同域不同面：平滑估计 vs
 * 反馈控制；与 GradientBandit（experiment 域）同根不同面：
 * 偏好学习 vs 误差驱动闭环。
 */
public final class PidController {

    private final double kp;
    private final double ki;
    private final double kd;
    private final double outputMin;
    private final double outputMax;

    private double integral;
    private Double previousError;

    /** 增益与输出界（增益 ≥0、min<max）。 */
    public PidController(double kp, double ki, double kd, double outputMin, double outputMax) {
        if (kp < 0 || ki < 0 || kd < 0 || Double.isNaN(kp) || Double.isNaN(ki) || Double.isNaN(kd)) {
            throw new IllegalArgumentException("增益非负（实际 " + kp + "/" + ki + "/" + kd + "）");
        }
        if (!(outputMin < outputMax) || Double.isNaN(outputMin) || Double.isNaN(outputMax)) {
            throw new IllegalArgumentException("输出界 min<max（实际 " + outputMin + "," + outputMax + "）");
        }
        this.kp = kp;
        this.ki = ki;
        this.kd = kd;
        this.outputMin = outputMin;
        this.outputMax = outputMax;
    }

    /**
     * 单步更新（setpoint 目标、measurement 当前测量——返回控制量）。
     *
     * @throws IllegalArgumentException NaN 输入
     */
    public double update(double setpoint, double measurement) {
        if (Double.isNaN(setpoint) || Double.isNaN(measurement)) {
            throw new IllegalArgumentException("输入有限（NaN 拒绝）");
        }
        double error = setpoint - measurement;
        if (previousError == null) {
            previousError = error;
        }
        double rawIntegral = integral + error;
        double derivative = error - previousError;
        double unbounded = kp * error + ki * rawIntegral + kd * derivative;
        if (unbounded > outputMax || unbounded < outputMin) {
            // 抗积分饱和：输出将越界——本拍误差不入积
            double output = clamp(unbounded);
            previousError = error;
            return output;
        }
        integral = rawIntegral;
        previousError = error;
        return clamp(unbounded);
    }

    /** 积分读数（同包测试面——抗饱和行为锚）。 */
    double integral() {
        return integral;
    }

    private double clamp(double value) {
        return Math.max(outputMin, Math.min(outputMax, value));
    }
}
