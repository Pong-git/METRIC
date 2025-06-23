package distance_function;

import db.MetricData;

/**
 * MetricDistance 是所有距离函数的抽象父类。
 * 它定义统一的距离接口，并统计距离函数被调用的次数。
 */
public abstract class MetricDistance {

    protected long count = 0;  // 距离计算计数器

    /**
     * 获取两个数据对象之间的距离。
     * 子类需实现具体算法。
     *
     * @param a 第一个数据对象
     * @param b 第二个数据对象
     * @return a 与 b 之间的距离（double）
     */
    public double getDistance(MetricData a, MetricData b) {
        count++;  // 每调用一次距离函数就计数
        return compute(a, b);
    }

    /**
     * 子类应实现此方法，定义具体距离计算逻辑。
     */
    protected abstract double compute(MetricData a, MetricData b);

    /**
     * 获取累计距离计算次数。
     *
     * @return 距离计算次数
     */
    public long getCount() {
        return count;
    }

    /**
     * 重置距离计算次数为 0。
     */
    public void resetCount() {
        count = 0;
    }
}
