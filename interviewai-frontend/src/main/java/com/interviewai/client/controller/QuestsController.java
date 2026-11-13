package com.interviewai.client.controller;

import com.interviewai.client.api.ProgressGateway;
import com.interviewai.client.model.User;
import com.interviewai.client.util.SessionContext;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.util.Duration;

import com.interviewai.client.api.ApiException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

/**
 * Controller for the Simplified Quests System
 */
public class QuestsController {

    @FXML private Label dailyQuestProgressLabel;
    @FXML private ProgressBar dailyQuestProgressBar;
    @FXML private Button dailyClaimButton;
    @FXML private Label dailyTimerLabel;

    @FXML private Label monthlyQuestProgressLabel;
    @FXML private ProgressBar monthlyQuestProgressBar;
    @FXML private Button monthlyClaimButton;
    @FXML private Label monthlyTimerLabel;

    @FXML private Label streakLabel;
    @FXML private Label totalXpLabel;

    private final ProgressGateway progressGateway = new ProgressGateway();
    private int dailyTarget = 20;
    private int monthlyTarget = 12;
    private static final int DAILY_REWARD = 900;
    private static final int MONTHLY_REWARD = 30000;
    
    private LocalDate currentDay = LocalDate.now(java.time.Clock.systemUTC()); // Track current day for reset detection

    @FXML
    public void initialize() {
        loadUserData();
        startTimers();
    }

    private void loadUserData() {
        User currentUser = SessionContext.getCurrentUser();
        if (currentUser == null) return;
        try {
            int userId = currentUser.getId();
            totalXpLabel.setText(String.format("%,d Total XP", progressGateway.getTotalXPForUser(userId)));
            streakLabel.setText(progressGateway.calculateUserStreak(userId) + " Day Streak");
            for (ProgressGateway.Quest quest : progressGateway.getQuests()) {
                if ("DAILY".equals(quest.code())) {
                    dailyTarget = quest.targetValue(); updateDailyQuestUI(quest.currentValue(), quest.claimed());
                } else if ("MONTHLY".equals(quest.code())) {
                    monthlyTarget = quest.targetValue(); updateMonthlyQuestUI(quest.currentValue(), quest.claimed());
                }
            }
        } catch (ApiException e) {
            new javafx.scene.control.Alert(javafx.scene.control.Alert.AlertType.ERROR, e.getMessage()).showAndWait();
        }
    }

    private void updateDailyQuestUI(int current, boolean claimed) {
        double progress = Math.min(1.0, (double) current / dailyTarget);
        dailyQuestProgressBar.setProgress(progress);
        dailyQuestProgressLabel.setText(current + " / " + dailyTarget);

        if (claimed) {
            dailyClaimButton.setText("Claimed");
            dailyClaimButton.setDisable(true);
            if (!dailyClaimButton.getStyleClass().contains("claimed-button")) {
                dailyClaimButton.getStyleClass().add("claimed-button");
            }
        } else if (current >= dailyTarget) {
            dailyClaimButton.setText("Claim Reward");
            dailyClaimButton.setDisable(false);
            dailyClaimButton.getStyleClass().remove("claimed-button");
        } else {
            dailyClaimButton.setText("In Progress");
            dailyClaimButton.setDisable(true);
            dailyClaimButton.getStyleClass().remove("claimed-button");
        }
    }

    private void updateMonthlyQuestUI(int current, boolean claimed) {
        double progress = Math.min(1.0, (double) current / monthlyTarget);
        monthlyQuestProgressBar.setProgress(progress);
        monthlyQuestProgressLabel.setText(current + " / " + monthlyTarget);

        if (claimed) {
            monthlyClaimButton.setText("Claimed");
            monthlyClaimButton.setDisable(true);
            if (!monthlyClaimButton.getStyleClass().contains("claimed-button")) {
                monthlyClaimButton.getStyleClass().add("claimed-button");
            }
        } else if (current >= monthlyTarget) {
            monthlyClaimButton.setText("Claim Reward");
            monthlyClaimButton.setDisable(false);
            monthlyClaimButton.getStyleClass().remove("claimed-button");
        } else {
            monthlyClaimButton.setText("In Progress");
            monthlyClaimButton.setDisable(true);
            monthlyClaimButton.getStyleClass().remove("claimed-button");
        }
    }

    @FXML
    private void handleClaimDaily() {
        User currentUser = SessionContext.getCurrentUser();
        if (currentUser == null) return;

        try {
            progressGateway.claimDailyQuest(currentUser.getId(), DAILY_REWARD);
            // Play animation (simple visual feedback for now)
            dailyClaimButton.setText("Claimed!");
            loadUserData(); // Refresh UI
        } catch (ApiException e) {
            e.printStackTrace();
        }
    }

    @FXML
    private void handleClaimMonthly() {
        User currentUser = SessionContext.getCurrentUser();
        if (currentUser == null) return;

        try {
            progressGateway.claimMonthlyQuest(currentUser.getId(), MONTHLY_REWARD);
            // Play animation
            monthlyClaimButton.setText("Claimed!");
            loadUserData(); // Refresh UI
        } catch (ApiException e) {
            e.printStackTrace();
        }
    }

    private void startTimers() {
        Timeline timeline = new Timeline(new KeyFrame(Duration.seconds(1), e -> updateTimers()));
        timeline.setCycleCount(Timeline.INDEFINITE);
        timeline.play();
        updateTimers(); // Initial call
    }

    private void updateTimers() {
        LocalDateTime now = LocalDateTime.now(java.time.Clock.systemUTC());
        LocalDate today = LocalDate.now(java.time.Clock.systemUTC());
        
        // Check if day has changed (midnight passed)
        if (!today.equals(currentDay)) {
            System.out.println("Day changed! Resetting daily quest progress...");
            currentDay = today;
            
            // Refresh when the server-defined UTC quest period changes.
            User currentUser = SessionContext.getCurrentUser();
            if (currentUser != null) {
                try {
                    // The backend derives quest periods from UTC; no client-side reset is needed.
                    loadUserData(); // Reload UI to show 0/20
                } catch (ApiException e) {
                    e.printStackTrace();
                }
            }
        }
        
        // Daily Timer (Midnight)
        LocalDateTime midnight = today.plusDays(1).atStartOfDay();
        long secondsUntilMidnight = ChronoUnit.SECONDS.between(now, midnight);
        long hours = secondsUntilMidnight / 3600;
        long minutes = (secondsUntilMidnight % 3600) / 60;
        dailyTimerLabel.setText(String.format("Ends in %dh %dm", hours, minutes));

        // Monthly Timer (1st of next month)
        LocalDateTime nextMonth = today.withDayOfMonth(1).plusMonths(1).atStartOfDay();
        long daysUntilNextMonth = ChronoUnit.DAYS.between(now, nextMonth);
        monthlyTimerLabel.setText(String.format("Ends in %dd", daysUntilNextMonth));
    }
}
