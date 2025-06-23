package distance_function;

import db.MetricData;
import db.VectorData;

/**
 * LDistance 实现一般的 Lp 距离（闵可夫斯基距离），包括 L1, L2, L3, ... ∞。
 * 默认使用欧几里得距离（L2）。
 */
public class LDistance extends MetricDistance {

    private final double p;  // Lp 中的 p 值

    /**
     * 默认构造函数，使用欧几里得距离（p = 2）
     */
    public LDistance() {
        this.p = 2.0;
    }

    /**
     * 自定义 p 值的构造函数。
     *
     * @param p 距离参数（必须大于 0）
     */
    public LDistance(double p) {
        if (p <= 0.0) {
            throw new IllegalArgumentException("p 值必须大于 0");
        }
        this.p = p;
    }

    /**
     * 获取当前使用的 p 值
     *
     * @return p
     */
    public double getP() {
        return p;
    }

    @Override
    protected double compute(MetricData a, MetricData b) {
        // 向量数据校验
        if (!(a instanceof VectorData) || !(b instanceof VectorData)) {
            throw new IllegalArgumentException("LDistance 仅支持 VectorData 类型");
        }

        double[] va = ((VectorData) a).getVector();
        double[] vb = ((VectorData) b).getVector();

        if (va.length != vb.length) {
            throw new IllegalArgumentException("向量维度不一致");
        }

        double sum = 0.0;
        double max = 0.0;

        for (int i = 0; i < va.length; i++) {
            double diff = Math.abs(va[i] - vb[i]);

            if (p == Double.POSITIVE_INFINITY) {
                // 切比雪夫距离（L∞）
                if (diff > max) max = diff;
            } else {
                // 一般 Lp 距离
                sum += Math.pow(diff, p);
            }
        }

        return (p == Double.POSITIVE_INFINITY) ? max : Math.pow(sum, 1.0 / p);
    }
}
