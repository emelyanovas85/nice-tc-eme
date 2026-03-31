# Основной тест-кейс

## Тест-кейс 159362

### Подробнее
#### Ключ
- VPEPVV-T800
#### Версия
- 5
#### Наименование
- САДД. Отправка запроса из ЛК КО в САДД (позитивный)
#### Задача тест-кейса
- В ТК осуществляются следующие проверки:<br><ol><li>Отправка формы без рубрики</li><li>Отправка формы с рубрикой</li><li>Переход ЭС по статусам</li><li>Проверка параметра рубрика в файле xml</li></ol>
#### Предварительные действия
<ol><li>Подписать файлы: File.doc, File.docx, File.pdf, File.xbrl, File.xls, File.xlsx, File.xml, File.xtdd, File.zip, УКЭП (Усиленная квалифицированная электронная подпись) физ. лица у которого МЧД в ХМЧД.</li><li>Зашифровать файлы: File.doc, File.docx, File.pdf, File.xbrl, File.xls, File.xlsx, File.xml, File.xtdd, File.zip, на два сертификата (<u>физ. лицо с ХМЧД и сертификат Банка РФ</u>), при успешном шифровании получаем файлы с расширением .enc</li><li>Выполнить <a href="http://jira.cbr.ru/secure/Tests.jspa#/testCase/VPEPVV-T799">Переход к форме "Обращение (запрос) в Банк России"</a></li></ol><br />Внимание, для ручного прохождения ТК!!!<br />В ТК есть запись вида: "script1.xml .doc_writer.name" - это означает следующее:<br />Необходимо открыть вложенный в ТК файл "script1.xml", внутри которого есть нода "doc_writer.name" значение которой надо использовать<br /><br /><br />MindMap:<br /><img src="https://jira.cbr.ru/rest/tests/1.0/attachment/image/532586" class="fr-fic fr-dii" />

### Наборы тестовых данных
|№|Файлы|Информационные файлы|Файлы подписей|Сертификат|Доверенность|Эталон|Описание_проверки|Черновик|
|-|-|-|-|-|-|-|-|-|
|1|File.doc, File.docx, File.pdf, File.xbrl, File.xls, File.xlsx, File.xml, File.xtdd, File.zip|form.xml.enc, File.doc.enc, File.docx.enc, File.pdf.enc, File.xbrl.enc, File.xls.enc, File.xlsx.enc, File.xml.enc, File.xtdd.enc, File.zip.enc|form.xml.sig, File.doc.sig, File.docx.sig, File.pdf.sig, File.xbrl.sig, File.xls.sig, File.xlsx.sig, File.xml.sig, File.xtdd.sig, File.zip.sig|Выполнить VPEPVV-T1081|Не требуется|script3.xml|ЮЛ. Файлы|draft3.xml|
|2|Без файлов|form.xml|form.xml.sig|Выполнить VPEPVV-T1082|Выполнить VPEPVV-T1076|script5.xml|ФЛ+МЧД. Без файлов|draft5.xml|
|3|File.doc, File.docx, File.pdf, File.xbrl, File.xls, File.xlsx, File.xml, File.xtdd, File.zip, File.doc.sig, File.docx.sig, File.pdf.sig, File.xbrl.sig, File.xls.sig, File.xlsx.sig, File.xml.sig, File.xtdd.sig, File.zip.sig|form.xml, File.doc, File.docx, File.pdf, File.xbrl, File.xls, File.xlsx, File.xml, File.xtdd, File.zip|form.xml.sig, File.doc.sig, File.docx.sig, File.pdf.sig, File.xbrl.sig, File.xls.sig, File.xlsx.sig, File.xml.sig, File.xtdd.sig, File.zip.sig|Выполнить VPEPVV-T1081|Не требуется|script7.xml|ЮЛ. Файлы+sig|draft7.xml|
|4|Без файлов|form.xml.enc|form.xml.sig|Выполнить VPEPVV-T1081|Не требуется|script2.xml|ЮЛ. Без файлов|draft2.xml|
|5|File.doc, File.docx, File.pdf, File.xbrl, File.xls, File.xlsx, File.xml, File.xtdd, File.zip, File.doc.sig, File.docx.sig, File.pdf.sig, File.xbrl.sig, File.xls.sig, File.xlsx.sig, File.xml.sig, File.xtdd.sig, File.zip.sig|form.xml.enc, File.doc.enc, File.docx.enc, File.pdf.enc, File.xbrl.enc, File.xls.enc, File.xlsx.enc, File.xml.enc, File.xtdd.enc, File.zip.enc|form.xml.sig, File.doc.sig, File.docx.sig, File.pdf.sig, File.xbrl.sig, File.xls.sig, File.xlsx.sig, File.xml.sig, File.xtdd.sig, File.zip.sig|Выполнить VPEPVV-T1082|Выполнить VPEPVV-T1076|script4.xml|ФЛ+МЧД. Файлы+sig|draft4.xml|
|6|File.doc.enc, File.doc.sig, File.docx.enc, File.docx.sig, File.pdf.enc, File.pdf.sig, File.xbrl.enc, File.xbrl.sig, File.xls.enc, File.xls.sig, File.xlsx.enc, File.xlsx.sig, File.xml.enc, File.xml.sig, File.xtdd.enc, File.xtdd.sig, File.zip.enc, File.zip.sig|form.xml.enc, File.doc.enc, File.docx.enc, File.pdf.enc, File.xbrl.enc, File.xls.enc, File.xlsx.enc, File.xml.enc, File.xtdd.enc, File.zip.enc|form.xml.sig, File.doc.sig, File.docx.sig, File.pdf.sig, File.xbrl.sig, File.xls.sig, File.xlsx.sig, File.xml.sig, File.xtdd.sig, File.zip.sig|Выполнить VPEPVV-T1082|Выполнить VPEPVV-T1076|script1.xml|ФЛ+МЧД. enc+sig|draft1.xml|
|7|File.doc, File.docx, File.pdf, File.xbrl, File.xls, File.xlsx, File.xml, File.xtdd, File.zip|form.xml, File.doc, File.docx, File.pdf, File.xbrl, File.xls, File.xlsx, File.xml, File.xtdd, File.zip|form.xml.sig, File.doc.sig, File.docx.sig, File.pdf.sig, File.xbrl.sig, File.xls.sig, File.xlsx.sig, File.xml.sig, File.xtdd.sig, File.zip.sig|Выполнить VPEPVV-T1082|Выполнить VPEPVV-T1076|script6.xml|ФЛ+МЧД. Файлы|draft6.xml|
|8|Password.zip|form.xml, Password.zip|form.xml.sig, Password.zip.sig|Выполнить VPEPVV-T1081|Не требуется|script8.xml|ЮЛ. Запароленный архив+ подпись|draft8.xml|

### Шаги
|№|наименование|вложения|тестовые данные|ожидаемый результат|
|-|-|-|-|-|
|1|Выполнить тест id = 116131||||
|2|Заполнить форму "Обращение (запрос) в Банк России" согласно тестовым данным||<u>Для РТ:</u><br />Загрузить черновик: {Черновик}   <br /><br /><u>Для АТ:</u><br />▶ Адресат:<br />Адресат первого уровня: {Эталон} .subject.level_1<br />Адресат второго уровня: {Эталон} .subject.level_2<br />Адресат третьего уровня: {Эталон} .subject.Addressee<br /><br /><br />Ограничение доступа к пакету документа: {Эталон}.doc_flag (0-Стандартная передача пакета; 1-Шифрование пакета)<br /><br />Рубрика: {Эталон}.doc_rubric.rubric_name (если нода отсутствует, то рубрика не добавляется)<br /><br />Сопроводительное письмо: {Эталон} .doc_text<br /><br />▶ Подписант<br /> ФИО: {Эталон} .org_official.name<br /> Должность: {Эталон} .org_official.post<br /> Замещение: {Эталон} .org_official.replace (Да - флажок в чекбокс ставится, Нет - флажок не ставится)<br /><br />▶ Исполнитель <br /> ФИО: {Эталон} .doc_writer.name<br /> Должность: {Эталон} .doc_writer.post<br /> Контактный номер телефона: {Эталон} .doc_writer.phone<br /><br />▶ Исходящий документ<br />Номер: {Эталон} .doc_out.Number<br />Дата: {Эталон} .doc_out.Date<br /> <br />Сертификат: {Сертификат} <br /><br />Доверенности: {Доверенность}|Форма заполнена в соответствии с тестовыми данными.|
|3|Добавить в форму файлы в соответствии с тестовыми данными, для этого выполнить любое из следующих действий:<ul><li>Перетащить указанные файлы в поле перетаскивания файлов (dropdown-space)</li><li>Нажать на кнопку "Выберите файл для добавления", в появившемся окне проводника выбрать требуемые файлы и нажать на кнопку "Открыть"</li></ul>||{Файлы}|Под заголовком "Добавленные файлы" появились контейнеры содержащие имя файла и его размер|
|4|В форме "Обращение (запрос) в Банк России" нажать на кнопку "Отправить"|||Отображено сообщение "Все данные успешно загружены. Отправка продолжится в фоновом режиме"|
|5|Перейти в раздел "История взаимодействия"|||В таблице присутствует запись (далее - ЭС1) со следующими значениями:<br />1.Значок исходящего сообщения:<br /><img src="https://jira.cbr.ru/rest/tests/1.0/attachment/image/519884" class="fr-fic fr-dii" /><br />2. Тема сообщения: "Обращение (запрос) в Банк России"<br />3. Текст сообщения: {Эталон} .doc_text<br />4. Регистрационный номер: &lt;прочерк&gt;<br />5. Исходящий номер: {Эталон} .doc_out.Number<br />6. Дата исходящего документа: {Эталон}.doc_out.Date<br />7. Дата получения регистрационного номера: "Не указано"<br />8. Дата последнего сообщения: &lt;прочерк&gt;<br />9. Дата создания: &lt;Дата в формате "dd.mm.yyyy hh:mi" со временем отправки ЭС&gt;<br />10. Размер/Признак вложения файла: &lt;указан размер и скрепка&gt;<br />11. Статус: &lt;заполнено одним из значений: Отправлено, Загружено, Принято в обработку, Отклонено, Зарегистрировано&gt;<br />12. Статус последнего сообщения:&lt;картинка "вопрос в круге" и прочерк&gt;<br />13. Периодичность, Шифр формы: &lt;прочерк&gt;|
|6|В разделе "История взаимодействия" дождаться изменения статуса до "Принято в обработку", "Отклонено" или "Зарегистрировано" у ЭС1.<br /><br />Для АТ<br />дожидаться можно при помощи GET-запроса::<br />https://portal5test.cbr.ru/back/rapi2/messages/&lt;DPID&gt;<br />, где:<br />DPID - значение атрибута data-id у строки ЭС1|||У ЭС1 отображается один из статусов: "Принято в обработку", "Отклонено" или "Зарегистрировано"|
|7|В разделе "История взаимодействия" кликнуть на значение поля "Тема сообщения" у ЭС1|||Открылось окно "Обращение (запрос) в Банк России" в котором:<ul><li>Отображается комментарий: {Эталон} .doc_text</li><li>Отображена цепочка статусов: "Отправлено-Загружено-Принято в обработку" слева от которых заполнена дата и время в формате "dd.mm.yyyy hh24:mi". Значение времени (поле StatusTime) можно получиться из GET-запроса:<ul><li>https://portal5test.cbr.ru/back/rapi2/messages/&lt;DPID&gt;</li></ul></li></ul>|
|8|В окне "Обращение (запрос) в Банк России" перейти на вкладку "Информационные файлы"|||На вкладке "Информационные файлы" присутствуют следующие файлы (в блоке каждого файла состоит из следующей частей: иконка типа расширения, имя файла, размер файла, текст: "Инфо..."):<ul><li>form.xml (если {Эталон} .doc_flag =0 (Стандартная передача пакета) или form.xml.enc, если .doc_flag =1 (Шифрование пакета))</li><li>{Информационные_файлы</li></ul><br />*Порядок может отличаться|
|9|Скачать со вкладки "Информационные файлы" все файлы<br /><br />|||Успешно скачаны файлы:<br /><ul><li>{Информационные файлы} </li></ul><br />|
|10|Проверить на соответствие эталонам следующие файлы: {Информационные файлы}|||<ul><li>Файл form.xml соответствует {Эталон}</li><li>Остальные файлы соответствуют добавленным на шаге 3 ( {Файлы} , кроме *.sig )</li></ul><br />* При необходимости, зашифрованные файлы предварительно расшифровать|
|11|В окне "Обращение (запрос) в Банк России" перейти на вкладку "Файлы подписей"|||На вкладке "Файлы подписей" присутствуют следующие файлы (в блоке каждого файла состоит из следующей частей: иконка типа расширения, имя файла, размер файла, текст: "Инфо..."):<br /><ul><li>{Файлы подписей} </li></ul><br />*Порядок может отличаться|
|12|Скачать со вкладки "Файлы подписей" все файлы <br /><br />|||Успешно скачаны файлы:<br /><ul><li>{Файлы подписей} </li></ul><br />|
|13|Проверить скаченные sig-файлы на соответствие эталонам|||<ul><li>Файл form.xml.sig содержит подпись из {Сертификат}</li><li>Остальные sig-файлы соответствуют sig-файлам из следующих: {Файлы} , если в этом списке sig-файлы отсутствуют, то проверить скаченные sig-файлы на содержание подписи из {Сертификат} </li></ul>|
|14|Выполнить открытие формы просмотра файла form.xml, для этого:<ol><li>В окне "Обращение (запрос) в Банк России" перейти на вкладку "Информационные файлы"</li><li>Выполнить клик по файлу form.xml</li><li>В появившемся выпадающем меню выбрать "Просмотреть"</li></ol><br />*Данная проверка выполняется только если была "Стандартная передача пакета", т.е. {Эталон} .doc_flag =0|||Открыта форма "Обращение (запрос) в Банк России" в режиме чтения (поля недоступны для редактирования)<br />Форма заполнена в соответствии с {Эталон}|

### Вложения
- 814083(script8.xml)
- 793287(script1.xml)
- 793290(script4.xml)
- 793291(script5.xml)
- 793282(File.doc)
- 814084(draft8.xml)
- 793289(all-all.rar)
- 793292(script7.xml)
- 814070(Password.zip.sig)
- 793279(File.pdf)
- 793280(File.zip)
- 793281(File.docx)
- 793285(File.xbrl)
- 793295(draft1.xml)
- 793297(draft7.xml)
- 793296(draft2.xml)
- 793298(draft3.xml)
- 793283(File.xtdd)
- 793299(draft4.xml)
- 793277(script2.xml)
- 793278(File.xml)
- 793284(File.xlsx)
- 793286(File.xls)
- 793288(script3.xml)
- 793293(script6.xml)
- 793300(draft5.xml)
- 793301(draft6.xml)
- 814069(Password.zip)

### Выполнения
|Дата|Статус|Исполнитель|Прогон|Ключ|automated|
|-|-|-|-|-|-|
|2021-09-18T09:10:59.508Z|Pass|40kolmakovmi|VPEPVV-C805|VPEPVV-E5704|false|
|2021-11-30T14:47:39.388Z|Fail|40kolmakovmi|VPEPVV-C936|VPEPVV-E6488|false|
|2023-06-30T09:55:48.372Z|Fail|JIRAUSER98745|VPEPVV-C2109|VPEPVV-E13217|false|
|2023-06-26T11:41:10.679Z|Fail|JIRAUSER98745|VPEPVV-C2110|VPEPVV-E13241|false|
|2022-03-30T12:40:11.039Z|Pass|40aleynikovov|VPEPVV-C1040|VPEPVV-E7070|false|
|2022-05-11T13:48:58.801Z|Fail|40shchelkanovoa|VPEPVV-C1042|VPEPVV-E7091|false|
||Not Executed||VPEPVV-C722|VPEPVV-E8110|false|
|2022-06-27T12:05:54.695Z|Pass|40kholmogorovaaa|VPEPVV-C1190|VPEPVV-E8112|false|
||Not Executed||VPEPVV-C1107|VPEPVV-E7539|false|
|2022-12-09T10:09:47.005Z|Pass|JIRAUSER98745|VPEPVV-C1667|VPEPVV-E10768|false|
|2023-02-22T12:07:12.275Z|Pass|JIRAUSER98745|VPEPVV-C1735|VPEPVV-E11196|false|
||Not Executed||VPEPVV-C1978|VPEPVV-E12560|false|
|2023-05-24T12:15:39.819Z|Fail|40shchelkanovoa|VPEPVV-C1997|VPEPVV-E12635|false|
|2021-12-11T11:01:05.317Z|Pass|40kolmakovmi|VPEPVV-C1013|VPEPVV-E6907|false|
||Not Executed||VPEPVV-C1311|VPEPVV-E8848|false|
|2022-09-05T10:54:14.291Z|Fail|40aleynikovov|VPEPVV-C1375|VPEPVV-E9265|false|
|2022-11-15T14:39:27.461Z|Pass|JIRAUSER98745|VPEPVV-C1513|VPEPVV-E9958|false|
||Not Executed||VPEPVV-C1446|VPEPVV-E9598|false|
|2022-11-23T12:58:15.760Z|Pass|JIRAUSER98745|VPEPVV-C1579|VPEPVV-E10292|false|
||Not Executed||VPEPVV-C1805|VPEPVV-E11579|false|
|2023-03-01T13:39:13.270Z|Pass|JIRAUSER98745|VPEPVV-C1901|VPEPVV-E12105|false|
|2023-05-29T07:27:48.533Z|Fail|JIRAUSER98745|VPEPVV-C2086|VPEPVV-E13082|false|
|2023-06-05T10:28:33.039Z|Pass|40aleynikovov|VPEPVV-C2087|VPEPVV-E13102|false|
|2023-06-22T14:56:04.961Z|Blocked|40andronovda|VPEPVV-C2103|VPEPVV-E13170|false|
||Not Executed||VPEPVV-C2205|VPEPVV-E13704|false|
|2023-07-25T07:11:07.533Z|Blocked|40nikonovaev|VPEPVV-C2188|VPEPVV-E13628|false|
||Not Executed||VPEPVV-C2163|VPEPVV-E13579|false|
||Not Executed||VPEPVV-C2206|VPEPVV-E13711|false|
|2023-08-01T12:27:47.310Z|Fail|40nikonovaev|VPEPVV-C2188|VPEPVV-E13745|false|
|2023-09-11T14:11:42.590Z|Blocked|40andronovda|VPEPVV-C2263|VPEPVV-E14071|false|
|2023-09-11T12:16:00.252Z|Fail|JIRAUSER98745|VPEPVV-C2264|VPEPVV-E14088|false|
|2023-08-21T07:32:51.998Z|Fail|JIRAUSER98745|VPEPVV-C2219|VPEPVV-E13809|false|
|2023-08-21T05:42:48.710Z|Fail|JIRAUSER98745|VPEPVV-C2220|VPEPVV-E13839|false|
||Not Executed||VPEPVV-C2868|VPEPVV-E17497|false|
|2023-09-22T07:31:49.707Z|Blocked|40andronovda|VPEPVV-C2356|VPEPVV-E14553|false|
|2023-09-20T13:30:39.941Z|Blocked|JIRAUSER98745|VPEPVV-C2357|VPEPVV-E14585|false|
|2023-09-28T11:34:39.437Z|Pass|40andronovda|VPEPVV-C2398|VPEPVV-E14688|false|
|2023-09-27T11:16:35.176Z|Pass|JIRAUSER98745|VPEPVV-C2399|VPEPVV-E14711|false|
|2024-01-29T11:27:36.793Z|Fail|40trofimovva|VPEPVV-C2465|VPEPVV-E15434|false|
|2024-04-16T11:12:21.546Z|Протестировано с замечаниями|40toropyninav|VPEPVV-C2777|VPEPVV-E17066|false|
|2024-02-29T14:51:17.674Z|Pass|40shchelkanovoa|VPEPVV-C2570|VPEPVV-E16044|false|
||Not Executed||VPEPVV-C2688|VPEPVV-E16646|false|
|2024-03-15T13:15:54.649Z|Pass|40konfetkinaiv|VPEPVV-C2618|VPEPVV-E16303|false|
|2024-08-02T08:07:17.431Z|Pass|JIRAUSER98745|VPEPVV-C2929|VPEPVV-E17806|false|
|2024-07-22T07:18:55.153Z|Fail|40skorikev|VPEPVV-C2933|VPEPVV-E17858|false|
|2025-01-27T08:38:33.743Z|Fail|JIRAUSER108830|VPEPVV-C3299|VPEPVV-E19327|false|
||Fail||VPEPVV-C3577|VPEPVV-E20689|false|
|2024-12-18T14:38:48.809Z|Fail|40andronovda|VPEPVV-C3210|VPEPVV-E18961|false|
||Not Executed||VPEPVV-C3401|VPEPVV-E19812|false|
|2025-04-04T10:50:45.247Z|Fail|JIRAUSER108830|VPEPVV-C3473|VPEPVV-E20137|false|
|2025-07-23T09:00:56.306Z|Pass|JIRAUSER108830|VPEPVV-C3822|VPEPVV-E21858|false|
|2025-06-27T08:39:24.726Z|Fail|40andronovda|VPEPVV-C3701|VPEPVV-E21257|false|
||Fail||VPEPVV-C4503|VPEPVV-E24676|false|
|2025-09-18T12:04:54.822Z|Fail|40zimatskovoe|VPEPVV-C4151|VPEPVV-E23461|false|
|2025-08-20T13:59:19.778Z|Fail|40zimatskovoe|VPEPVV-C3976|VPEPVV-E22574|false|
||Fail||VPEPVV-C4092|VPEPVV-E23138|false|
|2025-10-03T13:43:39.962Z|Fail|40zimatskovoe|VPEPVV-C4273|VPEPVV-E23990|false|
||Not Executed||VPEPVV-C2186|VPEPVV-E22317|false|
|2025-10-17T07:47:09.430Z|Fail|40zimatskovoe|VPEPVV-C4368|VPEPVV-E24325|false|



# Вложенные тест-кейсы

## Тест-кейс 116131

### Подробнее
#### Ключ
- VPEPVV-T799
#### Версия
- 2
#### Наименование
- Переход к форме "Обращение (запрос) в Банк России"
#### Задача тест-кейса
- Проверить открытие формы "Обращение (запрос) в Банк России"
#### Предварительные действия
Тестирование проводится "Яндекс Браузер", так же необходимо установленное ПО:<br />КриптоПро ЭЦП Browser plug-in <br />КриптоАРМ<br />КриптоCSP<br /><br /><a href="http://jira.cbr.ru/secure/Tests.jspa#/testCase/VPEPVV-T163">ТК Вход в личный кабинет</a><br />Выполнен вход в ЛК КО под логином:<br />95UZD007702235133 (Оператор)<br />43AleiOV007702235133 (Администратор)

### Параметры
нет параметров

### Шаги
|№|наименование|вложения|тестовые данные|ожидаемый результат|
|-|-|-|-|-|
|1|Перейти на вкладку "Электронный документооборот"|||Открыт раздел "Электронный документооборот"|
|2|Перейти в подраздел "В Банк России"|||Открыт подраздел отправки запросов в Банк России.<br />Появилась кнопка "Отправить запрос"|
|3|Нажать на кнопку "Отправить запрос"|||Открыта форма "Обращение (запрос) в Банк России"|

### Вложения
- нет вложений

### Выполнения
- нет выполнений
