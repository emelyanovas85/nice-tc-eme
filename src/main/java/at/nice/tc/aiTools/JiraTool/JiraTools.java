package at.nice.tc.aiTools.JiraTool;

import at.nice.tc.service.JiraService;
import at.nice.tc.utils.JiraUtils;
import at.nice.tc.utils.JsonTreeMap;
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


    @Cacheable(value = "jiraATestFromJira", key = "#id + ':' + #fields.toString()")
    @Tool(description = "Получает информацию о тест кейсе по его ID из Jira с выбранными полями (json-свойствами)")
    public String readTestFromJira(
            @ToolParam(description = "id тест кейса, который состоит из аббревиатуры проекта, тире, номера теста, " +
                    "который начинается на T, например ASPPODDELTA-T1150")
            String id,

            @ToolParam(description = "Список полей для получения. Каждый элемент списка может быть: " +
                    "1) простое поле из доступных (например: id, objective, createdOn); " +
                    "2) вложенное поле с подполями в скобках (например:" +
                    "testScript(id,text,steps(index,description,text,expectedResult,testData,attachments,customFieldValues,id," +
                    "stepParameters(id,testCaseParameterId,value),testCase(id,key,name,archived,majorVersion,latestVersion," +
                    "parameters(id,name,defaultValue,index)))), " +
                    "parameters(id,name,defaultValue,index)). " +
                    "Если список пуст, вернет все данные теста, но велик риск не уложиться в тайм-аут.")
            List<String> fields) {
        JsonTreeMap jsonTreeMap = jiraService.getTest(id, fields)
                .thenApplyAsync(JiraUtils::parseTestJson)
                .thenApplyAsync(JiraUtils::sortSteps)
                .join();
        return JiraUtils.toString(jsonTreeMap).replaceAll("\\s{2,}", " ");
    }


    @Tool(description = "Returns list of all possible JSON test properties from Jira Zephyr Scale. " +
            "Составные поля (с '.' в названии) нужно перобразовать: каждую точку нужно " +
            "Необходимо преобразовать составные поля, записанные через точку, в составные поля, которые записываются через вложенные скобки")
    public List<String> getZephyrScaleTestProperties() throws IOException {
        return jiraService.getZephyrScaleTestProperties();
    }

    @Tool(description = "Получает информацию о доступности Jira")
    public String isAvailable(@ToolParam(description = "обязательно передалй любую букву") String ignore) {//todo ignore избавиться от заплатки в виде параметра в методе
        return jiraService.isAvailable().join().toString();
    }

    @Cacheable(value = "jiraAllVersions", key = "#testKey")
    @Tool(description = "Получает информацию о версиях теста по его ID из Jira")
    public String getAllVersions(String testKey) {
        return jiraService.getAllVersionsAsync(testKey).join().toString();
    }
}
