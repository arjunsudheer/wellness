package com.arjunsudheer.backend;

import com.microsoft.playwright.*;
import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;

import java.nio.file.Paths;

public class AIChatControllerPlaywrightTest {
    static Playwright playwright;
    static Browser browser;
    static BrowserContext context;
    Page page;

    // Constants for selectors and timeouts
    private static final String CHAT_INPUT = "#chat-input";
    private static final String SEND_BUTTON = "#send-button";
    // Assuming the AI response messages have the class 'chat-response'
    private static final String CHAT_RESPONSE = ".chat-response";
    private static final String ERROR_MESSAGE = ".error-message";
    private static final int TIMEOUT = 30000; // 30 seconds timeout

    @BeforeAll
    static void launchBrowser() {
        playwright = Playwright.create();
        browser = playwright.chromium().launch(new BrowserType.LaunchOptions()
                .setHeadless(false) // Set to true for CI/CD
                .setSlowMo(100)); // Add slight delay between actions
    }

    @AfterAll
    static void closeBrowser() {
        if (browser != null) {
            browser.close();
        }
        if (playwright != null) {
            playwright.close();
        }
    }

    @BeforeEach
    void createContextAndPage() {
        context = browser.newContext();
        page = context.newPage();
        context.tracing().start(new Tracing.StartOptions()
                .setScreenshots(true)
                .setSnapshots(true)
                .setSources(true));

        // Navigate with timeout and wait until DOM content is loaded
        page.navigate("http://localhost:5173", new Page.NavigateOptions()
                .setTimeout(TIMEOUT));

        // Wait for chat window to be ready
        page.waitForSelector(CHAT_INPUT, new Page.WaitForSelectorOptions()
                .setTimeout(TIMEOUT));
    }

    @AfterEach
    void closeContext() {
        if (context != null) {
            // Save trace after each test
            context.tracing().stop(new Tracing.StopOptions()
                    .setPath(Paths.get("trace-" + System.currentTimeMillis() + ".zip")));
            context.close();
        }
    }

    // Test AI Chat Endpoint
    @Test
    void testAIChatEndpoint() {
        String testMessage = "Add a new task called \"Math Homework\" to the list \"My Tasks\".";

        // 1. Locate the input field and type the message.
        Locator inputField = page.locator(CHAT_INPUT);
        // Ensure the input field is ready before filling
        inputField.waitFor(new Locator.WaitForOptions().setTimeout(TIMEOUT));
        inputField.fill(testMessage);

        // 2. Click the send button.
        page.locator(SEND_BUTTON).click();

        // 3. Wait for the AI response element to appear AND contain text.
        // We use .first() to target the newest response if there are others.
        Locator responseLocator = page.locator(CHAT_RESPONSE).first();

        // Wait for the response element to become visible
        responseLocator
                .waitFor(new Locator.WaitForOptions()
                        .setTimeout(TIMEOUT));

        page.waitForTimeout(TIMEOUT);

        // 4. Retrieve the response text.
        String response = responseLocator.textContent();

        // 5. Assert the response is not null and not empty.
        assertNotNull(response, "The AI response should not be null.");
        assertFalse(response.trim().isEmpty(), "The AI response should not be empty.");

        // Assert that the response is reasonable (contains "added" keyword)
        assertTrue(response.contains("added"), "The response should contain 'added' keyword.");

        System.out.println("AI Response received: " + response.substring(0, Math.min(response.length(), 100)) + "...");
    }
}