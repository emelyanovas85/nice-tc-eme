
// API client для работы с Jira контроллером

class Jira {
    static API_BASE_URL = '/api/jira';

    /**
     * Проверка статуса соединения с Jira
     * @returns {Promise<string>} "connected" или "disconnected" или сообщение ошибки
     */
    static getStatus() {
        return fetch(`${this.API_BASE_URL}/status`)
            .then(response => response.text());
    }

    /**
     * Получение списка полей теста
     * @returns {Promise<Array<Object>>} Массив объектов JiraFieldDTO
     */
    static getFields() {
        return fetch(`${this.API_BASE_URL}/fields`)
            .then(response => {
                if (response.status === 404) {
                    throw new Error('Fields not found');
                }
                if (!response.ok) {
                    throw new Error('Failed to get fields');
                }
                return response.json();
            });
    }

    /**
     * Чтение теста по идентификатору версии
     * @param {string} versionId - Идентификатор версии (например, "12345")
     * @returns {Promise<Object>} Объект JiraTestDTO
     */
    static readTest(versionId) {
        return fetch(`${this.API_BASE_URL}/tests/${versionId}`)
            .then(response => {
                if (response.status === 404) {
                    throw new Error(`Test version ${versionId} not found`);
                }
                if (!response.ok) {
                    throw new Error('Failed to read test');
                }
                return response.json();
            });
    }

    /**
     * Получение значений полей теста по идентификатору теста
     * @param {string} versionId - Идентификатор версии (например, "12345")
     * @param {array} fieldIds - Идентификаторы полей
     * @returns {Promise<Array<Object>>} Массив объектов Object (что высчитывает поле на бэке)
     */
    static async getTestFieldValues(versionId, fieldIds) {
        const params = new URLSearchParams();
        params.set('fields', JSON.stringify(fieldIds));
        return fetch(`${this.API_BASE_URL}/versions/${encodeURIComponent(versionId)}?${params}`)
            .then(response => {
                if (response.status === 404) {
                    throw new Error(`Values not found: test ${versionId}, fields ${fieldIds}`);
                }
                if (!response.ok) {
                    throw new Error(`Failed to search field values: test ${versionId}, fields ${fieldIds}`);
                }
                return response.json();
            });
    }

    /**
     * Поиск версий теста по идентификатору теста
     * @param {string} testKey - Идентификатор теста (например, "ABCDE-T777")
     * @returns {Promise<Array<Object>>} Массив объектов JiraTestVersionDTO
     */
    static getAllVersions(testKey) {
        return fetch(`${this.API_BASE_URL}/tests/${testKey}`)
            .then(response => {
                if (response.status === 404) {
                    throw new Error(`Test ${testKey} not found`);
                }
                if (!response.ok) {
                    throw new Error('Failed to search test versions');
                }
                return response.json();
            });
    }

    /**
     * Получение списка версий тестов из прогона
     * @param {string} runId - Идентификатор прогона (например, "ABCDE-С777")
     * @returns {Promise<Array<Object>>} Массив объектов JiraTestVersionDTO в порядке использования
     */
    static searchRun(runId) {
        return fetch(`${this.API_BASE_URL}/runs/${runId}`)
            .then(response => {
                if (response.status === 404) {
                    throw new Error(`Run ${runId} not found`);
                }
                if (!response.ok) {
                    throw new Error('Failed to search run');
                }
                return response.json();
            });
    }
}

// Экспорт класса
// export default Jira;