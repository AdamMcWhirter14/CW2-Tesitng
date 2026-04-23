package tests;

import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvFileSource;
import org.openqa.selenium.*;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class NavigationTest {

    WebDriver driver;
    WebDriverWait wait;

    @BeforeEach
    void setup() {
        driver = new ChromeDriver();
        wait = new WebDriverWait(driver, Duration.ofSeconds(10));
        driver.manage().window().maximize();
    }

    @ParameterizedTest(name = "Navigation test: {0}")
    @CsvFileSource(resources = "/navigation.csv", numLinesToSkip = 1)
    void testNavigationLinks(String linkText, String expectedUrlPart, String expectedPageText) {
        driver.get("https://automationexercise.com");
        handleConsentIfPresent();

        try {
            WebElement navLink = wait.until(ExpectedConditions.elementToBeClickable(
                    By.xpath("//a[contains(normalize-space(.),'" + linkText + "')]")
            ));

            navLink.click();
            handleConsentIfPresent();

            wait.until(ExpectedConditions.urlContains(expectedUrlPart));

            Assertions.assertTrue(
                    driver.getCurrentUrl().contains(expectedUrlPart),
                    "URL did not contain expected part: " + expectedUrlPart
            );

            Assertions.assertTrue(
                    driver.getPageSource().contains(expectedPageText),
                    "Page did not contain expected text: " + expectedPageText
            );

        } catch (Exception e) {
            takeScreenshot("FAILED_" + linkText);
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

            Files.createDirectories(Path.of("screenshots"));

            String timestamp = LocalDateTime.now()
                    .format(DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss"));

            String safeName = fileName.replaceAll("[^a-zA-Z0-9_-]", "_");

            Files.copy(
                    src.toPath(),
                    Path.of("screenshots", safeName + "_" + timestamp + ".png"),
                    StandardCopyOption.REPLACE_EXISTING
            );
        } catch (Exception ex) {
            System.out.println("Could not save screenshot: " + ex.getMessage());
        }
    }

    @AfterEach
    void tearDown() {
        if (driver != null) {
            driver.quit();
        }
    }
}