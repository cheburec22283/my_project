# 1. Клонирование репозитория

Склонируйте репозиторий с исходным кодом и тестами:

```
git clone <URL репозитория>
cd <директория проекта>
```

# 2. Установка зависимостей при запуске

```
pip install subprocess

```

# Создайте виртуальное окружение

```bash
# Активируйте виртуальное окружение
python -m venv venv
# Для Windows:
venv\Scripts\activate
# Для MacOS/Linux:
source venv/bin/activate
```


# 3. Структура проекта
Проект содержит следующие файлы и директории:
```bash
unittests.py              # файл для тестирования
config.yaml             # конфигурационный файл 
hw2.py                  # файл с программой
Graphviz          # Graphviz для визуализации
```

# 4. Запуск проекта
```bash
py hw2.py config.yaml     # py название файла <файл с конфигом>
```


# 5. Тестирование с моим репозитеорием 
Вывод программы
```
digraph {
	rankdir=LR
	cf9886d615d0801e99f1c27c77e7db0c77c0bb67 [label="cf9886d
test.txt"]
	"15bb8d5eb138d2a03e08f065da959ea1319de79f" [label="15bb8d5
test.txt"]
}

```


