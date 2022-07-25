package tools;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static tools.CustomFilesTool.FileStatus.*;

/**
 * Класс для поиска файлов измененных в стандарте необходимых для подтягивания кастома
 * @author shcherbakov_d
 *         Date: 22.04.2022
 */
public class CustomFilesTool {

    /** Системный разделитель */
    private final static String SEP = File.separator;

    /** Команда для получения коммита сделанного раньше указанной даты */
    private static  final String GET_COMMIT_AFTER_DATE_COMMAND = "git log -1 --pretty=format:\"%cd\" --after=\"{0}\" {1}";

    /** Команда для получения последнего коммита */
    private static  final String GET_LAST_COMMIT_COMMAND = "git log -1 --pretty=format:\"%cd\" {0}";

    /** Путь к папке стандарта */
    private static String stdPath;

    /** Путь к папке кастома */
    private static String customPath;

    /** Возможные префиксы в названиях кастомных файлов, разделенные через точку с запятой */
    private static String[] listPrefix = new String[]{};

    /** Папка для сохранения результатов */
    private static String outPath;

    /** Флаг игнорирования документов прошивки */
    private static boolean skipFirmwareDocs;

    public static void main(String[] args) {
        stdPath = args[0];
        customPath = args[1];
        String moduleName = args[2];
        outPath = args[3];
        String customFullPath = customPath + (!".".equals(moduleName) ? SEP + moduleName : "");
        skipFirmwareDocs = Boolean.parseBoolean(args[4]);
        if (args.length > 5) {
            listPrefix = args[5].split(";");
        }

        List<String> customFiles = getFiles(customFullPath);
        Map<FileStatus, List<FileInfo>> groupingFiles = customFiles
                .stream()
                .map(CustomFilesTool::loadFileInfo)
                .collect(Collectors.groupingBy(fileInfo -> fileInfo.status));

        File dir = new File(outPath);
        if (dir.exists()) {
            deleteDirectory(dir);
        }
        // Копирование измененных файлов по папкам
        List<FileInfo> changed = groupingFiles.get(CHANGED);
        if (changed != null) {
            changed.forEach(file -> copyFiles(file, "changed"));
        }
        List<FileInfo> unChanged = groupingFiles.get(UN_CHANGED);
        if (unChanged != null) {
            unChanged.forEach(file -> copyFiles(file, "unchanged"));
        }

        // Вывод
        System.out.println(MessageFormat.format("Всего в папке {1} обработано файлов {0} ", customFiles.size(), customFullPath));
        groupingFiles.forEach((k, v) -> {
            System.out.println();
            System.out.println(MessageFormat.format("{0} {1}: ", k.getDescription() , v.size()));
            v.forEach(x -> System.out.println(getFileInfoPrintText(k, x)));
        });
    }

    private static String getFileInfoPrintText(FileStatus status, FileInfo fileInfo) {
        switch (status) {
            case CHANGED:
                return MessageFormat.format("[файл из кастома] = {0}{4}[дата последнего коммита в кастоме]={1}{4}" +
                        "[файл из стандарта] = {2}{4}[дата последнего коммита позже изменений в кастоме]={3}{4}",
                        fileInfo.customFileName, fileInfo.customCommitDate, fileInfo.stdFileName, fileInfo.stdCommitDate, System.lineSeparator());
            case UN_CHANGED:
                return MessageFormat.format("[файл из кастома] = {0}{3}[дата последнего коммита в кастоме]={1}{3}" +
                                "[файл из стандарта] = {2}{3}",
                        fileInfo.customFileName, fileInfo.customCommitDate, fileInfo.stdFileName, System.lineSeparator());
            default:
                return MessageFormat.format("[файл из кастома] = {0}", fileInfo.customFileName);
        }
    }

    /**
     * Копирует учитывая полный относительный путь файлы кастома и стандарта в папки out\custom std\custom
     * @param fileInfo информация о файле
     */
    private static void copyFiles(FileInfo fileInfo, String subDir) {
        Path customFilepath = Paths.get(fileInfo.customFileName);
        Path customOutPath = Paths.get(fileInfo.customFileName.replace(customPath, outPath + SEP + subDir + SEP + "custom"));

        Path stdFilepath = Paths.get(fileInfo.stdFileName);
        Path stdOutPath = Paths.get(fileInfo.customFileName.replace(customPath, outPath + SEP + subDir + SEP + "std"));

        try {
            createDirectories(customOutPath);
            Files.copy(customFilepath, customOutPath);

            createDirectories(stdOutPath);
            Files.copy(stdFilepath, stdOutPath);

        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Создает иерархию папок по пути файла
     * @param path путь файла
     */
    private static void createDirectories(Path path) {
        List<Path> pathList = new ArrayList<>();
        while (path.getParent() != null) {
            pathList.add(path.getParent());
            path = path.getParent();
        }
        for (int i = pathList.size() - 1; i >= 0 ; i--) {
            File dir = pathList.get(i).toFile();
            if (!dir.exists()) {
                dir.mkdir();
            }
        }
    }

    /**
     * Удаляет папку со всем ее содержим
     * @param path путь к папке
     */
    private static void deleteDirectory(File path) {
        if (path.isDirectory()) {
            for (File file : path.listFiles()) {
                if (file.isDirectory()) {
                    deleteDirectory(file);
                }
                else {
                    if (!file.delete()) {
                        throw new RuntimeException(MessageFormat.format("Ошибка при попытке удалить файл {0}", file));
                    }
                }
            }
        }
        if (!path.delete()) {
            throw new RuntimeException(MessageFormat.format("Ошибка при попытке удалить папку {0}", path));
        }
   }

    /**
     * Получает список файлов по указанному пути
     * @param path путь
     * @return список файлов
     */
    private static List<String> getFiles(String path) {
        try {
            return Files.walk(Paths.get(path))
                    .filter(Files::isRegularFile)
                    .map(Path::toString)
                    .filter(CustomFilesTool::isProjectFile)
                    .collect(Collectors.toList());
        } catch (IOException e) {
            throw new RuntimeException("Ошибка при получении списка файлов из " + path, e);
        }
    }

    /**
     * Возвращает признак того, что это используемый в проекте файл
     * @param fileName имя файла
     * @return признак того, что это используемый в проекте файл
     */
    private static boolean isProjectFile(String fileName) {
        return !(fileName.contains(".gitignore") ||
                fileName.contains(SEP + ".git" + SEP) ||
                fileName.contains(SEP + ".idea" + SEP) ||
                fileName.contains(SEP + "target" + SEP) ||
                fileName.contains(SEP + "node" + SEP) ||
                fileName.contains(SEP + "node_modules" + SEP) ||
                fileName.contains(SEP + ".flattened-pom-") ||
                fileName.endsWith(".iml"));
    }

    /**
     * Получает имя файла из стандарта по имени файла из кастома
     * @param customFileName имя файла из кастома
     * @return имя файла из стандарта
     */
    private static String getStdFileNameByCustomFileName(String customFileName) {
        // Замена пути к папке стандарта на путь к папке кастома
        String file = customFileName.replace(customPath, stdPath);
        if (new File(file).isFile()) {
            return file;
        }

        String[] fileNameParts = file.split(Pattern.quote(File.separator));
        for (int i = 0; i < fileNameParts.length ; i++) {
            // Удаление префикс из имени файла
            if (i == fileNameParts.length - 1) {
                for (String prefirx : listPrefix) {
                    String part = fileNameParts[i];
                    fileNameParts[i] = fileNameParts[i].replace(prefirx, "");
                    file = String.join(SEP, fileNameParts);
                    if (new File(file).isFile()) {
                        return file;
                    } else {
                        fileNameParts[i] = part;
                    }
                }
            }

            // Замена названий папок в прошивке ...\firmware\src\*
            if (customFileName.contains("firmware") && i >= 2 && "src".equals(fileNameParts[i - 1]) && "firmware".equals(fileNameParts[i - 2])) {
                if (fileNameParts[i].contains("2ts")) {
                    fileNameParts[i] = "standart2ts";
                }
                if (fileNameParts[i].contains("-")) {
                    fileNameParts[i] = fileNameParts[i].split("-")[1];
                }
            }
        }
        file = String.join(SEP, fileNameParts);
        if (new File(file).isFile()) {
            return file;
        }
        return null;
    }

    /**
     * Выполнить команду
     * @param cmd  команда
     * @param path рабочая директория
     * @return строка с результатом выполнения команды
     */
    private static String execCmd(String cmd, String path) {
        try {
            Runtime rt = Runtime.getRuntime();
            Process process = rt.exec(cmd, null, new File(path));
            InputStream is = process.getInputStream();
            InputStreamReader isr = new InputStreamReader(is);
            BufferedReader br = new BufferedReader(isr);
            return br.readLine();
        } catch (IOException e) {
            throw new RuntimeException(MessageFormat.format("Ошибка при выполнении команды {0} из папки {1}", cmd, path), e);
        }
    }

    /**
     * Проверка файла на то, что его нужно игнорировать
     * @param fileName имя файла
     * @return признак игнорируемого файла
     */
    private static boolean checkIgnoreStatus(String fileName) {
        return fileName.contains(MessageFormat.format("2ts{0}docs{0}currency_accreditiv{0}", SEP)) ||
                (skipFirmwareDocs && fileName.contains(MessageFormat.format("2ts{0}docs{0}", SEP)));
    }

    /**
     * Загружает информацию о файле
     * @param customFileName имя файла из кстома
     * @return информация о файле
     */
    private static FileInfo loadFileInfo(String customFileName) {
        log(MessageFormat.format("Обработка файла {0}", customFileName));
        if (checkIgnoreStatus(customFileName)) {
            return new FileInfo(customFileName, IGNORED);
        }
        String stdFileName = getStdFileNameByCustomFileName(customFileName);
        if (stdFileName != null) {
            // Получение для файла из кастома даты последнего коммита
            String customCommitDate = execCmd(MessageFormat.format(GET_LAST_COMMIT_COMMAND, customFileName), customPath);
            // Получение для файла из стандарта даты последнего коммита сделанного позже даты коммита из кастома
            String stdCommitDate = execCmd(MessageFormat.format(GET_COMMIT_AFTER_DATE_COMMAND, customCommitDate, stdFileName), stdPath);
            return new FileInfo(customFileName, customCommitDate, stdFileName, stdCommitDate, stdCommitDate != null ? CHANGED : UN_CHANGED);
        }
        return new FileInfo(customFileName, NOT_FOUND);
    }

    /**
     * Логирование
     * @param logMessage сообщение
     */
    private static void log(String logMessage) {
        System.out.println(logMessage);
    }

    /** Информация о файле */
    private static class FileInfo {

        /** Имя файла кастома */
        private final String customFileName;

        /** Дата коммита кастома */
        private final String customCommitDate;

        /** Имя файла стандарта */
        private final String stdFileName;

        /** Дата коммита стандарта */
        private final String stdCommitDate;

        /** Статус */
        private final FileStatus status;

        /**
         * Конструктор
         * @param customFileName имя файла кастома
         * @param status         статус
         */
        public FileInfo(String customFileName, FileStatus status) {
            this(customFileName, null, null, null, status);
        }

        /**
         * Конструктор
         * @param customFileName   имя файла кастома
         * @param customCommitDate дата коммита кастома
         * @param stdFileName      имя файла стандарта
         * @param stdCommitDate    дата коммита стандарта
         * @param status           статус
         */
        public FileInfo(String customFileName, String customCommitDate, String stdFileName, String stdCommitDate, FileStatus status) {
            this.customFileName = customFileName;
            this.customCommitDate = customCommitDate;
            this.stdFileName = stdFileName;
            this.stdCommitDate = stdCommitDate;
            this.status = status;
        }
    }

    /** Перечисление статусов файлов */
    enum FileStatus {

        /** Измененные */
        CHANGED("Измененные"),

        /** Не измененные */
        UN_CHANGED("Не измененные"),

        /** Не найденные */
        NOT_FOUND("Не найденные"),

        /** Игнорируемые */
        IGNORED("Игнорируемые");

        /** Описание */
        private final String description;

        /**
         * Конструктор
         * @param description описание
         */
        FileStatus(String description) {
            this.description = description;
        }

        /**
         * Возвращает описание
         * @return описание
         */
        public String getDescription() {
            return description;
        }
    }
}
