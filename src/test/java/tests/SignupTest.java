package tests;

import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvFileSource;
import org.openqa.selenium.*;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.support.ui.*;
import java.time.Duration;

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
        js.executeScript("document.querySelector(\"input[data-qa='signup-name']\").value=arguments[0]", name);
        js.executeScript("document.querySelector(\"input[data-qa='signup-email']\").value=arguments[0]", email);
        js.executeScript("document.querySelector(\"button[data-qa='signup-button']\").click()");
        handleConsent();

        // --- STEP 2: SCENARIO LOGIC ---
        switch (scenario) {
            case "FAIL_DUPLICATE":
                WebElement error = wait.until(ExpectedConditions.visibilityOfElementLocated(By.xpath("//p[contains(text(),'already exist')]")));
                Assertions.assertTrue(error.isDisplayed(), "Duplicate email error message not found.");
                break;

            case "SUCCESS":
                completeFullForm(password, day, month, year, fName, lName, addr1, country, state, city, zip, mobile);

                // 1. Verify basic account creation
                wait.until(ExpectedConditions.visibilityOfElementLocated(By.xpath("//b[text()='Account Created!']")));
                Assertions.assertTrue(driver.getPageSource().contains("Account Created"));

                // 2. Security Edge Case: Verify XSS protection
                // If we injected a <script> tag, ensure no alert popped up (which means the script executed)
                if (fName.contains("<script>")) {
                    Assertions.assertThrows(NoAlertPresentException.class, () -> driver.switchTo().alert(),
                            "CRITICAL: XSS vulnerability found! The script tag in the First Name field was executed.");
                }

                System.out.println("Handled " + scenario + " with data: " + fName);
                break;
        }
    }

    private void completeFullForm(String... f) {
        wait.until(ExpectedConditions.visibilityOfElementLocated(By.id("password")));
        // password[0], day[1], month[2], year[3], fName[4], lName[5], addr1[6], country[7], state[8], city[9], zip[10], mobile[11]
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