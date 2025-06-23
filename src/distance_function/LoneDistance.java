package distance_function;

import db.MetricData;
import db.VectorData;
import java.util.Arrays;

/**
 * 若两个对象完全相同，返回 0；
 * 否则，返回 1。
 */
public class LoneDistance extends MetricDistance {

    @Override
    protected double compute(MetricData a, MetricData b) {
        // 暂仅支持 VectorData，后续可扩展到 StringData 等
        if (!(a instanceof VectorData) || !(b instanceof VectorData)) {
            throw new IllegalArgumentException("LoneDistance 仅支持 VectorData 类型");
        }

        double[] va = ((VectorData) a).getVector();
        double[] vb = ((VectorData) b).getVector();

        // 若完全相等则距离为 0，否则为 1
        return Arrays.equals(va, vb) ? 0.0 : 1.0;
    }
}
