package tests;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Assertions;
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

public class NavigationTest {

    // WebDriver used to control the Chrome browser
    WebDriver driver;

    // Explicit wait used to make navigation interactions more stable
    WebDriverWait wait;

    @BeforeEach
    void setup() {
        // Start a new Chrome browser before each test
        driver = new ChromeDriver();

        // Create an explicit wait with a 10-second timeout
        wait = new WebDriverWait(driver, Duration.ofSeconds(10));

        // Maximise the browser window for consistent interaction
        driver.manage().window().maximize();
    }

    @ParameterizedTest(name = "Navigation test: {0}")
    @CsvFileSource(resources = "/navigation.csv", numLinesToSkip = 1)
    void testNavigationLinks(String linkText, String expectedUrlPart, String expectedPageText) {
        // Open the homepage before each navigation scenario
        driver.get("https://automationexercise.com");

        // Dismiss consent popup if it appears
        handleConsentIfPresent();

        try {
            // Wait for the navigation link to become clickable using the text from the CSV
            WebElement navLink = wait.until(ExpectedConditions.elementToBeClickable(
                    By.xpath("//a[contains(normalize-space(.),'" + linkText + "')]")
            ));

            // Click the required navigation link
            navLink.click();

            // Handle consent popup again in case it appears after navigation
            handleConsentIfPresent();

            // Wait for the URL to change to the expected destination
            wait.until(ExpectedConditions.urlContains(expectedUrlPart));

            // Check that the current URL contains the expected fragment
            Assertions.assertTrue(
                    driver.getCurrentUrl().contains(expectedUrlPart),
                    "URL did not contain expected part: " + expectedUrlPart
            );

            // Check that the destination page contains the expected text
            Assertions.assertTrue(
                    driver.getPageSource().contains(expectedPageText),
                    "Page did not contain expected text: " + expectedPageText
            );

            // Take a screenshot if the navigation test passes
            takeScreenshot("SUCCESS_" + makeSafeName(linkText));

        } catch (Exception e) {
            // Take a screenshot if the test fails
            takeScreenshot("FAIL_" + makeSafeName(linkText));

            // Rethrow the exception so JUnit records the failure
            throw e;
        }
    }

    private void handleConsentIfPresent() {
        try {
            // Use a shorter wait because the popup may not always appear
            WebDriverWait shortWait = new WebDriverWait(driver, Duration.ofSeconds(3));

            // Try to locate and click a consent / accept button
            WebElement consentButton = shortWait.until(ExpectedConditions.elementToBeClickable(
                    By.xpath("//button[contains(.,'Consent') or contains(.,'AGREE') or contains(.,'Accept')]")
            ));
            consentButton.click();
        } catch (Exception ignored) {
            // Ignore exceptions because the popup will not be present on every page
        }
    }

    private void takeScreenshot(String fileName) {
        try {
            // Capture the current browser window as an image file
            File src = ((TakesScreenshot) driver).getScreenshotAs(OutputType.FILE);

            // Ensure the screenshots folder exists
            Path folderPath = Path.of("screenshots");
            Files.createDirectories(folderPath);

            // Add a timestamp so screenshots do not overwrite each other
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

            // Print the save path to the console for easier checking
            System.out.println("Screenshot saved to: " + destination.toAbsolutePath());

        } catch (Exception ex) {
            // Print an error message if the screenshot could not be saved
            System.out.println("Could not save screenshot: " + ex.getMessage());
        }
    }

    private String makeSafeName(String text) {
        // Replace characters that may cause issues in filenames with underscores
        return text.replaceAll("[^a-zA-Z0-9]", "_");
    }

    @AfterEach
    void tearDown() {
        // Close the browser after each test if it is still open
        if (driver != null) {
            driver.quit();
        }
    }
}