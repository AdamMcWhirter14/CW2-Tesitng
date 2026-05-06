package tests;

import com.aventstack.extentreports.ExtentReports;
import com.aventstack.extentreports.reporter.ExtentSparkReporter;

// Shared manager that creates and holds a single ExtentReports instance
// Used by all three test classes to log results into one report
public class ExtentReportManager {

    private static ExtentReports extent;

    // Returns the shared ExtentReports instance, creating it if it doesn't exist
    public static ExtentReports getInstance() {
        if (extent == null) {
            ExtentSparkReporter reporter = new ExtentSparkReporter("reports/TestReport.html");
            reporter.config().setReportName("CW2 Test Report");
            reporter.config().setDocumentTitle("CMP329 Coursework 2");
            extent = new ExtentReports();
            extent.attachReporter(reporter);
            extent.setSystemInfo("Tester", "Adam");
            extent.setSystemInfo("Environment", "automationexercise.com");
        }
        return extent;
    }

    // Flushes the report to disk - call this after all tests finish
    public static void flush() {
        if (extent != null) {
            extent.flush();
        }
    }
}
