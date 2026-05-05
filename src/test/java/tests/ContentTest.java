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
    // WebDriver used to control the browser
    WebDriver driver;
    // Explicit wait used for page elements and timing stability
    WebDriverWait wait;

    @BeforeEach
    void setup() {
        // Start a new Chrome browser before each test
        driver = new ChromeDriver();
        // Create a wait object with a 10-second timeout
        wait = new WebDriverWait(driver, Duration.ofSeconds(10));
        // Maximise browser window so page elements are easier to interact with
        driver.manage().window().maximize();
    }

    @ParameterizedTest(name = "Content test: {2}")
    @CsvFileSource(resources = "/content.csv", numLinesToSkip = 1)
    void testPageContent(String pageUrl, String expectedText, String screenshotName) {
        // Open the page URL from the CSV file
        driver.get(pageUrl);
        // Dismiss consent popup if it appears
        handleConsentIfPresent();

        try {
            // Wait until the page body has loaded before checking content
            wait.until(ExpectedConditions.presenceOfElementLocated(By.tagName("body")));
            // Check that the expected text exists in the page source
            // This confirms the main content for that page is displayed
            Assertions.assertTrue(
                    driver.getPageSource().contains(expectedText),
                    "Expected text not found: " + expectedText
            );
            // Take a screenshot if the content check passes
            takeScreenshot("SUCCESS_" + makeSafeName(screenshotName));

        } catch (Exception e) {
            // Take a screenshot if the test fails
            takeScreenshot("FAIL_" + makeSafeName(screenshotName));
            // Rethrow the exception so JUnit marks the test as failed
            throw e;
        }
    }

    private void handleConsentIfPresent() {
        try {
            // Use a shorter wait because the consent popup may not always appear
            WebDriverWait shortWait = new WebDriverWait(driver, Duration.ofSeconds(3));
            // Try to locate and click a consent / accept button
            WebElement consentButton = shortWait.until(ExpectedConditions.elementToBeClickable(
                    By.xpath("//button[contains(.,'Consent') or contains(.,'AGREE') or contains(.,'Accept')]")
            ));
            consentButton.click();
        } catch (Exception ignored) {
            // Ignore exceptions here because the popup may not be present on every page
        }
    }

    private void takeScreenshot(String fileName) {
        try {
            // Capture the current browser window as an image file
            File src = ((TakesScreenshot) driver).getScreenshotAs(OutputType.FILE);

            // Ensure the screenshots folder exists in the project root
            Path folderPath = Path.of("screenshots");
            Files.createDirectories(folderPath);

            // Create a timestamp so each screenshot has a unique filename
            String timestamp = LocalTime.now()
                    .format(DateTimeFormatter.ofPattern("HH-mm-ss-SSS"));

            // Build the final screenshot path
            Path destination = folderPath.resolve(fileName + "_" + timestamp + ".png");

            // Copy the screenshot into the screenshots folder
            Files.copy(
                    src.toPath(),
                    destination,
                    StandardCopyOption.REPLACE_EXISTING
            );
            // Print the file location to the console for easier checking
            System.out.println("Screenshot saved to: " + destination.toAbsolutePath());

        } catch (Exception ex) {
            // Print an error message if the screenshot cannot be saved
            System.out.println("Could not save screenshot: " + ex.getMessage());
        }
    }

    private String makeSafeName(String text) {
        // Replace characters that are unsuitable for filenames with underscores
        return text.replaceAll("[^a-zA-Z0-9]", "_");
    }

    @AfterEach
    void tearDown() {
        if (driver != null) {
            // Close the browser after each test if it is open
            driver.quit();
        }
    }
}