package algorithms.pivotselection;

import db.MetricData;
import distance_function.MetricDistance;

import java.util.List;

/**
 * 支撑点选择方法的接口。
 * 所有支撑点选择算法都应实现此接口。
 */
public interface PivotSelectionMethod {
    /**
     * 从数据集中选择 numPivots 个支撑点，返回其在原数据集中的下标。
     *
     * @param dataSet 数据集
     * @param numPivots 支撑点数量
     * @param distance 距离函数
     * @return 支撑点下标列表
     */
    List<MetricData> selectPivots(List<MetricData> dataSet, int numPivots, MetricDistance distance);
}
