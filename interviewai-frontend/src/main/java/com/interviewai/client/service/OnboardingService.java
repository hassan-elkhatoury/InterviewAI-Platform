package com.interviewai.client.service;
import com.interviewai.client.api.OnboardingGateway;
import com.interviewai.client.model.*;
import com.interviewai.client.util.SessionContext;
public class OnboardingService {
    public void saveOnboardingDataAsync(OnboardingData data,MultiStageAIService.ProgressCallback progress,MultiStageAIService.CompletionCallback completion) {
        SessionContext.setOnboardingData(data);
        new MultiStageAIService().generateCourseAsync(data,progress,completion);
    }
    public OnboardingData getOnboardingData(int uid) { return new OnboardingGateway().getByUserId(uid); }
    public boolean hasCompletedOnboarding(int uid) { return new OnboardingGateway().hasCompletedOnboarding(uid); }
}
