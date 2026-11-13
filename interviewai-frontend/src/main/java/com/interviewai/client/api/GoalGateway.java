package com.interviewai.client.api;

import static com.interviewai.client.api.ApiClient.body;

import com.fasterxml.jackson.core.type.TypeReference;
import com.interviewai.client.model.UserGoal;
import java.util.*;

public class GoalGateway {
  private final ApiClient api = ApiClient.getInstance();

  public List<UserGoal> getUserGoals(int uid) {
    return api.get("/users/me/goals", new TypeReference<List<UserGoal>>() {});
  }

  public List<UserGoal> getUserGoalsWithProgress(int uid) {
    return getUserGoals(uid);
  }

  private Object request(UserGoal g) {
    return body(
        "goalName",
        g.getGoalName(),
        "goalType",
        g.getGoalType(),
        "targetValue",
        g.getTargetValue());
  }

  public void createGoal(UserGoal g) {
    g.setId(api.post("/users/me/goals", request(g), Integer.class));
  }

  public void updateGoal(UserGoal g) {
    api.put("/users/me/goals/" + g.getId(), request(g), Integer.class);
  }

  public void deleteGoal(int id) {
    api.delete("/users/me/goals/" + id);
  }
}
