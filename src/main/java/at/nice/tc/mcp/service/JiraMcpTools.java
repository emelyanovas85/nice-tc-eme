package at.nice.tc.mcp.service;

import at.nice.tc.ai.aggregator.TestCheckersAggregator;
import at.nice.tc.ai.tools.googleTool.GoogleTools;
import at.nice.tc.service.JiraService;
import at.nice.tc.utils.ThrowableUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springaicommunity.mcp.annotation.McpTool;
import org.springaicommunity.mcp.annotation.McpToolParam;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import static java.util.stream.Collectors.toList;

/**
 * MCP-совместимые инструменты для работы с Jira тест-кейсами.
 * <p>
 * Все методы с аннотацией @McpTool автоматически регистрируются
 * как доступные инструменты в MCP сервере.
 * <p>
 * Используется через:
 * - Open WebUI с mcpo прокси
 * - Прямое подключение через MCP клиент
 * - Claude Desktop / Cursor через claude_desktop_config.json
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class JiraMcpTools {

    private final JiraService jiraService;

    private final TestCheckersAggregator aggregatorChecker;

    @McpTool(name = "checkTestCaseByRequirements",
            description = "Проверяет тест-кейс по требованиям. " +
                    "Получает требования из Google Docs и запускает проверку для основного и всех вложенных тест-кейсов. " +
                    "Возвращает результаты проверки по тест-кейсу и вложенным в него тест-кейсам.")
    public String checkTestCaseByRequirements(
            @McpToolParam(description = "Ключ или ID основного тест-кейса для проверки")
            String keyTestCase) {

        return String.join("",
                aggregatorChecker.checkTestCase(keyTestCase)
                        .thenCompose(futures -> CompletableFuture
                                .allOf(futures.toArray(new CompletableFuture[0]))
                                .thenApply(v -> futures
                                        .stream()
                                        .map(future -> future.exceptionally(ex ->
                                                "Не удалось проверить тест" + keyTestCase + "Ошибка: " + ex.getMessage()))
                                        .map(CompletableFuture::join)
                                        .collect(toList())))
                        .join());
    }


    @McpTool(name = "askAboutTestCase",
            description = "Задает дополнительный вопрос о тест-кейсе, который был проверен ранее. " +
                    "Вопрос передается в соответствующий чат-клиент, который имеет контекст проверки.")
    public String askAboutTestCase(
            @McpToolParam(description = "Ключ или ID тест-кейса")
            String keyTestCase,
            @McpToolParam(description = "Дополнительный вопрос о тест-кейсе")
            String question) {

        return aggregatorChecker.askQuestionByKey(keyTestCase, question).join();
    }

    /**
     * Инструмент 1: Получение доступных свойств тест-кейса
     *
     * @return список доступных свойств для фильтрации
     */
    @McpTool(
            name = "getAvailableTestProperties",
            description = "Получить перечень всех доступных свойств тест-кейса. " +
                    "Используется для получения списка полей, которые можно использовать в фильтрах и запросах"
    )
    public List<String> getAvailableTestProperties(
            @McpToolParam(
                    description = "Передай букву 'a'",
                    required = false
            )
            String ignore
    ) throws IOException {
        log.debug("MCP Tool called: getAvailableTestProperties");
        return jiraService.getAvailableTestProperties();
    }

    /**
     * Инструмент 2: Проверка доступности Jira
     *
     * @return статус доступности Jira сервера
     */
    @McpTool(
            name = "isJiraAvailable",
            description = "Проверить доступность и статус Jira сервера. " +
                    "Возвращает информацию о соединении с Jira"
    )
    public String isAvailable(
            @McpToolParam(
                    description = "Передай букву 'a'",
                    required = false
            )
            String ignore
    ) {
        log.debug("MCP Tool called: isJiraAvailable");
        try {
            return jiraService.isAvailable().join().toString();
        } catch (Exception e) {
            log.error("Error checking Jira availability", e);
            return "UNAVAILABLE: " + e.getMessage();
        }
    }

    /**
     * Инструмент 3: Получение всех версий тест-кейса
     * <p>
     * Пример ответа:
     * {
     * "0": {
     * "updatedOn": "2025-10-24T09:04:21.657Z",
     * "id": 159362,
     * "majorVersion": 5,
     * "createdOn": "2025-04-02T11:56:26.757Z"
     * }
     * }
     *
     * @param testKey ключ тест-кейса (например, VPEPVV-T800)
     * @return JSON со всеми версиями тест-кейса
     */
    @Cacheable(value = "jiraAllVersions", key = "#testKey")
    @McpTool(
            name = "getAllVersions",
            description = "Получить все версии тест-кейса по его ключу. " +
                    "Возвращает информацию о каждой версии: ID, номер версии, дату создания и изменения. " +
                    "Пример ключа: VPEPVV-T800"
    )
    public String getAllVersions(
            @McpToolParam(
                    description = "Ключ тест-кейса в Jira (например, VPEPVV-T800)",
                    required = true
            )
            String testKey
    ) {
        log.debug("MCP Tool called: getAllVersions with testKey={}", testKey);
        return jiraService.getAllVersionsAsync(testKey)
                .exceptionally(ThrowableUtils::asString)
                .join();
    }

    /**
     * Инструмент 4: Получение выполнений теста
     * <p>
     * Пример ответа:
     * {
     * "data": [{
     * "automated": false,
     * "testResultStatus": {"name": "Pass"},
     * "executionDate": "2024-12-03T05:55:10.469Z",
     * "key": "PERUFR-E35"
     * }]
     * }
     *
     * @param versionId ID версии из getAllVersions
     * @param fields    список полей из getAvailableTestExecutionProperties
     * @return JSON с выполнениями теста
     */
    @McpTool(
            name = "getTestExecutions",
            description = "Получить все выполнения конкретной версии тест-кейса. " +
                    "Включает информацию о статусе, дате выполнения, и других свойствах. " +
                    "Используй getAllVersions для получения versionId, " +
                    "и getAvailableTestExecutionProperties для получения доступных полей"
    )
    public String getTestExecutions(
            @McpToolParam(
                    description = "ID версии тест-кейса (получить через getAllVersions). " +
                            "Пример: 159362",
                    required = true
            )
            int versionId,

            @McpToolParam(
                    description = "Список полей для включения в результат (JSON массив). " +
                            "Пример: [\"executionDate\",\"testResultStatus\",\"automated\"]. " +
                            "Все доступные поля получи через getAvailableTestExecutionProperties",
                    required = true
            )
            List<String> fields
    ) {
        log.debug("MCP Tool called: getTestExecutions with versionId={}, fields={}", versionId, fields);
        return jiraService.getTestExecutions(versionId, fields).join();
    }

    /**
     * Инструмент 5: Получение доступных свойств выполнений теста
     *
     * @return список доступных полей для выполнений теста
     */
    @Cacheable(value = "availableTestExecutionProperties")
    @McpTool(
            name = "getAvailableTestExecutionProperties",
            description = "Получить перечень всех доступных свойств выполнений тест-кейса. " +
                    "Используется для параметра 'fields' в getTestExecutions"
    )
    public List<String> getAvailableTestExecutionProperties() {
        return jiraService.getAvailableTestExecutionProperties();
    }

}
