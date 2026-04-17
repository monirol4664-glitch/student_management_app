class Student {
  int? id;
  String name;
  String rollNumber;
  String className;
  String parentPhone;
  DateTime admissionDate;

  Student({
    this.id,
    required this.name,
    required this.rollNumber,
    required this.className,
    required this.parentPhone,
    required this.admissionDate,
  });

  Map<String, dynamic> toMap() {
    return {
      'id': id,
      'name': name,
      'rollNumber': rollNumber,
      'className': className,
      'parentPhone': parentPhone,
      'admissionDate': admissionDate.toIso8601String(),
    };
  }

  factory Student.fromMap(Map<String, dynamic> map) {
    return Student(
      id: map['id'],
      name: map['name'],
      rollNumber: map['rollNumber'],
      className: map['className'],
      parentPhone: map['parentPhone'],
      admissionDate: DateTime.parse(map['admissionDate']),
    );
  }
}