package search;

import db.MetricData;
import distance_function.MetricDistance;
import index.PivotTable;
import index.VantagePointTree;

import java.util.ArrayList;
import java.util.List;

/**
 * 实现 Vantage Point Tree 的范围查询功能。
 */
public class VPRangeSearch {

    /**
     * 执行 VPT 的范围查询。
     *
     * @param node     当前节点（VPTInternalNode 或 PivotTable）
     * @param query    查询点
     * @param radius   查询半径
     * @param distance 距离函数（包含计数器）
     * @return 查询结果封装 SearchResult
     */
    public static SearchResult rangeSearch(Object node, MetricData query, double radius, MetricDistance distance) {

        List<MetricData> result = doRangeSearch(node, query, radius, distance);
        return new SearchResult(result, distance.getCount());
    }

    /**
     * 实际递归的查询过程。
     */
    private static List<MetricData> doRangeSearch(Object node, MetricData query, double radius, MetricDistance distance) {

        List<MetricData> result = new ArrayList<>();

        // 叶子节点直接交给 PTRangeSearch 处理
        if (node instanceof PivotTable) {
            return PTRangeSearch.rangeSearch((PivotTable) node, query, radius, distance).results;
        }

        // 内部节点处理
        VantagePointTree.VPTInternalNode internal = (VantagePointTree.VPTInternalNode) node;

        double d_vq = distance.getDistance(query, internal.pivot);
        if (d_vq <= radius) {
            result.add(internal.pivot);
        }

        // 球内全部包含
        if (d_vq + internal.splitRadius <= radius && internal.left != null) {
            result.addAll(getAllData(internal.left));
        }
        // 球内不能排除
        else if (d_vq <= internal.splitRadius + radius && internal.left != null) {
            result.addAll(doRangeSearch(internal.left, query, radius, distance));
        }

        // 球外不能排除
        if (d_vq + radius > internal.splitRadius && internal.right != null) {
            result.addAll(doRangeSearch(internal.right, query, radius, distance));
        }

        return result;
    }

    /**
     * 包含时获取子树所有数据（包括支撑点和数据点）。
     */
    private static List<MetricData> getAllData(Object node) {
        List<MetricData> all = new ArrayList<>();

        if (node instanceof PivotTable) {
            PivotTable pt = (PivotTable) node;
            all.addAll(pt.getPivots());
            all.addAll(pt.getDataPoints());
        } else if (node instanceof VantagePointTree.VPTInternalNode) {
            VantagePointTree.VPTInternalNode internal = (VantagePointTree.VPTInternalNode) node;
            all.add(internal.pivot);
            if (internal.left != null) all.addAll(getAllData(internal.left));
            if (internal.right != null) all.addAll(getAllData(internal.right));
        }

        return all;
    }
}
