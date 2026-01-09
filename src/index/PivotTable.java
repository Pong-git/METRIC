package index;

import db.MetricData;
import distance_function.MetricDistance;
import algorithms.pivotselection.PivotSelectionMethod;

import java.util.ArrayList;
import java.util.List;

/**
 * PivotTable 是索引树中的叶子节点结构，
 * 自动选择支撑点并预计算距离矩阵。
 */
public class PivotTable {

    private final List<MetricData> dataPoints;    // 当前叶子节点中的所有非支撑点数据
    private final List<MetricData> pivots;        // 自动选出的支撑点集合
    private final double[][] distanceTable;       // 支撑点到数据点的距离矩阵

    /**
     * 构造函数，自动选择支撑点。
     *
     * @param dataPoints 当前叶子节点中的数据点（不超过 maxLeafSize）
     * @param maxLeafSize 叶子节点最大容量（保留参数）
     * @param numPivots 支撑点数量
     * @param selector 支撑点选择策略
     * @param distance 距离函数
     */
    public PivotTable(List<MetricData> dataPoints, int maxLeafSize, int numPivots, PivotSelectionMethod selector, MetricDistance distance) {

        if (dataPoints == null || selector == null || distance == null) {
            throw new IllegalArgumentException("PivotTable 构造参数不能为 null");
        }

        if (numPivots <= 0) {
            System.out.println("[警告] 支撑点数量非法，已重置为 1");
            numPivots = 1;
        } else if (numPivots > dataPoints.size()) {
            System.out.println("[警告] 支撑点数量大于数据点数，已重置为数据点数量");
            numPivots = dataPoints.size();
        }

        // 创建数据副本，防止原始集合被修改
        List<MetricData> dataCopy = new ArrayList<>(dataPoints);

        // 选择支撑点
        this.pivots = selector.selectPivots(dataCopy, numPivots, distance);
        System.out.println("叶子节点支撑点: " + pivots);

        // 将支撑点从数据中移除
        for (MetricData pivot : pivots) {
            dataCopy.remove(pivot);  // 只移除匹配的第一个
        }
        this.dataPoints = dataCopy;

        // 初始化距离矩阵：支撑点 × 数据点
        int numPoints = this.dataPoints.size();
        this.distanceTable = new double[numPivots][numPoints];

        for (int i = 0; i < numPivots; i++) {
            MetricData pivot = pivots.get(i);
            for (int j = 0; j < numPoints; j++) {
                this.distanceTable[i][j] = distance.getDistance(pivot, this.dataPoints.get(j));
            }
        }
    }

    public List<MetricData> getDataPoints() {
        return dataPoints;
    }

    public List<MetricData> getPivots() {
        return pivots;
    }

    public double getDistance(int pivotIndex, int pointIndex) {
        return distanceTable[pivotIndex][pointIndex];
    }

    public double[][] getAllDistances() {
        return distanceTable;
    }

    public int size() {
        return dataPoints.size();
    }
}
