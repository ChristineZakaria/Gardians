import 'package:flutter/material.dart';
import '../../services/alert_service.dart';

class AlertsScreen extends StatefulWidget {
  final AlertService alertService;
  final String deviceId;
  final String childName;

  const AlertsScreen({
    super.key,
    required this.alertService,
    required this.deviceId,
    required this.childName,
  });

  @override
  State<AlertsScreen> createState() => _AlertsScreenState();
}

class _AlertsScreenState extends State<AlertsScreen>
    with SingleTickerProviderStateMixin {
  late TabController _tabs;
  List<AlertModel> _all = [];
  List<AlertModel> _today = [];
  bool _loading = true;
  String? _error;

  @override
  void initState() {
    super.initState();
    _tabs = TabController(length: 2, vsync: this);
    _load();
  }

  @override
  void dispose() {
    _tabs.dispose();
    super.dispose();
  }

  Future<void> _load() async {
    setState(() { _loading = true; _error = null; });
    try {
      final results = await Future.wait([
        widget.alertService.getDeviceAlerts(widget.deviceId),
        widget.alertService.getTodayAlerts(widget.deviceId),
      ]);
      setState(() {
        _all = results[0];
        _today = results[1];
        _loading = false;
      });
    } catch (e) {
      setState(() { _error = e.toString(); _loading = false; });
    }
  }

  Future<void> _markRead(AlertModel alert) async {
    if (alert.read) return;
    await widget.alertService.markAlertRead(alert.id);
    setState(() {
      _all = _all.map((a) => a.id == alert.id ? _withRead(a) : a).toList();
      _today = _today.map((a) => a.id == alert.id ? _withRead(a) : a).toList();
    });
  }

  AlertModel _withRead(AlertModel a) => AlertModel(
        id: a.id, type: a.type, title: a.title, message: a.message,
        severity: a.severity, read: true, metadata: a.metadata, createdAt: a.createdAt);

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(
        title: Text('${widget.childName} — Alerts'),
        bottom: TabBar(
          controller: _tabs,
          tabs: [
            Tab(text: 'All (${_all.length})'),
            Tab(text: 'Today (${_today.length})'),
          ],
        ),
        actions: [
          IconButton(icon: const Icon(Icons.refresh), onPressed: _load),
        ],
      ),
      body: _loading
          ? const Center(child: CircularProgressIndicator())
          : _error != null
              ? _ErrorView(error: _error!, onRetry: _load)
              : TabBarView(
                  controller: _tabs,
                  children: [
                    _AlertList(alerts: _all, onTap: _markRead),
                    _AlertList(alerts: _today, onTap: _markRead),
                  ],
                ),
    );
  }
}

class _AlertList extends StatelessWidget {
  final List<AlertModel> alerts;
  final Future<void> Function(AlertModel) onTap;

  const _AlertList({required this.alerts, required this.onTap});

  @override
  Widget build(BuildContext context) {
    if (alerts.isEmpty) {
      return const Center(
        child: Column(mainAxisSize: MainAxisSize.min, children: [
          Icon(Icons.check_circle_outline, size: 64, color: Colors.green),
          SizedBox(height: 12),
          Text('No alerts', style: TextStyle(fontSize: 16, color: Colors.grey)),
        ]),
      );
    }
    return RefreshIndicator(
      onRefresh: () async {},
      child: ListView.separated(
        padding: const EdgeInsets.symmetric(vertical: 8),
        itemCount: alerts.length,
        separatorBuilder: (_, __) => const Divider(height: 1),
        itemBuilder: (context, i) => _AlertTile(alert: alerts[i], onTap: onTap),
      ),
    );
  }
}

class _AlertTile extends StatelessWidget {
  final AlertModel alert;
  final Future<void> Function(AlertModel) onTap;

  const _AlertTile({required this.alert, required this.onTap});

  @override
  Widget build(BuildContext context) {
    final color = _severityColor(alert.severity);
    final unread = !alert.read;

    return InkWell(
      onTap: () => onTap(alert),
      child: Container(
        color: unread ? color.withOpacity(0.05) : null,
        padding: const EdgeInsets.symmetric(horizontal: 16, vertical: 12),
        child: Row(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            _CategoryIcon(category: alert.detectedCategory, color: color),
            const SizedBox(width: 12),
            Expanded(
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  Row(children: [
                    Expanded(
                      child: Text(
                        alert.appName.isNotEmpty ? alert.appName : alert.title,
                        style: TextStyle(
                          fontWeight: unread ? FontWeight.bold : FontWeight.w500,
                          fontSize: 15,
                        ),
                      ),
                    ),
                    if (unread)
                      Container(
                        width: 8, height: 8,
                        decoration: BoxDecoration(color: color, shape: BoxShape.circle),
                      ),
                  ]),
                  const SizedBox(height: 4),
                  Row(children: [
                    _CategoryChip(category: alert.detectedCategory, color: color),
                    const SizedBox(width: 8),
                    Text(
                      '${alert.confidencePercent}% confidence',
                      style: const TextStyle(fontSize: 12, color: Colors.grey),
                    ),
                  ]),
                  const SizedBox(height: 4),
                  Text(
                    _formatTime(alert.createdAt),
                    style: const TextStyle(fontSize: 12, color: Colors.grey),
                  ),
                ],
              ),
            ),
          ],
        ),
      ),
    );
  }

  Color _severityColor(String severity) => switch (severity.toUpperCase()) {
        'CRITICAL' => Colors.red,
        'HIGH'     => Colors.orange,
        'MEDIUM'   => Colors.amber,
        _          => Colors.blue,
      };

  String _formatTime(DateTime dt) {
    final now = DateTime.now();
    final diff = now.difference(dt);
    if (diff.inMinutes < 1) return 'Just now';
    if (diff.inMinutes < 60) return '${diff.inMinutes}m ago';
    if (diff.inHours < 24) return '${diff.inHours}h ago';
    return '${dt.day}/${dt.month} ${dt.hour.toString().padLeft(2, '0')}:${dt.minute.toString().padLeft(2, '0')}';
  }
}

class _CategoryIcon extends StatelessWidget {
  final String category;
  final Color color;

  const _CategoryIcon({required this.category, required this.color});

  @override
  Widget build(BuildContext context) {
    final icon = switch (category.toLowerCase()) {
      'adult content'   => Icons.no_adult_content,
      'harmful content' => Icons.warning_amber_rounded,
      'suicide'         => Icons.favorite_border,
      _                 => Icons.report_outlined,
    };
    return Container(
      width: 44, height: 44,
      decoration: BoxDecoration(
        color: color.withOpacity(0.12),
        borderRadius: BorderRadius.circular(12),
      ),
      child: Icon(icon, color: color, size: 22),
    );
  }
}

class _CategoryChip extends StatelessWidget {
  final String category;
  final Color color;

  const _CategoryChip({required this.category, required this.color});

  @override
  Widget build(BuildContext context) => Container(
        padding: const EdgeInsets.symmetric(horizontal: 8, vertical: 2),
        decoration: BoxDecoration(
          color: color.withOpacity(0.12),
          borderRadius: BorderRadius.circular(20),
        ),
        child: Text(
          category,
          style: TextStyle(color: color, fontSize: 11, fontWeight: FontWeight.w600),
        ),
      );
}

class _ErrorView extends StatelessWidget {
  final String error;
  final VoidCallback onRetry;

  const _ErrorView({required this.error, required this.onRetry});

  @override
  Widget build(BuildContext context) => Center(
        child: Column(mainAxisSize: MainAxisSize.min, children: [
          const Icon(Icons.error_outline, size: 48, color: Colors.red),
          const SizedBox(height: 12),
          Text('Failed to load alerts', style: Theme.of(context).textTheme.titleMedium),
          const SizedBox(height: 8),
          TextButton(onPressed: onRetry, child: const Text('Retry')),
        ]),
      );
}
