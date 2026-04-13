package tests;

import org.junit.jupiter.api.*;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;

public class BasicTest {
//test
    WebDriver driver;

    @BeforeEach
    void setup() {
        driver = new ChromeDriver();
    }

    @Test
    void openWebsite() {
        driver.get("https://automationexercise.com");
        Assertions.assertTrue(driver.getTitle().contains("Automation"));
    }

    @AfterEach
    void teardown() {
        driver.quit();
    }
}