import subprocess
import os
import yaml

def read_config(config_path):
    """Чтение конфигурационного файла YAML."""
    with open(config_path, 'r') as f:
        config = yaml.safe_load(f)
    return config

def get_commits_with_file(repo_path, target_file):
    """Получение коммитов, в которых фигурирует указанный файл."""
    result = subprocess.run(
        ['git', '-C', repo_path, 'log', '--name-only', '--pretty=format:%H', '--', target_file],
        stdout=subprocess.PIPE,
        stderr=subprocess.PIPE,
        text=True
    )

    if result.returncode != 0:
        print("Ошибка при выполнении команды git log:", result.stderr)
        return []

    lines = result.stdout.strip().split("\n")
    commits = []
    current_commit = None

    for line in lines:
        if line.strip() == "":
            continue
        if len(line) == 40:  # Если строка имеет длину хэша коммита
            current_commit = line
            commits.append({"hash": current_commit, "files": []})
        else:
            if current_commit:
                commits[-1]["files"].append(line)

    return commits

def get_parents(repo_path, commit_hash):
    """Получение родительских коммитов для заданного коммита."""
    result = subprocess.run(
        ['git', '-C', repo_path, 'rev-list', '--parents', '-n', '1', commit_hash],
        stdout=subprocess.PIPE,
        stderr=subprocess.PIPE,
        text=True
    )

    if result.returncode != 0:
        print(f"Ошибка при получении родителей для коммита {commit_hash}: {result.stderr}")
        return []

    parts = result.stdout.strip().split()
    return parts[1:]  # Первый элемент — это сам коммит, остальные — родители

def generate_dot_file(repo_path, commits, output_path):
    """Генерация файла в формате DOT."""
    commit_hashes = {commit['hash'] for commit in commits}  # Множество хэшей коммитов для быстрого поиска

    with open(output_path, 'w') as f:
        f.write("digraph G {\n")
        
        for commit in commits:
            commit_hash = commit['hash']
            short_hash = commit_hash[:8]
            label = f"{short_hash}\n" + "\n".join(commit["files"])
            f.write(f'  "{commit_hash}" [label="{label}"];\n')


        # Добавляем связи между родительскими коммитами
        for i in range(len(commits) - 1):
            current_commit = commits[i]['hash']
            next_commit = commits[i + 1]['hash']
            f.write(f'  "{next_commit}" -> "{current_commit}";\n')  # Добавляем связь от следующего к текущему
        
        f.write("}\n")


def visualize_graph(dot_file):
    """Отображение графа из файла формата DOT."""
    output_path = os.path.splitext(dot_file)[0] + '.png'
    
    # Используем команду dot для генерации изображения из файла DOT
    subprocess.run(['dot', '-Tpng', dot_file, '-o', output_path])
    
    print(f"Граф сохранен в файл: {output_path}")

def main(config_path):
    """Главная функция."""
    config = read_config(config_path)
    repo_path = config['repo_path']
    target_file = config['target_file']

    print(f"Анализируем репозиторий: {repo_path}")
    
    # Получаем коммиты с указанным файлом
    commits_with_file = get_commits_with_file(repo_path, target_file)

    if not commits_with_file:
        print("Не найдено коммитов с указанным файлом.")
        return


    # Генерируем файл в формате DOT
    dot_file_path = os.path.join(os.getcwd(), 'graph.dot')
    generate_dot_file(repo_path, commits_with_file, dot_file_path)

    # Визуализируем граф из файла DOT
    visualize_graph(dot_file_path)

if __name__ == "__main__":
    import sys
    if len(sys.argv) != 2:
        print("Использование: python script.py <config.yaml>")
        sys.exit(1)

    main(sys.argv[1])
