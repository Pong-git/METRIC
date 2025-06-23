package distance_function;

import db.MetricData;
import db.StringData;

/**
 * 计算两个等长字符串的 Hamming 距离。
 */
public class HammingDistance extends MetricDistance {

    @Override
    protected double compute(MetricData a, MetricData b) {
        String s1 = ((StringData) a).getValue();
        String s2 = ((StringData) b).getValue();

        if (s1.length() != s2.length()) {
            throw new IllegalArgumentException("Hamming 距离要求两个字符串长度一致");
        }

        int dist = 0;
        for (int i = 0; i < s1.length(); i++) {
            if (s1.charAt(i) != s2.charAt(i)) {
                dist++;
            }
        }
        return dist;
    }
}
