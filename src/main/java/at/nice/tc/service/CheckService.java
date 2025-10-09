package at.nice.tc.service;

import at.nice.tc.dto.CheckDTO;
import at.nice.tc.utils.LocalStorage;
import at.nice.tc.utils.ThrowableUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.Validate;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Сервис для работы с проверками.
 * Все проверки хранятся в проекте (/checks) для версионирования гитом
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class CheckService {

    private static final LocalStorage LOCAL_STORAGE = new LocalStorage(Paths.get("checks"));

    private static String key(CheckDTO check) {
        return check.getType() + "~~" + check.getId();
    }


    public List<CheckDTO> getAllChecks() {
        try (Stream<Path> files = Files.list(LOCAL_STORAGE.getRoot())) {
            String extension = ".json";
            return files.filter(Files::isRegularFile)
                    .filter(path -> path.toFile().getName().toLowerCase().endsWith(extension))
                    .map(Path::getFileName)
                    .map(Path::toString)
                    .map(name -> name.substring(0, name.length() - (extension.length())))
                    .map(this::readFromLocalStorage)
                    .collect(Collectors.toList());
        } catch (IOException e) {
           return ThrowableUtils.reThrow(e);
        }
    }

    private CheckDTO readFromLocalStorage(String key) {
        return LOCAL_STORAGE.getSaved(key).as(CheckDTO.class);
    }

    public void updateCheck(String id, String newPrompt) {
        Validate.notNull(newPrompt, "Значение промпта не может быть null");
        Validate.notBlank(newPrompt, "Значение промпта не может быть пустым");
        List<CheckDTO> allChecks = getAllChecks();
        CheckDTO check = allChecks.stream()
                .filter(che -> che.getId().equals(id))
                .findAny()
                .orElseThrow(() -> new RuntimeException(
                        "Не найдена проверка с id '" + id + "'.\nДоступны проверки: " +
                                allChecks.stream().map(CheckDTO::getId).collect(Collectors.toList())
                ));
        check.setPrompt(newPrompt);
        LOCAL_STORAGE.saveAs(key(check), check);
    }

}
