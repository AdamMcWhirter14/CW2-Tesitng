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

    // WebDriver used to control the browser
    WebDriver driver;

    // Explicit wait used to improve reliability when elements take time to appear
    WebDriverWait wait;

    // JavascriptExecutor is used because some site elements are blocked by overlays
    // and standard Selenium interactions may fail
    JavascriptExecutor js;

    @BeforeEach
    void setup() {
        // Start a new Chrome browser before each test
        driver = new ChromeDriver();

        // Create an explicit wait with a 10-second timeout
        wait = new WebDriverWait(driver, Duration.ofSeconds(10));

        // Cast the driver so JavaScript can be executed in the browser
        js = (JavascriptExecutor) driver;
        // Maximise the browser window for easier interaction
        driver.manage().window().maximize();
    }

    @ParameterizedTest
    @CsvFileSource(resources = "/users.csv", numLinesToSkip = 1)
    void signupScenarios(String name, String email, String password, String day, String month, String year,
                         String fName, String lName, String company, String addr1, String addr2,
                         String country, String state, String city, String zip, String mobile, String scenario) {

        // Open the signup page
        driver.get("https://automationexercise.com/signup");

        // Dismiss consent popup if it appears
        handleConsent();

        // Wait until the first signup field is visible
        wait.until(ExpectedConditions.visibilityOfElementLocated(By.cssSelector("input[data-qa='signup-name']")));

        // Make email unique for SUCCESS cases to avoid duplicate errors on repeated runs
        // This prevents duplicate account errors when the test is rerun
        if (scenario.equals("SUCCESS")) {
            email = email.replace("@", System.currentTimeMillis() + "@");
        }

        // Enter the initial name and email fields using JavaScript
        // This helps avoid issues caused by overlays blocking sendKeys()
        js.executeScript("document.querySelector(\"input[data-qa='signup-name']\").value=arguments[0]", name == null ? "" : name);
        js.executeScript("document.querySelector(\"input[data-qa='signup-email']\").value=arguments[0]", email == null ? "" : email);

        // Capture a screenshot of the initial signup page values
        takeScreenshot("Step1", "Entry_" + name);

        // Submit the initial signup form
        js.executeScript("document.querySelector(\"button[data-qa='signup-button']\").click()");

        // Handle consent popup again if it appears
        handleConsent();

        // Run different validation logic depending on the scenario type from the CSV
        switch (scenario) {
            case "FAIL_DUPLICATE":
                // For duplicate email scenarios, wait for the error message to appear
                WebElement error = wait.until(ExpectedConditions.visibilityOfElementLocated(
                        By.xpath("//p[contains(text(),'already exist')]")));

                // Capture screenshot showing duplicate email failure
                takeScreenshot("FAIL", "DuplicateError_" + name);

                // Assert that the duplicate email error is visible
                Assertions.assertTrue(error.isDisplayed(), "Duplicate email error not shown.");
                break;

            case "FAIL_MISSING_EMAIL":
            case "FAIL_INVALID_EMAIL":
                // For missing or invalid email, the browser should keep the user on the signup page
                takeScreenshot("FAIL", "InvalidEmail_" + name);
                Assertions.assertTrue(driver.getCurrentUrl().contains("/signup"),
                        "Expected to stay on signup page for invalid email.");
                break;

            case "FAIL_REQUIRED":
                // Wait for the second form to load before filling required fields
                wait.until(ExpectedConditions.visibilityOfElementLocated(By.id("password")));

                // Fill the account creation form, leaving one required field empty depending on the CSV row
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

                // Scroll to whichever required field is empty so it is visible in the screenshot
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

                // Capture a screenshot showing the required field failure
                takeScreenshot("FAIL", "RequiredField_" + name);

                // Attempt to submit the form
                js.executeScript("document.querySelector(\"button[data-qa='create-account']\").click()");

                // Assert that the account is not created when a required field is empty
                Assertions.assertFalse(driver.getPageSource().contains("Account Created"),
                        "Form should not have submitted with a required field empty.");
                break;

            case "SUCCESS":
                // Fill in the full form with valid data
                completeFullForm(password, day, month, year, fName, lName, addr1,
                        country, state, city, zip, mobile);

                // Wait for the success message to appear
                wait.until(ExpectedConditions.visibilityOfElementLocated(
                        By.xpath("//b[text()='Account Created!']")));
                // Capture a screenshot of the successful account creation
                takeScreenshot("PASS", "AccountCreated_" + fName);
                // Assert that the success text is shown
                Assertions.assertTrue(driver.getPageSource().contains("Account Created"));

                // If the first name contains script tags, confirm no alert was executed
                if (fName != null && fName.contains("<script>")) {
                    Assertions.assertThrows(NoAlertPresentException.class,
                            () -> driver.switchTo().alert());
                }
                break;
        }
    }

    private void takeScreenshot(String folder, String fileName) {
        try {
            // Create a timestamp so screenshots have unique filenames
            String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("HH-mm-ss-SSS"));

            // Replace unsafe filename characters with underscores
            String cleanName = fileName.replaceAll("[^a-zA-Z0-9]", "_");

            // Build the output folder path
            String path = "screenshots/" + folder + "/";

            // Create the folder if it does not already exist
            Files.createDirectories(Paths.get(path));

            // Capture the current browser view
            File src = ((TakesScreenshot) driver).getScreenshotAs(OutputType.FILE);

            // Save the screenshot into the correct folder
            Files.copy(src.toPath(), Paths.get(path + cleanName + "_" + timestamp + ".png"));
        } catch (Exception e) {
            // Print a message if screenshot saving fails
            System.err.println("Screenshot failed: " + e.getMessage());
        }
    }

    private void completeFullForm(String... f) {
        //Wait until the account form is visible before filling it
        wait.until(ExpectedConditions.visibilityOfElementLocated(By.id("password")));

        // Populate all required fields using JavaScript
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

        // Submit the account creation form
        js.executeScript("document.querySelector(\"button[data-qa='create-account']\").click()");
    }

    void handleConsent() {
        try {
            // Try to dismiss any consent popup that may block interaction
            new WebDriverWait(driver, Duration.ofSeconds(3))
                    .until(ExpectedConditions.elementToBeClickable(
                            By.xpath("//button[contains(.,'Consent')]|//button[text()='AGREE']")))
                    .click();
        } catch (Exception ignored) {}
        // Ignore exceptions because the popup may not always appear
    }

    @AfterEach // Close the browser after each test if it is open
    void teardown() {
        if (driver != null) driver.quit();
    }
}