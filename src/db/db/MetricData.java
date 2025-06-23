package db;

/**
 * MetricData 是所有度量空间中数据对象的抽象基类。
 */
public abstract class MetricData {

    /**
     * 获取该数据对象的维度。
     *
     * @return 维度（int）
     */
    public abstract int getDimension();

    /**
     * 获取底层的原始数据对象（如 double[] 或 String）。
     *
     * @return 原始数据（Object 类型）
     */
    public abstract Object getRawData();
}
