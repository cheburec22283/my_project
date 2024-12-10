# Установка
1. Установка программы и переход в директорию
   ```bash
   git clone <URL репозитория>
   cd <директория проекта>
   ```
2. Создайте и активируйте виртуальное окружение:
   ```bash
   python3 -m venv venv
   source venv/bin/activate  # Для Linux/Mac
   venv\Scripts\activate     # Для Windows
   ```
3. Установите необходимые зависимости :
   ```bash
   Зависимости не требуются
   ```

# Запуск скрипта

Скрипт принимает текст Toml через стандартный ввод и выводит учебный конфигурационный яызке в файл.

```bash
Get-Content input.toml | py hw3.py output.txt
```

### Пример 
```
#ввод  TOML
[constants]
app_pi = 3.14
app_name = "MyApp"

[general]
name = "#(app_name)"
version = 1

[database]
host = "localhost"
port = 5432

[logging]
level = "info"

[math]
circle_area = "#(app_pi)"


#Вывод на УЧЯ

const app_pi = 3.14
const app_name = "MyApp"
{
  name = "MyApp";
  version = 1;
}
{
  host = "localhost";
  port = 5432;
}
{
  level = "info";
}
{
  circle_area = 3.14;
}

```


# Тесты

Шаги запуска тестов:
1. Установить библиотеку pytest (необходимо, если не сделано ранее):
   ```bash
   pip install pytest
   ```
   
2. Для запуска тестирования необходимо запустить следующий скрипт:
   ```shell
   py unittests.py
   ```

## Прохождение тестов:
![image](https://github.com/user-attachments/assets/785fcee7-2ab0-4fb0-84cd-f32518086fd0)
