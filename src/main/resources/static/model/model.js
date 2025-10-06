class SelectableElement {
    var element;
    var group;

    constructor(element, group) {
        this.element = element;
        this.group = group;
    }

    element() {
        return this.element;
    }

    /**
     * выделяет текущий, отменяет выделение у остальных в группе
     */
    select() {
        this.element.classList.add('active');
        this.group.forEach(e => e.classList.remove('active'));
    }
}

// import EmbeddedStorage from ./js/storage.js

const tests = [];
const checks = [];
const fields = [];


class Test extends SelectableElement {
    this.id;   // "12345"
    this.testId;  // "XXXXX-T777"

    constructor(id, testId) {
        super(document.createElement('div'), tests);
    }

    static createElement() {
        const container = document.getElementById('tabs-container');

        const tab = document.createElement('button');
        tab.classList.add('tab', this.id);
        tab.onclick = this.select;

        const leftPart = document.createElement('div');
        leftPart.className = 'tab-left';

        const status = document.createElement('span');
        status.className = 'tab-status';
        status.innerHTML = this.getStatusIcon(this.savedData.statuses.tests[test.id]);       // FIXME: данные из локального хранилища EmbeddedStorage

        const content = document.createElement('span');
        content.textContent = test.name;

        const spinner = document.createElement('span');             // использовать объект Spinner
        spinner.className = 'tab-spinner';                          // использовать объект Spinner
        if (this.savedData.statuses.processing.tests[test.id]) {    // FIXME: объект Spinner должен использовать данные из локального хранилища EmbeddedStorage
            spinner.innerHTML = '<div class="spinner"></div>';
        }

        leftPart.appendChild(status);
        leftPart.appendChild(content);
        tab.appendChild(leftPart);
        tab.appendChild(spinner);

        container.appendChild(tab);
    }
}