package com.interviewai.client.service;
import com.interviewai.client.api.ApiClient;
import com.fasterxml.jackson.core.type.TypeReference;
import java.util.*;
public class AnalyticsService {
    public Map<String,Object> getDashboardMetrics() { return ApiClient.getInstance().get("/admin/analytics",new TypeReference<Map<String,Object>>(){}); }
}
