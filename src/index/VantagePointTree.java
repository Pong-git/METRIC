package index;

import db.MetricData;
import distance_function.MetricDistance;
import algorithms.pivotselection.PivotSelectionMethod;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * Vantage Point Tree（VPT）索引的构建模块。
 * 包括内部节点数据结构 VPTInternalNode 和索引构建函数 VPBulkLoad。
 */
public class VantagePointTree {

    /**
     * VPT 的内部节点，包含一个支撑点、划分半径、左右子树。
     */
    public static class VPTInternalNode {
        public MetricData pivot;
        public double splitRadius;
        public Object left;
        public Object right;

        public VPTInternalNode(MetricData pivot, double splitRadius, Object left, Object right) {
            this.pivot = pivot;
            this.splitRadius = splitRadius;
            this.left = left;
            this.right = right;
        }
    }

    /**
     * 批量构建 VPT 索引树。
     *
     * @param data         当前节点数据集
     * @param maxLeafSize  叶子节点最大容量
     * @param distance     距离函数
     * @param selector     支撑点选择方法
     * @param numPivots    支撑点数量（VPT 固定为 1）
     * @return 构建出的子树（VPTInternalNode 或 PivotTable）
     */
    public static Object VPBulkLoad(List<MetricData> data, int maxLeafSize, MetricDistance distance, PivotSelectionMethod selector, int numPivots) {
        if (data == null || data.isEmpty()) return null;

        // 若数据量小于等于最大叶子容量，构造 PivotTable
        if (data.size() <= maxLeafSize) {
            return new PivotTable(data, maxLeafSize, numPivots, selector, distance);
        }

        // 选取一个支撑点
        List<MetricData> pivots = selector.selectPivots(data, 1, distance);
        MetricData vp = pivots.get(0);
        // 从数据集中移除支撑点
        List<MetricData> remainingData = new ArrayList<>(data);
        remainingData.remove(vp);

        // 计算数据集中每个点到支撑点的距离
        List<DistancePointPair> pairs = new ArrayList<>();
        for (MetricData point : remainingData) {
            if (!point.equals(vp)) {
                double d = distance.getDistance(vp, point);
                pairs.add(new DistancePointPair(d, point));
            }
        }

        // 按照距离排序
        Collections.sort(pairs, Comparator.comparingDouble(p -> p.distance));

        // 使用中位数距离作为划分半径
        int medianIndex = pairs.size() / 2;
        double splitRadius = pairs.get(medianIndex).distance;

        // 按照划分半径划分为左右子集
        List<MetricData> leftData = new ArrayList<>();
        List<MetricData> rightData = new ArrayList<>();
        for (int i = 0; i < pairs.size(); i++) {
            MetricData point = pairs.get(i).point;
            if (i < medianIndex) {
                leftData.add(point);
            } else {
                rightData.add(point);
            }
        }

        // 构建左右子树
        Object left = VPBulkLoad(leftData, maxLeafSize, distance, selector, numPivots);
        Object right = VPBulkLoad(rightData, maxLeafSize, distance, selector, numPivots);

        return new VPTInternalNode(vp, splitRadius, left, right);
    }

    /**
     * 用于距离排序的数据结构。
     */
    private static class DistancePointPair {
        double distance;
        MetricData point;

        DistancePointPair(double distance, MetricData point) {
            this.distance = distance;
            this.point = point;
        }
    }
}
