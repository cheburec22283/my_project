import unittest
import os
import shutil
import tempfile
import yaml
import zipfile
from emulator import ShellEmulator


class TestShellEmulator(unittest.TestCase):
    def setUp(self):
        """Создаем временную виртуальную файловую систему и конфигурацию для тестов."""
        # Создаем временный каталог для тестов
        self.test_dir = tempfile.mkdtemp()
        self.virtual_fs_path = os.path.join(self.test_dir, "virtual_fs.zip")
        self.log_file_path = os.path.join(self.test_dir, "log.xml")

        # Создаем виртуальную файловую систему
        self.virtual_fs_content = {
            "dir1": {"file1.txt": "Hello, World!", "file2.txt": "Test file"},
            "dir2": {"file3.txt": "Another file"},
        }
        self.create_virtual_fs(self.virtual_fs_content)

        # Создаем конфигурационный файл
        self.config_file = os.path.join(self.test_dir, "config.yaml")
        config = {
            "username": "test_user",
            "virtual_fs_path": self.virtual_fs_path,
            "log_file_path": self.log_file_path,
        }
        with open(self.config_file, "w") as f:
            yaml.dump(config, f)

        # Инициализируем эмулятор
        self.emulator = ShellEmulator(self.config_file)

    def tearDown(self):
        """Удаляем временные файлы после тестов."""
        shutil.rmtree(self.test_dir)

    def create_virtual_fs(self, content):
        """Создает zip-архив с виртуальной файловой системой."""
        temp_fs_dir = tempfile.mkdtemp()  # Временный каталог для виртуальной ФС
        try:
            for folder, files in content.items():
                folder_path = os.path.join(temp_fs_dir, folder)
                os.makedirs(folder_path, exist_ok=True)  # Создаем директорию

                for file_name, file_content in files.items():
                    file_path = os.path.join(folder_path, file_name)
                    with open(file_path, "w") as f:
                        f.write(file_content)  # Записываем содержимое файлов

            # Упаковываем в ZIP-архив
            with zipfile.ZipFile(self.virtual_fs_path, "w") as zipf:
                for root, dirs, files in os.walk(temp_fs_dir):
                    for file in files:
                        abs_path = os.path.join(root, file)
                        arcname = os.path.relpath(abs_path, temp_fs_dir)
                        zipf.write(abs_path, arcname)
        finally:
            shutil.rmtree(temp_fs_dir)  # Удаляем временные файлы после упаковки


    def test_ls_command(self):
        """Проверяем команду ls."""
        self.emulator.current_path = "/"
        output = self.capture_output(self.emulator.list_files)
        self.assertIn("dir1", output)
        self.assertIn("dir2", output)

    def test_cd_command(self):
        """Проверяем команду cd."""
        self.emulator.change_directory("dir1")
        self.assertEqual(self.emulator.current_path, "/dir1")

        self.emulator.change_directory("..")
        self.assertEqual(self.emulator.current_path, "/")

        # Переход в несуществующую директорию
        self.emulator.change_directory("nonexistent")
        self.assertEqual(self.emulator.current_path, "/")

    def test_whoami_command(self):
        """Проверяем команду whoami."""
        output = self.capture_output(self.emulator.whoami)
        self.assertIn("test_user", output)

    def test_du_command(self):
        """Проверяем команду du."""
        output = self.capture_output(self.emulator.disk_usage)
        expected_size = 77
        self.assertIn(str(expected_size), output)

    def test_log_creation(self):
        """Проверяем создание файла логов."""
        self.assertTrue(os.path.exists(self.log_file_path))

    def capture_output(self, func, *args, **kwargs):
        """Перехватывает вывод функции."""
        from io import StringIO
        import sys

        old_stdout = sys.stdout
        sys.stdout = captured_output = StringIO()
        try:
            func(*args, **kwargs)
        finally:
            sys.stdout = old_stdout
        return captured_output.getvalue()


if __name__ == "__main__":
    unittest.main()
