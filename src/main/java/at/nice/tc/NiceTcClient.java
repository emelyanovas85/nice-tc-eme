package at.nice.tc;

import at.nice.tc.ai.tools.jiraTool.JiraImpl;
import at.nice.tc.service.JiraService;
import jira.api.testCaseAPI.JiraTestCaseAPI;
import jira.api.testRunAPI.JiraTestRunAPI;
import jiraClient.JiraClient;
import jiraClient.JiraClientSingleton;

/**
 * Фасадный клиент для использования nice-tc-eme как библиотеки без Spring-контекста.
 * <p>
 * Использование с дефолтными учётными данными (JiraClient defaults):
 * <pre>
 * String result = NiceTcClient.getTestWithNestedMarkdown("PROJ-123");
 * </pre>
 *
 * Использование с явными учётными данными:
 * <pre>
 * String result = NiceTcClient.getTestWithNestedMarkdown("my-login", "my-token", "PROJ-123");
 * </pre>
 */
public class NiceTcClient {

    /**
     * Получить тест с вложенными тестами в формате Markdown.
     * Использует дефолтные учётные данные из JiraClient.
     *
     * @param testId ключ (PROJ-T123) или id версии теста
     * @return текст в формате Markdown
     */
    public static String getTestWithNestedMarkdown(String testId) throws Exception {
        return buildService().getTestWithNestedMarkdown(testId).get();
    }

    /**
     * Получить тест с вложенными тестами в формате Markdown.
     * Использует явно указанные учётные данные.
     *
     * @param jiraUsername логин Jira
     * @param jiraPassword пароль / токен Jira
     * @param testId       ключ (PROJ-T123) или id версии теста
     * @return текст в формате Markdown
     */
    public static String getTestWithNestedMarkdown(
            String jiraUsername,
            String jiraPassword,
            String testId
    ) throws Exception {
        System.setProperty("jira.username", jiraUsername);
        System.setProperty("jira.password", jiraPassword);
        return buildService().getTestWithNestedMarkdown(testId).get();
    }

    private static JiraService buildService() {
        JiraClient jiraClient = JiraClientSingleton.getJiraClient();
        JiraTestCaseAPI testCaseAPI = JiraTestCaseAPI.getDefault();
        JiraTestRunAPI testRunAPI = JiraTestRunAPI.getDefault();
        JiraImpl jira = new JiraImpl(testCaseAPI, testRunAPI, jiraClient);
        return new JiraService(jira);
    }
}
