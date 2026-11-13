package com.interviewai.client.service;
import com.interviewai.client.api.*;
import com.interviewai.client.model.OnboardingData;
import javafx.application.Platform;
public class MultiStageAIService {
    public interface ProgressCallback { void onProgress(String message,int percent); }
    public interface CompletionCallback { void onComplete(boolean success,String errorMessage); }
    public void generateCourseAsync(OnboardingData data,ProgressCallback progress,CompletionCallback completion) {
        Thread worker=new Thread(() -> {
            try {
                Platform.runLater(() -> progress.onProgress("Generating your preparation courses...",10));

                ApiClient.getInstance().post("/courses/generate",ApiClient.body("preferences",OnboardingGateway.preferences(data),"cvText",null),Object.class);
                Platform.runLater(() -> { progress.onProgress("Your courses are ready",100); completion.onComplete(true,null); });
            } catch(Exception e) { Platform.runLater(() -> completion.onComplete(false,e.getMessage())); }
        },"course-generation");
        worker.setDaemon(true); worker.start();
    }
}
