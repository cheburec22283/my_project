import os
import zipfile
import yaml
import xml.etree.ElementTree as ET
import sys
import argparse
import datetime

class ShellEmulator:
    def __init__(self, config_file):
        self.current_path = "/"
        self.history = []
        self.username = "user"
        self.computer_name = "localhost"
        self.virtual_fs_path = ""
        self.log_file_path = ""
        self.virtual_fs_dir = "virtual_fs"

        self.read_config(config_file)
        self.extract_virtual_fs()
        self.log_action("Эмулятор запущен")

    def read_config(self, config_file):
        """Load configuration from the provided YAML file."""
        try:
            with open(config_file, 'r') as file:
                config = yaml.safe_load(file)

            self.username = config.get("username", self.username)
            self.virtual_fs_path = config.get("virtual_fs_path", "")
            self.log_file_path = config.get("log_file_path", "")

            if not self.virtual_fs_path or not os.path.exists(self.virtual_fs_path):
                print("Ошибка: путь к виртуальной файловой системе некорректен.")
                sys.exit(1)

        except Exception as e:
            print(f"Ошибка чтения конфигурационного файла: {str(e)}")
            sys.exit(1)

    def extract_virtual_fs(self):
        """Extract the virtual file system from the zip archive."""
        if not os.path.exists(self.virtual_fs_dir):
            os.mkdir(self.virtual_fs_dir)

        with zipfile.ZipFile(self.virtual_fs_path, 'r') as zip_ref:
            zip_ref.extractall(self.virtual_fs_dir)

    def log_action(self, action):
        """Log an action with a timestamp in XML format."""
        timestamp = datetime.datetime.now().strftime("%Y-%m-%d %H:%M:%S")
        
        log_entry = ET.Element("log_entry")
        ET.SubElement(log_entry, "timestamp").text = timestamp
        ET.SubElement(log_entry, "action").text = action
        ET.SubElement(log_entry, "user").text = self.username

        if os.path.exists(self.log_file_path):
            tree = ET.parse(self.log_file_path)
            root = tree.getroot()
            root.append(log_entry)
            tree.write(self.log_file_path)
        else:
            root = ET.Element("log")
            root.append(log_entry)
            tree = ET.ElementTree(root)
            tree.write(self.log_file_path)

    def list_files(self):
        """Simulate the 'ls' command."""
        path = os.path.join(self.virtual_fs_dir, self.current_path.strip("/"))
        try:
            files = os.listdir(path)
            if files:
                print("\n".join(files))
            else:
                print("Пустая директория")
            self.log_action("ls")
        except FileNotFoundError:
            print("Директория не найдена")
            self.log_action("ls (ошибка: директория не найдена)")

    def change_directory(self, path):
        """Simulate the 'cd' command."""
        if path == "..":
            # Проверка, находимся ли мы в корневой директории
            if self.current_path == "" or self.current_path == "/":
                print("Вы не можете выйти за пределы виртуальной файловой системы.")
                self.log_action("cd .. (ошибка: выход из корневой директории запрещен)")
                return
            else:
                # Переход на уровень выше
                parts = self.current_path.split("/")
                parts.pop()  # Удаляем последний элемент (текущую директорию)
                self.current_path = "/".join(parts) or "/"  # Если ничего не осталось, устанавливаем в корень
                return

        # Формируем новый путь
        new_path = os.path.join(f"virtual_fs{self.current_path}", path)

        # Проверка существования директории
        if os.path.isdir(new_path):
            self.current_path = new_path.replace("virtual_fs", "")
            return
        else:
            print("Директория не найдена")
            self.log_action(f"cd {path} (ошибка: директория не найдена)")
    


    def whoami(self):
        """Simulate the 'whoami' command."""
        print(f"Пользователь: {self.username}")
        self.log_action("whoami")

    def disk_usage(self):
        """Simulate the 'du' command."""
        path = os.path.join(self.virtual_fs_dir, self.current_path.strip("/"))
        total_size = 0
        for dirpath, dirnames, filenames in os.walk(path):
            for f in filenames:
                fp = os.path.join(dirpath, f)
                total_size += os.path.getsize(fp)
        
        print(f"Используемое пространство: {total_size} байт")
        self.log_action("du")

    def exit_shell(self):
        """Exit the shell emulator."""
        self.log_action("Эмулятор завершен")
        print("Выход из эмулятора.")
        sys.exit(0)

    def execute_command(self, command):
        """Parse and execute commands."""
        self.history.append(command)
        parts = command.split()
        
        if not parts:
            return
        
        cmd = parts[0]
        
        if cmd == "ls":
            self.list_files()
        elif cmd == "cd":
            if len(parts) > 1:
                self.change_directory(parts[1])
            else:
                print("Нужен аргумент для команды cd.")
                
        elif cmd == "whoami":
            self.whoami()
            
        elif cmd == "du":
            self.disk_usage()
            
        elif cmd == "exit":
            self.exit_shell()
            
        else:
            print(f"{self.username}: команда не найдена")
            self.log_action(f"{cmd} (неизвестная команда)")

if __name__ == "__main__":
    parser = argparse.ArgumentParser(description="Запуск эмулятора командной строки.")
    parser.add_argument("config", type=str, help="Путь к конфигурационному файлу.")
    args = parser.parse_args()

    emulator = ShellEmulator(args.config)

    while True:
        try:
            command = input(f"{emulator.username}@{emulator.computer_name}:{emulator.current_path} $ ")
            emulator.execute_command(command.strip())
        except EOFError:
            emulator.exit_shell()