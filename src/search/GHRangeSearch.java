package search;

import db.MetricData;
import distance_function.MetricDistance;
import index.GeneralHyperPlaneTree;
import index.PivotTable;


import java.util.ArrayList;
import java.util.List;

/**
 * GHRangeSearch 实现 General Hyperplane Tree 的范围查询功能。
 */
public class GHRangeSearch {

    /**
     * 执行 GH 树的范围查询。
     *
     * @param node     当前查询节点（GHInternalNode 或 PivotTable）
     * @param query    查询点
     * @param radius   查询半径
     * @param distance 距离函数（内部包含距离计算计数器）
     * @return 查询结果封装类，包括命中对象列表和距离计算次数
     */
    public static SearchResult rangeSearch(Object node, MetricData query, double radius, MetricDistance distance) {

        // 执行递归查询
        List<MetricData> results = doRangeSearch(node, query, radius, distance);

        // 返回查询结果 + 累积距离计算次数
        return new SearchResult(results, distance.getCount());
    }

    /**
     * 内部递归搜索过程。
     *
     * @param node     当前树节点
     * @param query    查询点
     * @param radius   查询半径
     * @param distance 距离函数
     * @return 命中的数据点列表
     */
    private static List<MetricData> doRangeSearch(Object node, MetricData query, double radius, MetricDistance distance) {

        List<MetricData> result = new ArrayList<>();

        if (node instanceof PivotTable) {
            // 叶子节点：使用 PivotTable 查询模块
            return PTRangeSearch.rangeSearch((PivotTable) node, query, radius, distance).results;
        }

        // 内部节点：进行剪枝判断
        GeneralHyperPlaneTree.GHInternalNode internal = (GeneralHyperPlaneTree.GHInternalNode) node;

        double d1 = distance.getDistance(query, internal.pivot1);
        double d2 = distance.getDistance(query, internal.pivot2);

        // 支撑点自身是否在查询范围内
        if (d1 <= radius) result.add(internal.pivot1);
        if (d2 <= radius) result.add(internal.pivot2);

        // 根据三角不等式判断是否递归左右子树
        if (d1 - d2 <= 2 * radius && internal.left != null) {
            result.addAll(doRangeSearch(internal.left, query, radius, distance));
        }

        if (d2 - d1 <= 2 * radius && internal.right != null) {
            result.addAll(doRangeSearch(internal.right, query, radius, distance));
        }

        return result;
    }

}
