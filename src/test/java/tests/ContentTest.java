package tests;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvFileSource;
import org.openqa.selenium.By;
import org.openqa.selenium.OutputType;
import org.openqa.selenium.TakesScreenshot;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.Duration;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

public class ContentTest {

    WebDriver driver;
    WebDriverWait wait;

    @BeforeEach
    void setup() {
        driver = new ChromeDriver();
        wait = new WebDriverWait(driver, Duration.ofSeconds(10));
        driver.manage().window().maximize();
    }

    @ParameterizedTest(name = "Content test: {2}")
    @CsvFileSource(resources = "/content.csv", numLinesToSkip = 1)
    void testPageContent(String pageUrl, String expectedText, String screenshotName) {
        driver.get(pageUrl);
        handleConsentIfPresent();

        try {
            wait.until(ExpectedConditions.presenceOfElementLocated(By.tagName("body")));

            Assertions.assertTrue(
                    driver.getPageSource().contains(expectedText),
                    "Expected text not found: " + expectedText
            );

            takeScreenshot("SUCCESS_" + makeSafeName(screenshotName));

        } catch (Exception e) {
            takeScreenshot("FAIL_" + makeSafeName(screenshotName));
            throw e;
        }
    }

    private void handleConsentIfPresent() {
        try {
            WebDriverWait shortWait = new WebDriverWait(driver, Duration.ofSeconds(3));
            WebElement consentButton = shortWait.until(ExpectedConditions.elementToBeClickable(
                    By.xpath("//button[contains(.,'Consent') or contains(.,'AGREE') or contains(.,'Accept')]")
            ));
            consentButton.click();
        } catch (Exception ignored) {
        }
    }

    private void takeScreenshot(String fileName) {
        try {
            File src = ((TakesScreenshot) driver).getScreenshotAs(OutputType.FILE);

            Path folderPath = Path.of("screenshots");
            Files.createDirectories(folderPath);

            String timestamp = LocalTime.now()
                    .format(DateTimeFormatter.ofPattern("HH-mm-ss-SSS"));

            Path destination = folderPath.resolve(fileName + "_" + timestamp + ".png");

            Files.copy(
                    src.toPath(),
                    destination,
                    StandardCopyOption.REPLACE_EXISTING
            );

            System.out.println("Screenshot saved to: " + destination.toAbsolutePath());

        } catch (Exception ex) {
            System.out.println("Could not save screenshot: " + ex.getMessage());
        }
    }

    private String makeSafeName(String text) {
        return text.replaceAll("[^a-zA-Z0-9]", "_");
    }

    @AfterEach
    void tearDown() {
        if (driver != null) {
            driver.quit();
        }
    }
}