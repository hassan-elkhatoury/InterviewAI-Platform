package com.interviewai.client.controller;

import java.io.File;

import com.interviewai.client.model.OnboardingData;
import com.interviewai.client.model.User;
import com.interviewai.client.service.OnboardingService;
import com.interviewai.client.util.Routes;
import com.interviewai.client.util.SceneNavigator;
import com.interviewai.client.util.SessionContext;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

/**
 * Handles the multi-step onboarding flow with Duolingo-inspired UI.
 * Collects user preferences: interview type, language, timeline, and context.
 */
public class OnboardingController {

    // Progress indicator
    @FXML private HBox progressContainer;
    
    // Step containers
    @FXML private VBox step1Container;
    @FXML private VBox step2Container;
    @FXML private VBox step3Container;
    @FXML private VBox step4Container;
    
    // Step 1: Interview Type Options
    @FXML private HBox optionJobInterview;
    @FXML private HBox optionVisaInterview;
    @FXML private HBox optionInternshipInterview;
    @FXML private HBox optionUniversityInterview;
    
    // Step 2: Language Options
    @FXML private HBox optionEnglish;
    @FXML private HBox optionFrench;
    @FXML private HBox optionArabic;
    @FXML private HBox optionSpanish;
    
    // Step 3: Timeline Options
    @FXML private HBox optionTomorrow;
    @FXML private HBox optionThisWeek;
    @FXML private HBox optionLater;
    
    // Step 4: Context
    @FXML private Label step4Question;
    @FXML private Label step4Subtitle;
    @FXML private TextField contextField;
    @FXML private TextField descriptionField;
    @FXML private VBox uploadArea;
    @FXML private Label uploadedFileName;
    
    // Navigation buttons
    @FXML private Button backButton;
    @FXML private Button nextButton;
    
    // State management
    private int currentStep = 1;
    private static final int TOTAL_STEPS = 4;
    
    private String selectedInterviewType;
    private String selectedLanguage;
    private String selectedTimeline;
    private String contextInfo;
    private String description;

    private boolean uploading;
    @FXML private Button uploadCvButton;
    @FXML private Label resumeProfileLabel;
    private java.util.concurrent.CompletableFuture<com.interviewai.client.api.ResumeProfile> resumeUpload;
    
    private HBox selectedCard; // Track currently selected option card
    
    @FXML
    public void initialize() {
        updateProgressDots();
        updateNavigationButtons();
    }
   
    @FXML
    public void updateProgressDots(){
        
        progressContainer.getChildren().clear();
        for (int i=1; i<=TOTAL_STEPS; i++){
            Region dot= new Region();

            if(i<=currentStep){
                dot.getStyleClass().add("progress-dot-active");
            } else{
                dot.getStyleClass().add("progress-dot");
            }

            progressContainer.getChildren().add(dot);
        } 
    }

    public void onNext(ActionEvent event){
            if (currentStep < TOTAL_STEPS) {
            currentStep++;
            updateStepView();
            updateNavigationButtons();
            updateProgressDots();

            if (currentStep == 4) {
                updateStep4Content();
            }

            

        } else {
            // Final step - complete onboarding
            completeOnboarding(event);
        }
        
        
    }
    public void onBack(ActionEvent event){
        if(currentStep > 1){
            currentStep --;
            updateStepView();
            updateNavigationButtons();
            updateProgressDots();
            return;
        }
        // If launched from Dashboard and at step 1, go back to Dashboard
        if (SessionContext.isOnboardingFromDashboard()) {
            try {
                Stage stage = (Stage) backButton.getScene().getWindow();
                SceneNavigator.switchTo(stage, Routes.DASHBOARD, stage.getWidth()-15, stage.getHeight()-38);
            } catch (Exception e) {
                Alert alert = new Alert(Alert.AlertType.ERROR);
                alert.setTitle("Navigation Error");
                alert.setContentText("Failed to navigate back to dashboard: " + e.getMessage());
                alert.showAndWait();
            } finally {
                SessionContext.setOnboardingFromDashboard(false);
            }
        }
    }

    public void updateStepView(){
        step1Container.setVisible(false);
        step1Container.setManaged(false);
        step2Container.setVisible(false);
        step2Container.setManaged(false);
        step3Container.setVisible(false);
        step3Container.setManaged(false);
        step4Container.setVisible(false);
        step4Container.setManaged(false);

        switch (currentStep){
            case 1 :
                step1Container.setVisible(true);
                step1Container.setManaged(true);
                break;

            case 2 :
                step2Container.setVisible(true);
                step2Container.setManaged(true);
                break;

            case 3 :
                step3Container.setVisible(true);
                step3Container.setManaged(true);
                break;

            case 4 :
                step4Container.setVisible(true);
                step4Container.setManaged(true);
                break;
            
        }
    }


    public void onSelectInterviewType(MouseEvent event){

        HBox source = (HBox) event.getSource();
       
        selectCard(source);

        if (source == optionJobInterview) {
            selectedInterviewType = "JOB";
        } else if (source == optionVisaInterview) {
            selectedInterviewType = "VISA";
        } else if (source == optionInternshipInterview) {
            selectedInterviewType = "INTERNSHIP";
        } else if (source == optionUniversityInterview) {
            selectedInterviewType = "UNIVERSITY";
        }

        updateNavigationButtons();

    }


    
    
    public  void onSelectLanguage(MouseEvent event) {

        HBox source = (HBox) event.getSource();
        selectCard(source);
        
        if (source == optionEnglish) {
            selectedLanguage = "ENGLISH";
        } else if (source == optionFrench) {
            selectedLanguage = "FRENCH";
        } else if (source == optionArabic) {
            selectedLanguage = "ARABIC";
        } else if (source == optionSpanish) {
            selectedLanguage = "SPANISH";
        }
        
        updateNavigationButtons();
    }
    
    
    public void onSelectTimeline(MouseEvent event) {
        HBox source = (HBox) event.getSource();
        selectCard(source);
        
        if (source == optionTomorrow) {
            selectedTimeline = "TOMORROW";
        } else if (source == optionThisWeek) {
            selectedTimeline = "THIS_WEEK";
        } else if (source == optionLater) {
            selectedTimeline = "LATER";
        }
        
        updateNavigationButtons();
    }
    
    
    @FXML
    public void onUploadCV() {
        if (uploading) return;
        FileChooser chooser = new FileChooser(); chooser.setTitle("Upload PDF CV (up to 5 MB)");
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("PDF files", "*.pdf"));
        File file = chooser.showOpenDialog(uploadArea.getScene().getWindow());
        if (file == null) return;
        uploading = true; uploadCvButton.setDisable(true); nextButton.setDisable(true); backButton.setDisable(true);
        uploadedFileName.setVisible(true); uploadedFileName.setText("Reading your CV...");
        User uploadingUser = SessionContext.getCurrentUser();
        resumeUpload = com.interviewai.client.api.ApiClient.getInstance().uploadResume(file.toPath());
        resumeUpload.whenComplete((profile, failure) -> javafx.application.Platform.runLater(() -> {
            uploading = false; uploadCvButton.setDisable(false); backButton.setDisable(false); updateNavigationButtons();
            if (SessionContext.getCurrentUser() != uploadingUser) return;
            if (failure != null) {
                Throwable cause = failure; while (cause.getCause() != null) cause = cause.getCause();
                uploadedFileName.setText("Upload failed. Your previous CV remains available.");
                new Alert(Alert.AlertType.ERROR, cause.getMessage()).showAndWait(); return;
            }
            uploadedFileName.setText(file.getName());
            resumeProfileLabel.setText("Detected skills: " + (profile.skills().isEmpty() ? "Not specified" : String.join(", ", profile.skills()))
                + "\nSeniority: " + profile.seniority() + "\nExperience: " + profile.experience()
                + "\nPersonalized tracks: " + String.join(" • ", profile.tracks()));
            resumeProfileLabel.setVisible(true); resumeProfileLabel.setManaged(true);
        }));
    }

    public void selectCard(HBox card){

            if (selectedCard != null) {
                selectedCard.getStyleClass().remove("option-card-selected");
            }
            selectedCard = card;
            card.getStyleClass().add("option-card-selected");
    }

    private void updateNavigationButtons() {
     // Show Back on step > 1; also show on step 1 if launched from Dashboard
     boolean showBack = currentStep > 1 || SessionContext.isOnboardingFromDashboard();
     backButton.setVisible(showBack);

     boolean canProceed = false;
     switch (currentStep) {
            case 1:
                canProceed = selectedInterviewType != null;
                break;
            case 2:
                canProceed = selectedLanguage != null;
                break;
            case 3:
                canProceed = selectedTimeline != null;
                break;
            case 4:
                canProceed = contextField.getText() != null && !contextField.getText().isBlank();
                break;
        }
        nextButton.setDisable(uploading || !canProceed);
        
        // Change button text on last step
        nextButton.setText(currentStep == TOTAL_STEPS ? "Start Practicing" : "Next");
    }



    
    private void updateStep4Content() {
        if ("JOB".equals(selectedInterviewType) || "INTERNSHIP".equals(selectedInterviewType)) {
            step4Question.setText("Which industry or position are you applying for?");
            contextField.setPromptText("e.g., Software Developer, Marketing Manager, Designer");
        } else if ("UNIVERSITY".equals(selectedInterviewType)) {
            step4Question.setText("Which program or major are you applying to?");
            contextField.setPromptText("e.g., Computer Science, Business Administration, Medicine");
        } else if ("VISA".equals(selectedInterviewType)) {
            step4Question.setText("What type of visa are you applying for?");
            contextField.setPromptText("e.g., Work Visa, Student Visa, Tourist Visa");
        }
        
        // Add listener to enable next button when text is entered
        contextField.textProperty().addListener((obs, old, newVal) -> updateNavigationButtons());
    }


    private void completeOnboarding(ActionEvent event) {
        contextInfo = contextField.getText();
        description = descriptionField.getText();

        
        // Persist onboarding data via OnboardingService
        User currentUser = SessionContext.getCurrentUser();
        if (currentUser != null) {
            OnboardingData data = new OnboardingData();
            data.setUserId(currentUser.getId());
            data.setInterviewType(selectedInterviewType);
            data.setLanguage(selectedLanguage);
            data.setTimeline(selectedTimeline);
            data.setContext(contextInfo);
            data.setDescription(description);

            
            try {
                Stage stage = (Stage) nextButton.getScene().getWindow();
                // Navigate to waiting page
                SceneNavigator.switchTo(stage, Routes.ONBOARDINGWAITING, stage.getWidth()-15, stage.getHeight()-38);
                
                // Start multi-stage AI generation in background
                OnboardingService service = new OnboardingService();
                service.saveOnboardingDataAsync(
                    data,
                    // Progress callback - update the waiting page UI
                    (message, percent) -> {
                        System.out.println(String.format("Progress: %d%% - %s", percent, message));
                        // Update the waiting page controller
                        OnboardingWaitingController waitingController = OnboardingWaitingController.getInstance();
                        if (waitingController != null) {
                            waitingController.updateProgress(message, percent);
                        }
                    },
                    // Completion callback - navigate to dashboard on success, show error on failure
                    (success, errorMessage) -> {
                        javafx.application.Platform.runLater(() -> {
                            try {
                                if (!success) {
                                    SceneNavigator.switchTo(stage, Routes.ONBOARDING, stage.getWidth()-15, stage.getHeight()-38);
                                    new Alert(Alert.AlertType.ERROR, errorMessage == null ? "Course generation failed. Please try again." : errorMessage).showAndWait();
                                    return;
                                }
                                SceneNavigator.switchTo(stage, Routes.DASHBOARD, stage.getWidth()-15, stage.getHeight()-38);
                                SessionContext.setOnboardingFromDashboard(false);
                            } catch (Exception e) {
                                System.err.println("Failed to navigate to dashboard: " + e.getMessage());
                            }
                        });
                    }
                );
                
            } catch (Exception e) {
                Alert alert = new Alert(Alert.AlertType.ERROR);
                alert.setTitle("Navigation Error");
                alert.setContentText("Failed to navigate to waiting page: " + e.getMessage());
                alert.showAndWait();
            }
        }
        
    }
}
