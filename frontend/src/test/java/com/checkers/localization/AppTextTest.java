package com.checkers.localization;

import static org.junit.jupiter.api.Assertions.assertEquals;

import javafx.geometry.NodeOrientation;
import javafx.scene.layout.Pane;
import org.junit.jupiter.api.Test;

class AppTextTest {

    @Test
    void translatesAndFormatsTextForEachAvailableLanguage() {
        AppLanguage previousLanguage = AppText.language();
        try {
            AppText.setLanguage(AppLanguage.SPANISH);
            assertEquals("¡Bienvenido, Ana!", AppText.text("Welcome, {0}!", "Ana"));

            AppText.setLanguage(AppLanguage.ARABIC);
            assertEquals("اللغة", AppText.text("Language"));

            AppText.setLanguage(AppLanguage.THAI);
            assertEquals("ภาษา", AppText.text("Language"));

            AppText.setLanguage(AppLanguage.ENGLISH);
            assertEquals("Unmapped text", AppText.text("Unmapped text"));
        } finally {
            AppText.setLanguage(previousLanguage);
        }
    }

    @Test
    void appliesRightToLeftOrientationForArabic() {
        AppLanguage previousLanguage = AppText.language();
        try {
            Pane root = new Pane();
            AppText.setLanguage(AppLanguage.ARABIC);
            AppText.applyOrientation(root);
            assertEquals(NodeOrientation.RIGHT_TO_LEFT, root.getNodeOrientation());

            AppText.setLanguage(AppLanguage.SPANISH);
            AppText.applyOrientation(root);
            assertEquals(NodeOrientation.LEFT_TO_RIGHT, root.getNodeOrientation());
        } finally {
            AppText.setLanguage(previousLanguage);
        }
    }
}
