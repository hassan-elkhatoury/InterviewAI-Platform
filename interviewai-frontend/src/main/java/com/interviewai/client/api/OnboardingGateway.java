package com.interviewai.client.api;

import static com.interviewai.client.api.ApiClient.body;

import com.interviewai.client.model.OnboardingData;

public class OnboardingGateway {
  private final ApiClient api = ApiClient.getInstance();

  public static Object preferences(OnboardingData d) {
    return body(
        "interviewType",
        d.getInterviewType(),
        "language",
        d.getLanguage(),
        "timeline",
        d.getTimeline(),
        "context",
        d.getContext(),
        "description",
        d.getDescription());
  }

  public boolean saveOnboardingData(OnboardingData d) {
    api.put("/users/me/onboarding", preferences(d), Void.class);
    return true;
  }

  public OnboardingData getByUserId(int uid) {
    OnboardingData d = api.get("/users/me/onboarding", OnboardingData.class);
    if (d != null) d.setUserId(uid);
    return d;
  }

  public boolean hasCompletedOnboarding(int uid) {
    return getByUserId(uid) != null;
  }
}
