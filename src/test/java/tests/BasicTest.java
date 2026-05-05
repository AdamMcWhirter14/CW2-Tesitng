package tests;

import org.junit.jupiter.api.*;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;

public class BasicTest {
//test
    //WebDriver object used to control the Chrome Browser
    WebDriver driver;

    @BeforeEach // Create a new Chrome browser session before each test
    void setup() {
        driver = new ChromeDriver();
    }

    @Test
    void openWebsite() {
        // Open the Automation Exercise homepage
        driver.get("https://automationexercise.com");
        Assertions.assertTrue(driver.getTitle().contains("Automation"));
    }

    @AfterEach   // Close the browser after each test to avoid leaving sessions open
    void teardown() {
        driver.quit();
    }
}