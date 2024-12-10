import unittest
import re
from io import StringIO
import sys
from hw3 import parse_dictionary

class TestParseDictionary(unittest.TestCase):

    def test_correct_input(self):
        input_data = '$[ name : "Ivan", age : 30, address : "Moscow" ]'
        expected_output = '<dictionary>\n    <entry key="name">Ivan</entry>\n    <entry key="age">30</entry>\n    <entry key="address">Moscow</entry>\n</dictionary>'
        self.assertEqual(parse_dictionary(input_data), expected_output)

    def test_correct_input_with_comments(self):
        input_data = '/*\nЭто пример конфигурации пользователя\n*/$[ name : "Alex", age : 25, city : "Novosibirsk" ]'
        expected_output = '<dictionary>\n    <entry key="name">Alex</entry>\n    <entry key="age">25</entry>\n    <entry key="city">Novosibirsk</entry>\n</dictionary>'
        self.assertEqual(parse_dictionary(input_data), expected_output)

    def test_input_with_extra_field(self):
        input_data = '$[ name : "Kostya", age : 19, address : "Russia", sport: "Football" ]'
        expected_output = '<dictionary>\n    <entry key="name">Kostya</entry>\n    <entry key="age">19</entry>\n    <entry key="address">Russia</entry>\n    <entry key="sport">Football</entry>\n</dictionary>'
        self.assertEqual(parse_dictionary(input_data), expected_output)

    def test_syntax_error_missing_key(self):
        with self.assertRaises(SyntaxError) as context:
            parse_dictionary('$[ age : 80, address : "Italia",  : "Mia", food: "Pizza" ]')
        self.assertIn("Ошибка: Неверное имя ''. Имя должно начинаться с буквы или '_'.", str(context.exception))

    def test_syntax_error_invalid_key(self):
        with self.assertRaises(SyntaxError) as context:
            parse_dictionary('$[ 123name : "Ivan", age : 30 ]')
        self.assertIn("Ошибка: Неверное имя '123name'", str(context.exception))

if __name__ == '__main__':
    unittest.main()