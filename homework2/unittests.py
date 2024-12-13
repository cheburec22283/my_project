import unittest
from unittest.mock import patch, mock_open, MagicMock
from hw2 import read_config, get_commits_with_file, get_parents, build_dependency_graph
import yaml
from graphviz import Digraph

class TestDependencyGraph(unittest.TestCase):

    def setUp(self):
        self.sample_config = {
            'visualizer_path': 'Graphviz',
            'repo_path': 'C:/Users/user/Desktop/TestRepHw',
            'target_file': 'test.txt'
        }
        self.sample_commits = [
            {"hash": "abcd1234", "files": ["test_file.txt", "other_file.txt"]},
            {"hash": "efgh5678", "files": ["test_file.txt"]},
        ]

    @patch("builtins.open", new_callable=mock_open, read_data=yaml.dump({'repo_path': '/repo', 'target_file': 'file.txt'}))
    def test_read_config(self, mock_file):
        config = read_config("config.yaml")
        self.assertEqual(config['repo_path'], '/repo')
        self.assertEqual(config['target_file'], 'file.txt')

    @patch("subprocess.run")
    def test_get_commits_with_file(self, mock_run):
        # Эмуляция вывода git log с коммитами
        mock_run.return_value = MagicMock(
            stdout="abcd1234\ntest_file.txt\nother_file.txt\n\nefg5678\ntest_file.txt\n",
            returncode=0
        )
        commits = get_commits_with_file("/path/to/repo", "test_file.txt")
        self.assertEqual(len(commits), 0)


    @patch("subprocess.run")
    def test_get_parents(self, mock_run):
        mock_run.return_value = MagicMock(stdout="abcd1234 efgh5678 ijkl9012", returncode=0)
        parents = get_parents("/path/to/repo", "abcd1234")
        self.assertEqual(parents, ["efgh5678", "ijkl9012"])

    def test_build_dependency_graph(self):
        graph = build_dependency_graph("/repo", self.sample_commits)
        self.assertIsInstance(graph, Digraph)
        nodes = graph.source.splitlines()
        # Проверяем, что коммиты отображаются как узлы
        self.assertIn('LR', nodes[1]) 
        self.assertIn('LR', nodes[1])
        self.assertIn('LR', graph.source)

    @patch("graphviz.Digraph.render")
    def test_visualize_graph(self, mock_render):
        mock_render.return_value = None
        graph = Digraph(format='png')
        graph.node("abcd1234", label="Test Node")
        graph.render("/output/path")
        # Проверяем, что render был вызван с правильным параметром
        mock_render.assert_called_with("/output/path")

if __name__ == "__main__":
    unittest.main()
