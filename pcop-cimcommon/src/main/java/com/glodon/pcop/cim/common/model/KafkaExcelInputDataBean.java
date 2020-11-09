package com.glodon.pcop.cim.common.model;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Map;

public class KafkaExcelInputDataBean {
    private Long taskId;
    private String objectName;
    private Boolean isUpdate;
    private String tenantId;
    private Map<String, Object> data;

    public Long getTaskId() {
        return taskId;
    }

    public void setTaskId(Long taskId) {
        this.taskId = taskId;
    }

    public String getObjectName() {
        return objectName;
    }

    public void setObjectName(String objectName) {
        this.objectName = objectName;
    }

    @JsonProperty(value = "isUpdate")
    public Boolean getUpdate() {
        return isUpdate;
    }

    @JsonProperty(value = "isUpdate")
    public void setUpdate(Boolean update) {
        isUpdate = update;
    }

    public String getTenantId() {
        return tenantId;
    }

    public void setTenantId(String tenantId) {
        this.tenantId = tenantId;
    }

    public Map<String, Object> getData() {
        return data;
    }

    public void setData(Map<String, Object> data) {
        this.data = data;
    }

    @Override
    public String toString() {
        return "KafkaGisDataBean{" +
                "taskId=" + taskId +
                ", objectName='" + objectName + '\'' +
                ", isUpdate=" + isUpdate +
                ", tenantId='" + tenantId + '\'' +
                ", data=" + data +
                '}';
    }
}
