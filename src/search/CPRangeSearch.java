package search;

import db.MetricData;
import distance_function.MetricDistance;
import index.CompletePartitionTree.CPTInternalNode;
import index.PivotTable;
import java.util.ArrayList;
import java.util.List;
import java.util.Vector;

/**
 * CPT（完全划分树）的范围查询类 - 修正版
 * 使用基于截距投影的正确排除和包含规则
 */
public class CPRangeSearch {

    /**
     * 执行 CPT 的范围查询。
     */
    public static SearchResult rangeSearch(Object node, MetricData query, double radius, MetricDistance distance) {
        List<MetricData> result = doRangeSearch(node, query, radius, distance);
        return new SearchResult(result, distance.getCount());
    }

    /**
     * 递归搜索过程 - 修正版
     */
    private static List<MetricData> doRangeSearch(Object node, MetricData query, double radius, MetricDistance distance) {
        List<MetricData> result = new ArrayList<>();

        // 如果是叶子节点
        if (node instanceof PivotTable) {
            return PTRangeSearch.rangeSearch((PivotTable) node, query, radius, distance).results;
        }

        CPTInternalNode internal = (CPTInternalNode) node;
        int numPivots = internal.pivots.size();
        int numChildren = internal.children.size();

        // Step 1: 计算查询点到每个支撑点的距离
        double[] distToPivots = new double[numPivots];
        for (int i = 0; i < numPivots; i++) {
            MetricData pivot = internal.pivots.get(i);
            double d = distance.getDistance(query, pivot);
            distToPivots[i] = d;
            // 支撑点本身如果在查询范围内，加入结果
            if (d <= radius) {
                result.add(pivot);
            }
        }

        // Step 2: 对每个子节点判断是否剪枝或包含
        for (int i = 0; i < numChildren; i++) {
            Object child = internal.children.get(i);
            if (child == null) continue;

            boolean skip = false;
            
            // === 修正1：基于截距投影的排除规则 ===
            boolean canExclude = false;
            for (int j = 0; j < numPivots; j++) {
                Vector<Double> normalVector = internal.normalVectors.get(j);
                double nodeMin = internal.lowerBounds[j][i];  // 子节点最小截距
                double nodeMax = internal.upperBounds[j][i];  // 子节点最大截距
                
                // 计算查询点在当前法向量上的投影
                double queryIntercept = computeQueryIntercept(distToPivots, normalVector);
                
                // 计算查询半径在法向量方向上的投影宽度 Δ = r × Σ|v_j|
                double delta = computeProjectionWidth(radius, normalVector);
                
                // 查询范围在当前法向量上的投影区间
                double queryMin = queryIntercept - delta;
                double queryMax = queryIntercept + delta;
                
                // 如果查询范围与子节点范围无交集，可以排除
                if (queryMax < nodeMin || queryMin > nodeMax) {
                    canExclude = true;
                    break;
                }
            }
            
            if (canExclude) {
                // 排除整个子节点
                continue;
            }
            
        //    // === 修正2：基于截距投影的包含规则 ===
        //    // === 正确的包含规则：基于截距投影 ===
        //     boolean fullyContained = true;
        //     for (int j = 0; j < numPivots; j++) {
        //         Vector<Double> normalVector = internal.normalVectors.get(j);
        //         double nodeMin = internal.lowerBounds[j][i];
        //         double nodeMax = internal.upperBounds[j][i];
                
        //         double queryIntercept = computeQueryIntercept(distToPivots, normalVector);
        //         double delta = computeProjectionWidth(radius, normalVector);
        //         double queryMin = queryIntercept - delta;
        //         double queryMax = queryIntercept + delta;
                
        //         // 关键：检查子节点是否完全在查询投影范围内
        //         // 需要同时满足两个条件
        //         boolean minCondition = nodeMin >= queryMin;  // 子节点最小值在查询范围内
        //         boolean maxCondition = nodeMax <= queryMax;  // 子节点最大值在查询范围内
                
        //         if (!(minCondition && maxCondition)) {
        //             fullyContained = false;
        //             break;
        //         }
        //     }

        //     if (fullyContained) {
        //         // 包含整个子节点
        //         result.addAll(getAllData(child));
        //         continue;
        //     }
            // 既不能排除也不能直接包含，递归搜索
            result.addAll(doRangeSearch(child, query, radius, distance));
        }

        return result;
    }
    
    /**
     * 计算查询点在给定法向量上的投影（截距）
     * proj = Σ v_j × d(Q,P_j)
     */
    private static double computeQueryIntercept(double[] distToPivots, Vector<Double> normalVector) {
        double intercept = 0.0;
        for (int i = 0; i < distToPivots.length; i++) {
            intercept += distToPivots[i] * normalVector.get(i);
        }
        return intercept;
    }
    
    /**
     * 计算查询半径在给定法向量方向上的投影宽度
     * Δ = r × Σ|v_j|
     */
    private static double computeProjectionWidth(double radius, Vector<Double> normalVector) {
        double sumAbs = 0.0;
        for (Double weight : normalVector) {
            sumAbs += Math.abs(weight);
        }
        return radius * sumAbs;
    }
   
    /**
     * 递归获取节点下所有数据
     */
    private static List<MetricData> getAllData(Object node) {
        List<MetricData> all = new ArrayList<>();

        if (node instanceof PivotTable) {
            PivotTable pt = (PivotTable) node;
            all.addAll(pt.getPivots());
            all.addAll(pt.getDataPoints());
            return all;
        }

        CPTInternalNode internal = (CPTInternalNode) node;
        all.addAll(internal.pivots);
        for (Object child : internal.children) {
            all.addAll(getAllData(child));
        }
        return all;
    }
}