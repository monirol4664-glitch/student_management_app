import 'package:flutter/material.dart';
import '../services/database_service.dart';
import '../models/student.dart';
import 'add_student_screen.dart';

class HomeScreen extends StatefulWidget {
  @override
  _HomeScreenState createState() => _HomeScreenState();
}

class _HomeScreenState extends State<HomeScreen> {
  List<Student> _students = [];
  final DatabaseService _db = DatabaseService.instance;

  @override
  void initState() {
    super.initState();
    _loadStudents();
  }

  Future<void> _loadStudents() async {
    final students = await _db.getAllStudents();
    setState(() {
      _students = students;
    });
  }

  Future<void> _deleteStudent(int id) async {
    await _db.deleteStudent(id);
    _loadStudents();
    ScaffoldMessenger.of(context).showSnackBar(
      SnackBar(content: Text('Student deleted successfully')),
    );
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(
        title: Text('Student Management'),
        centerTitle: true,
      ),
      body: _students.isEmpty
          ? Center(
              child: Text('No students found. Add your first student!'),
            )
          : ListView.builder(
              itemCount: _students.length,
              itemBuilder: (context, index) {
                final student = _students[index];
                return Card(
                  margin: EdgeInsets.all(8.0),
                  child: ListTile(
                    leading: CircleAvatar(
                      child: Text(student.name[0].toUpperCase()),
                    ),
                    title: Text(student.name),
                    subtitle: Text('Roll: ${student.rollNumber} | Class: ${student.className}'),
                    trailing: IconButton(
                      icon: Icon(Icons.delete, color: Colors.red),
                      onPressed: () => _deleteStudent(student.id!),
                    ),
                    onTap: () {
                      // Navigate to details
                    },
                  ),
                );
              },
            ),
      floatingActionButton: FloatingActionButton(
        onPressed: () async {
          await Navigator.push(
            context,
            MaterialPageRoute(builder: (context) => AddStudentScreen()),
          );
          _loadStudents();
        },
        child: Icon(Icons.add),
      ),
    );
  }
}
