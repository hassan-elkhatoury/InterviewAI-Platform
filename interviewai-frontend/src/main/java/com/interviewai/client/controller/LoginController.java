package com.interviewai.client.controller;

import com.interviewai.client.api.CourseGateway;
import com.interviewai.client.api.OnboardingGateway;
import com.interviewai.client.api.UserGateway;
import com.interviewai.client.model.User;
import com.interviewai.client.service.AuthService;
import com.interviewai.client.util.Routes;
import com.interviewai.client.util.SceneNavigator;
import com.interviewai.client.util.SessionContext;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.stage.Stage;

public class LoginController {
    @FXML
    private TextField usernameField;
    @FXML
    private PasswordField passwordField;
    @FXML
    private Button loginButton;

    private final AuthService authService = new AuthService();

    @FXML
    private void onLogin(ActionEvent event) {
        if (loginButton.isDisabled()) return;
        String username = usernameField.getText();
        String password = passwordField.getText();
        if (username == null || username.isBlank() || password == null || password.isBlank()) {
            new Alert(Alert.AlertType.WARNING, "Username and password are required.").showAndWait();
            return;
        }
        SessionContext.clear();
        loginButton.setDisable(true);
        record LoginSession(User user, Integer[] lastUsed, boolean hasCourses) {}
        javafx.concurrent.Task<LoginSession> task = new javafx.concurrent.Task<>() {
            @Override protected LoginSession call() {
                UserGateway users = new UserGateway();
                User user = users.login(username, password);
                return new LoginSession(user, users.getLastUsedIds(user.getId()),
                    !user.hasRole("ROLE_ADMIN") && new CourseGateway().checkUserCourse(user.getId()));
            }
        };
        task.setOnSucceeded(e -> {
            loginButton.setDisable(false);
            LoginSession result = task.getValue();
            SessionContext.setCurrentUser(result.user());
            SessionContext.setActiveCourseId(result.lastUsed()[0]);
            SessionContext.setActiveChapterId(result.lastUsed()[1]);
            passwordField.clear();
            try {
                Stage stage = (Stage) usernameField.getScene().getWindow();
                String route = result.user().hasRole("ROLE_ADMIN") ? Routes.ADMIN : result.hasCourses() ? Routes.DASHBOARD : Routes.ONBOARDING;
                SceneNavigator.switchTo(stage, route, stage.getWidth() - 15, stage.getHeight() - 38);
            } catch (Exception failure) {
                new Alert(Alert.AlertType.ERROR, "Failed to open the next screen: " + failure.getMessage()).showAndWait();
            }
        });
        task.setOnFailed(e -> {
            loginButton.setDisable(false); SessionContext.clear();
            new Alert(Alert.AlertType.ERROR, task.getException().getMessage()).showAndWait();
        });
        Thread worker = new Thread(task, "sign-in"); worker.setDaemon(true); worker.start();
    }
    
    @FXML
    private void onOpenCreateAccount(ActionEvent event) {
        try {
            Stage stage = (Stage) usernameField.getScene().getWindow();
            SceneNavigator.switchTo(stage, Routes.CREATE_ACCOUNT, stage.getWidth()-15, stage.getHeight()-38);
        } catch (Exception ex) {
            Alert err = new Alert(Alert.AlertType.ERROR, "Failed to open Create Account: " + ex.getMessage());
            err.showAndWait();
        }
    }

    @FXML
    private void onForgotPassword(ActionEvent event) {
        Alert a = new Alert(Alert.AlertType.INFORMATION);
        a.setTitle("Password Help");
        a.setHeaderText("Forgot password");
        a.setContentText("Password reset isn't set up yet. Please contact your administrator.");
        a.showAndWait();
    }
}
