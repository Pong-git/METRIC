// src/search/SearchResult.java
package search;

import db.MetricData;
import java.util.List;

/**
 * 通用的查询结果类。
 */
public class SearchResult {
    public final List<MetricData> results;
    public final long distanceCount;

    public SearchResult(List<MetricData> results, long distanceCount) {
        this.results = results;
        this.distanceCount = distanceCount;
    }
}
