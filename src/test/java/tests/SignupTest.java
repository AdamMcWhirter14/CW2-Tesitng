package tests;

import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvFileSource;
import org.openqa.selenium.*;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.support.ui.*;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class SignupTest {

    WebDriver driver;
    WebDriverWait wait;
    JavascriptExecutor js;

    @BeforeEach
    void setup() {
        driver = new ChromeDriver();
        wait = new WebDriverWait(driver, Duration.ofSeconds(10));
        js = (JavascriptExecutor) driver;
        driver.manage().window().maximize();
    }

    @ParameterizedTest
    @CsvFileSource(resources = "/users.csv", numLinesToSkip = 1)
    void signupScenarios(String name, String email, String password, String day, String month, String year,
                         String fName, String lName, String company, String addr1, String addr2,
                         String country, String state, String city, String zip, String mobile, String scenario) {

        driver.get("https://automationexercise.com/signup");
        handleConsent();

        // --- STEP 1: INITIAL SIGNUP ---
        wait.until(ExpectedConditions.visibilityOfElementLocated(By.cssSelector("input[data-qa='signup-name']")));

        // Use empty string if CSV value is null
        js.executeScript("document.querySelector(\"input[data-qa='signup-name']\").value=arguments[0]", name == null ? "" : name);
        js.executeScript("document.querySelector(\"input[data-qa='signup-email']\").value=arguments[0]", email == null ? "" : email);

        // Screenshot BEFORE clicking
        takeScreenshot(scenario, "Step1_Entry_" + name);

        js.executeScript("document.querySelector(\"button[data-qa='signup-button']\").click()");
        handleConsent();

        // --- STEP 2: SCENARIO VALIDATION ---
        switch (scenario) {
            case "FAIL_DUPLICATE":
                WebElement error = wait.until(ExpectedConditions.visibilityOfElementLocated(By.xpath("//p[contains(text(),'already exist')]")));
                takeScreenshot("FAIL", "DuplicateError_" + name);
                Assertions.assertTrue(error.isDisplayed());
                break;

            case "FAIL_MISSING_EMAIL":
            case "FAIL_INVALID_EMAIL":
                // Take shot of browser tooltip or stayed-on-page state
                takeScreenshot("FAIL", "InvalidEmail_" + name);
                Assertions.assertTrue(driver.getCurrentUrl().contains("/signup"));
                break;

            case "SUCCESS":
                completeFullForm(password, day, month, year, fName, lName, addr1, country, state, city, zip, mobile);
                wait.until(ExpectedConditions.visibilityOfElementLocated(By.xpath("//b[text()='Account Created!']")));

                // Screenshot of final success
                takeScreenshot("PASS", "AccountCreated_" + fName);

                // Security Check for XSS
                if (fName != null && fName.contains("<script>")) {
                    Assertions.assertThrows(NoAlertPresentException.class, () -> driver.switchTo().alert());
                }
                break;
        }
    }

    /**
     * Captures screenshot and organizes into Pass/Fail subfolders with timestamps.
     */
    private void takeScreenshot(String folder, String fileName) {
        try {
            String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("HH-mm-ss-SSS"));
            String cleanName = fileName.replaceAll("[^a-zA-Z0-9]", "_");
            String path = "screenshots/" + folder + "/";

            Files.createDirectories(Paths.get(path));

            File src = ((TakesScreenshot) driver).getScreenshotAs(OutputType.FILE);
            Files.copy(src.toPath(), Paths.get(path + cleanName + "_" + timestamp + ".png"));
        } catch (Exception e) {
            System.err.println("Screenshot failed: " + e.getMessage());
        }
    }

    private void completeFullForm(String... f) {
        wait.until(ExpectedConditions.visibilityOfElementLocated(By.id("password")));
        js.executeScript("document.getElementById('password').value=arguments[0]", f[0]);
        js.executeScript("document.getElementById('days').value=arguments[0]", f[1]);
        js.executeScript("document.getElementById('months').value=arguments[0]", f[2]);
        js.executeScript("document.getElementById('years').value=arguments[0]", f[3]);
        js.executeScript("document.getElementById('first_name').value=arguments[0]", f[4]);
        js.executeScript("document.getElementById('last_name').value=arguments[0]", f[5]);
        js.executeScript("document.getElementById('address1').value=arguments[0]", f[6]);
        js.executeScript("document.getElementById('country').value=arguments[0]", f[7]);
        js.executeScript("document.getElementById('state').value=arguments[0]", f[8]);
        js.executeScript("document.getElementById('city').value=arguments[0]", f[9]);
        js.executeScript("document.getElementById('zipcode').value=arguments[0]", f[10]);
        js.executeScript("document.getElementById('mobile_number').value=arguments[0]", f[11]);
        js.executeScript("document.querySelector(\"button[data-qa='create-account']\").click()");
    }

    void handleConsent() {
        try {
            new WebDriverWait(driver, Duration.ofSeconds(3))
                    .until(ExpectedConditions.elementToBeClickable(By.xpath("//button[contains(.,'Consent')]|//button[text()='AGREE']")))
                    .click();
        } catch (Exception ignored) {}
    }

    @AfterEach
    void teardown() {
        if (driver != null) driver.quit();
    }
}