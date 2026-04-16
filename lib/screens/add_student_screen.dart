import 'package:flutter/material.dart';
import '../services/database_service.dart';
import '../models/student.dart';

class AddStudentScreen extends StatefulWidget {
  @override
  _AddStudentScreenState createState() => _AddStudentScreenState();
}

class _AddStudentScreenState extends State<AddStudentScreen> {
  final _formKey = GlobalKey<FormState>();
  final _nameController = TextEditingController();
  final _rollController = TextEditingController();
  final _classController = TextEditingController();
  final _phoneController = TextEditingController();
  DateTime _selectedDate = DateTime.now();

  Future<void> _saveStudent() async {
    if (_formKey.currentState!.validate()) {
      final student = Student(
        name: _nameController.text,
        rollNumber: _rollController.text,
        className: _classController.text,
        parentPhone: _phoneController.text,
        admissionDate: _selectedDate,
      );

      final db = DatabaseService.instance;
      await db.createStudent(student);
      
      ScaffoldMessenger.of(context).showSnackBar(
        SnackBar(content: Text('Student added successfully')),
      );
      Navigator.pop(context);
    }
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(title: Text('Add Student')),
      body: Padding(
        padding: EdgeInsets.all(16.0),
        child: Form(
          key: _formKey,
          child: Column(
            children: [
              TextFormField(
                controller: _nameController,
                decoration: InputDecoration(labelText: 'Full Name'),
                validator: (value) => value!.isEmpty ? 'Enter name' : null,
              ),
              TextFormField(
                controller: _rollController,
                decoration: InputDecoration(labelText: 'Roll Number'),
                validator: (value) => value!.isEmpty ? 'Enter roll number' : null,
              ),
              TextFormField(
                controller: _classController,
                decoration: InputDecoration(labelText: 'Class'),
                validator: (value) => value!.isEmpty ? 'Enter class' : null,
              ),
              TextFormField(
                controller: _phoneController,
                decoration: InputDecoration(labelText: 'Parent Phone'),
                validator: (value) => value!.isEmpty ? 'Enter phone number' : null,
              ),
              ListTile(
                title: Text('Admission Date'),
                subtitle: Text('${_selectedDate.toLocal()}'.split(' ')[0]),
                trailing: Icon(Icons.calendar_today),
                onTap: () async {
                  final date = await showDatePicker(
                    context: context,
                    initialDate: _selectedDate,
                    firstDate: DateTime(2000),
                    lastDate: DateTime.now(),
                  );
                  if (date != null) {
                    setState(() => _selectedDate = date);
                  }
                },
              ),
              SizedBox(height: 20),
              ElevatedButton(
                onPressed: _saveStudent,
                child: Text('Save Student'),
              ),
            ],
          ),
        ),
      ),
    );
  }
}
