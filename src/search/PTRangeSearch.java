package search;

import db.MetricData;
import distance_function.MetricDistance;
import index.PivotTable;

import java.util.ArrayList;
import java.util.List;

/**
 * PivotTable 的范围查询实现。
 * 可被多种索引结构叶子节点复用。
 */
public class PTRangeSearch {

    /**
     * 叶子节点范围查询。
     * 不重置距离计数，由调用者控制。
     *
     * @param pivotTable 叶子节点 PivotTable
     * @param query 查询点
     * @param radius 查询半径
     * @param distance 距离函数（带计数器）
     * @return 查询结果及距离计算次数封装
     */
    public static SearchResult rangeSearch(PivotTable pivotTable, MetricData query, double radius, MetricDistance distance) {

        List<MetricData> result = new ArrayList<>();
        List<MetricData> pivots = pivotTable.getPivots();

        int pivotCount = pivots.size();
        int dataCount = pivotTable.size();

        // 计算查询点到每个支撑点的距离
        double[] distQueryToPivots = new double[pivotCount];
        for (int i = 0; i < pivotCount; i++) {
            distQueryToPivots[i] = distance.getDistance(query, pivots.get(i));
            if (distQueryToPivots[i] <= radius) {
                result.add(pivots.get(i));
            }
        }

        // 遍历数据点进行剪枝与判断
        List<MetricData> dataPoints = pivotTable.getDataPoints();
        for (int j = 0; j < dataCount; j++) {
            MetricData point = dataPoints.get(j);

            boolean prunedOrIncluded = false;
            for (int i = 0; i < pivotCount; i++) {
                double distPivotToPoint = pivotTable.getDistance(i, j);
                double distQueryToPivot = distQueryToPivots[i];

                // 包含规则：支撑点与查询点距离和加点到支撑点距离 <= radius
                if (distQueryToPivot + distPivotToPoint <= radius) {
                    result.add(point);
                    prunedOrIncluded = true;
                    break;
                }

                // 排除规则：两距离差 > radius，点肯定不在范围内
                if (Math.abs(distQueryToPivot - distPivotToPoint) > radius) {
                    prunedOrIncluded = true;
                    break;
                }
            }

            // 需要直接距离计算确认
            if (!prunedOrIncluded) {
                if (distance.getDistance(point, query) <= radius) {
                    result.add(point);
                }
            }
        }

        // 返回结果和当前累计的距离计算次数
        return new SearchResult(result, distance.getCount());
    }
}
