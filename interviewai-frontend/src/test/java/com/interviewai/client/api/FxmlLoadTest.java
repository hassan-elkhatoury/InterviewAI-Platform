package com.interviewai.client.api;

import static org.junit.jupiter.api.Assertions.*;

import java.util.concurrent.*;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import org.junit.jupiter.api.*;

class FxmlLoadTest {
  @BeforeAll
  static void toolkit() throws Exception {
    CountDownLatch ready = new CountDownLatch(1);
    Platform.startup(
        () -> {
          Platform.setImplicitExit(false);
          ready.countDown();
        });
    assertTrue(ready.await(20, TimeUnit.SECONDS));
  }

  @AfterAll
  static void stopToolkit() {
    Platform.exit();
  }

  @Test
  void signInAndRegistrationViewsLoadAfterPackageIsolation() throws Exception {
    FutureTask<Void> task =
        new FutureTask<>(
            () -> {
              for (String view :
                  new String[] {
                    "LoginView.fxml", "CreateAccountView.fxml", "OnboardingView.fxml"
                  }) {
                FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/" + view));
                Parent root = loader.load();
                assertNotNull(root);
                assertNotNull(loader.getController());
              }
              return null;
            });
    Platform.runLater(task);
    task.get(20, TimeUnit.SECONDS);
  }
}
