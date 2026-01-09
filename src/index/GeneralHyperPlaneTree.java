package index;

import db.MetricData;
import distance_function.MetricDistance;
import algorithms.pivotselection.PivotSelectionMethod;

import java.util.ArrayList;
import java.util.List;

/**
 * General Hyperplane Tree 索引的构建模块。
 * 包含内部节点 GHInternalNode 的定义及索引构建函数 GHBulkLoad。
 */
public class GeneralHyperPlaneTree {

    /**
     * GHT 的内部节点，包含两个支撑点及左右子树。
     */
    public static class GHInternalNode {
        public MetricData pivot1;
        public MetricData pivot2;
        public Object left;
        public Object right;

        public GHInternalNode(MetricData pivot1, MetricData pivot2, Object left, Object right) {
            this.pivot1 = pivot1;
            this.pivot2 = pivot2;
            this.left = left;
            this.right = right;
        }
    }

    /**
     * 批量构建 GHT 索引树。
     *
     * @param data           当前节点的数据集
     * @param maxLeafSize    叶子节点最大数据数
     * @param distance       距离函数
     * @param pivotSelector  支撑点选择策略
     * @param numPivots      支撑点数量（统一参数）
     * @return 构建完成的索引节点（GHInternalNode 或 PivotTable）
     */
    public static Object GHBulkLoad(List<MetricData> data,int maxLeafSize,MetricDistance distance,PivotSelectionMethod pivotSelector,int numPivots) {

        if (data == null || data.isEmpty()) return null;

        // 如果数据量不超过 maxLeafSize，构建 PivotTable 作为叶子节点
        if (data.size() <= maxLeafSize) {
            return new PivotTable(data, maxLeafSize, numPivots, pivotSelector, distance);
        }

        // 构建内部节点，默认使用两个支撑点
        List<MetricData> pivots = pivotSelector.selectPivots(data, 2, distance);
        MetricData pivot1 = pivots.get(0);
        MetricData pivot2 = pivots.get(1);
        System.out.println("内部节点: " + pivot1 + ", " + pivot2);

        //移除支撑点
        List<MetricData> remainingData = new ArrayList<>(data);
        remainingData.remove(pivot1);
        remainingData.remove(pivot2);

        //数据划分
        List<MetricData> leftList = new ArrayList<>();
        List<MetricData> rightList = new ArrayList<>();

        for (MetricData point : remainingData) {
            double d1 = distance.getDistance(point, pivot1);
            double d2 = distance.getDistance(point, pivot2);
            if (d1 <= d2) {
                leftList.add(point);
            } else {
                rightList.add(point);
            }
        }

        Object left = GHBulkLoad(leftList, maxLeafSize, distance, pivotSelector, numPivots);
        Object right = GHBulkLoad(rightList, maxLeafSize, distance, pivotSelector, numPivots);

        return new GHInternalNode(pivot1, pivot2, left, right);
    }
}
