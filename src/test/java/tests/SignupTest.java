package tests;

import org.junit.jupiter.api.*;
import org.openqa.selenium.*;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.support.ui.*;
import java.io.BufferedReader;
import java.io.FileReader;
import java.time.Duration;

public class SignupTest {

    WebDriver driver;
    WebDriverWait wait;
    JavascriptExecutor js;

    /**
     * Reusable setup to ensure a fresh session for every user in the CSV.
     */
    void setup() {
        ChromeOptions options = new ChromeOptions();
        options.addArguments("--remote-allow-origins=*");

        driver = new ChromeDriver(options);
        wait = new WebDriverWait(driver, Duration.ofSeconds(15));
        js = (JavascriptExecutor) driver;
        driver.manage().window().maximize();
    }

    @Test
    void registerUsersFromCSV() throws Exception {
        // Points to your specific file location
        String csvFile = "src/test/resources/users.csv";
        String line;

        try (BufferedReader br = new BufferedReader(new FileReader(csvFile))) {
            br.readLine(); // Skip CSV Header

            while ((line = br.readLine()) != null) {
                String[] userData = line.split(",");

                setup(); // Start browser for THIS user
                try {
                    performSignup(userData);
                } finally {
                    teardown(); // Close browser for THIS user
                }
            }
        }
    }

    void performSignup(String[] data) {
        // Start directly on the Signup Page [cite: 42]
        driver.get("https://automationexercise.com/signup");
        handleConsent();

        // --- STEP 1: INITIAL SIGNUP ---
        wait.until(ExpectedConditions.visibilityOfElementLocated(By.cssSelector("input[data-qa='signup-name']")));
        js.executeScript("document.querySelector(\"input[data-qa='signup-name']\").value=arguments[0]", data[0]);
        js.executeScript("document.querySelector(\"input[data-qa='signup-email']\").value=arguments[0]", data[1]);
        js.executeScript("document.querySelector(\"button[data-qa='signup-button']\").click()");
        handleConsent();

        // --- STEP 2: ACCOUNT INFORMATION [cite: 20] ---
        wait.until(ExpectedConditions.visibilityOfElementLocated(By.id("id_gender1")));

        js.executeScript("document.getElementById('id_gender1').click()"); // Title: Mr. [cite: 13]
        js.executeScript("document.getElementById('password').value=arguments[0]", data[2]); // Password [cite: 19]

        // Date of Birth [cite: 21, 22, 25, 26]
        js.executeScript("document.getElementById('days').value=arguments[0]", data[3]);
        js.executeScript("document.getElementById('months').value=arguments[0]", data[4]);
        js.executeScript("document.getElementById('years').value=arguments[0]", data[5]);

        // Newsletter/Offers [cite: 23, 24]
        js.executeScript("document.getElementById('newsletter').click()");
        js.executeScript("document.getElementById('optin').click()");

        // --- STEP 3: ADDRESS INFORMATION [cite: 36] ---
        js.executeScript("document.getElementById('first_name').value=arguments[0]", data[6]);  // [cite: 27]
        js.executeScript("document.getElementById('last_name').value=arguments[0]", data[7]);   // [cite: 28]
        js.executeScript("document.getElementById('company').value=arguments[0]", data[8]);     // [cite: 29]
        js.executeScript("document.getElementById('address1').value=arguments[0]", data[9]);    // [cite: 30]
        js.executeScript("document.getElementById('address2').value=arguments[0]", data[10]);   // [cite: 31]
        js.executeScript("document.getElementById('country').value=arguments[0]", data[11]);    // [cite: 32]
        js.executeScript("document.getElementById('state').value=arguments[0]", data[12]);      // [cite: 34]
        js.executeScript("document.getElementById('city').value=arguments[0]", data[13]);       // [cite: 46]
        js.executeScript("document.getElementById('zipcode').value=arguments[0]", data[14]);    // [cite: 47]
        js.executeScript("document.getElementById('mobile_number').value=arguments[0]", data[15]); // [cite: 48]

        // Final Submit [cite: 49]
        js.executeScript("document.querySelector(\"button[data-qa='create-account']\").click()");
        handleConsent();

        // Verification
        wait.until(ExpectedConditions.visibilityOfElementLocated(By.xpath("//b[text()='Account Created!']")));
        System.out.println("Registration Successful for: " + data[1]);
    }

    /**
     * Automatically handles both Google Consent and Privacy overlays [cite: 61, 62]
     */
    void handleConsent() {
        try {
            WebDriverWait shortWait = new WebDriverWait(driver, Duration.ofSeconds(4));
            WebElement consent = shortWait.until(ExpectedConditions.elementToBeClickable(
                    By.xpath("//button[contains(@class, 'fc-button') and contains(., 'Consent')] | " +
                            "//button[text()='AGREE'] | " +
                            "//button[contains(text(),'Accept')]")
            ));
            consent.click();
        } catch (Exception ignored) { }
    }

    void teardown() {
        if (driver != null) {
            driver.quit();
        }
    }
}