package search;

import db.MetricData;
import distance_function.MetricDistance;

import java.util.*;

/**
 * 线性扫描k近邻查询实现。
 * 通过遍历所有数据点并维护一个大小为k的优先队列来找到最近的k个点。
 */
public class LinearKnnSearch {

    /**
     * 线性扫描k近邻查询。
     * 遍历所有数据点，维护距离最小的k个点。
     *
     * @param dataset 数据集
     * @param query 查询点
     * @param k 近邻数量
     * @param distance 距离函数（带计数器）
     * @return 查询结果及距离计算次数封装
     */
    public static SearchResult knnSearch(List<MetricData> dataset, MetricData query, int k, MetricDistance distance) {
        // 参数校验
        if (k <= 0) {
            throw new IllegalArgumentException("k必须大于0");
        }
        if (dataset == null || dataset.isEmpty()) {
            return new SearchResult(new ArrayList<>(), 0);
        }
        
        // 如果k大于数据集大小，返回所有点
        k = Math.min(k, dataset.size());
        
        // 记录初始距离计算次数
        long initialCount = distance.getCount();
        
        // 使用最大堆来维护最近的k个点
        PriorityQueue<Neighbor> maxHeap = new PriorityQueue<>(k, Collections.reverseOrder());
        
        // 线性扫描：遍历数据集中的每个点
        for (MetricData point : dataset) {
            // 直接计算查询点与当前点的距离
            double dist = distance.getDistance(query, point);
            
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
        
        // 将堆中的点按距离从小到大排序
        List<MetricData> result = new ArrayList<>(k);
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
        
        @Override
        public String toString() {
            return String.format("Neighbor{distance=%.4f, point=%s}", distance, point);
        }
    }
}