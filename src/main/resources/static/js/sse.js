// SSE (Server-Sent Events) управление - полностью переписано
console.log('📡 Загрузка SSE модуля...');

class SSEManager {
    constructor() {
        this.eventSource = null;
        this.reconnectAttempts = 0;
        this.maxReconnectAttempts = 10;
        this.reconnectInterval = 5000; // 5 секунд
        this.isConnected = false;

        console.log('📡 SSEManager инициализирован');
    }

    connect() {
        console.log('🔌 Попытка подключения к SSE...');

        if (this.eventSource) {
            console.log('🔌 Закрытие предыдущего соединения');
            this.eventSource.close();
        }

        try {
            this.eventSource = new EventSource('/api/sse');

            // Обработчик открытия соединения
            this.eventSource.onopen = () => {
                console.log('✅ SSE соединение установлено');
                this.isConnected = true;
                this.reconnectAttempts = 0;
                this.updateConnectionStatus('connected');
            };

            // Обработчик входящих сообщений
            this.eventSource.onmessage = (event) => {
                try {
                    const data = JSON.parse(event.data);
                    console.log('📨 SSE сообщение получено:', data);
                    this.handleMessage(data);
                } catch (error) {
                    console.error('❌ Ошибка парсинга SSE сообщения:', error);
                }
            };

            // Обработчик ошибок
            this.eventSource.onerror = (error) => {
                console.error('❌ SSE ошибка:', error);
                this.isConnected = false;
                this.updateConnectionStatus('error');

                if (this.reconnectAttempts < this.maxReconnectAttempts) {
                    this.scheduleReconnect();
                } else {
                    console.error('❌ Превышено количество попыток переподключения');
                    this.updateConnectionStatus('failed');
                }
            };

        } catch (error) {
            console.error('❌ Ошибка создания SSE соединения:', error);
            this.updateConnectionStatus('error');
            this.scheduleReconnect();
        }
    }

    disconnect() {
        console.log('🔌 Отключение от SSE...');

        if (this.eventSource) {
            this.eventSource.close();
            this.eventSource = null;
        }

        this.isConnected = false;
        this.updateConnectionStatus('disconnected');
    }

    scheduleReconnect() {
        this.reconnectAttempts++;
        const delay = this.reconnectInterval * Math.min(this.reconnectAttempts, 5);

        console.log(`🔄 Переподключение через ${delay}ms (попытка ${this.reconnectAttempts})`);
        this.updateConnectionStatus('connecting');

        setTimeout(() => {
            if (this.reconnectAttempts <= this.maxReconnectAttempts) {
                this.connect();
            }
        }, delay);
    }

    handleMessage(data) {
        console.log('📨 Обработка SSE сообщения:', data);

        if (!data || typeof data !== 'object') {
            console.warn('⚠️ Неверный формат SSE сообщения');
            return;
        }

        switch (data.type) {
            case 'chat_message':
                this.handleChatMessage(data);
                break;
            case 'status_update':
                this.handleStatusUpdate(data);
                break;
            case 'system_notification':
                this.handleSystemNotification(data);
                break;
            default:
                console.log('ℹ️ Неизвестный тип SSE сообщения:', data.type);
        }
    }

    handleChatMessage(data) {
        console.log('💬 Обработка чат сообщения:', data);

        if (window.testSystem && data.message) {
            window.testSystem.addChatMessage('system', data.message);
        }
    }

    handleStatusUpdate(data) {
        console.log('📊 Обновление статуса:', data);

        if (window.testSystem && data.testId && data.status) {
            // Обновляем статус в системе
            console.log(`📊 Статус ${data.testId}: ${data.status}`);
        }
    }

    handleSystemNotification(data) {
        console.log('📢 Системное уведомление:', data);

        if (data.message) {
            // Можно показать уведомление в интерфейсе
            console.log('📢 Уведомление:', data.message);
        }
    }

    updateConnectionStatus(status) {
        const statusElement = document.getElementById('connection-status');

        if (statusElement) {
            statusElement.className = `connection-status ${status}`;

            const statusText = {
                'connected': 'Подключено',
                'connecting': 'Подключение...',
                'error': 'Ошибка подключения',
                'failed': 'Подключение не удалось',
                'disconnected': 'Отключено'
            };

            statusElement.textContent = statusText[status] || 'Неизвестно';
        }

        console.log(`📶 Статус соединения: ${status}`);
    }

    // Отправка тестового события (для отладки)
    sendTestEvent(message) {
        fetch('/api/sse/test', {
            method: 'POST',
            headers: {
                'Content-Type': 'application/x-www-form-urlencoded',
            },
            body: `message=${encodeURIComponent(message || 'Тестовое событие')}`
        })
        .then(response => response.text())
        .then(result => {
            console.log('📨 Тестовое событие отправлено:', result);
        })
        .catch(error => {
            console.error('❌ Ошибка отправки тестового события:', error);
        });
    }
}

// Глобальный экземпляр SSE менеджера
window.sseManager = null;

// Инициализация SSE при загрузке основного интерфейса
document.addEventListener('DOMContentLoaded', function() {
    console.log('📡 Инициализация SSE...');

    // Создаем SSE менеджер
    window.sseManager = new SSEManager();

    // Подключаемся к SSE с небольшой задержкой
    setTimeout(() => {
        if (window.sseManager) {
            window.sseManager.connect();
        }
    }, 1000);
});

// Обработка событий окна
window.addEventListener('beforeunload', function() {
    console.log('🌐 Закрытие страницы, отключение SSE');
    if (window.sseManager) {
        window.sseManager.disconnect();
    }
});

window.addEventListener('online', function() {
    console.log('🌐 Интернет восстановлен, переподключение SSE');
    if (window.sseManager && !window.sseManager.isConnected) {
        window.sseManager.connect();
    }
});

window.addEventListener('offline', function() {
    console.log('🌐 Интернет потерян');
    if (window.sseManager) {
        window.sseManager.updateConnectionStatus('offline');
    }
});

console.log('✅ SSE модуль загружен');
