@Echo Off

rem ===========================================================================================
rem Утилита для облегчения процесса подтягивания кастома до стандарта.
rem Пробегает по файлам в кастоме и проверяет были ли по ним более поздние коммиты в стандарте.
rem На выходе копирует файлы в папку out сортируя по подпапкам:
rem "changed\std" - файлы из стандарта, в которых есть коммиты позднее коммитов кастома
rem "changed\custom" - файлы из кастома, соответсвующие файлам из папки "changed\std"
rem "unchanged\std" - файлы из стандарта, в которых нет коммитов позднее коммитов кастома
rem "unchanged\custom" - файлы из кастома, соответсвующие файлам из папки "unchanged\std"
rem ============================================================================================

rem Путь к папке стандарта
set STD_PATH=C:\work\ibank

rem Путь к папке кастома
set CUSTOM_PATH=C:\work\custom_absolutbank

rem Имя модуля - для работы по всем модулям MODULE_NAME=.
set MODULE_NAME=import_core

rem Флаг игнорирования документов прошивки - значения true/false
set SKIP_FIRMWARE_DOCS=true

rem Список префиксов к имени файла кастома, разделенных через точку с запятой.
rem Необязательный параметр.
rem Необходим для сопставления файлов стандарта и кастома.
rem Например префикс Absolut для CurrencyPaymentContentPrepareHandler.java -> AbsolutBankCurrencyPaymentContentPrepareHandler.java
set LIST_PREFIX=Absolut;Absolute;AbsolutBank

set CUSTOM_TOOL_HOME=..
set LIB_DIR=%CUSTOM_TOOL_HOME%\lib
set CP_OPTS=-Dibank.jars.path="%LIB_DIR%" -Xbootclasspath/a:%LIB_DIR%\bootstrap.jar -Djava.system.class.loader=com.bifit.ibank.util.Bootstrap
set OUT_PATH=%CUSTOM_TOOL_HOME%\out

java -Xmx256m -Dibank.root=%CUSTOM_TOOL_HOME% %CP_OPTS% tools.CustomFilesTool %STD_PATH% %CUSTOM_PATH% %MODULE_NAME% %OUT_PATH% %SKIP_FIRMWARE_DOCS% %LIST_PREFIX%