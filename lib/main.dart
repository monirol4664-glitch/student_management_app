import 'package:flutter/material.dart';

void main() {
  runApp(const MyApp());
}

class MyApp extends StatelessWidget {
  const MyApp({super.key});

  @override
  Widget build(BuildContext context) {
    return MaterialApp(
      title: 'Student App',
      home: Scaffold(
        appBar: AppBar(
          title: const Text('Student Management'),
          backgroundColor: Colors.blue,
        ),
        body: const Center(
          child: Text(
            'Welcome!',
            style: TextStyle(fontSize: 24),
          ),
        ),
      ),
    );
  }
}
