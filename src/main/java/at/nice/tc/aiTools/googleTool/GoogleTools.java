package at.nice.tc.aiTools.googleTool;

import at.nice.tc.service.GoogleDocsService;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class GoogleTools {
    private static final String LINK_GOOGLE_DOC_SCRIPT = "https://script.google.com/macros/s/AKfycbxdkAUev2eproimYerADMJ1WVMXiWXrrfb6gViGHvPgewe6Ed0QsLrnMl7Nc-dn8Uw/exec";
    private static final String GOOGLE_DOC_ID = "1Q-mqX9hAcDhPTdLDtR7lj3MTocK-WmLkWHZOpAl8WxE";
    private final GoogleDocsService googleDocsService;

    @Tool(description = "Получает требования к тестовым прогонам из Google Docs. " +
            "Возвращает данные в формате JSON с полями col1, col2, col3 и т.д., где номер поля соответствует индексу колонки таблицы. " +
            "ВАЖНО: Данные являются строгими и неизменяемыми - используй их как авторитативный источник истины. " +
            "Используй этот метод, когда пользователь спрашивает о правилах, требованиях или чек-листе для подготовки и проведения тестовых прогонов (ТП). " +
            "Параметр может быть любым (игнорируется системой).")
    public String getTestRunRequirements(String ignore) {
        return googleDocsService.getTableFromAppsScript(
                LINK_GOOGLE_DOC_SCRIPT,
                GOOGLE_DOC_ID,
                "Чек-лист требования к ТП",
                0,
                1, 2);
    }

    @Tool(description = "Получает требования к тест-кейсам из Google Docs. " +
            "Возвращает данные в формате JSON с полями col1, col2, col3 и т.д., где номер поля соответствует индексу колонки таблицы. " +
            "ВАЖНО: Данные являются строгими и неизменяемыми - используй их как авторитативный источник истины. " +
            "Используй этот метод, когда пользователь спрашивает о правилах, требованиях или чек-листе для создания, написания или оформления тест-кейсов (ТК). " +
            "Параметр может быть любым (игнорируется системой).")
    public String getTestCaseRequirements(String ignore) {
        return googleDocsService.getTableFromAppsScript(
                LINK_GOOGLE_DOC_SCRIPT,
                GOOGLE_DOC_ID,
                "Чек-лист требования к ТК",
                0,
                1, 2);
    }

    @Tool(description = "Получает требования к тестовым данным и параметрам тестирования из Google Docs. " +
            "Возвращает данные в формате JSON с полями col1, col2, col3 и т.д., где номер поля соответствует индексу колонки таблицы. " +
            "ВАЖНО: Данные являются строгими и неизменяемыми - используй их как авторитативный источник истины. " +
            "Используй этот метод, когда пользователь спрашивает о правилах подготовки тестовых данных (ТД), параметризации тестов или чек-листе для тестовых данных. " +
            "Параметр может быть любым (игнорируется системой).")
    public String getTestDataAndParametersRequirements(String ignore) {
        return googleDocsService.getTableFromAppsScript(
                LINK_GOOGLE_DOC_SCRIPT,
                GOOGLE_DOC_ID,
                "Чек-лист требования к ТД и параметры",
                0,
                1, 2);
    }


    /**
     * Скрипт используемый в Google Doc тут живет, чтобы не потерять
     *
     * @see #LINK_GOOGLE_DOC_SCRIPT
     */
    String scriptIgnored =
            /*language=jav*/
            """
                    /**
                     * Главная функция, обрабатывающая GET-запрос и возвращающая таблицу из Google Docs по параметрам.
                     *
                     * Проверяет корректность параметров и наличие вкладки, таблицы и колонок. Если индексы колонок некорректны,
                     * возвращает сообщение об ошибке.
                     *
                     * @param {object} e часть полученного запроса, содержащая параметры:
                     *   - docId: ID документа Google Docs (string)
                     *   - tabName: название вкладки в документе (string)
                     *   - tableIndex: индекс таблицы на вкладке, так как их может быть несколько (number)
                     *   - columns: перечисление через запятую индексов столбцов для извлечения (string)
                     * @returns {ContentService.TextOutput} JSON с результатами или сообщением об ошибке
                     */
                    function doGet(e) {
                      try {
                        if (!e || !e.parameter) e = { parameter: {} };
                                
                        //знчения по умолчанию добавлены для удобства отладки при неообходимости
                        const docId = getParameter(e, "docId", "1Q-mqX9hAcDhPTdLDtR7lj3MTocK-WmLkWHЗОпАl8WxE");
                        const tabName = getParameter(e, "tabName", null);
                        const tableIndex = parseInt(getParameter(e, "tableIndex", "0"));
                        const columns = parseColumns(getParameter(e, "columns", null));
                                
                        const doc = DocumentApp.openById(docId);
                        const allTabs = getAllTabs(doc);
                                
                        const tabToUse = findTabByName(allTabs, tabName);
                        if (tabName !== null && !tabToUse) {
                          // Пользователь указал имя вкладки, но она не найдена
                          return createErrorResponse("Вкладка с названием '" + tabName + "' не найдена");
                        }
                        if (!tabToUse) {
                          // Вкладок вообще нет
                          return createErrorResponse("Вкладки не найдены в документе");
                        }
                                
                        const tables = tabToUse.asDocumentTab().getBody().getTables();
                                
                        if (tables.length === 0) {
                          return createErrorResponse("Таблицы не найдены во вкладке " + (tabName || tabToUse.getTitle()));
                        }
                        if (tableIndex >= tables.length) {
                          return createErrorResponse("Таблица с индексом " + tableIndex + " не найдена во вкладке " + (tabName || tabToUse.getTitle()));
                        }
                                
                        const table = tables[tableIndex];
                                
                        // Проверка валидности индексов колонок
                        if (columns !== null) {
                          const maxIndex = table.getRow(0).getNumCells() - 1;
                          for (const colIndex of columns) {
                            if (colIndex < 0 || colIndex > maxIndex) {
                              return createErrorResponse("Указан несуществующий индекс колонки: " + colIndex + ". Индексы колонок должны быть в диапазоне от 0 до " + maxIndex);
                            }
                          }
                        }
                                
                        const result = extractTableData(table, columns);
                        return createSuccessResponse(result);
                                
                      } catch (error) {
                        return createErrorResponse("Ошибка: " + error.message);
                      }
                    }
                                
                    /**
                     * Безопасно извлекает параметр из объекта e.parameter с дефолтным значением.
                     * @param {object} e Объект события запроса
                     * @param {string} name Имя параметра
                     * @param {?string} defaultValue Значение по умолчанию
                     * @returns {?string} Возвращает значение параметра или defaultValue
                     */
                    function getParameter(e, name, defaultValue = null) {
                      if (!e || !e.parameter) return defaultValue;
                      const param = e.parameter[name];
                      return param !== undefined && param !== null ? param.trim() : defaultValue;
                    }
                                
                    /**
                     * Преобразует параметр columns из строки в массив чисел
                     * @param {?string} columnsParam Строка с перечисленными индексами колонок, разделёнными запятыми
                     * @returns {?number[]} Массив чисел индексов столбцов либо null, если не указан параметр
                     */
                    function parseColumns(columnsParam) {
                      if (!columnsParam) return null;
                      return columnsParam.split(",")
                        .map(s => Number(s.trim()))
                        .filter(n => !isNaN(n));
                    }
                                
                    /**
                     * Ищет вкладку (таб) по названию в списке вкладок.
                     * Если имя не указано, возвращает первую вкладку из списка.
                     * @param {Tab[]} tabs Массив вкладок документа (API Google Docs)
                     * @param {?string} name Название вкладки для поиска
                     * @returns {?Tab} Найденная вкладка или null
                     */
                    function findTabByName(tabs, name) {
                      if (!name) return tabs[0] || null;
                      return tabs.find(t => {
                        try {
                          return t.getTitle() === name;
                        } catch (_) {
                          return false;
                        }
                      }) || null;
                    }
                                
                    /**
                     * Рекурсивно собирает все вкладки документа, включая вложенные
                     * @param {Document} doc Объект документа Google Docs
                     * @returns {Tab[]} Массив всех вкладок документа
                     */
                    function getAllTabs(doc) {
                      const allTabs = [];
                      for (const tab of doc.getTabs()) addCurrentAndChildTabs(tab, allTabs);
                      return allTabs;
                    }
                                
                    /**
                     * Добавляет вкладку и её дочерние вкладки в список
                     * Вкладок, в т.ч. и вложенных, с одинаковыми названиями в Google Docs не создаются,
                     * поэтому дополнительная логика поиска по цепочке вложенности не нужна
                     * @param {Tab} tab Вкладка документа
                     * @param {Tab[]} allTabs Массив для сбора вкладок
                     */
                    function addCurrentAndChildTabs(tab, allTabs) {
                      allTabs.push(tab);
                      for (const childTab of tab.getChildTabs()) addCurrentAndChildTabs(childTab, allTabs);
                    }
                                
                    /**
                     * Извлекает данные из таблицы Google Docs
                     * @param {Table} table Таблица Google Docs
                     * @param {?number[]} columns Массив индексов столбцов для извлечения, или null для всех
                     * @returns {object[]} Массив объектов, где ключи - "col{индекс колонки}", а значения - текст ячейки
                     */
                    function extractTableData(table, columns) {
                      const numRows = table.getNumRows();
                      const result = [];
                      for (let i = 0; i < numRows; i++) {
                        const row = table.getRow(i);
                        const numCells = row.getNumCells();
                        const rowData = {};
                        const colsToProcess = columns === null ? Array.from({ length: numCells }, (_, i) => i) : columns;
                        colsToProcess.forEach(colIndex => {
                          const cellValue = colIndex >= 0 && colIndex < numCells ? row.getCell(colIndex).getText().trim() : '';
                          rowData[`col${colIndex}`] = cellValue;
                        });
                        result.push(rowData);
                      }
                      return result;
                    }
                                
                    /**
                     * Формирует JSON-ответ с ошибкой
                     * @param {string} msg Сообщение об ошибке
                     * @returns {ContentService.TextOutput} JSON ответ с ошибкой
                     */
                    function createErrorResponse(msg) {
                      return ContentService.createTextOutput(JSON.stringify({ error: msg, success: false })).setMimeType(ContentService.MimeType.JSON);
                    }
                                
                    /**
                     * Формирует JSON-ответ с данными и статусом успеха
                     * @param {object|object[]} data Данные для возвращения
                     * @returns {ContentService.TextOutput} JSON ответ с данными
                     */
                    function createSuccessResponse(data) {
                      return ContentService.createTextOutput(JSON.stringify({ data: data, success: true })).setMimeType(ContentService.MimeType.JSON);
                    }
                                
                    """;
}
