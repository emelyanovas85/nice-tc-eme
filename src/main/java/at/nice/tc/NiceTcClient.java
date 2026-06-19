package at.nice.tc;

import at.nice.tc.ai.tools.jiraTool.JiraImpl;
import at.nice.tc.service.JiraService;
import jira.api.testCaseAPI.JiraTestCaseAPI;
import jira.api.testRunAPI.JiraTestRunAPI;
import jiraClient.JiraClient;
import jiraClient.JiraClientSingleton;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

/**
 * Фасадный клиент для использования nice-tc-eme как библиотеки без Spring-контекста.
 *
 * <p>Если нужны нестандартные учётные данные — вызовите {@link #setCredentials(String, String)}
 * один раз перед любыми другими методами:
 * <pre>
 * NiceTcClient.setCredentials("my-login", "my-token");
 * String test           = NiceTcClient.getTestWithNestedMarkdown("PROJ-T123");
 * String issue          = NiceTcClient.getIssueMarkdown("VPEPVV-1123");
 * String issueShort     = NiceTcClient.getIssueMarkdownShort("VPEPVV-1123");
 * String defects        = NiceTcClient.getDefectsMarkdown("PROJ-T123");
 * String defectsShort   = NiceTcClient.getDefectsMarkdownShort("PROJ-T123");
 * </pre>
 *
 * <p>Без вызова {@link #setCredentials} используются дефолтные учётные данные из JiraClient.
 */
public class NiceTcClient {

    /**
     * Устанавливает учётные данные Jira для всех последующих запросов.
     * Вызывать один раз до первого обращения к методам клиента.
     *
     * @param jiraUsername логин Jira
     * @param jiraPassword пароль / токен Jira
     */
    public static void setCredentials(String jiraUsername, String jiraPassword) {
        System.setProperty("jira.username", jiraUsername);
        System.setProperty("jira.password", jiraPassword);
    }

    /**
     * Получить тест с вложенными тестами в формате Markdown.
     *
     * @param testId ключ (PROJ-T123) или id версии теста
     * @return текст в формате Markdown
     */
    public static String getTestWithNestedMarkdown(String testId) throws Exception {
        return buildService().getTestWithNestedMarkdown(testId).get();
    }

    /**
     * Получить задачу Jira Issue в формате Markdown (все поля).
     *
     * @param issueKey ключ задачи, например VPEPVV-1123
     * @return текст в формате Markdown
     */
    public static String getIssueMarkdown(String issueKey) throws Exception {
        return buildService().getIssueMarkdown(issueKey).get();
    }

    /**
     * Получить краткое описание задачи Jira Issue в формате Markdown:
     * только заголовок (ключ — summary) и раздел «Описание».
     *
     * @param issueKey ключ задачи, например VPEPVV-1123
     * @return текст в формате Markdown
     */
    public static String getIssueMarkdownShort(String issueKey) throws Exception {
        return buildService().getIssueMarkdownShort(issueKey).get();
    }

    /**
     * Получить единый Markdown-контекст со всеми активными дефектами,
     * связанными с тестом через issueLinks (полный Markdown всех полей).
     *
     * <p>Фильтрация:
     * <ul>
     *   <li>только задачи типа «Дефект»;</li>
     *   <li>статус не «Закрыт (Fixed)» и не «Отменен (Canceled)».</li>
     * </ul>
     *
     * @param testIdOrKey числовой id версии теста (только цифры) или ключ теста (PROJ-T123)
     * @return единая Markdown-строка, объединяющая полные описания всех найденных дефектов
     * @throws Exception если запросы к Jira завершились ошибкой
     */
    public static String getDefectsMarkdown(String testIdOrKey) throws Exception {
        JiraService service = buildService();
        List<Integer> defectIds = service.getDefectIssueIds(testIdOrKey).get();

        List<CompletableFuture<String>> markdownFutures = defectIds.stream()
                .map(id -> service.getIssueMarkdown(String.valueOf(id)))
                .collect(Collectors.toList());

        CompletableFuture.allOf(markdownFutures.toArray(new CompletableFuture[0])).get();

        return markdownFutures.stream()
                .map(CompletableFuture::join)
                .collect(Collectors.joining("\n\n---\n\n"));
    }

    /**
     * Получить единый Markdown-контекст со всеми активными дефектами
     * в кратком формате: только заголовок (ключ — summary) и «Описание».
     *
     * <p>Фильтрация:
     * <ul>
     *   <li>только задачи типа «Дефект»;</li>
     *   <li>статус не «Закрыт (Fixed)» и не «Отменен (Canceled)».</li>
     * </ul>
     *
     * @param testIdOrKey числовой id версии теста (только цифры) или ключ теста (PROJ-T123)
     * @return единая Markdown-строка, объединяющая краткие описания всех найденных дефектов
     * @throws Exception если запросы к Jira завершились ошибкой
     */
    public static String getDefectsMarkdownShort(String testIdOrKey) throws Exception {
        JiraService service = buildService();
        List<Integer> defectIds = service.getDefectIssueIds(testIdOrKey).get();

        List<CompletableFuture<String>> markdownFutures = defectIds.stream()
                .map(id -> service.getIssueMarkdownShort(String.valueOf(id)))
                .collect(Collectors.toList());

        CompletableFuture.allOf(markdownFutures.toArray(new CompletableFuture[0])).get();

        return markdownFutures.stream()
                .map(CompletableFuture::join)
                .collect(Collectors.joining("\n\n---\n\n"));
    }

    private static JiraService buildService() {
        JiraClient jiraClient = JiraClientSingleton.getJiraClient();
        JiraTestCaseAPI testCaseAPI = JiraTestCaseAPI.getDefault();
        JiraTestRunAPI testRunAPI = JiraTestRunAPI.getDefault();
        JiraImpl jira = new JiraImpl(testCaseAPI, testRunAPI, jiraClient);
        return new JiraService(jira);
    }
}
