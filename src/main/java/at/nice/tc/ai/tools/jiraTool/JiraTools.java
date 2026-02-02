package at.nice.tc.ai.tools.jiraTool;

import at.nice.tc.events.ToolEventPublisher;
import at.nice.tc.events.impl.OnGetValue;
import at.nice.tc.service.JiraService;
import at.nice.tc.utils.EventUtils;
import at.nice.tc.utils.ThrowableSupplier;
import at.nice.tc.utils.ThrowableUtils;
import at.nice.tc.utils.ToolUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.chat.model.ToolContext;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class JiraTools {

    private final JiraService jiraService;
    private final ToolEventPublisher.Factory publisherFactory;

    //    @Cacheable("availableTestProperties")
    @Tool(name = "getAvailableTestProperties", description = "Перечень доступных свойств тест-кейса")
    public List<String> getAvailableTestProperties(@ToolParam(description = "передай букву 'a'") String ignore,
                                                   ToolContext toolContext) {
        return sendEvents("Получение списка доступных json-свойств теста",
                jiraService::getAvailableTestProperties,
                toolContext);
    }


    @Tool(description = "Получает информацию о доступности Jira")
    public String isAvailable(@ToolParam(description = "передай букву 'a'") String ignore, ToolContext context) {//todo ignore избавиться от заплатки в виде параметра в методе
        return sendEvents("Проверка доступности Jira",
                () -> jiraService.isAvailable().join().toString(),
                context);
    }

    @Cacheable(value = "jiraAllVersions", key = "#testKey")
    @Tool(name = "getAllVersions",
            description = """
                    Получает информацию о версиях теста по его ключу.
                    Пример ключа: "VPEPVV-T800";
                    Пример ответа:
                                    [
                                    	"0": {
                                    		"updatedOn": "2025-10-24T09:04:21.657Z",
                                    		"id": 159362,
                                    		"majorVersion": 5,
                                    		"createdOn": "2025-04-02T11:56:26.757Z"
                                    	},
                                    	...
                                    ]
                    """)
    public String getAllVersions(String testKey, ToolContext context) {
        return sendEvents("Получение версий теста " + testKey,
                () -> jiraService.getAllVersionsAsync(testKey)
                        .exceptionally(ThrowableUtils::asString)
                        .join(),
                context);
    }


    @Tool(description = """
            Получает информацию произведенных выполнениях теста.
            Пример ответа:
                    {
                    	"data": [
                    		{
                    			"automated": false,
                    			"testResultStatus": {
                    				"name": "Pass"
                    			},
                    			"issueLinks": [],
                    			"executionDate": "2024-12-03T05:55:10.469Z",
                    			"key": "PERUFR-E35",
                    			"testCase": {
                    				"id": 134595,
                    				"majorVersion": 1
                    			},
                    			"testRun": {
                    				"id": 65649,
                    				"key": "PERUFR-C1"
                    			}
                    		}
                    	]
                    }
            """)
    public String getTestExecutions(
            @ToolParam(description = "id конкретной версии тест-кейса, например 12345. Можно получить с помощью getAllVersions")
            int versionId,
            @ToolParam(description = "Перечень свойств, которые должен содержать результирующий json. " +
                    "Список доступных свойств можно получить с помощью getAvailableTestExecutionProperties. " +
                    "Выбирай только необходимые свойства!")
            List<String> fields,
            ToolContext context) {
        return sendEvents("Получение перечня выполнений теста " + versionId,
                jiraService.getTestExecutions(versionId, fields)::join,
                context);
    }

    //    @Cacheable("availableTestExecutionProperties")
    @Tool(name = "getAvailableTestExecutionProperties", description = "Перечень доступных свойств выполнений тест-кейс")
    public List<String> getAvailableTestExecutionProperties(
            @ToolParam(description = "передай букву 'a'") String ignore,
            ToolContext context) {
        return sendEvents("Получение перечня доступных json-свойств выполнений теста",
                jiraService::getAvailableTestExecutionProperties,
                context);
    }

    private <T> T sendEvents(String description, ThrowableSupplier<T> valueSupplier, ToolContext context) {
        String chatId = ToolUtils.conversationId(context);
        ToolEventPublisher publisher = publisherFactory.forConversation(chatId);
        var eventBase = new OnGetValue<T>(description, chatId);
        return EventUtils.getValueSendingEvents(valueSupplier, eventBase, publisher);
    }

    /*    @Cacheable(value = "jiraATestFromJira", key = "#id + ':' + #fields.toString()") // FIXME: кэширование тупое
    @Tool(description = """
            Получает данные тест-кейса из Jira.
            Возвращает полный тест-кейс в виде markdown.
            Содержит вложенные тест-кейсы из шагов.
            Содержит всю доступную информацию о тест-кейсе.
            Нет необходимости преобразовывать ответ.
            """)
    public String readTestFromJira(
            @ToolParam(description = "Уникальный идентификатор версии тест-кейса (вида 123456) или ключ (вида PERUFR-E35)")
            String idOrKey
    ) {
        return jiraService.getTestWithNestedMarkdown(idOrKey)
                .thenApplyAsync(JiraUtils::simplifyHtmlVariables)
                .thenApplyAsync(JiraUtils::parseTreeMapJson)
                .thenApplyAsync(JiraUtils::sortSteps)
                .thenApplyAsync(JiraUtils::toString)
                .join();
    }*/
}
