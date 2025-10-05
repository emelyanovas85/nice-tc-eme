// Storage управление - сохранение и загрузка данных
console.log('💾 Загрузка Storage модуля...');

class StorageManager {
    constructor() {
        this.storageKey = 'testSystemData';
        console.log('💾 StorageManager инициализирован');
    }

    // Сохранение данных в localStorage
    save(data) {
        try {
            const dataToSave = {
                ...data,
                timestamp: new Date().toISOString(),
                version: '1.0'
            };

            localStorage.setItem(this.storageKey, JSON.stringify(dataToSave));
            console.log('💾 Данные сохранены в localStorage');
            return true;
        } catch (error) {
            console.error('❌ Ошибка сохранения данных:', error);
            return false;
        }
    }

    // Загрузка данных из localStorage
    load() {
        try {
            const savedData = localStorage.getItem(this.storageKey);
            if (savedData) {
                const data = JSON.parse(savedData);
                console.log('📦 Данные загружены из localStorage');
                return data;
            }
            return null;
        } catch (error) {
            console.error('❌ Ошибка загрузки данных:', error);
            return null;
        }
    }

    // Очистка localStorage
    clear() {
        try {
            localStorage.removeItem(this.storageKey);
            console.log('🗑️ localStorage очищен');
            return true;
        } catch (error) {
            console.error('❌ Ошибка очистки localStorage:', error);
            return false;
        }
    }

    // Экспорт данных в файл
    export() {
        try {
            const data = this.load();
            if (!data) {
                console.warn('⚠️ Нет данных для экспорта');
                return false;
            }

            const timestamp = new Date().toISOString().replace(/[:.]/g, '-').slice(0, -5);
            const filename = `test-system-export-${timestamp}.json`;

            const blob = new Blob([JSON.stringify(data, null, 2)], {
                type: 'application/json'
            });

            const url = URL.createObjectURL(blob);
            const link = document.createElement('a');
            link.href = url;
            link.download = filename;
            link.style.display = 'none';

            document.body.appendChild(link);
            link.click();
            document.body.removeChild(link);

            URL.revokeObjectURL(url);

            console.log('📄 Данные экспортированы:', filename);
            return true;
        } catch (error) {
            console.error('❌ Ошибка экспорта данных:', error);
            return false;
        }
    }

    // Импорт данных из файла
    import(file) {
        return new Promise((resolve, reject) => {
            if (!file) {
                reject(new Error('Файл не выбран'));
                return;
            }

            const reader = new FileReader();

            reader.onload = (e) => {
                try {
                    const data = JSON.parse(e.target.result);
                    const success = this.save(data);

                    if (success) {
                        console.log('📁 Данные импортированы из файла');
                        resolve(data);
                    } else {
                        reject(new Error('Ошибка сохранения импортированных данных'));
                    }
                } catch (error) {
                    console.error('❌ Ошибка парсинга импортированного файла:', error);
                    reject(error);
                }
            };

            reader.onerror = () => {
                reject(new Error('Ошибка чтения файла'));
            };

            reader.readAsText(file);
        });
    }

    // Получение статистики хранилища
    getStats() {
        try {
            const data = this.load();
            if (!data) {
                return {
                    hasData: false,
                    size: 0,
                    timestamp: null
                };
            }

            const jsonString = JSON.stringify(data);
            return {
                hasData: true,
                size: jsonString.length,
                sizeKB: Math.round(jsonString.length / 1024 * 100) / 100,
                timestamp: data.timestamp,
                version: data.version,
                keys: Object.keys(data).length
            };
        } catch (error) {
            console.error('❌ Ошибка получения статистики:', error);
            return { hasData: false, error: error.message };
        }
    }
}

// Глобальный экземпляр менеджера хранилища
window.storageManager = new StorageManager();

// Автосохранение каждые 30 секунд
setInterval(() => {
    if (window.testSystem && window.testSystem.savedData) {
        window.storageManager.save(window.testSystem.savedData);
    }
}, 30000);

console.log('✅ Storage модуль загружен');
