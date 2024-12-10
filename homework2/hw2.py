import subprocess
import os
import yaml
from graphviz import Digraph

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

def build_dependency_graph(repo_path, commits):
    """Построение графа зависимостей."""
    graph = Digraph(format='png')
    graph.attr(rankdir='LR')  # Для горизонтального отображения

    commit_map = {commit['hash']: commit for commit in commits}

    for commit in commits:
        commit_hash = commit['hash']
        graph.node(commit_hash, label=f"{commit_hash[:7]}\n" + "\n".join(commit["files"]))
        parents = get_parents(repo_path, commit_hash)

        for parent in parents:
            if parent in commit_map:  # Учитываем только нужные коммиты
                graph.edge(parent, commit_hash)

    return graph

def visualize_graph(graph, visualizer_path):
    """Отображение графа."""
    output_path = os.path.join(os.getcwd(), 'graph')
    graph.render(output_path, view=True)
    print(f"Граф сохранен в файл: {output_path}.png")

def main(config_path):
    """Главная функция."""
    config = read_config(config_path)
    repo_path = config['repo_path']
    target_file = config['target_file']

    print(f"Анализируем репозиторий: {repo_path}")
    print(f"Фильтруем коммиты с файлом: {target_file}")

    commits = get_commits_with_file(repo_path, target_file)
    if not commits:
        print("Не найдено коммитов с указанным файлом.")
        return

    graph = build_dependency_graph(repo_path, commits)
    visualize_graph(graph, config['visualizer_path'])

if __name__ == "__main__":
    import sys
    if len(sys.argv) != 2:
        print("Использование: python script.py <config.yaml>")
        sys.exit(1)

    main(sys.argv[1])
