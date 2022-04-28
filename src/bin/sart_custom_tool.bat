@Echo Off

rem ===========================================================================================
rem ������� ��� ���������� �������� ������������ ������� �� ���������.
rem ��������� �� ������ � ������� � ��������� ���� �� �� ��� ����� ������� ������� � ���������.
rem �� ������ �������� ����� � ����� out �������� �� ���������:
rem "changed\std" - ����� �� ���������, � ������� ���� ������� ������� �������� �������
rem "changed\custom" - ����� �� �������, �������������� ������ �� ����� "changed\std"
rem "unchanged\std" - ����� �� ���������, � ������� ��� �������� ������� �������� �������
rem "unchanged\custom" - ����� �� �������, �������������� ������ �� ����� "unchanged\std"
rem ============================================================================================

rem ���� � ����� ���������
set STD_PATH=C:\work\ibank

rem ���� � ����� �������
set CUSTOM_PATH=C:\work\custom_absolutbank

rem ��� ������ - ��� ������ �� ���� ������� MODULE_NAME=.
set MODULE_NAME=firmware

rem ���� ������������� ���������� �������� - �������� true/false
set SKIP_FIRMWARE_DOCS=true

rem ������ ��������� � ����� ����� �������, ����������� ����� ����� � �������.
rem �������������� ��������.
rem ��������� ��� ������������ ������ ��������� � �������.
rem �������� ������� Absolut ��� CurrencyPaymentContentPrepareHandler.java -> AbsolutBankCurrencyPaymentContentPrepareHandler.java
set LIST_PREFIX=Absolut;Absolute;AbsolutBank

set CUSTOM_TOOL_HOME=..
set LIB_DIR=%CUSTOM_TOOL_HOME%\lib
set CP_OPTS=-Dibank.jars.path="%LIB_DIR%" -Xbootclasspath/a:%LIB_DIR%\bootstrap.jar -Djava.system.class.loader=com.bifit.ibank.util.Bootstrap
set OUT_PATH=%CUSTOM_TOOL_HOME%\out

java -Xmx256m -Dibank.root=%CUSTOM_TOOL_HOME% %CP_OPTS% tools.CustomFilesTool %STD_PATH% %CUSTOM_PATH% %MODULE_NAME% %OUT_PATH% %SKIP_FIRMWARE_DOCS% %LIST_PREFIX%