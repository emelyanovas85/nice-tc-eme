/**
 * Управление хранением данных без Thymeleaf
 */

TestSystemApp.prototype.exportReport = function() {
    try {
        const timestamp = new Date().toISOString().replace(/[:.]/g, '-').slice(0, -5);
        const filename = `test-report-no-thymeleaf-${timestamp}.html`;

        const htmlContent = document.documentElement.outerHTML;
        const blob = new Blob([htmlContent], { type: 'text/html;charset=utf-8' });
        const url = URL.createObjectURL(blob);

        const link = document.createElement('a');
        link.href = url;
        link.download = filename;
        link.click();

        URL.revokeObjectURL(url);
        this.showBanner(`Отчет экспортирован: ${filename}`, 'success');

    } catch (error) {
        console.error('Ошибка экспорта отчета:', error);
        this.showBanner('Ошибка экспорта отчета', 'error');
    }
};

TestSystemApp.prototype.importReport = function(event) {
    const file = event.target.files[0];
    if (!file) return;

    if (!file.name.toLowerCase().endsWith('.html')) {
        this.showBanner('Можно импортировать только HTML файлы', 'error');
        return;
    }

    const reader = new FileReader();
    reader.onload = (e) => {
        try {
            this.processImportedHTML(e.target.result, file.name);
        } catch (error) {
            console.error('Ошибка импорта отчета:', error);
            this.showBanner('Ошибка импорта отчета', 'error');
        }
    };

    reader.readAsText(file);
    event.target.value = '';
};

TestSystemApp.prototype.processImportedHTML = function(htmlContent, filename) {
    if (confirm(`Импортировать отчет "${filename}"?\nВерсия: без Thymeleaf`)) {
        this.showBanner('Отчет успешно импортирован', 'success');
    }
};

TestSystemApp.prototype.showBanner = function(message, type = 'info') {
    const banner = document.getElementById('notification-banner');
    const bannerText = document.getElementById('banner-text');

    if (!banner || !bannerText) return;

    bannerText.textContent = message;
    banner.classList.remove('hidden');

    setTimeout(() => {
        this.hideBanner();
    }, 10000);
};

TestSystemApp.prototype.hideBanner = function() {
    const banner = document.getElementById('notification-banner');
    if (banner) {
        banner.classList.add('hidden');
    }
};

TestSystemApp.prototype.setProcessing = function(testId, checkId, isProcessing) {
    const key = `${testId}-${checkId}`;

    if (isProcessing) {
        this.state.processing.add(key);
    } else {
        this.state.processing.delete(key);
    }
};

TestSystemApp.prototype.stopProcessing = async function() {
    try {
        const response = await fetch(`${this.apiBase}/ai/stop`, {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json'
            },
            body: JSON.stringify({
                testId: this.state.currentTest,
                checkId: this.state.currentCheck
            })
        });

        if (response.ok) {
            console.log('Обработка остановлена через REST API');
        }

    } catch (error) {
        console.error('Ошибка остановки обработки:', error);
    }
};
