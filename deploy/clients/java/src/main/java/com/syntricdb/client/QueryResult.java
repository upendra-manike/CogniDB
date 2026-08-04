package com.syntricdb.client;

import java.util.List;
import java.util.Map;

public class QueryResult {
    private boolean success;
    private String message;
    private double executionTimeMs;
    private int rowCount;
    private String planStrategy;
    private String planDescription;
    private Double estimatedCost;
    private List<Map<String, Object>> data;

    public boolean isSuccess() { return success; }
    public void setSuccess(boolean success) { this.success = success; }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }

    public double getExecutionTimeMs() { return executionTimeMs; }
    public void setExecutionTimeMs(double executionTimeMs) { this.executionTimeMs = executionTimeMs; }

    public int getRowCount() { return rowCount; }
    public void setRowCount(int rowCount) { this.rowCount = rowCount; }

    public String getPlanStrategy() { return planStrategy; }
    public void setPlanStrategy(String planStrategy) { this.planStrategy = planStrategy; }

    public String getPlanDescription() { return planDescription; }
    public void setPlanDescription(String planDescription) { this.planDescription = planDescription; }

    public Double getEstimatedCost() { return estimatedCost; }
    public void setEstimatedCost(Double estimatedCost) { this.estimatedCost = estimatedCost; }

    public List<Map<String, Object>> getData() { return data; }
    public void setData(List<Map<String, Object>> data) { this.data = data; }
}
