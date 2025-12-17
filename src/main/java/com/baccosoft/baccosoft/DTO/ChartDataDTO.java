package com.baccosoft.baccosoft.DTO;

public class ChartDataDTO {
    private String label;
    private Number value;

    public ChartDataDTO() {}

    public ChartDataDTO(String label, Long value) {
        this.label = label;
        this.value = value;
    }

    public ChartDataDTO(String label, Double value) {
        this.label = label;
        this.value = value;
    }

    public ChartDataDTO(String label, Integer value) {
        this.label = label;
        this.value = value;
    }

    public String getLabel() {
        return label;
    }

    public void setLabel(String label) {
        this.label = label;
    }

    public Number getValue() {
        return value;
    }

    public void setValue(Number value) {
        this.value = value;
    }

    @Override
    public String toString() {
        return "ChartDataDTO{label='" + label + "', value=" + value + "}";
    }
}