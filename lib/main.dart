import 'package:flutter/material.dart';
import 'package:math_expressions/math_expressions.dart';

void main() {
  runApp(const CalculatorApp());
}

class CalculatorApp extends StatelessWidget {
  const CalculatorApp({super.key});

  @override
  Widget build(BuildContext context) {
    return MaterialApp(
      title: 'Calculator',
      theme: ThemeData(
        primarySwatch: Colors.blue,
        useMaterial3: true,
      ),
      home: const CalculatorScreen(),
      debugShowCheckedModeBanner: false,
    );
  }
}

class CalculatorScreen extends StatefulWidget {
  const CalculatorScreen({super.key});

  @override
  State<CalculatorScreen> createState() => _CalculatorScreenState();
}

class _CalculatorScreenState extends State<CalculatorScreen> {
  String _expression = '';
  String _result = '';

  void _onButtonPressed(String value) {
    setState(() {
      if (value == 'C') {
        _expression = '';
        _result = '';
      } else if (value == '=') {
        _calculateResult();
      } else if (value == '⌫') {
        if (_expression.isNotEmpty) {
          _expression = _expression.substring(0, _expression.length - 1);
        }
      } else {
        _expression += value;
      }
    });
  }

  void _calculateResult() {
    try {
      // Replace display symbols with actual operators
      String expressionToEval = _expression
          .replaceAll('×', '*')
          .replaceAll('÷', '/');

      Parser parser = Parser();
      Expression exp = parser.parse(expressionToEval);
      ContextModel cm = ContextModel();
      double eval = exp.evaluate(EvaluationType.REAL, cm);
      
      // Format result (remove .0 if whole number)
      if (eval == eval.toInt()) {
        _result = eval.toInt().toString();
      } else {
        _result = eval.toString();
      }
    } catch (e) {
      _result = 'Error';
    }
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(
        title: const Text('Calculator'),
        centerTitle: true,
        backgroundColor: Colors.blue,
        foregroundColor: Colors.white,
      ),
      body: Column(
        children: [
          // Display
          Container(
            padding: const EdgeInsets.all(20),
            alignment: Alignment.centerRight,
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.end,
              children: [
                Text(
                  _expression.isEmpty ? '0' : _expression,
                  style: const TextStyle(
                    fontSize: 36,
                    fontWeight: FontWeight.w500,
                  ),
                ),
                const SizedBox(height: 10),
                Text(
                  _result.isEmpty ? '' : '= $_result',
                  style: const TextStyle(
                    fontSize: 30,
                    color: Colors.green,
                    fontWeight: FontWeight.bold,
                  ),
                ),
              ],
            ),
          ),
          const Divider(thickness: 1),
          // Buttons
          Expanded(
            child: GridView.count(
              crossAxisCount: 4,
              childAspectRatio: 1.5,
              padding: const EdgeInsets.all(10),
              mainAxisSpacing: 10,
              crossAxisSpacing: 10,
              children: [
                _buildButton('7', Colors.grey[200]!),
                _buildButton('8', Colors.grey[200]!),
                _buildButton('9', Colors.grey[200]!),
                _buildButton('÷', Colors.orange, textColor: Colors.white),
                _buildButton('4', Colors.grey[200]!),
                _buildButton('5', Colors.grey[200]!),
                _buildButton('6', Colors.grey[200]!),
                _buildButton('×', Colors.orange, textColor: Colors.white),
                _buildButton('1', Colors.grey[200]!),
                _buildButton('2', Colors.grey[200]!),
                _buildButton('3', Colors.grey[200]!),
                _buildButton('-', Colors.orange, textColor: Colors.white),
                _buildButton('C', Colors.red, textColor: Colors.white),
                _buildButton('0', Colors.grey[200]!),
                _buildButton('=', Colors.green, textColor: Colors.white),
                _buildButton('+', Colors.orange, textColor: Colors.white),
                _buildButton('⌫', Colors.grey[350]!, fontSize: 20),
                _buildButton('(', Colors.grey[200]!),
                _buildButton(')', Colors.grey[200]!),
                _buildButton('.', Colors.grey[200]!),
              ],
            ),
          ),
        ],
      ),
    );
  }

  Widget _buildButton(String text, Color bgColor, {Color textColor = Colors.black, double fontSize = 28}) {
    return ElevatedButton(
      onPressed: () => _onButtonPressed(text),
      style: ElevatedButton.styleFrom(
        backgroundColor: bgColor,
        foregroundColor: textColor,
        shape: RoundedRectangleBorder(
          borderRadius: BorderRadius.circular(12),
        ),
        elevation: 2,
      ),
      child: Text(
        text,
        style: TextStyle(fontSize: fontSize, fontWeight: FontWeight.bold),
      ),
    );
  }
}
