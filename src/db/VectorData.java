package db;

import java.util.Arrays;

/**
 * VectorData 表示一个向量类型的数据对象。
 * 它封装了一个 double[] 向量。
 */
public class VectorData extends MetricData {

    private final double[] vector;

    /**
     * 构造函数。
     *
     * @param vector 一个 double 类型的一维数组
     */
    public VectorData(double[] vector) {
        this.vector = vector;
    }

    /**
     * 获取维度。
     *
     * @return 向量的维度
     */
    @Override
    public int getDimension() {
        return vector.length;
    }

    /**
     * 获取原始向量数据。
     *
     * @return double[] 向量
     */
    @Override
    public Object getRawData() {
        return vector;
    }

    /**
     * 获取具体向量（更具体的类型方法）。
     *
     * @return double[] 向量
     */
    public double[] getVector() {
        return vector;
    }

    @Override
    public String toString() {
        return Arrays.toString(vector);
    }
}
