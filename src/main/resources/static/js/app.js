/**
 * Главный файл приложения системы тестов
 * Взаимодействие с REST API, управление состоянием
 */

class TestSystemApp {
    constructor() {
        this.state = {
            currentPage: 'selection',
            currentTest: null,
            currentCheck: null,
            tests: [],
            chats: {},
            prompts: {},
            placeholders: {},
            statuses: {},
            processing: new Set(),
            sseConnected: false,
            version: 'no-thymeleaf'
        };

        this.apiBase = '/api'; // Базовый путь для REST API
        this.init();
    }

    /**
     * Инициализация приложения
     */
    init() {
        console.log('Инициализация приложения без Thymeleaf');
        this.loadEmbeddedData();
        this.bindEvents();
        this.initSSE();

        // Показать нужную страницу
        if (this.state.tests.length > 0) {
            this.showMainPage();
        } else {
            this.showSelectionPage();
        }
    }

    /**
     * Загрузка встроенных данных
     */
    loadEmbeddedData() {
        try {
            const dataElement = document.getElementById('embedded-data');
            if (dataElement && dataElement.textContent.trim()) {
                const data = JSON.parse(dataElement.textContent);
                this.state = { ...this.state, ...data };
            }
        } catch (error) {
            console.error('Ошибка загрузки встроенных данных:', error);
        }
    }

    /**
     * Сохранение данных
     */
    saveData() {
        try {
            const dataElement = document.getElementById('embedded-data');
            if (dataElement) {
                const dataToSave = {
                    ...this.state,
                    metadata: {
                        ...this.state.metadata,
                        lastModified: new Date().toISOString(),
                        version: 'no-thymeleaf'
                    }
                };
                dataElement.textContent = JSON.stringify(dataToSave, null, 2);
            }
        } catch (error) {
            console.error('Ошибка сохранения данных:', error);
        }
    }

    /**
     * Привязка событий
     */
    bindEvents() {
        // Страница выбора теста
        const testInput = document.getElementById('test-input');
        const backBtn = document.getElementById('back-to-selection');

        if (testInput) {
            testInput.addEventListener('input', this.handleTestInput.bind(this));
            testInput.addEventListener('keydown', this.handleTestInputKeydown.bind(this));
        }

        if (backBtn) {
            backBtn.addEventListener('click', this.showSelectionPage.bind(this));
        }

        // Закрытие баннера
        const closeBanner = document.getElementById('close-banner');
        if (closeBanner) {
            closeBanner.addEventListener('click', this.hideBanner.bind(this));
        }

        // Чат
        const sendBtn = document.getElementById('send-message');
        const stopBtn = document.getElementById('stop-processing');
        const chatInput = document.getElementById('chat-input');
        const promptInput = document.getElementById('prompt-input');

        if (sendBtn) {
            sendBtn.addEventListener('click', this.sendMessage.bind(this));
        }

        if (stopBtn) {
            stopBtn.addEventListener('click', this.stopProcessing.bind(this));
        }

        if (chatInput) {
            chatInput.addEventListener('keydown', this.handleChatKeydown.bind(this));
        }

        if (promptInput) {
            promptInput.addEventListener('keydown', this.handlePromptKeydown.bind(this));
            promptInput.addEventListener('input', this.handlePromptChange.bind(this));
        }

        // Экспорт/импорт
        const exportBtn = document.getElementById('export-report');
        const importBtn = document.getElementById('import-report');
        const importFile = document.getElementById('import-file');

        if (exportBtn) {
            exportBtn.addEventListener('click', this.exportReport.bind(this));
        }

        if (importBtn) {
            importBtn.addEventListener('click', () => importFile.click());
        }

        if (importFile) {
            importFile.addEventListener('change', this.importReport.bind(this));
        }
    }

    showSelectionPage() {
        document.getElementById('test-selection-page').classList.remove('hidden');
        document.getElementById('main-test-page').classList.add('hidden');
        document.getElementById('control-panel').classList.add('hidden');
        this.state.currentPage = 'selection';
    }

    showMainPage() {
        document.getElementById('test-selection-page').classList.add('hidden');
        document.getElementById('main-test-page').classList.remove('hidden');
        document.getElementById('control-panel').classList.remove('hidden');
        this.state.currentPage = 'main';
    }

    /**
     * Загрузка данных теста через REST API
     */
    async loadTestData(testId) {
        console.log('Загрузка данных теста через REST API:', testId);

        try {
            this.showLoading(true);

            let response;
            if (testId.startsWith('T')) {
                // Запрос версий теста
                response = await fetch(`${this.apiBase}/jira/tests/${testId}`);
            } else if (testId.startsWith('C')) {
                // Запрос версий из прогона
                response = await fetch(`${this.apiBase}/jira/runs/${testId}`);
            } else {
                throw new Error('Неверный формат ID');
            }

            if (!response.ok) {
                throw new Error('Тест не найден');
            }

            const versions = await response.json();
            console.log('Получены версии:', versions);

            // В реальности здесь будет логика выбора версии
            // Пока просто переходим на главную страницу
            this.showMainPage();

        } catch (error) {
            console.error('Ошибка загрузки теста:', error);
            this.showError(error.message);
        } finally {
            this.showLoading(false);
        }
    }

    showLoading(show) {
        const loadingEl = document.getElementById('loading-indicator');
        if (loadingEl) {
            if (show) {
                loadingEl.classList.remove('hidden');
            } else {
                loadingEl.classList.add('hidden');
            }
        }
    }

    showError(message) {
        const errorEl = document.getElementById('error-message');
        if (errorEl) {
            errorEl.textContent = message;
            errorEl.classList.remove('hidden');

            setTimeout(() => {
                errorEl.classList.add('hidden');
            }, 5000);
        }
    }

    // Остальные методы будут дополнены в других JS файлах
}

// Инициализация приложения
document.addEventListener('DOMContentLoaded', () => {
    window.testSystemApp = new TestSystemApp();
});
