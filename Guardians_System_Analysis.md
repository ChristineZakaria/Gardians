# 🛡️ Guardians — Child Safety System

> An AI-powered mobile application designed to ensure a safe digital environment for children by monitoring, detecting, and preventing harmful activities.

---

## 📋 Table of Contents

1. [Project Overview](#1-project-overview)
2. [Problem Statement](#2-problem-statement)
3. [Actors](#3-actors)
4. [System Context Diagram](#4-system-context-diagram)
5. [Use Case Diagram](#5-use-case-diagram)
6. [Use Case Descriptions](#6-use-case-descriptions)
7. [User Stories](#7-user-stories)
8. [Acceptance Criteria](#8-acceptance-criteria)
9. [Functional Requirements](#9-functional-requirements)
10. [Non-Functional Requirements](#10-non-functional-requirements)
11. [System Components](#11-system-components)
12. [Data Description](#12-data-description)
13. [System Flow](#13-system-flow)
14. [Error Handling & Edge Cases](#14-error-handling--edge-cases)
15. [Assumptions & Constraints](#15-assumptions--constraints)

---

## 1. Project Overview

**Guardians** is an AI-Powered Parental Control and Child Protection mobile application developed to address the growing risks children face in the digital environment. As smartphones become indispensable in children's lives, parents require sophisticated tools — not merely to restrict access, but to genuinely understand and protect their children's online experiences.

The application is built with **Flutter** for cross-platform deployment and a **Java Spring Boot** backend for robust API management. The system serves two distinct user roles:
- **Parent** — gains full monitoring, control, and reporting capabilities
- **Child** — uses the device normally within defined safety boundaries

### Core System Capabilities

| Capability | Description |
|---|---|
| 🤖 AI Content Analysis | Real-time analysis of images, videos, text messages, and URLs |
| 📵 Remote App Control | Block / unblock specific applications on the child's device |
| 🔒 Remote Device Lock | Instantly lock or unlock the child's device from anywhere |
| 📍 Location Tracking | Live GPS location tracking with geofencing and safe-zone alerts |
| 📊 Behavioral Reports | AI-generated weekly behavioral pattern analysis |
| 💬 Daily Safety Check-in | Child wellbeing check-in with distress signaling |
| 🔐 RBAC Security | Role-Based Access Control for all data and features |

---

## 2. Problem Statement

Parents of school-age children (ages 6–17) who own smartphones face an inadequate set of tools for protecting their children from digital harms. Existing parental control solutions are **rule-based systems** that cannot detect context-dependent threats such as:

- Cyberbullying within approved applications
- Predatory grooming language in private messages
- Harmful imagery embedded within otherwise safe content streams

> In Egypt specifically, over **60% of parents** expressed concern about their children's online activities but felt unable to effectively monitor or control device usage *(National Council for Childhood and Motherhood, 2023)*.

This gap is particularly acute in the Arab-speaking world, where most AI-powered safety tools are designed **exclusively for English-language content**.

### Problem vs. Solution Mapping

| Problem Dimension | Impact | Guardians Response |
|---|---|---|
| Cyberbullying in approved apps | 37% of children aged 12–17 affected globally | AI NLP text analysis detects harmful language in real time |
| Inappropriate imagery | Majority of children with unrestricted access exposed | MobileNetV3 image classifier — 7 harm categories |
| Predatory grooming | Occurs within trusted platforms; invisible to parents | Grooming pattern NLP detection — Arabic & English |
| Unsafe URLs | New malicious sites evade static blacklists daily | 3-layer URL analysis: blocklist + ML classifier + threat intel |
| No child voice in safety | Children feel surveilled; circumvention increases | Daily Safety Check-in empowers child communication |
| Arabic-language gap | Arabic tools absent in consumer safety market | Bilingual BERT model — Arabic MSA + Egyptian dialect |

---

## 3. Actors

### 3.1 Primary Actors

| Actor | Type | Description | Key Capabilities |
|---|---|---|---|
| **Parent** | Human — Primary | The legal guardian who owns and manages the child's device and configures safety policies | Monitor usage, block/allow apps, lock device, view location, receive AI alerts, view reports, configure settings |
| **Child** | Human — Primary | The minor user of the monitored device. Interacts with the Guardian app through the child interface | Normal device usage within restrictions, Daily Safety Check-in, signal distress, view blocked-app notices |

### 3.2 External Service Actors

| Actor | Role |
|---|---|
| **Firebase Cloud Messaging (FCM)** | Delivers real-time push notifications from the backend to both parent and child devices |
| **Google Maps / FusedLocation API** | Provides accurate, battery-efficient GPS location data and map rendering |
| **AI Model Server** (TensorFlow Serving / Hugging Face) | Hosts and serves the trained image classification, NLP text, and URL analysis models |

---

## 4. System Context Diagram

```
┌─────────────────────────────────────────────────────────────────┐
│                        GUARDIANS SYSTEM                         │
│                                                                 │
│  ┌─────────────┐    usage events / content / GPS / check-ins   │
│  │             │ ──────────────────────────────────────────►   │
│  │  Child      │                                               │
│  │  Device     │ ◄──────────────────────────────────────────   │
│  └─────────────┘    restriction commands / lock / app blocks   │
│                                                                 │
│  ┌─────────────┐    alerts / reports / dashboard / location    │
│  │             │ ◄──────────────────────────────────────────   │
│  │  Parent     │                                               │
│  │  Device     │ ──────────────────────────────────────────►   │
│  └─────────────┘    control commands / settings / config       │
│                                                                 │
│  ┌─────────────┐    notification payloads                      │
│  │  Firebase   │ ◄──────────────────────────────────────────   │
│  │  FCM        │                                               │
│  └─────────────┘                                               │
│                                                                 │
│  ┌─────────────┐    content data (images / text / URLs)        │
│  │  AI Model   │ ◄──────────────────────────────────────────   │
│  │  Server     │ ──────────────────────────────────────────►   │
│  └─────────────┘    classification results + confidence scores │
└─────────────────────────────────────────────────────────────────┘
```

### Data Flows Summary

| Source | Destination | Data Flow |
|---|---|---|
| Child Device | System | Usage events, captured content, GPS location, check-in responses |
| System | Child Device | Restriction commands, lock/unlock signals, app block updates |
| System | Parent Device | Real-time alerts, behavioral reports, dashboard data, location updates |
| Parent Device | System | Control commands, configuration changes, alert acknowledgments |
| System | Firebase (FCM) | Push notification payloads |
| System | AI Model Server | Content data (images, text, URLs) for analysis |
| AI Model Server | System | Classification results and confidence scores |

---

## 5. Use Case Diagram

```
                        ┌─────────────────────────────┐
                        │       GUARDIANS SYSTEM       │
                        │                             │
  ┌──────────┐          │  [UC-P-01] Login/Register   │
  │          │─────────►│  [UC-P-02] Pair Device      │
  │  PARENT  │─────────►│  [UC-P-03] View Dashboard   │
  │          │─────────►│  [UC-P-04] Monitor Usage    │
  └──────────┘─────────►│  [UC-P-05] Block/Unblock App│
       │      ─────────►│  [UC-P-06] Lock/Unlock Dev  │
       │      ─────────►│  [UC-P-07] View Location    │
       │      ─────────►│  [UC-P-08] Config Geofence  │
       │      ─────────►│  [UC-P-09] View AI Alert    │
       │      ─────────►│  [UC-P-10] Acknowledge Alert│
       │      ─────────►│  [UC-P-11] View Report      │
       │      ─────────►│  [UC-P-12] View Check-ins   │
       │      ─────────►│  [UC-P-13] Configure Settings│
       │                │                             │
  ┌──────────┐          │  [UC-C-01] Use Device       │
  │          │─────────►│  [UC-C-02] Blocked App      │
  │  CHILD   │─────────►│  [UC-C-03] Submit Check-in  │
  │          │─────────►│  [UC-C-04] Signal Distress  │
  └──────────┘─────────►│  [UC-C-05] View Lock Screen │
                        └─────────────────────────────┘
```

### 5.1 Parent Use Cases

| Use Case ID | Use Case Name | Brief Description |
|---|---|---|
| UC-P-01 | Login / Register | Parent creates an account or authenticates to access the parent dashboard |
| UC-P-02 | Pair Child Device | Parent links a child's device to their account via a secure pairing code |
| UC-P-03 | View Dashboard | Parent views a summary of screen time, blocked apps, AI monitor status, and usage |
| UC-P-04 | Monitor App Usage | Parent views per-application usage time, trends, and behavioral pattern insights |
| UC-P-05 | Block / Unblock Application | Parent toggles a specific application to blocked or allowed state |
| UC-P-06 | Lock / Unlock Device | Parent remotely locks the child's device; emergency calling remains available |
| UC-P-07 | View Real-Time Location | Parent views the child device's live GPS location on a map |
| UC-P-08 | Configure Geofence | Parent defines a geographic safe zone and receives exit alerts |
| UC-P-09 | View AI Content Alert | Parent reviews a flagged content alert: category, confidence, source app |
| UC-P-10 | Acknowledge Alert | Parent marks an alert as reviewed and optionally blocks app or locks device |
| UC-P-11 | View Behavioral Report | Parent views the AI-generated weekly behavioral analysis |
| UC-P-12 | View Safety Check-in History | Parent reviews the child's daily check-in responses and mood timeline |
| UC-P-13 | Configure Settings | Parent adjusts screen time limits, AI thresholds, and notification preferences |

### 5.2 Child Use Cases

| Use Case ID | Use Case Name | Brief Description |
|---|---|---|
| UC-C-01 | Use Device Normally | Child uses installed applications within defined restrictions without disruption |
| UC-C-02 | Encounter Blocked App | Child attempts to launch a blocked application and sees 'Blocked by Parent' |
| UC-C-03 | Submit Daily Check-in | Child completes the daily mood and safety check-in prompt |
| UC-C-04 | Signal Distress | Child presses 'I need help', triggering an immediate priority alert to the parent |
| UC-C-05 | View Lock Screen | Child views the lock screen when the device has been remotely locked |

---

## 6. Use Case Descriptions

### UC-P-05: Block / Unblock Application

| Field | Detail |
|---|---|
| **Use Case ID** | UC-P-05 |
| **Actor** | Parent |
| **Precondition** | Parent is authenticated. At least one child device is linked. |
| **Trigger** | Parent navigates to App Control screen and toggles an application's block switch. |
| **Main Flow** | 1. Parent opens App Control screen → 2. System loads full app list → 3. Parent toggles app to BLOCKED → 4. Flutter sends PATCH to `/api/apps/{deviceId}/{appId}` → 5. Backend validates auth and updates DB → 6. Backend sends high-priority FCM to child device → 7. Child background service applies the block → 8. Parent receives confirmation |
| **Alt Flow A** | Device offline: command queued, applied within 60 sec of reconnection |
| **Alt Flow B** | App already blocked: system shows current status, no duplicate command sent |
| **Postcondition** | App is blocked. Subsequent launch attempts show 'Blocked by Parent' screen. |
| **Performance SLA** | < 3 seconds activation latency under normal network conditions |

---

### UC-C-03: Submit Daily Check-in

| Field | Detail |
|---|---|
| **Use Case ID** | UC-C-03 |
| **Actor** | Child |
| **Precondition** | It is the configured check-in time. Check-in not yet completed today. |
| **Trigger** | Background service displays the full-screen check-in prompt. |
| **Main Flow** | 1. Check-in screen appears → 2. Child selects mood (5-emoji scale) → 3. Child answers safety question (Yes/No) → 4. Child optionally adds a note → 5. Child taps Submit → 6. Response transmitted to backend → 7. Alert Engine evaluates (Green / Yellow / Red) → 8. Parent receives appropriate notification |
| **Alt Flow A** | Child presses 'I need help': RED alert + immediate high-priority FCM to parent |
| **Alt Flow B** | No network: response saved locally, synced when connectivity restored |
| **Postcondition** | Check-in recorded. Parent dashboard shows updated check-in timeline. |
| **Performance SLA** | Distress alert delivery < 3 seconds from submission |

---

### UC-P-09: View AI Content Alert

| Field | Detail |
|---|---|
| **Use Case ID** | UC-P-09 |
| **Actor** | Parent |
| **Precondition** | An AI analysis result has exceeded the configured confidence threshold. |
| **Trigger** | Parent receives push notification or opens the Alerts tab. |
| **Main Flow** | 1. AI pipeline detects harmful content → 2. Alert record created in DB → 3. FCM high-priority notification sent to parent → 4. Parent opens alert detail → 5. Alert shows: category, confidence, source app, timestamp, blurred preview → 6. Parent acknowledges and optionally blocks app or locks device |
| **Postcondition** | Alert marked acknowledged. Optional actions applied to child device. |
| **Performance SLA** | Alert delivery < 5 seconds from content detection |

---

## 7. User Stories

| ID | Role | User Story | Priority |
|---|---|---|---|
| US-01 | Parent | As a parent, I want to see a summary of my child's daily screen time and top apps so that I can quickly assess their digital activity. | HIGH |
| US-02 | Parent | As a parent, I want to block a specific app on my child's device so that I can prevent inappropriate usage. | CRITICAL |
| US-03 | Parent | As a parent, I want to remotely lock my child's device so that I can enforce bedtime or study time from anywhere. | CRITICAL |
| US-04 | Parent | As a parent, I want to see my child's live GPS location on a map so that I know they are safe. | HIGH |
| US-05 | Parent | As a parent, I want to receive an immediate alert when harmful content is detected so that I can take action quickly. | CRITICAL |
| US-06 | Parent | As a parent, I want to review a weekly behavioral analysis report so that I can identify concerning patterns over time. | HIGH |
| US-07 | Parent | As a parent, I want to define a safe geographic zone around my child's school so that I receive an alert if they leave unexpectedly. | MEDIUM |
| US-08 | Parent | As a parent, I want to configure the AI sensitivity threshold so that I can balance alert frequency with detection coverage. | MEDIUM |
| US-09 | Child | As a child, I want to see a clear message when an app is blocked so that I understand why I cannot access it. | HIGH |
| US-10 | Child | As a child, I want a safe, non-threatening way to tell my parent I feel unsafe online so that I can get help without fear. | CRITICAL |
| US-11 | Child | As a child, I want the daily check-in to be quick and emoji-based so that it does not feel intimidating. | HIGH |
| US-12 | Parent | As a parent, I want app blocking commands to apply even when my child's device is temporarily offline so that restrictions are reliable. | HIGH |

---

## 8. Acceptance Criteria

| US ID | Acceptance Criteria |
|---|---|
| US-01 | • Dashboard loads in < 3 seconds • Screen time widget displays today's total and daily limit percentage • Top 5 applications shown with usage bars |
| US-02 | • Toggle responds in < 1 second in the UI • App is blocked on child device within 3 seconds • Blocked app shows 'Blocked by Parent' screen on launch attempt |
| US-03 | • Device lock activates within 5 seconds • Emergency calling remains functional during lock • Lock state persists through device reboot |
| US-04 | • Location updates within 30 seconds when device is moving • Map shows last-known location with timestamp when stationary |
| US-05 | • Alert delivered via FCM within 5 seconds of detection • Alert includes: category, confidence score, source app, and blurred preview • Parent can act (block app / lock device) from the alert detail screen |
| US-06 | • Weekly report available every Monday by 8:00 AM • Report covers: top apps, screen time trends, behavioral anomalies detected |
| US-07 | • Geofence exit alert delivered within 30 seconds of boundary crossing • Alert includes: geofence name, direction, timestamp, map snapshot |
| US-08 | • Threshold configurable from 0.5 to 0.95 in Settings • Change takes effect immediately without app restart |
| US-09 | • Blocked screen displays app name and parent-configurable message • Emergency dial pad link visible on block screen |
| US-10 | • 'I need help' button visible on every check-in screen • Pressing it delivers a RED-level FCM high-priority alert to parent within 3 seconds • Child receives confirmation: 'Your parent has been notified.' |
| US-11 | • Check-in completed in < 60 seconds (validated in UAT) • No text entry required — emoji and Yes/No answers sufficient |
| US-12 | • Queued commands applied within 60 seconds of reconnection • Parent dashboard shows 'Pending — device offline' for queued commands |

---

## 9. Functional Requirements

### 9.1 Activity Monitoring

| ID | Requirement | Priority |
|---|---|---|
| FR-AM-01 | The system shall track the usage duration of every application on the child device, precise to the second. | CRITICAL |
| FR-AM-02 | Usage data shall be synchronized with the backend every 15 minutes or on parent-triggered refresh. | HIGH |
| FR-AM-03 | The system shall detect and flag late-night usage (11 PM – 6 AM) as a behavioral anomaly. | HIGH |
| FR-AM-04 | The system shall send a notification when the child reaches 80% and 100% of the daily screen time limit. | HIGH |
| FR-AM-05 | The behavioral analysis engine shall identify week-over-week usage increases > 50% for specific applications. | MEDIUM |

### 9.2 App Control & Device Management

| ID | Requirement | Priority |
|---|---|---|
| FR-AC-01 | The system shall allow parents to block or unblock any installed application on the child device. | CRITICAL |
| FR-AC-02 | Block/unblock commands shall be applied within 3 seconds under normal network conditions. | CRITICAL |
| FR-AC-03 | Block commands issued while the child device is offline shall be queued and applied within 60 seconds of reconnection. | HIGH |
| FR-AC-04 | The system shall allow parents to remotely lock the child device entirely, with emergency calling remaining available. | CRITICAL |
| FR-AC-05 | The device locked state shall persist through device restart (re-acquired from server on boot). | HIGH |
| FR-AC-06 | All parent control actions shall be logged with timestamp and actor for audit purposes. | MEDIUM |

### 9.3 Location Tracking

| ID | Requirement | Priority |
|---|---|---|
| FR-LT-01 | The system shall provide real-time GPS location with updates every 30 seconds when the device is moving. | HIGH |
| FR-LT-02 | The system shall reduce location update frequency to every 5 minutes when the device is stationary. | HIGH |
| FR-LT-03 | The system shall support geofence definition by center point and radius (100 m – 5 km). | HIGH |
| FR-LT-04 | The system shall deliver a parent notification within 30 seconds of a geofence entry or exit event. | HIGH |
| FR-LT-05 | Location history shall be stored for 90 days and accessible to the parent via the dashboard. | MEDIUM |

### 9.4 AI Content Analysis

| ID | Requirement | Priority |
|---|---|---|
| FR-AI-01 | The image analysis module shall classify images into 7 categories: NSFW, Violence, Drugs, Weapon, Self-Harm, Hate Symbols, Safe. | CRITICAL |
| FR-AI-02 | The text NLP module shall detect 6 harm categories in Arabic and English: Cyberbullying, Threat, Sexual, Grooming, Self-Harm, Hate Speech. | CRITICAL |
| FR-AI-03 | The URL analysis module shall assess URLs against a local blocklist (2.5M+ domains), ML classifier, and real-time threat intelligence feed. | CRITICAL |
| FR-AI-04 | The video analysis module shall use keyframe sampling (1 frame/10 sec) and scene change detection before full analysis. | HIGH |
| FR-AI-05 | An AI alert shall be generated when detected content confidence exceeds the parent-configured threshold (default: 0.70). | CRITICAL |
| FR-AI-06 | AI alerts shall be delivered to the parent device within 5 seconds of detection. | HIGH |

### 9.5 Daily Safety Check-in

| ID | Requirement | Priority |
|---|---|---|
| FR-CI-01 | The system shall present a daily check-in prompt to the child once per day at the parent-configured time. | HIGH |
| FR-CI-02 | The check-in shall include a 5-level emoji mood selector and a binary unsafe-event question. | HIGH |
| FR-CI-03 | The child shall not be able to dismiss the check-in without providing a response. | HIGH |
| FR-CI-04 | A distress signal (helpPressed = true OR unsafeEvent = true) shall trigger an immediate RED-level FCM high-priority notification to the parent. | CRITICAL |
| FR-CI-05 | Check-in history shall be accessible to the parent in timeline format for the past 90 days. | MEDIUM |

---

## 10. Non-Functional Requirements

### 10.1 Performance

| ID | Requirement | Target | Priority |
|---|---|---|---|
| NFR-P-01 | API average response time | < 200 ms | HIGH |
| NFR-P-02 | Image analysis end-to-end latency | < 2 seconds | HIGH |
| NFR-P-03 | Text analysis end-to-end latency | < 1 second | HIGH |
| NFR-P-04 | URL analysis end-to-end latency | < 500 ms | HIGH |
| NFR-P-05 | App block / unblock activation latency | < 3 seconds | CRITICAL |
| NFR-P-06 | Device lock / unlock activation latency | < 5 seconds | CRITICAL |
| NFR-P-07 | Distress alert delivery latency | < 3 seconds | CRITICAL |
| NFR-P-08 | Background service battery drain | < 5% per day | HIGH |
| NFR-P-09 | Location update frequency (moving) | Every 30 seconds | MEDIUM |
| NFR-P-10 | Dashboard data maximum staleness | < 15 minutes | MEDIUM |

### 10.2 Security

| ID | Requirement | Implementation |
|---|---|---|
| NFR-S-01 | All API communication encrypted | TLS 1.3 for all HTTPS connections |
| NFR-S-02 | Authentication token expiry | JWT access tokens expire in 15 minutes; refresh tokens rotated on use |
| NFR-S-03 | Brute force protection | Max 5 login attempts per 15 min per IP |
| NFR-S-04 | Role isolation | RBAC enforced at both API gateway and service layer |
| NFR-S-05 | Sensitive data encryption at rest | AES-256 for location and content analysis data |
| NFR-S-06 | Secure credential storage | Android Keystore for all on-device credentials |
| NFR-S-07 | Certificate pinning | SSL pinning on mobile app for all backend API calls |
| NFR-S-08 | Audit logging | All parent control actions logged with timestamp and actor |

### 10.3 Usability

| ID | Requirement | Target Metric |
|---|---|---|
| NFR-U-01 | Parent onboarding completion | < 10 minutes from install to first linked device |
| NFR-U-02 | Feature discoverability | Any core feature reachable within 3 taps from home screen |
| NFR-U-03 | Child check-in completion time | < 60 seconds — validated in UAT |
| NFR-U-04 | Accessibility compliance | WCAG 2.1 AA — text contrast and touch targets |
| NFR-U-05 | Language support | Full Arabic RTL and English LTR UI and NLP support |

---

## 11. System Components

The Guardians system follows a **three-tier client-server architecture**. Components communicate through a secure RESTful API layer, with Firebase Cloud Messaging handling all real-time push notifications.

```
┌──────────────────────────────────────────────────────────────────────┐
│                         TIER 1 — MOBILE                              │
│  ┌─────────────────────────┐   ┌──────────────────────────────────┐  │
│  │   Parent Dashboard      │   │   Child Interface                │  │
│  │   (Flutter)             │   │   (Flutter)                      │  │
│  └─────────────────────────┘   └──────────────────────────────────┘  │
│  ┌──────────────────────────────────────────────────────────────────┐ │
│  │   Background Monitoring Service (Android Foreground Service)     │ │
│  └──────────────────────────────────────────────────────────────────┘ │
└────────────────────────────────┬─────────────────────────────────────┘
                                 │ HTTPS / TLS 1.3
┌────────────────────────────────▼─────────────────────────────────────┐
│                         TIER 2 — BACKEND                             │
│  ┌─────────────────┐  ┌──────────────┐  ┌────────────────────────┐  │
│  │  REST API Layer │  │ AI Orchestr. │  │  Notification Service  │  │
│  │  Spring Boot    │  │              │  │  (FCM)                 │  │
│  └─────────────────┘  └──────┬───────┘  └────────────────────────┘  │
└──────────────────────────────┼───────────────────────────────────────┘
                                │ gRPC (internal)
┌────────────────────────────────▼─────────────────────────────────────┐
│                         TIER 3 — DATA & AI                           │
│  ┌──────────┐  ┌──────────┐  ┌──────────────┐  ┌─────────────────┐  │
│  │ MySQL 8  │  │ Redis 7  │  │  TF Serving  │  │  Hugging Face   │  │
│  │ Database │  │  Cache   │  │  (Images)    │  │  NLP (Text)     │  │
│  └──────────┘  └──────────┘  └──────────────┘  └─────────────────┘  │
└──────────────────────────────────────────────────────────────────────┘
```

### Component Overview

| Component | Technology | Role |
|---|---|---|
| Mobile App (Parent) | Flutter 3.x | Parent dashboard: monitoring, control, alerts, reports, location map |
| Mobile App (Child) | Flutter 3.x | Child interface: normal device use, daily check-in, restriction notices |
| Background Monitoring Service | Android Foreground Service | Continuously captures usage events, content, and GPS on child device |
| Backend API Server | Java Spring Boot 3.x | RESTful API: business logic, authorization, alert generation, command dispatch |
| AI Orchestrator | Spring Boot — internal module | Routes captured content to appropriate AI analysis model |
| Image Analysis Service | TensorFlow Serving + MobileNetV3 | Classifies images into 7 harm categories with confidence scores |
| NLP Text Analysis Service | Hugging Face + multilingual BERT | Detects 6 harm categories in Arabic and English text |
| URL Safety Service | Custom Python microservice | 3-layer analysis: blocklist + ML classifier + threat intel API |
| Push Notification Service | Firebase Cloud Messaging | Delivers real-time alerts and control commands to devices |
| Primary Database | MySQL 8.x | Persistent storage for all structured monitoring and alert data |
| Cache Layer | Redis 7.x | Session management, real-time device status, blocklist fast lookup |

### Communication Protocols

| Communication Path | Protocol | Security |
|---|---|---|
| Mobile app ↔ Backend | HTTPS REST API | TLS 1.3 + JWT Bearer Authentication |
| Backend → Mobile (alerts & commands) | Firebase Cloud Messaging | FCM encryption + high-priority flag |
| Backend ↔ AI Model Server | gRPC (internal) | Internal network only |
| Backend ↔ Database | JDBC over TCP | Internal network + credential authentication |
| Backend ↔ Redis | Redis Protocol | Internal network + AUTH command |

---

## 12. Data Description

### 12.1 Core Data Entities

| Entity | Key Attributes | Relationships |
|---|---|---|
| **Users** | user_id, email, hashed_password, role (PARENT/CHILD), full_name, created_at | Parent has many Devices; Child belongs to one Device |
| **Devices** | device_id, device_name, parent_user_id, child_user_id, is_locked, fcm_token, last_seen | Belongs to Parent; associated with Child |
| **App_Blocks** | block_id, device_id, app_package, app_name, is_blocked, blocked_by, updated_at | Belongs to Device |
| **Activity_Sessions** | session_id, device_id, app_package, start_time, end_time, duration_seconds | Belongs to Device; indexed on (device_id, start_time) |
| **Locations** | location_id, device_id, latitude, longitude, accuracy, timestamp | Belongs to Device; retained for 90 days |
| **AI_Alerts** | alert_id, device_id, content_type, category, confidence, app_source, is_acknowledged, created_at | Belongs to Device |
| **Safety_Checkins** | checkin_id, device_id, mood_score (1–5), unsafe_event, help_pressed, notes, alert_level, submitted_at | Belongs to Device; triggers parent notification on RED/YELLOW |
| **Geofences** | geofence_id, parent_user_id, name, center_lat, center_lng, radius_meters, is_active | Belongs to Parent User |

### 12.2 Entity Relationship Diagram

```
USERS (1) ──────────────── (N) DEVICES
  │                               │
  │                    ┌──────────┼──────────┐
  │                    │          │          │
  │               APP_BLOCKS  LOCATIONS  ACTIVITY_
  │               (N)          (N)       SESSIONS (N)
  │
  │                         DEVICES (1) ─── (N) AI_ALERTS
  │                         DEVICES (1) ─── (N) SAFETY_CHECKINS
  │
USERS (1) ──────────────── (N) GEOFENCES
```

### 12.3 AI Alert Content Types

| Content Type | Analysis Model | Categories Detected | Default Threshold |
|---|---|---|---|
| IMAGE | MobileNetV3 — TensorFlow Serving | NSFW, Violence, Drugs, Weapon, Self-Harm, Hate Symbols, Safe | 0.70 |
| TEXT | Multilingual BERT — Hugging Face | Cyberbullying, Threat, Sexual, Grooming, Self-Harm, Hate Speech, Clean | 0.70 |
| URL | 3-layer pipeline | Adult, Gambling, Violence, Weapons, Drugs, Phishing, Safe | Blocklist match = 0.99 |
| VIDEO | MobileNetV3 (keyframe sampling) | Same 7 categories as IMAGE | 0.70 |

---

## 13. System Flow

### 13.1 Primary Operational Flow

```
Child uses device
      │
      ▼
Background Service captures events
(app usage / content / location)
      │
      ├──── Routine data ────► Batch sync every 15 min ────► Backend DB
      │
      └──── Urgent content ──► Send immediately ──────────► AI Orchestrator
                                                                   │
                                                    ┌──────────────┼──────────────┐
                                                    ▼              ▼              ▼
                                              Image Module   Text Module    URL Module
                                                    │              │              │
                                                    └──────────────┼──────────────┘
                                                                   │
                                                            Confidence > threshold?
                                                                   │
                                                YES ───────────────┘
                                                 │
                                                 ▼
                                         Create Alert Record
                                                 │
                                                 ▼
                                    FCM High-Priority Notification
                                                 │
                                                 ▼
                                        Parent Views Alert
                                                 │
                                    ┌────────────┼────────────┐
                                    ▼            ▼            ▼
                               Acknowledge   Block App   Lock Device
```

### 13.2 App Block Sequence

| Step | Actor | Action | System Response |
|---|---|---|---|
| 1 | Parent | Opens App Control screen | System loads installed app list from backend API |
| 2 | System | Renders app list | Shows all apps with current block status and today's usage |
| 3 | Parent | Toggles YouTube to BLOCKED | UI immediately updates toggle to red/blocked state |
| 4 | Flutter App | `PATCH /api/apps/{deviceId}/youtube` | Backend validates parent authorization |
| 5 | Backend | Updates `app_blocks` table | Sets `is_blocked=true` for YouTube on this device |
| 6 | Backend | Sends FCM to child device | High-priority message: `BLOCK_APP: com.google.android.youtube` |
| 7 | Child Device | `FCMCommandReceiver` processes | Adds YouTube to local blocked apps list |
| 8 | System | Sends confirmation to parent | Backend updates device sync status |
| 9 | Child | Attempts to open YouTube | `AppLaunchInterceptor` shows 'Blocked by Parent' screen |

### 13.3 Check-in Alert Flow

| Step | Actor / Component | Action |
|---|---|---|
| 1 | Background Service | Detects configured check-in time; check-in not yet completed today |
| 2 | Child Device | Full-screen check-in prompt displayed; child cannot dismiss without responding |
| 3 | Child | Selects mood score, answers safety question, optionally adds notes, taps Submit |
| 4 | Flutter App | POSTs check-in payload to `/api/checkin/{deviceId}` |
| 5 | Backend | Saves to `safety_checkins` table. Evaluates alert level (Green / Yellow / Red) |
| 6a | If GREEN | Added to parent dashboard daily summary. No push notification. |
| 6b | If YELLOW | Standard FCM notification: 'Check-in response received — review recommended.' |
| 6c | If RED | HIGH-PRIORITY FCM to parent: 'Safety Check-in Alert — immediate action may be needed.' |
| 7 | Parent | Opens alert, reviews check-in details, contacts child or takes device action |

### 13.4 AI Content Detection Sequence

| Step | Actor / Component | Action |
|---|---|---|
| 1 | Child Device | `AccessibilityService` captures image/text/URL from active app |
| 2 | Content Scanner | Preprocesses content (resize, validate, base64 encode) |
| 3 | Flutter App | POSTs to `/api/analysis/image` (or `/text` / `/url`) |
| 4 | Backend API | Validates device token, routes to appropriate Analysis Service |
| 5 | AI Service | Runs model inference — returns category probabilities |
| 6 | Alert Engine | Checks if max probability > configured threshold (default 0.70) |
| 7 | Alert Engine | Creates `AlertEntity` record in `ai_alerts` table |
| 8 | FCM Service | Sends HIGH PRIORITY notification to parent device |
| 9 | Parent | Opens alert detail: category, confidence, blurred preview, source app |
| 10 | Parent | Chooses action: Acknowledge / Block App / Lock Device |

---

## 14. Error Handling & Edge Cases

| Scenario | System Response | User Impact |
|---|---|---|
| Child device offline when block command issued | Command queued in backend. Applied within 60 sec of reconnection. Parent sees 'Pending — device offline'. | Parent informed; restriction eventually applied automatically. |
| Child device offline when lock command issued | Lock command queued. `DevicePolicyManager.lockNow()` called immediately on reconnect. | Child device locked automatically on reconnecting. |
| AI model server unavailable | Analysis returns 503. No alert generated. Incident logged. Auto-retry after 30 sec. | Parent may miss alerts during model downtime; other monitoring continues. |
| Check-in submitted without network | Response saved to local SQLite queue. Synced to backend when connectivity restored. | Check-in recorded; parent notification may be slightly delayed. |
| Image submitted for analysis is corrupted | Backend validates image format. Returns 400. No alert generated. Event logged. | No false alert. Corrupted data silently rejected. |
| JWT access token expired mid-session | Flutter HTTP interceptor detects 401. Automatic refresh token exchange. Request retried transparently. | User experiences no interruption. |
| GPS location unavailable (indoor / tunnels) | Last known location shown with timestamp. Parent notified if location is > 30 min stale. | Parent sees staleness indicator and last-known timestamp. |
| Background service killed by Android OS | Foreground service notification + WorkManager scheduled restart within 30 seconds. | Brief monitoring gap. Service resumes automatically. |
| Child uninstalls or clears Guardian app | Backend detects missing FCM token on next sync. Parent receives alert: 'Guardian may have been removed.' | Parent alerted to investigate. |
| False positive AI alert | Parent sees blurred preview and confidence score. Can dismiss/acknowledge. Threshold adjustable in Settings. | Parent can tune sensitivity to reduce false positive frequency. |

---

## 15. Assumptions & Constraints

### 15.1 Technical Assumptions

| ID | Assumption |
|---|---|
| TA-01 | The child's device runs Android 8.0 (API level 26) or higher — required for `UsageStatsManager` and `DevicePolicyManager` APIs. |
| TA-02 | The parent grants Device Administrator permission during onboarding, enabling remote device lock/unlock. |
| TA-03 | The child's device has an active internet connection for the majority of the day (≥ 80% uptime assumed for real-time features). |
| TA-04 | Firebase Cloud Messaging is available and not blocked by the network or device manufacturer's notification management. |
| TA-05 | The AI model server maintains ≥ 99% uptime SLA to ensure consistent alert coverage. |
| TA-06 | The parent has access to a modern Android or iOS device for the parent dashboard application. |

### 15.2 Technical Constraints

| Constraint | Description | Impact |
|---|---|---|
| iOS Not Supported (v1.0) | Apple platform security model prevents equivalent monitoring to Android `AccessibilityService`. | iOS child devices cannot be monitored in version 1.0. |
| End-to-End Encrypted Apps | WhatsApp, Signal, and similar E2EE apps prevent content capture by design. | Text messages within E2EE apps cannot be analyzed. |
| Accessibility Permission | Content monitoring requires Accessibility Service permission, restricted on Android 14+. | Reduced monitoring coverage without explicit permission grants. |
| AI Model Size — Cloud Required | Production AI models require cloud inference infrastructure. | Monitoring degrades to usage-only mode without network connectivity. |
| Geofencing Accuracy | GPS accuracy in dense urban areas (Cairo) may vary ±50–200 metres. | Small geofences (< 200 m radius) may generate spurious alerts. |

### 15.3 Out of Scope — Version 1.0

| Feature Area | What Is Excluded |
|---|---|
| iOS Deep Monitoring | Full accessibility/content capture on iOS — requires separate architecture |
| Social Media Direct APIs | WhatsApp, Instagram, TikTok API integration (not publicly available) |
| End-to-End Encrypted Content | Analysis of E2EE messages (technically impossible by design) |
| School System Integration | Real-time coordination with school information systems |
| Additional Languages (NLP) | Languages other than Arabic (MSA + Egyptian dialect) and English |
| Network-Level Filtering | Router / ISP-level content filtering |
| Screen Recording / Keylogging | Real-time screen capture or keystroke logging (planned for future release) |

---

<div align="center">

**Guardians — Child Safety System**

*Modern Academy for Computer Science and Management Technology · Maadi*

*Supervised by: Dr. Lamia Hassan · Eng. Noha Ayman*

Team:

Christine Zakaria 

Mariam Alaa

Tassnem Mohammed

Jovan George

Johnny George

*Academic Year 2025 / 2026*



</div>
