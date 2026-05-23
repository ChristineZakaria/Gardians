import 'dart:convert';
import 'package:http/http.dart' as http;

class AlertModel {
  final int id;
  final String type;
  final String title;
  final String message;
  final String severity;
  final bool read;
  final Map<String, dynamic> metadata;
  final DateTime createdAt;

  const AlertModel({
    required this.id,
    required this.type,
    required this.title,
    required this.message,
    required this.severity,
    required this.read,
    required this.metadata,
    required this.createdAt,
  });

  factory AlertModel.fromJson(Map<String, dynamic> json) => AlertModel(
        id: json['id'] as int,
        type: json['type'] as String? ?? '',
        title: json['title'] as String? ?? '',
        message: json['message'] as String? ?? '',
        severity: json['severity'] as String? ?? 'MEDIUM',
        read: json['read'] as bool? ?? false,
        metadata: (json['metadata'] as Map<String, dynamic>?) ?? {},
        createdAt: DateTime.parse(json['createdAt'] as String),
      );

  String get appName => metadata['appName'] as String? ?? '';
  String get detectedCategory => metadata['detectedCategory'] as String? ?? type;
  double get confidence => (metadata['confidence'] as num?)?.toDouble() ?? 0.0;
  int get confidencePercent => (confidence * 100).round();
}

class AlertService {
  final String baseUrl;
  final String authToken;

  const AlertService({required this.baseUrl, required this.authToken});

  // Called by child — no auth needed
  Future<void> reportContentDetection({
    required String deviceId,
    required String childName,
    required String appName,
    required String detectedCategory,
    required double confidence,
    required String timestamp,
  }) async {
    final response = await http.post(
      Uri.parse('$baseUrl/alerts/content-detection'),
      headers: {'Content-Type': 'application/json'},
      body: jsonEncode({
        'deviceId': deviceId,
        'childName': childName,
        'appName': appName,
        'detectedCategory': detectedCategory,
        'confidence': confidence,
        'timestamp': timestamp,
      }),
    );
    if (response.statusCode != 200) {
      throw Exception('Failed to report detection: ${response.statusCode}');
    }
  }

  // Parent — requires auth token
  Future<List<AlertModel>> getDeviceAlerts(String deviceId) async {
    final response = await http.get(
      Uri.parse('$baseUrl/alerts/device/$deviceId'),
      headers: _authHeaders,
    );
    if (response.statusCode != 200) {
      throw Exception('Failed to load alerts: ${response.statusCode}');
    }
    final list = jsonDecode(response.body) as List<dynamic>;
    return list.map((e) => AlertModel.fromJson(e as Map<String, dynamic>)).toList();
  }

  Future<List<AlertModel>> getTodayAlerts(String deviceId) async {
    final response = await http.get(
      Uri.parse('$baseUrl/alerts/device/$deviceId/today'),
      headers: _authHeaders,
    );
    if (response.statusCode != 200) {
      throw Exception('Failed to load today\'s alerts: ${response.statusCode}');
    }
    final list = jsonDecode(response.body) as List<dynamic>;
    return list.map((e) => AlertModel.fromJson(e as Map<String, dynamic>)).toList();
  }

  Future<void> markAlertRead(int alertId) async {
    await http.patch(
      Uri.parse('$baseUrl/alerts/$alertId/read'),
      headers: _authHeaders,
    );
  }

  Map<String, String> get _authHeaders => {
        'Content-Type': 'application/json',
        'Authorization': 'Bearer $authToken',
      };
}
