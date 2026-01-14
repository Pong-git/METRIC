package algorithms.pivotselection;

import db.MetricData;
import distance_function.MetricDistance;

import java.util.ArrayList;
import java.util.List;

/**
 * FarthestFirstTraversalSelection 是支撑点选择的一种实现方式。
 * 它使用最远优先遍历算法选择支撑点：
 * 1. 随机选择第一个支撑点
 * 2. 每次选择距离已选支撑点集合最远的数据点作为下一个支撑点
 * 3. 重复直到选择足够数量的支撑点
 */
public class FarthestFirstTraversalSelection implements PivotSelectionMethod {

    @Override
    public List<MetricData> selectPivots(List<MetricData> dataSet, int numPivots, MetricDistance distance) {
        int n = dataSet.size();
        if (numPivots > n) {
            throw new IllegalArgumentException("支撑点数量不能超过数据集大小");
        }
        if (numPivots <= 0) {
            throw new IllegalArgumentException("支撑点数量必须大于0");
        }

        List<MetricData> pivots = new ArrayList<>(numPivots);
        
        // 1. 随机选择第一个支撑点
        int firstIdx = (int) (Math.random() * n);
        MetricData firstPivot = dataSet.get(firstIdx);
        pivots.add(firstPivot);

        // 2. 初始化距离数组：每个点到已选支撑点的最小距离
        double[] minDistances = new double[n];
        for (int i = 0; i < n; i++) {
            if (i == firstIdx) {
                minDistances[i] = 0;  // 支撑点自身的距离为0
            } else {
                minDistances[i] = distance.getDistance(dataSet.get(i), firstPivot);
            }
        }

        // 3. 迭代选择剩余的支撑点
        for (int p = 1; p < numPivots; p++) {
            // 找到距离已选支撑点最远的点
            int farthestIdx = -1;
            double maxDistance = -1.0;
            
            for (int i = 0; i < n; i++) {
                if (minDistances[i] > maxDistance) {
                    maxDistance = minDistances[i];
                    farthestIdx = i;
                }
            }
            
            // 如果所有点都已被选为支撑点或距离为0，提前结束
            if (farthestIdx == -1 || maxDistance <= 0) {
                break;
            }
            
            // 添加新的支撑点
            MetricData newPivot = dataSet.get(farthestIdx);
            pivots.add(newPivot);
            
            // 更新所有点到已选支撑点的最小距离
            for (int i = 0; i < n; i++) {
                if (minDistances[i] > 0) {  // 只更新非支撑点
                    double distToNewPivot = distance.getDistance(dataSet.get(i), newPivot);
                    minDistances[i] = Math.min(minDistances[i], distToNewPivot);
                }
            }
        }

        return pivots;
    }
}