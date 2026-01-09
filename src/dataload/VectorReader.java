package dataload;

import db.VectorData;
import java.io.*;
import java.util.*;

/**
 * VectorReader 用于从文件中加载向量数据。
 * 数据文件第一行为信息（维度，总数量），之后为向量数据（可能跨越多行）。
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
            // 读取并解析信息
            int[] meta = parseMetaData(br.readLine());
            int totalDim = meta[0];
            int totalCount = meta[1];
            
            // 校验请求参数
            validateRequest(dim, count, totalDim, totalCount);
            
            // 直接读取所需数量的向量
            return readVectors(br, dim, count, totalDim);
        }
    }

    // 解析第一行元信息
    private int[] parseMetaData(String line) {
        if (line == null || line.trim().isEmpty()) {
            throw new IllegalArgumentException("文件缺少首行信息（维度 数量）");
        }
        String[] parts = line.trim().split("\\s+");
        if (parts.length < 2) {
            throw new IllegalArgumentException("信息格式错误，应包含维度和数量");
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

    // 改进后的向量读取方法：支持跨行向量数据
    private List<VectorData> readVectors(BufferedReader br, int dim, int count, int totalDim) throws IOException {
        List<VectorData> vectors = new ArrayList<>(count);
        int vectorsRead = 0;
        List<Double> currentVectorBuffer = new ArrayList<>(totalDim); // 当前向量缓冲区
        
        String line;
        while (vectorsRead < count && (line = br.readLine()) != null) {
            String[] parts = line.trim().split("\\s+");
            
            // 将当前行的所有数值添加到缓冲区
            for (String part : parts) {
                if (!part.isEmpty()) {
                    currentVectorBuffer.add(Double.parseDouble(part));
                }
            }
            
            // 检查缓冲区是否收集了足够一个完整向量的数据
            while (currentVectorBuffer.size() >= totalDim && vectorsRead < count) {
                // 提取前dim个维度构建向量
                double[] vector = new double[dim];
                for (int i = 0; i < dim; i++) {
                    vector[i] = currentVectorBuffer.get(i);
                }
                
                vectors.add(new VectorData(vector));
                vectorsRead++;
                
                // 移除已处理的向量数据，保留可能的多余数据用于下一个向量
                if (currentVectorBuffer.size() == totalDim) {
                    currentVectorBuffer.clear();
                } else {
                    // 如果缓冲区有超过一个向量的数据，移除已处理的部分
                    for (int i = 0; i < totalDim; i++) {
                        currentVectorBuffer.remove(0);
                    }
                }
            }
        }
        
        // 检查是否成功读取了足够数量的向量
        if (vectorsRead < count) {
            throw new IOException("文件中的向量数量不足，期望: " + count + ", 实际: " + vectorsRead);
        }
        
        return vectors;
    }
}