package at.nice.tc.aiTools.JiraTool;

import at.nice.tc.service.JiraService;
import at.nice.tc.utils.ThrowableUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.List;

@Component
@RequiredArgsConstructor
public class JiraTools {

    private final JiraService jiraService;


    //    @Cacheable(value = "jiraATestFromJira", key = "#id + ':' + #fields.toString()") // FIXME: кэширование тупое
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
//                .thenApplyAsync(JiraUtils::simplifyHtmlVariables)
//                .thenApplyAsync(JiraUtils::parseTreeMapJson)
//                .thenApplyAsync(JiraUtils::sortSteps)
//                .thenApplyAsync(JiraUtils::toString)
                .join();
    }

    //    @Cacheable("availableTestProperties")
    @Tool(name = "getAvailableTestProperties", description = "Перечень доступных свойств тест-кейса")
    public List<String> getAvailableTestProperties(@ToolParam(description = "передай букву 'a'") String ignore) throws IOException {
        return jiraService.getAvailableTestProperties();
    }


    @Tool(description = "Получает информацию о доступности Jira")
    public String isAvailable(@ToolParam(description = "передай букву 'a'") String ignore) {//todo ignore избавиться от заплатки в виде параметра в методе
        return jiraService.isAvailable().join().toString();
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
    public String getAllVersions(String testKey) {
        return jiraService.getAllVersionsAsync(testKey)
                .exceptionally(ThrowableUtils::asString)
                .join();
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
            List<String> fields) {
        return jiraService.getTestExecutions(versionId, fields).join();
    }

    //    @Cacheable("availableTestExecutionProperties")
    @Tool(name = "getAvailableTestExecutionProperties", description = "Перечень доступных свойств выполнений тест-кейс")
    public List<String> getAvailableTestExecutionProperties(@ToolParam(description = "передай букву 'a'") String ignore) throws IOException {
        return jiraService.getAvailableTestExecutionProperties();
    }
}
