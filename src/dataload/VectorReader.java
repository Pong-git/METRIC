package dataload;

import db.VectorData;
import java.io.*;
import java.util.*;

/**
 * VectorReader 用于从文件中加载向量数据。
 * 数据文件第一行为元信息（维度，总数量），之后每行为向量。
 */
public class VectorReader {

    /**
     * 加载向量数据。
     *
     * @param path   文件路径
     * @param dim    希望读取的维度（<= 文件中维度）
     * @param count  希望读取的向量个数（<= 文件中数量）
     * @return List<VectorData>
     * @throws IOException 读取失败
     */
    public List<VectorData> load(String path, int dim, int count) throws IOException {
        try (BufferedReader br = new BufferedReader(new FileReader(path))) {
            int[] meta = parseMetaData(br.readLine());
            validateRequest(dim, count, meta[0], meta[1]);
            List<Double> values = readValues(br);
            return buildVectors(values, dim, count, meta[0]);
        }
    }

    // 解析第一行元信息
    private int[] parseMetaData(String line) {
        if (line == null || line.trim().isEmpty()) {
            throw new IllegalArgumentException("文件缺少元信息（第一行）");
        }
        String[] parts = line.trim().split("\\s+");
        if (parts.length < 2) {
            throw new IllegalArgumentException("元信息格式错误，应包含维度和数量");
        }
        return new int[]{
            Integer.parseInt(parts[0]),
            Integer.parseInt(parts[1])
        };
    }

    // 校验用户请求是否合法
    private void validateRequest(int reqDim, int reqCount, int totalDim, int totalCount) {
        if (reqDim <= 0 || reqDim > totalDim) {
            throw new IllegalArgumentException("非法维度: " + reqDim + "（最大支持维度: " + totalDim + "）");
        }
        if (reqCount <= 0 || reqCount > totalCount) {
            throw new IllegalArgumentException("非法数量: " + reqCount + "（最大支持数量: " + totalCount + "）");
        }
    }

    // 读取所有浮点数值
    private List<Double> readValues(BufferedReader br) throws IOException {
        List<Double> values = new ArrayList<>();
        String line;
        while ((line = br.readLine()) != null) {
            for (String part : line.trim().split("\\s+")) {
                if (!part.isEmpty()) {
                    values.add(Double.parseDouble(part));
                }
            }
        }
        return values;
    }

    // 构建向量对象（VectorData）
    private List<VectorData> buildVectors(List<Double> values, int dim, int count, int totalDim) {
        List<VectorData> vectors = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            double[] v = new double[dim];
            int start = i * totalDim;
            for (int j = 0; j < dim; j++) {
                v[j] = values.get(start + j);
            }
            vectors.add(new VectorData(v));
        }
        return vectors;
    }
}
