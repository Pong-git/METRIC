package index;

import db.MetricData;
import distance_function.MetricDistance;
import algorithms.pivotselection.PivotSelectionMethod;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * 多优势点树（MVPT）索引结构构建类。
 */
public class MultipleVantagePointTree {

    /**
     * MVPT 的内部节点类。
     */
    public static class MVPInternalNode {
        public List<MetricData> pivots;
        public List<Object> children;
        public double[][] lowerBounds;
        public double[][] upperBounds;

        public MVPInternalNode(List<MetricData> pivots, List<Object> children, double[][] lowerBounds, double[][] upperBounds) {
            this.pivots = pivots;
            this.children = children;
            this.lowerBounds = lowerBounds;
            this.upperBounds = upperBounds;
        }
    }

    /**
     * 构建 MVPT 索引树。
     *
     * @param data               数据集
     * @param maxLeafSize        叶子节点最大容量
     * @param distance           距离函数
     * @param pivotSelector      支撑点选择器
     * @param numPivots          叶子节点支撑点数量
     * @param numRegions         每个支撑点划分的区域数
     * @param internal_numPivots  内部节点的支撑点数量
     * @return MVPT 索引树的根节点（MVPInternalNode 或 PivotTable）
     */
    public static Object MVPBulkLoad(List<MetricData> data, int maxLeafSize, MetricDistance distance,
                                    PivotSelectionMethod pivotSelector, int numPivots, int numRegions, int internal_numPivots) {

        if (data == null || data.isEmpty()) {
            return null;
        }

        // 小于等于叶子容量或支撑点数量不足，构建叶子节点
        if (data.size() <= maxLeafSize || data.size() < internal_numPivots) {
            return new PivotTable(data, maxLeafSize, numPivots, pivotSelector, distance);
        }

        // 选择内部节点支撑点
        List<MetricData> pivots = pivotSelector.selectPivots(data, internal_numPivots, distance);

        // 从数据中移除支撑点
        List<MetricData> remaining = new ArrayList<>(data);
        for (MetricData pivot : pivots) {
            remaining.remove(pivot);
        }

        // 初始划分为一个分区
        List<List<MetricData>> partitions = new ArrayList<>();
        partitions.add(remaining);

        // 多轮划分，每轮按当前支撑点划分所有已有分区
        for (MetricData pivot : pivots) {
            List<List<MetricData>> newPartitions = new ArrayList<>();
            for (List<MetricData> part : partitions) {
                if (!part.isEmpty()) {
                    newPartitions.addAll(splitByPivot(part, pivot, numRegions, distance));
                }
            }
            partitions = newPartitions;
        }

        int numChildren = partitions.size();
        double[][] lowerBound = new double[internal_numPivots][numChildren];
        double[][] upperBound = new double[internal_numPivots][numChildren];
        List<Object> children = new ArrayList<>();

        for (int i = 0; i < numChildren; i++) {
            List<MetricData> sub = partitions.get(i);

            for (int j = 0; j < internal_numPivots; j++) {
                if (sub.isEmpty()) {
                    lowerBound[j][i] = 0;
                    upperBound[j][i] = 0;
                } else {
                    List<Double> dists = new ArrayList<>();
                    for (MetricData p : sub) {
                        dists.add(distance.getDistance(pivots.get(j), p));
                    }
                    lowerBound[j][i] = Collections.min(dists);
                    upperBound[j][i] = Collections.max(dists);
                }
            }

            // 递归构建子节点
            Object child = MVPBulkLoad(sub, maxLeafSize, distance, pivotSelector, numPivots, numRegions, internal_numPivots);
            children.add(child);
        }

        return new MVPInternalNode(pivots, children, lowerBound, upperBound);
    }

    /**
     * 将数据根据某个支撑点划分为 numRegions 个区域。
     */
    private static List<List<MetricData>> splitByPivot(List<MetricData> data, MetricData pivot, int numRegions, MetricDistance distance) {

        List<DistancePointPair> pairs = new ArrayList<>();
        for (MetricData point : data) {
            double dist = distance.getDistance(pivot, point);
            pairs.add(new DistancePointPair(dist, point));
        }

        pairs.sort(Comparator.comparingDouble(a -> a.distance));

        int total = pairs.size();
        int regionSize = total / numRegions;
        List<List<MetricData>> regions = new ArrayList<>();

        for (int i = 0; i < numRegions; i++) {
            int start = i * regionSize;
            int end = (i == numRegions - 1) ? total : start + regionSize;
            List<MetricData> region = new ArrayList<>();
            for (int j = start; j < end; j++) {
                region.add(pairs.get(j).point);
            }
            regions.add(region);
            }

        return regions;
    }

    /**
     * 简单封装距离和点的配对关系。
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
