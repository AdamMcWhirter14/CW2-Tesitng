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

// Data-driven test suite for the user registration form on automationexercise.com
// Test data is read from users.csv in src/test/resources
// ScenarioType column in CSV controls which validation logic runs per row
public class SignupTest {

    WebDriver driver;
    WebDriverWait wait;
    JavascriptExecutor js; // Used to bypass ad overlays that block standard Selenium interactions

    // Runs before each test - opens a new Chrome browser window
    @BeforeEach
    void setup() {
        driver = new ChromeDriver();
        wait = new WebDriverWait(driver, Duration.ofSeconds(10));
        js = (JavascriptExecutor) driver;
        driver.manage().window().maximize();
    }

    // Runs once per CSV row - routes to correct test logic based on ScenarioType
    @ParameterizedTest
    @CsvFileSource(resources = "/users.csv", numLinesToSkip = 1)
    void signupScenarios(String name, String email, String password, String day, String month, String year,
                         String fName, String lName, String company, String addr1, String addr2,
                         String country, String state, String city, String zip, String mobile, String scenario) {

        // Open the signup page and dismiss any consent popup
        driver.get("https://automationexercise.com/signup");
        handleConsent();

        // Wait for the initial signup form fields to be visible
        wait.until(ExpectedConditions.visibilityOfElementLocated(By.cssSelector("input[data-qa='signup-name']")));

        // Append timestamp to email for SUCCESS tests to prevent duplicate errors on repeat runs
        if (scenario.equals("SUCCESS")) {
            email = email.replace("@", System.currentTimeMillis() + "@");
        }

        // Set name and email via JS to avoid ad overlays blocking the fields
        js.executeScript("document.querySelector(\"input[data-qa='signup-name']\").value=arguments[0]", name == null ? "" : name);
        js.executeScript("document.querySelector(\"input[data-qa='signup-email']\").value=arguments[0]", email == null ? "" : email);

        // Screenshot of first page before submitting
        takeScreenshot("Step1", "Entry_" + name);

        // Click signup button via JS to avoid ad overlay blocking the click
        js.executeScript("document.querySelector(\"button[data-qa='signup-button']\").click()");
        handleConsent();

        switch (scenario) {

            // TC-02: Duplicate email - expects error message on screen
            case "FAIL_DUPLICATE":
                WebElement error = wait.until(ExpectedConditions.visibilityOfElementLocated(
                        By.xpath("//p[contains(text(),'already exist')]")));
                takeScreenshot("FAIL", "DuplicateError_" + name);
                Assertions.assertTrue(error.isDisplayed(), "Duplicate email error not shown.");
                break;

            // TC-03, TC-04: Missing or invalid email - expects browser to block and stay on /signup
            case "FAIL_MISSING_EMAIL":
            case "FAIL_INVALID_EMAIL":
                takeScreenshot("FAIL", "InvalidEmail_" + name);
                Assertions.assertTrue(driver.getCurrentUrl().contains("/signup"),
                        "Expected to stay on signup page for invalid email.");
                break;

            // TC-08 to TC-14: Required field empty - expects submission to be blocked
            case "FAIL_REQUIRED":
                // Wait for account details form to load
                wait.until(ExpectedConditions.visibilityOfElementLocated(By.id("password")));

                // Fill all fields - the field under test comes through as empty string from CSV
                js.executeScript("document.getElementById('password').value=arguments[0]", password == null ? "" : password);
                js.executeScript("document.getElementById('days').value=arguments[0]", day == null ? "" : day);
                js.executeScript("document.getElementById('months').value=arguments[0]", month == null ? "" : month);
                js.executeScript("document.getElementById('years').value=arguments[0]", year == null ? "" : year);
                js.executeScript("document.getElementById('first_name').value=arguments[0]", fName == null ? "" : fName);
                js.executeScript("document.getElementById('last_name').value=arguments[0]", lName == null ? "" : lName);
                js.executeScript("document.getElementById('address1').value=arguments[0]", addr1 == null ? "" : addr1);
                js.executeScript("document.getElementById('country').value=arguments[0]", country == null ? "" : country);
                js.executeScript("document.getElementById('state').value=arguments[0]", state == null ? "" : state);
                js.executeScript("document.getElementById('city').value=arguments[0]", city == null ? "" : city);
                js.executeScript("document.getElementById('zipcode').value=arguments[0]", zip == null ? "" : zip);
                js.executeScript("document.getElementById('mobile_number').value=arguments[0]", mobile == null ? "" : mobile);

                // Scroll to the empty field so it is centred and visible in the screenshot
                if (password == null || password.isEmpty()) {
                    js.executeScript("document.getElementById('password').scrollIntoView({block: 'center'})");
                } else if (fName == null || fName.isEmpty()) {
                    js.executeScript("document.getElementById('first_name').scrollIntoView({block: 'center'})");
                } else if (lName == null || lName.isEmpty()) {
                    js.executeScript("document.getElementById('last_name').scrollIntoView({block: 'center'})");
                } else if (addr1 == null || addr1.isEmpty()) {
                    js.executeScript("document.getElementById('address1').scrollIntoView({block: 'center'})");
                } else if (city == null || city.isEmpty()) {
                    js.executeScript("document.getElementById('city').scrollIntoView({block: 'center'})");
                } else if (zip == null || zip.isEmpty()) {
                    js.executeScript("document.getElementById('zipcode').scrollIntoView({block: 'center'})");
                } else if (mobile == null || mobile.isEmpty()) {
                    js.executeScript("document.getElementById('mobile_number').scrollIntoView({block: 'center'})");
                }

                // Take screenshot showing empty field before submitting
                takeScreenshot("FAIL", "RequiredField_" + name);

                // Attempt to submit the form
                js.executeScript("document.querySelector(\"button[data-qa='create-account']\").click()");

                // Assert account was NOT created - submission should have been blocked
                Assertions.assertFalse(driver.getPageSource().contains("Account Created"),
                        "Form should not have submitted with a required field empty.");
                break;

            // TC-01, TC-05, TC-06, TC-07: Valid data - expects successful account creation
            case "SUCCESS":
                completeFullForm(password, day, month, year, fName, lName, addr1,
                        country, state, city, zip, mobile);

                // Wait for confirmation page
                wait.until(ExpectedConditions.visibilityOfElementLocated(
                        By.xpath("//b[text()='Account Created!']")));
                takeScreenshot("PASS", "AccountCreated_" + fName);
                Assertions.assertTrue(driver.getPageSource().contains("Account Created"));

                // Extra XSS check - assert no browser alert was triggered by the script tag
                if (fName != null && fName.contains("<script>")) {
                    Assertions.assertThrows(NoAlertPresentException.class,
                            () -> driver.switchTo().alert());
                }
                break;
        }
    }

    // Saves a screenshot to the specified subfolder with a timestamp to avoid overwriting
    private void takeScreenshot(String folder, String fileName) {
        try {
            String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("HH-mm-ss-SSS"));
            String cleanName = fileName.replaceAll("[^a-zA-Z0-9]", "_");
            String path = "screenshots/" + folder + "/";
            Files.createDirectories(Paths.get(path)); // Creates folder if it doesn't exist
            File src = ((TakesScreenshot) driver).getScreenshotAs(OutputType.FILE);
            Files.copy(src.toPath(), Paths.get(path + cleanName + "_" + timestamp + ".png"));
        } catch (Exception e) {
            System.err.println("Screenshot failed: " + e.getMessage());
        }
    }

    // Fills all fields on the account details form using JS to bypass ad overlays
    private void completeFullForm(String... f) {
        wait.until(ExpectedConditions.visibilityOfElementLocated(By.id("password")));
        js.executeScript("document.getElementById('password').value=arguments[0]", f[0] == null ? "" : f[0]);
        js.executeScript("document.getElementById('days').value=arguments[0]", f[1] == null ? "" : f[1]);
        js.executeScript("document.getElementById('months').value=arguments[0]", f[2] == null ? "" : f[2]);
        js.executeScript("document.getElementById('years').value=arguments[0]", f[3] == null ? "" : f[3]);
        js.executeScript("document.getElementById('first_name').value=arguments[0]", f[4] == null ? "" : f[4]);
        js.executeScript("document.getElementById('last_name').value=arguments[0]", f[5] == null ? "" : f[5]);
        js.executeScript("document.getElementById('address1').value=arguments[0]", f[6] == null ? "" : f[6]);
        js.executeScript("document.getElementById('country').value=arguments[0]", f[7] == null ? "" : f[7]);
        js.executeScript("document.getElementById('state').value=arguments[0]", f[8] == null ? "" : f[8]);
        js.executeScript("document.getElementById('city').value=arguments[0]", f[9] == null ? "" : f[9]);
        js.executeScript("document.getElementById('zipcode').value=arguments[0]", f[10] == null ? "" : f[10]);
        js.executeScript("document.getElementById('mobile_number').value=arguments[0]", f[11] == null ? "" : f[11]);
        js.executeScript("document.querySelector(\"button[data-qa='create-account']\").click()");
    }

    // Dismisses cookie/consent popups if they appear - called after every page load
    void handleConsent() {
        try {
            new WebDriverWait(driver, Duration.ofSeconds(3))
                    .until(ExpectedConditions.elementToBeClickable(
                            By.xpath("//button[contains(.,'Consent')]|//button[text()='AGREE']")))
                    .click();
        } catch (Exception ignored) {}
    }

    // Runs after each test - closes the browser
    @AfterEach
    void teardown() {
        if (driver != null) driver.quit();
    }
}