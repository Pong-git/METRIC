package search;

import db.MetricData;
import distance_function.MetricDistance;

import java.util.*;

/**
 * 线性扫描距离受限k近邻查询实现。
 * 找到在指定半径范围内的k个最近邻点。
 */
public class LinearDknnSearch {

    /**
     * 线性扫描距离受限k近邻查询。
     * 使用最大堆维护半径内的k个最近点，避免全排序。
     *
     * @param dataset 数据集
     * @param query 查询点
     * @param k 近邻数量
     * @param radius 查询半径
     * @param distance 距离函数（带计数器）
     * @return 查询结果及距离计算次数封装
     */
    public static SearchResult dknnSearch(List<MetricData> dataset, MetricData query, int k, 
                                        double radius, MetricDistance distance) {
        // 参数校验
        if (k <= 0) {
            throw new IllegalArgumentException("k必须大于0");
        }
        if (radius < 0) {
            throw new IllegalArgumentException("半径必须大于等于0");
        }
        if (dataset == null || dataset.isEmpty()) {
            return new SearchResult(new ArrayList<>(), 0);
        }
        
        // 记录初始距离计算次数
        long initialCount = distance.getCount();
        
        // 使用最大堆维护半径内的k个最近点
        PriorityQueue<Neighbor> maxHeap = new PriorityQueue<>(k, Collections.reverseOrder());
        
        // 遍历所有数据点
        for (MetricData point : dataset) {
            double dist = distance.getDistance(query, point);
            
            // 只考虑在半径范围内的点
            if (dist <= radius) {
                Neighbor neighbor = new Neighbor(point, dist);
                
                if (maxHeap.size() < k) {
                    // 堆未满，直接加入
                    maxHeap.offer(neighbor);
                } else {
                    // 堆已满，如果当前点比堆顶更近，替换堆顶
                    if (dist < maxHeap.peek().distance) {
                        maxHeap.poll();
                        maxHeap.offer(neighbor);
                    }
                }
            }
        }
        
        // 将堆中的点按距离从小到大排序
        List<MetricData> result = new ArrayList<>(maxHeap.size());
        while (!maxHeap.isEmpty()) {
            result.add(maxHeap.poll().point);
        }
        Collections.reverse(result); // 反转得到距离从小到大的顺序
        
        // 计算本次查询新增的距离计算次数
        long newDistanceCount = distance.getCount() - initialCount;
        
        return new SearchResult(result, newDistanceCount);
    }
    
    /**
     * 内部类，用于存储数据点及其距离
     */
    private static class Neighbor implements Comparable<Neighbor> {
        final MetricData point;
        final double distance;
        
        Neighbor(MetricData point, double distance) {
            this.point = point;
            this.distance = distance;
        }
        
        @Override
        public int compareTo(Neighbor other) {
            return Double.compare(this.distance, other.distance);
        }
    }
}