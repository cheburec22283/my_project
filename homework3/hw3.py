import sys
import re
import toml

def toml_to_custom_config(data):
    """
    Преобразует TOML-данные в текст формата учебного конфигурационного языка.
    """
    output = []
    constants = {}

    def format_value(value):
        if isinstance(value, str):
            return f'"{value}"'  # Строки
        elif isinstance(value, int) or isinstance(value, float):
            return str(value)  # Числа
        elif isinstance(value, dict):
            return dict_to_config(value)  # Словари
        else:
            raise ValueError(f"Неподдерживаемое значение: {value}")

    def dict_to_config(d):
        lines = ["{"]
        for key, value in d.items():
            if isinstance(value, str) and value.startswith("#(") and value.endswith(")"):
                const_name = value[2:-1]
                if const_name in constants:
                    value = constants[const_name]
                else:
                    raise ValueError(f"Константа '{const_name}' не определена.")
            lines.append(f"  {key} = {format_value(value)};")
        lines.append("}")
        return "\n".join(lines)

    def process_constants(constants_section):
        for key, value in constants_section.items():
            constants[key] = value
            output.append(f"const {key} = {format_value(value)}")

    # Сначала обрабатываем константы
    if "constants" in data:
        process_constants(data.pop("constants"))

    # Теперь обрабатываем остальной контент
    for key, value in data.items():
        if isinstance(value, dict):
            output.append(dict_to_config(value))
        else:
            raise ValueError(f"Неподдерживаемый верхнеуровневый элемент: {key}")

    return "\n".join(output)

def main():
    if len(sys.argv) != 2:
        print("Usage: python hw3.py <output_file>")
        sys.exit(1)

    output_file = sys.argv[1]

    # Чтение TOML-данных из стандартного ввода
    input_text = sys.stdin.read()

    try:
        # Парсинг TOML
        toml_data = toml.loads(input_text)
    except toml.TomlDecodeError as e:
        print(f"Ошибка парсинга TOML: {e}")
        sys.exit(1)

    try:
        # Преобразование TOML в учебный конфигурационный язык
        result = toml_to_custom_config(toml_data)
    except Exception as e:
        print(f"Ошибка преобразования: {e}")
        sys.exit(1)

    # Запись результата в файл
    with open(output_file, 'w') as f:
        f.write(result)

    print(f"Конфигурация преобразована и сохранена в {output_file}")

if __name__ == "__main__":
    main()
