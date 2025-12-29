package com.arjunsudheer.backend;

import com.microsoft.playwright.*;
import com.microsoft.playwright.options.AriaRole;
import org.junit.jupiter.api.*;

import java.time.LocalDate;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;

public class ToDoTests {
    static Playwright playwright;
    static Browser browser;
    BrowserContext context;
    Page page;
    String currentGroupName;

    private final String BASE_URL = "http://localhost:5173";

    @BeforeAll
    static void launchBrowser() {
        playwright = Playwright.create();
        browser = playwright.chromium().launch(new BrowserType.LaunchOptions().setHeadless(false));
    }

    @AfterAll
    static void closeBrowser() {
        if (playwright != null) playwright.close();
    }

    @BeforeEach
    void setup() {
        context = browser.newContext();
        page = context.newPage();
        page.navigate(BASE_URL);
        currentGroupName = null;
    }

    @AfterEach
    void tearDown() {
        if (currentGroupName != null) {
            try {
                Locator sidebar = page.locator("aside").first();
                Locator groupRow = sidebar.locator("div.flex.items-center")
                        .filter(new Locator.FilterOptions().setHasText(currentGroupName));
                if (groupRow.isVisible()) {
                    groupRow.getByLabel("Delete To-Do List").click();
                }
            } catch (Exception e) {
                System.out.println("Cleanup skipped: " + e.getMessage());
            }
        }
        if (context != null) context.close();
    }

    private void createAndSelectUniqueGroup() {
        currentGroupName = "Test Project " + System.currentTimeMillis();
        page.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName("Add To-Do List")).click();
        page.getByPlaceholder("Enter list name...").fill(currentGroupName);
        page.locator("button:has-text('Submit')").click();
        
        page.locator("aside").first().getByText(currentGroupName).click();
    }

    private void submitTaskModal() {
        Locator modal = page.locator("div.fixed.inset-0").filter(new Locator.FilterOptions().setHasText("Add New Task"));
        modal.locator("button.bg-blue-600").click(new Locator.ClickOptions().setForce(true));
        assertThat(modal).not().isVisible();
    }

    @Test
    @DisplayName("POST /addToDoList - Create a new Group")
    void shouldCreateNewGroup() {
        createAndSelectUniqueGroup();
        assertThat(page.locator("aside").first().getByText(currentGroupName)).isVisible();
    }

    @Test
    @DisplayName("GET /getToDoLists - Verify default list load")
    void shouldLoadDefaultList() {
        assertThat(page.locator("aside").first().getByText("My Tasks")).isVisible();
    }

    @Test
    @DisplayName("POST /addToDoItem - Add a new task")
    void shouldAddTask() {
        createAndSelectUniqueGroup();
        page.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName("Add New Task")).click();
        page.getByPlaceholder("Enter task name...").fill("New Task");
        page.locator("input[type='date']").fill(LocalDate.now().plusDays(1).toString());
        submitTaskModal();
        assertThat(page.locator("input[value='New Task']")).isVisible();
    }

    @Test
    @DisplayName("PATCH /updateToDoItem - Complete a task")
    void shouldMarkTaskAsComplete() {
        createAndSelectUniqueGroup();
        page.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName("Add New Task")).click();
        page.getByPlaceholder("Enter task name...").fill("Task to Finish");
        page.locator("input[type='date']").fill("2030-12-31");
        submitTaskModal();

        page.getByLabel("Mark as complete").first().click(new Locator.ClickOptions().setForce(true));

        page.waitForTimeout(1500);

        assertThat(page.getByRole(AriaRole.HEADING, new Page.GetByRoleOptions().setName("Completed Tasks"))).isVisible();
        assertThat(page.locator("input[value='Task to Finish']")).isVisible();
    }

    @Test
    @DisplayName("DELETE /deleteCompletedToDoItems - Clear finished tasks")
    void shouldClearAllCompleted() {
        createAndSelectUniqueGroup();
        page.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName("Add New Task")).click();
        page.getByPlaceholder("Enter task name...").fill("Disposable Task");
        page.locator("input[type='date']").fill("2030-12-31");
        submitTaskModal();

        page.waitForTimeout(2000);

        page.getByLabel("Mark as complete").first().click(new Locator.ClickOptions().setForce(true));
        
        page.waitForTimeout(1500);
        
        assertThat(page.getByRole(AriaRole.HEADING, new Page.GetByRoleOptions().setName("Completed Tasks"))).isVisible();

        page.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName("Delete All Completed Tasks")).click();

        assertThat(page.locator("input[value='Disposable Task']")).not().isVisible();
    }

    @Test
    @DisplayName("DELETE /deleteToDoList - Remove a group")
    void shouldDeleteGroup() {
        createAndSelectUniqueGroup();
        String groupToDelete = currentGroupName;
        currentGroupName = null;

        Locator sidebar = page.locator("aside").first();
        sidebar.locator("div.flex.items-center").filter(new Locator.FilterOptions().setHasText(groupToDelete))
               .getByLabel("Delete To-Do List").click();

        assertThat(sidebar).not().hasText(groupToDelete);
    }
}