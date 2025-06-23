package db;

public class StringData extends MetricData {
    private final String value;

    public StringData(String value) {
        this.value = value;
    }

    @Override
    public int getDimension() {
        return 1;  // 字符串数据按单维处理
    }

    @Override
    public Object getRawData() {
        return value;
    }

    public String getValue() {
        return value;
    }

    @Override
    public String toString() {
        return "StringData{\"" + value + "\"}";
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (!(obj instanceof StringData)) return false;
        return this.value.equals(((StringData) obj).value);
    }

    @Override
    public int hashCode() {
        return value.hashCode();
    }
}
