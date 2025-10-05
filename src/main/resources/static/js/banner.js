
/**
 * Показать баннер с заданным текстом
 * @param {string} text - Текст для отображения в баннере
 */
function showBanner(text) {
    const banner = document.getElementById('topBanner');
    const bannerText = document.getElementById('bannerText');

    bannerText.textContent = text;
    banner.classList.add('visible');
}

/**
 * Скрыть баннер
 */
function hideBanner() {
    const banner = document.getElementById('topBanner');
    banner.classList.remove('visible');
}
