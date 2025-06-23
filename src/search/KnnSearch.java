package search;

import db.MetricData;
import distance_function.MetricDistance;

import java.util.*;

public class KnnSearch {

    // 在 KnnSearch.java 类中添加此方法（作为工具方法）
    public static Map<MetricData, Double> precomputePivotDistances(
            List<MetricData> dataset,
            MetricData pivot,
            MetricDistance distance
    ) {
        Map<MetricData, Double> pivotDistances = new HashMap<>();
        for (MetricData data : dataset) {
            pivotDistances.put(data, distance.getDistance(pivot, data));
        }
        // 预处理阶段不计入距离计算次数
        distance.resetCount();
        return pivotDistances;
    }


    /**
     * 使用一个支撑点和三角不等式剪枝执行 k=1 最近邻查询。
     *
     * @param query 查询点
     * @param dataset 数据集合
     * @param distance 距离函数（用于计数）
     * @param pivot 支撑点
     * @param pivotDistances 支撑点到各数据点的预计算距离（不计入查询距离计数）
     * @return 最近邻数据点及其距离（键值对）
     */
    public Map.Entry<MetricData, Double> searchK1(
            MetricData query,
            List<MetricData> dataset,
            MetricDistance distance,
            MetricData pivot,
            Map<MetricData, Double> pivotDistances
    ) {
        distance.resetCount();
        System.out.println("========== 最近邻查询 ==========");
        System.out.println("查询点: " + query);
        System.out.println("使用支撑点: " + pivot);
        double d_qp = distance.getDistance(query, pivot);
        System.out.println("查询点与支撑点间的距离为: " + d_qp);

        MetricData bestCandidate = null;
        double bestUpperBound = Double.MAX_VALUE;
        Map<MetricData, Double> lowerBounds = new HashMap<>(); 
        // 1. 计算所有点的上下界
        for (MetricData data : dataset) {
            double d_pd = pivotDistances.get(data);
            double lower = Math.abs(d_qp - d_pd);
            double upper = d_qp + d_pd;

            lowerBounds.put(data, lower);
            if (upper < bestUpperBound) {
                bestUpperBound = upper;
                bestCandidate = data;
            }
        }
        // 2. 尝试剪枝
        List<MetricData> candidates = new ArrayList<>();
        for (MetricData data : dataset) {
            if (data.equals(bestCandidate)) continue;
            double lower = lowerBounds.get(data);
            if (bestUpperBound <= lower) {
                // 可以剪枝
                System.out.println("已剪枝数据点: " + data);
                continue;
            } else {
                candidates.add(data);
            }
        }
        // 3. 检查 bestCandidate 是否就是最终结果
        if (candidates.isEmpty()) {
            System.out.println("查询结果：" + bestCandidate);
            System.out.println("最近距离：" + bestUpperBound);
            System.out.println("距离计算次数 = " + distance.getCount());
            return Map.entry(bestCandidate, bestUpperBound);
        }
        // 4. 对剩余未被剪枝的点计算实际距离
        double bestDist = bestUpperBound;
        for (MetricData data : candidates) {
            double actualDist;
            if (data.equals(query)) {
                actualDist = 0.0;
            } else if (data.equals(pivot)) {
                actualDist = d_qp;
            } else {
                actualDist = distance.getDistance(query, data);
            }

            if (actualDist < bestDist) {
                bestDist = actualDist;
                bestCandidate = data;
            }
        }
        System.out.println("查询结果：" + bestCandidate);
        System.out.println("最近距离：" + bestDist);
        System.out.println("距离计算次数 = " + distance.getCount());
        return Map.entry(bestCandidate, bestDist);
    }

}

