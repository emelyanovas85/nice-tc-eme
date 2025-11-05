package at.nice.tc.utils;



import org.apache.commons.configuration2.Configuration;
import org.apache.commons.configuration2.PropertiesConfiguration;
import org.apache.commons.configuration2.builder.fluent.Configurations;

public class ApacheConfig {
    public static void main(String[] args) {
        Configurations configs = new Configurations();
        try {
            // Загрузка конфигурации из файла config.properties
            Configuration config = configs.properties("config.properties");

            // Чтение значения по ключу
            String value = config.getString("some.key");
            System.out.println("Value: " + value);

            // Установка/изменение значения
            config.setProperty("another.key", "newValue");

            // Интерполяция переменных (если в конфиге есть ссылки на другие ключи)
            String interpolated = config.getString("interpolated.key");
            System.out.println("Interpolated: " + interpolated);

            // Удаление свойства
            config.clearProperty("some.key");

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
