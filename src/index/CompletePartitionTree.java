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
 * 完全划分树（CPTree）索引结构构建类。
 * 使用线性无关的法向量组进行超平面划分。
 */
public class CompletePartitionTree {

    /**
     * CPT 的内部节点类。
     */
    public static class CPTInternalNode {
        public List<MetricData> pivots;           // 支撑点集合
        public List<Vector<Double>> normalVectors; // 线性无关的法向量组
        public List<Object> children;             // 子节点列表
        public double[][] lowerBounds;            // 每个子节点在各法向量方向上的下界
        public double[][] upperBounds;            // 每个子节点在各法向量方向上的上界

        public CPTInternalNode(List<MetricData> pivots, List<Vector<Double>> normalVectors, 
                               List<Object> children, double[][] lowerBounds, double[][] upperBounds) {
            this.pivots = pivots;
            this.normalVectors = normalVectors;
            this.children = children;
            this.lowerBounds = lowerBounds;
            this.upperBounds = upperBounds;
        }
    }

    /**
     * 构建 CPT 索引树。
     *
     * @param data               数据集
     * @param maxLeafSize        叶子节点最大容量
     * @param distance           距离函数
     * @param pivotSelector      支撑点选择器
     * @param numPivots          叶子节点支撑点数量
     * @param numRegions         每个法向量划分的区域数（扇出）
     * @param internal_numPivots 内部节点的支撑点数量
     * @return CPT 索引树的根节点（CPTInternalNode 或 PivotTable）
     */
    public static Object CPBulkLoad(List<MetricData> data, int maxLeafSize, MetricDistance distance,
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

        // 3. 使用变换矩阵法生成线性无关的法向量组（不一定正交）
        List<Vector<Double>> linearIndependentVectors = 
            generateLinearIndependentVectorsByTransformation(internal_numPivots);

        // 4. 从数据中移除支撑点
        List<MetricData> remaining = new ArrayList<>(data);
        for (MetricData pivot : pivots) {
            remaining.remove(pivot);
        }

        // 5. 初始划分为一个分区
        List<List<MetricData>> partitions = new ArrayList<>();
        partitions.add(remaining);

        // 6. 多轮划分，每轮按当前法向量划分所有已有分区
        for (int i = 0; i < linearIndependentVectors.size(); i++) {
            Vector<Double> normalVector = linearIndependentVectors.get(i);
            List<List<MetricData>> newPartitions = new ArrayList<>();
            
            for (List<MetricData> part : partitions) {
                if (!part.isEmpty()) {
                    // 基于法向量进行划分
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
                                                                   linearIndependentVectors.get(j), 
                                                                   distanceMatrix, data);
                    lowerBounds[j][i] = interceptRange[0];
                    upperBounds[j][i] = interceptRange[1];
                }
            }

            // 递归构建子节点
            Object child = CPBulkLoad(sub, maxLeafSize, distance, pivotSelector, 
                                     numPivots, numRegions, internal_numPivots);
            children.add(child);
        }

        return new CPTInternalNode(pivots, linearIndependentVectors, children, lowerBounds, upperBounds);
    }

    /**
     * 使用变换矩阵法生成线性无关的法向量组。
     * 对于d维空间，生成d个线性无关的法向量（不一定正交）。
     * 方法：从标准基开始，应用一个随机可逆线性变换。
     */
    private static List<Vector<Double>> generateLinearIndependentVectorsByTransformation(int dim) {
        // 1. 从标准基开始
        List<Vector<Double>> standardBasis = new ArrayList<>();
        for (int i = 0; i < dim; i++) {
            Vector<Double> basisVector = new Vector<>(dim);
            for (int j = 0; j < dim; j++) {
                basisVector.add((j == i) ? 1.0 : 0.0);
            }
            standardBasis.add(basisVector);
        }
        
        // 2. 生成一个随机可逆矩阵作为变换矩阵
        // 通过生成下三角矩阵L（对角线为1）和上三角矩阵U（对角线随机非零）的乘积来确保可逆
        double[][] L = generateRandomLowerTriangularMatrix(dim, true);  // 对角线为1的下三角矩阵
        double[][] U = generateRandomUpperTriangularMatrix(dim, false); // 对角线非零的上三角矩阵
        
        // 3. 计算变换矩阵 T = L * U（确保可逆）
        double[][] transformationMatrix = matrixMultiply(L, U, dim);
        
        // 4. 应用变换到标准基
        List<Vector<Double>> transformedVectors = new ArrayList<>();
        for (Vector<Double> basisVector : standardBasis) {
            Vector<Double> transformedVector = new Vector<>(dim);
            for (int i = 0; i < dim; i++) {
                double sum = 0.0;
                for (int j = 0; j < dim; j++) {
                    sum += transformationMatrix[i][j] * basisVector.get(j);
                }
                transformedVector.add(sum);
            }
            transformedVectors.add(transformedVector);
        }
        
        return transformedVectors;
    }
    
    /**
     * 生成随机下三角矩阵。
     * @param dim 矩阵维度
     * @param unitDiagonal 是否使用单位对角线（对角线为1）
     */
    private static double[][] generateRandomLowerTriangularMatrix(int dim, boolean unitDiagonal) {
        double[][] matrix = new double[dim][dim];
        
        for (int i = 0; i < dim; i++) {
            for (int j = 0; j < dim; j++) {
                if (i < j) {
                    // 上三角部分设为0
                    matrix[i][j] = 0.0;
                } else if (i == j) {
                    // 对角线
                    if (unitDiagonal) {
                        matrix[i][j] = 1.0;
                    } else {
                        // 生成非零随机数
                        matrix[i][j] = 0.5 + Math.random(); // 确保在(0.5, 1.5)范围内
                    }
                } else {
                    // 下三角部分（非对角线）
                    matrix[i][j] = (Math.random() * 2.0) - 1.0; // [-1, 1]范围内
                }
            }
        }
        
        return matrix;
    }
    
    /**
     * 生成随机上三角矩阵。
     * @param dim 矩阵维度
     * @param unitDiagonal 是否使用单位对角线
     */
    private static double[][] generateRandomUpperTriangularMatrix(int dim, boolean unitDiagonal) {
        double[][] matrix = new double[dim][dim];
        
        for (int i = 0; i < dim; i++) {
            for (int j = 0; j < dim; j++) {
                if (i > j) {
                    // 下三角部分设为0
                    matrix[i][j] = 0.0;
                } else if (i == j) {
                    // 对角线
                    if (unitDiagonal) {
                        matrix[i][j] = 1.0;
                    } else {
                        // 生成非零随机数
                        matrix[i][j] = 0.5 + Math.random(); // 确保在(0.5, 1.5)范围内
                    }
                } else {
                    // 上三角部分（非对角线）
                    matrix[i][j] = (Math.random() * 2.0) - 1.0; // [-1, 1]范围内
                }
            }
        }
        
        return matrix;
    }
    
    /**
     * 矩阵乘法。
     */
    private static double[][] matrixMultiply(double[][] A, double[][] B, int dim) {
        double[][] result = new double[dim][dim];
        
        for (int i = 0; i < dim; i++) {
            for (int j = 0; j < dim; j++) {
                result[i][j] = 0.0;
                for (int k = 0; k < dim; k++) {
                    result[i][j] += A[i][k] * B[k][j];
                }
            }
        }
        
        return result;
    }
    
    /**
     * 可选方法：使用随机矩阵方法生成线性无关向量
     * 这种方法可能生成正交的向量，也可能不生成，取决于随机性
     */
    private static List<Vector<Double>> generateLinearIndependentVectorsRandom(int dim) {
        List<Vector<Double>> vectors = new ArrayList<>();
        
        for (int i = 0; i < dim; i++) {
            Vector<Double> vector = new Vector<>(dim);
            // 为每个向量生成随机值
            for (int j = 0; j < dim; j++) {
                // 生成[-1, 1]范围内的随机数
                vector.add((Math.random() * 2.0) - 1.0);
            }
            vectors.add(vector);
        }
        
        // 这里不进行正交化，保持原始随机向量
        // 注意：这些向量线性无关的概率很高，但不是100%保证
        return vectors;
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
     * 基于法向量将数据划分为 numRegions 个区域（平衡划分）。
     */
    private static List<List<MetricData>> splitByNormalVector(
            List<MetricData> data, List<MetricData> pivots, Vector<Double> normalVector,
            int numRegions, double[][] distanceMatrix, List<MetricData> originalData) {

        // 计算每个数据点在法向量方向上的截距
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
     * 计算数据子集在某个法向量方向上的截距范围。
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
     * 向量归一化（辅助方法）。
     */
    private static Vector<Double> normalize(Vector<Double> vector) {
        double norm = 0.0;
        for (Double val : vector) {
            norm += val * val;
        }
        norm = Math.sqrt(norm);
        
        // 如果向量是零向量，返回第一个标准基向量
        if (norm < 1e-10) {
            Vector<Double> unitVector = new Vector<>(vector.size());
            for (int i = 0; i < vector.size(); i++) {
                unitVector.add(0.0);
            }
            if (vector.size() > 0) {
                unitVector.set(0, 1.0);
            }
            return normalize(unitVector);
        }
        
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