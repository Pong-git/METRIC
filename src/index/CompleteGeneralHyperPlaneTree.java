package index;

import db.MetricData;
import distance_function.MetricDistance;
import algorithms.pivotselection.PivotSelectionMethod;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Vector;

/**
 * 共轭梯度超平面树（CGHT）索引结构构建类。
 * 仅使用CGH法向量进行超平面划分。
 */
public class CompleteGeneralHyperPlaneTree {

    /**
     * CGHT 的内部节点类。
     */
    public static class CGHTInternalNode {
        public List<MetricData> pivots;           // 支撑点集合
        public List<Vector<Double>> normalVectors; // CGH法向量组（每个支撑点对应一个法向量）
        public List<Object> children;             // 子节点列表
        public double[][] lowerBounds;            // 每个子节点在各法向量方向上的下界
        public double[][] upperBounds;            // 每个子节点在各法向量方向上的上界

        public CGHTInternalNode(List<MetricData> pivots, List<Vector<Double>> normalVectors, 
                               List<Object> children, double[][] lowerBounds, double[][] upperBounds) {
            this.pivots = pivots;
            this.normalVectors = normalVectors;
            this.children = children;
            this.lowerBounds = lowerBounds;
            this.upperBounds = upperBounds;
        }
    }

    /**
     * 构建 CGHT 索引树。
     *
     * @param data               数据集
     * @param maxLeafSize        叶子节点最大容量
     * @param distance           距离函数
     * @param pivotSelector      支撑点选择器
     * @param numPivots          叶子节点支撑点数量
     * @param numRegions         每个法向量划分的区域数（扇出）
     * @param internal_numPivots 内部节点的支撑点数量
     * @return CGHT 索引树的根节点（CGHTInternalNode 或 PivotTable）
     */
    public static Object CGHBulkLoad(List<MetricData> data, int maxLeafSize, MetricDistance distance,
                                    PivotSelectionMethod pivotSelector, int numPivots, int numRegions, 
                                    int internal_numPivots) {

        if (data == null || data.isEmpty()) {
            return null;
        }

        // 小于等于叶子容量或支撑点数量不足，构建叶子节点
        if (data.size() <= maxLeafSize || data.size() < internal_numPivots) {
            return new PivotTable(data, maxLeafSize, numPivots, pivotSelector, distance);
        }

        // 1. 选择内部节点支撑点
        List<MetricData> pivots = pivotSelector.selectPivots(data, internal_numPivots, distance);

        // 2. 计算数据在支撑点空间的坐标（距离矩阵）
        double[][] distanceMatrix = computeDistanceMatrix(data, pivots, distance);

        // 3. 生成CGH法向量组
        List<Vector<Double>> cghNormalVectors = generateCGHNormalVectors(internal_numPivots);

        // 4. 从数据中移除支撑点
        List<MetricData> remaining = new ArrayList<>(data);
        for (MetricData pivot : pivots) {
            remaining.remove(pivot);
        }

        // 5. 初始划分为一个分区
        List<List<MetricData>> partitions = new ArrayList<>();
        partitions.add(remaining);

        // 6. 多轮划分，每轮按当前CGH法向量划分所有已有分区
        for (int i = 0; i < cghNormalVectors.size(); i++) {
            Vector<Double> normalVector = cghNormalVectors.get(i);
            List<List<MetricData>> newPartitions = new ArrayList<>();
            
            for (List<MetricData> part : partitions) {
                if (!part.isEmpty()) {
                    // 基于CGH法向量进行划分
                    newPartitions.addAll(splitByNormalVector(part, pivots, normalVector, 
                                                           numRegions, distanceMatrix, data));
                }
            }
            partitions = newPartitions;
        }

        // 7. 构建子节点并计算边界信息
        int numChildren = partitions.size();
        double[][] lowerBounds = new double[internal_numPivots][numChildren];
        double[][] upperBounds = new double[internal_numPivots][numChildren];
        List<Object> children = new ArrayList<>();

        for (int i = 0; i < numChildren; i++) {
            List<MetricData> sub = partitions.get(i);

            // 计算子节点在各法向量方向上的边界
            for (int j = 0; j < internal_numPivots; j++) {
                if (sub.isEmpty()) {
                    lowerBounds[j][i] = 0;
                    upperBounds[j][i] = 0;
                } else {
                    // 计算子节点中所有数据在第j个法向量方向上的截距范围
                    double[] interceptRange = computeInterceptRange(sub, pivots, 
                                                                   cghNormalVectors.get(j), 
                                                                   distanceMatrix, data);
                    lowerBounds[j][i] = interceptRange[0];
                    upperBounds[j][i] = interceptRange[1];
                }
            }

            // 递归构建子节点
            Object child = CGHBulkLoad(sub, maxLeafSize, distance, pivotSelector, 
                                      numPivots, numRegions, internal_numPivots);
            children.add(child);
        }

        return new CGHTInternalNode(pivots, cghNormalVectors, children, lowerBounds, upperBounds);
    }

    /**
     * 生成CGH法向量组。
     * 对于d维空间，生成d个CGH法向量。
     */
    private static List<Vector<Double>> generateCGHNormalVectors(int dim) {
        List<Vector<Double>> cghVectors = new ArrayList<>();
        
        // 1. 前dim-1个CGH法向量：(1, -1, 0, ..., 0)形式
        for (int i = 0; i < dim - 1; i++) {
            Vector<Double> cghVector = new Vector<>(dim);
            for (int j = 0; j < dim; j++) {
                if (j == i) {
                    cghVector.add(1.0);
                } else if (j == i + 1) {
                    cghVector.add(-1.0);
                } else {
                    cghVector.add(0.0);
                }
            }
            cghVectors.add(normalize(cghVector));
        }
        
        // 2. 最后一个CGH法向量：(1, 0, ..., 0, 1)形式
        Vector<Double> lastCghVector = new Vector<>(dim);
        lastCghVector.add(1.0);
        for (int i = 1; i < dim - 1; i++) {
            lastCghVector.add(0.0);
        }
        if (dim > 1) {
            lastCghVector.add(1.0);
        } else {
            // 如果只有1维，则使用(1)
            lastCghVector.add(1.0);
        }
        cghVectors.add(normalize(lastCghVector));
        
        return cghVectors;
    }

    /**
     * 计算数据到所有支撑点的距离矩阵。
     */
    private static double[][] computeDistanceMatrix(List<MetricData> data, 
                                                   List<MetricData> pivots, 
                                                   MetricDistance distance) {
        double[][] matrix = new double[data.size()][pivots.size()];
        for (int i = 0; i < data.size(); i++) {
            for (int j = 0; j < pivots.size(); j++) {
                matrix[i][j] = distance.getDistance(data.get(i), pivots.get(j));
            }
        }
        return matrix;
    }

    /**
     * 基于CGH法向量将数据划分为 numRegions 个区域（平衡划分）。
     */
    private static List<List<MetricData>> splitByNormalVector(
            List<MetricData> data, List<MetricData> pivots, Vector<Double> normalVector,
            int numRegions, double[][] distanceMatrix, List<MetricData> originalData) {

        // 计算每个数据点在CGH法向量方向上的截距
        List<InterceptPointPair> pairs = new ArrayList<>();
        for (int idx = 0; idx < data.size(); idx++) {
            MetricData point = data.get(idx);
            int originalIdx = originalData.indexOf(point);
            
            double intercept = 0.0;
            for (int i = 0; i < pivots.size(); i++) {
                intercept += distanceMatrix[originalIdx][i] * normalVector.get(i);
            }
            
            pairs.add(new InterceptPointPair(intercept, point));
        }

        // 按截距排序
        pairs.sort(Comparator.comparingDouble(a -> a.intercept));

        // 平衡划分
        int total = pairs.size();
        List<List<MetricData>> regions = new ArrayList<>();
        
        if (numRegions >= total) {
            // 每个数据点一个区域
            for (InterceptPointPair pair : pairs) {
                List<MetricData> region = new ArrayList<>();
                region.add(pair.point);
                regions.add(region);
            }
            // 补齐空区域
            while (regions.size() < numRegions) {
                regions.add(new ArrayList<>());
            }
        } else {
            // 平衡划分（尽量使每个区域大小相近）
            int regionSize = total / numRegions;
            int remainder = total % numRegions;
            
            int start = 0;
            for (int i = 0; i < numRegions; i++) {
                int end = start + regionSize + (i < remainder ? 1 : 0);
                List<MetricData> region = new ArrayList<>();
                for (int j = start; j < end; j++) {
                    region.add(pairs.get(j).point);
                }
                regions.add(region);
                start = end;
            }
        }

        return regions;
    }

    /**
     * 计算数据子集在某个CGH法向量方向上的截距范围。
     */
    private static double[] computeInterceptRange(List<MetricData> subset, 
                                                 List<MetricData> pivots, 
                                                 Vector<Double> normalVector,
                                                 double[][] distanceMatrix, 
                                                 List<MetricData> originalData) {
        double minIntercept = Double.MAX_VALUE;
        double maxIntercept = Double.MIN_VALUE;
        
        for (MetricData point : subset) {
            int originalIdx = originalData.indexOf(point);
            double intercept = 0.0;
            for (int i = 0; i < pivots.size(); i++) {
                intercept += distanceMatrix[originalIdx][i] * normalVector.get(i);
            }
            
            minIntercept = Math.min(minIntercept, intercept);
            maxIntercept = Math.max(maxIntercept, intercept);
        }
        
        return new double[]{minIntercept, maxIntercept};
    }

    /**
     * 向量归一化。
     */
    private static Vector<Double> normalize(Vector<Double> vector) {
        double norm = 0.0;
        for (Double val : vector) {
            norm += val * val;
        }
        norm = Math.sqrt(norm);
        
        Vector<Double> normalized = new Vector<>();
        for (Double val : vector) {
            normalized.add(val / norm);
        }
        return normalized;
    }

    /**
     * 截距和点的配对关系。
     */
    private static class InterceptPointPair {
        double intercept;
        MetricData point;

        InterceptPointPair(double intercept, MetricData point) {
            this.intercept = intercept;
            this.point = point;
        }
    }
}