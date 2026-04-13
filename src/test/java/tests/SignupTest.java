package tests;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.io.BufferedReader;
import java.io.FileReader;
import java.time.Duration;

public class SignupTest {

    WebDriver driver;
    WebDriverWait wait;
    JavascriptExecutor js;

    @BeforeEach
    void setup() {
        driver = new ChromeDriver();
        wait = new WebDriverWait(driver, Duration.ofSeconds(15));
        js = (JavascriptExecutor) driver;
        driver.manage().window().maximize();
    }

    @Test
    void registerUsersFromCSV() throws Exception {
        String csvFile = "src/test/resources/users.csv"; // Ensure this file is in your project root
        String line;
        String cvsSplitBy = ",";

        try (BufferedReader br = new BufferedReader(new FileReader(csvFile))) {
            // Skip the header row
            br.readLine();

            while ((line = br.readLine()) != null) {
                String[] user = line.split(cvsSplitBy);
                performSignup(user);
            }
        }
    }

    void performSignup(String[] data) {
        // 1. Open registration page directly [cite: 42]
        driver.get("https://automationexercise.com/signup");
        handleConsent();

        // --- STEP 1: INITIAL SIGNUP ---
        wait.until(ExpectedConditions.visibilityOfElementLocated(By.cssSelector("input[data-qa='signup-name']")));
        js.executeScript("document.querySelector(\"input[data-qa='signup-name']\").value=arguments[0]", data[0]); // Name [cite: 15]
        js.executeScript("document.querySelector(\"input[data-qa='signup-email']\").value=arguments[0]", data[1]); // Email [cite: 17]
        js.executeScript("document.querySelector(\"button[data-qa='signup-button']\").click()");
        handleConsent();

        // --- STEP 2: ACCOUNT INFORMATION --- [cite: 20]
        wait.until(ExpectedConditions.visibilityOfElementLocated(By.id("id_gender1")));

        js.executeScript("document.getElementById('id_gender1').click()"); // Mr. [cite: 13]
        js.executeScript("document.getElementById('password').value=arguments[0]", data[2]); // Password [cite: 19]

        // DOB [cite: 21, 22, 25, 26]
        js.executeScript("document.getElementById('days').value=arguments[0]", data[3]);
        js.executeScript("document.getElementById('months').value=arguments[0]", data[4]);
        js.executeScript("document.getElementById('years').value=arguments[0]", data[5]);

        // Newsletter checkboxes [cite: 23, 24]
        js.executeScript("document.getElementById('newsletter').click()");
        js.executeScript("document.getElementById('optin').click()");

        // --- STEP 3: ADDRESS INFORMATION --- [cite: 36]
        js.executeScript("document.getElementById('first_name').value=arguments[0]", data[6]); // [cite: 27]
        js.executeScript("document.getElementById('last_name').value=arguments[0]", data[7]);  // [cite: 28]
        js.executeScript("document.getElementById('company').value=arguments[0]", data[8]);    //
        js.executeScript("document.getElementById('address1').value=arguments[0]", data[9]);   // [cite: 30]
        js.executeScript("document.getElementById('address2').value=arguments[0]", data[10]);  //
        js.executeScript("document.getElementById('country').value=arguments[0]", data[11]);   // [cite: 32, 33]
        js.executeScript("document.getElementById('state').value=arguments[0]", data[12]);     //
        js.executeScript("document.getElementById('city').value=arguments[0]", data[13]);      // [cite: 46]
        js.executeScript("document.getElementById('zipcode').value=arguments[0]", data[14]);   // [cite: 47]
        js.executeScript("document.getElementById('mobile_number').value=arguments[0]", data[15]); // [cite: 48]

        // Submit
        js.executeScript("document.querySelector(\"button[data-qa='create-account']\").click()"); // [cite: 49]
        handleConsent();

        // Verification
        wait.until(ExpectedConditions.visibilityOfElementLocated(By.xpath("//b[text()='Account Created!']")));
        System.out.println("Successfully registered: " + data[1]);
    }

    void handleConsent() {
        try {
            // Updated to handle multiple variations of the Google/IAB consent popups
            WebDriverWait shortWait = new WebDriverWait(driver, Duration.ofSeconds(3));
            WebElement consent = shortWait.until(ExpectedConditions.elementToBeClickable(
                    By.xpath("//button[contains(@class, 'fc-button') and contains(., 'Consent')] | " +
                            "//button[text()='AGREE'] | " +
                            "//button[contains(text(),'Accept')]")
            ));
            consent.click();
        } catch (Exception ignored) {
            // No popup appeared
        }
    }

    @AfterEach
    void teardown() {
        if (driver != null) driver.quit();
    }
}