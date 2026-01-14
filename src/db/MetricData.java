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

    /**
     * 获取向量形式的数据（主要用于向量索引和计算距离）。
     * 
     * @return double[] 向量
     * @throws UnsupportedOperationException 如果该 MetricData 不支持向量形式
     */
    public double[] getValues() {
        throw new UnsupportedOperationException("This MetricData does not support getValues()");
    }
}
