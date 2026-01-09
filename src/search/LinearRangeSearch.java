package search;

import db.MetricData;
import distance_function.MetricDistance;

import java.util.ArrayList;
import java.util.List;

/**
 * 线性扫描范围查询实现。
 * 通过遍历所有数据点并直接计算距离来进行范围查询。
 */
public class LinearRangeSearch {

    /**
     * 线性扫描范围查询。
     * 遍历所有数据点，直接计算查询点与每个数据点的距离。
     *
     * @param dataset 数据集
     * @param query 查询点
     * @param radius 查询半径
     * @param distance 距离函数（带计数器）
     * @return 查询结果及距离计算次数封装
     */
    public static SearchResult rangeSearch(List<MetricData> dataset, MetricData query, double radius, MetricDistance distance) {
        List<MetricData> result = new ArrayList<>();
        
        // 记录初始距离计算次数
        long initialCount = distance.getCount();
        
        // 线性扫描：遍历数据集中的每个点
        for (MetricData point : dataset) {
            // 直接计算查询点与当前点的距离
            double dist = distance.getDistance(query, point);
            
            // 如果距离小于等于半径，加入结果集
            if (dist <= radius) {
                result.add(point);
            }
        }
        
        // 计算本次查询新增的距离计算次数
        long newDistanceCount = distance.getCount() - initialCount;
        
        return new SearchResult(result, newDistanceCount);
    }
}