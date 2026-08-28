# NETRA AI (नेत्रसहयोग) 👁️🎙️
### Intelligent Multimodal Vision, Spatial Navigation & Voice-First AI Companion for the Visually Impaired

**NETRA AI** is a state-of-the-art Android assistive application engineered to provide real-time spatial awareness, obstacle hazard detection, step-by-step pedestrian navigation, and natural conversational vision assistance for blind and visually impaired individuals.

The system combines on-device neural computer vision (ML Kit), sensor fusion (Compass, Gyroscope, Step Detector), and multi-tier generative AI models (Google Gemini 2.5 Flash, OpenRouter, and Teamo Router) to translate the physical world into low-latency voice and tactile feedback.

---

## 🌟 Key Capabilities at a Glance

- **🎙️ Voice-First Interaction**: Single-tap speech activation with an instant audio confirmation chime, live transcription, and natural conversational intelligence.
- **👁️ Real-Time Spatial Perception**: Instant detection of people, vehicles, animals, furniture, electronics, and drop-offs using camera frame analysis and 9-zone spatial mapping.
- **🧭 Free Space & Detour Guidance**: Proactively calculates clear corridor widths and recommends exact detours (*"Clear space on left, move left"*, *"रास्ता दाईं ओर साफ है"*).
- **🗣️ Trilingual Spoken Engine**: Complete, native-quality speech input and voice synthesis in **English**, **Hindi (हिन्दी)**, and **Marathi (मराठी)**.
- **📳 Intelligent Haptic Engine**: Tamed vibration feedback designed to eliminate sensory fatigue, with automatic tilt suppression when the phone is facing down.
- **🗺️ Offline Pedestrian Navigation**: Full routing engine with pre-mapped pedestrian tactile crossings, stairs warnings, and landmark guidance (e.g. Pune City FC Road / Deccan OSM dataset).
- **⚡ Dual Mode (Offline + Cloud AI)**: Operates 100% offline with zero latency, while seamlessly routing complex visual queries to cloud multimodal models when connected.

---

## 🏗️ System Architecture & AI Model Pipeline

```
                                  ┌────────────────────────┐
                                  │   Android Device       │
                                  │  CameraX Video Stream  │
                                  └──────────┬─────────────┘
                                             │
                       ┌─────────────────────┴─────────────────────┐
                       ▼                                           ▼
         ┌───────────────────────────┐               ┌───────────────────────────┐
         │ Realtime Perception Engine │               │   Gemini Vision Assistant │
         │   (Google ML Kit Vision)  │               │   (Multimodal Generative) │
         └─────────────┬─────────────┘               └─────────────┬─────────────┘
                       │                                           │
         ┌─────────────┴─────────────┐               ┌─────────────┴─────────────┐
         │  9-Zone Spatial Analyzer  │               │ Tier 1: Gemini 2.5 Flash  │
         │  & Step Distance Estimator│               │ Tier 2: OpenRouter Vision │
         └─────────────┬─────────────┘               │ Tier 3: Teamo / NVIDIA    │
                       │                             │ Tier 4: On-Device Reasoner│
                       ▼                             └─────────────┬─────────────┘
         ┌───────────────────────────┐                             │
         │  Obstacle Priority Engine │                             │
         │  (URGENT / WARN / INFO)   │                             │
         └─────────────┬─────────────┘                             │
                       │                                           │
                       └─────────────────────┬─────────────────────┘
                                             ▼
                              ┌─────────────────────────────┐
                              │     BlindAIViewModel        │
                              │ (Sensor Fusion & Arbitrator)│
                              └──────────────┬──────────────┘
                                             │
                       ┌─────────────────────┴─────────────────────┐
                       ▼                                           ▼
         ┌───────────────────────────┐               ┌───────────────────────────┐
         │    VoiceAlertManager      │               │   HapticFeedbackManager   │
         │ (TTS with Auto-Mute Logic)│               │ (Tactile Chimes & Alerts) │
         └───────────────────────────┘               └───────────────────────────┘
```

---

## 🧠 AI Models & Visual Reasoning Tiers

NETRA AI employs a failover multi-tier architecture to ensure fast responses and high reliability:

### 1. Tier 1: Google Gemini 2.5 Flash / 1.5 Flash (Native Multimodal)
- **Model**: `models/gemini-2.5-flash` / `models/gemini-1.5-flash`
- **Purpose**: High-fidelity multimodal visual understanding, scene layout description, reading complex text, and answering freeform user queries.
- **Latency**: Sub-second streaming with optimized token limits and reduced temperature (0.3).

### 2. Tier 2: OpenRouter Multimodal Gateway
- **Endpoint**: `https://openrouter.ai/api/v1/chat/completions`
- **Model**: `google/gemini-2.5-flash`
- **Purpose**: Resilient secondary fallback with zero configuration required.

### 3. Tier 3: Teamo Router & NVIDIA NIM Endpoints
- **Endpoint**: `https://api.teamorouter.com/v1/chat/completions`
- **Purpose**: Enterprise high-throughput routing redundancy.

### 4. Tier 4: On-Device Semantic Reasoning Engine
- **Purpose**: Operates with **zero internet connection**. Takes detected object bounding boxes, distance calculations, walking corridor clearance, and compass bearings to synthesize natural voice descriptions.

---

## 🎯 Object Detection & Spatial Perception

The offline perception pipeline processes camera frames in real time using **Google ML Kit Object Detection and Tracking**:

### 1. Detected Obstacle Classes
| Category | Recognized Objects | Spoken Alert Examples |
| :--- | :--- | :--- |
| **People** | Pedestrians, groups of people, crowds | *"Person 3 steps ahead. Clear space on left."* / *"सामने लोग खड़े हैं। बाईं ओर जगह है।"* |
| **Vehicles** | Cars, Motorcycles, Bicycles, Buses, Trucks | *"Car 4 steps ahead. Move right."* / *"आगे 3 कदम पर कार है। दाईं ओर मुड़ें।"* |
| **Animals** | Dogs, Cats, Street Animals | *"Dog 2 steps ahead. Step left."* / *"पुढे २ पावलांवर कुत्रा आहे."* |
| **Furniture** | Chairs, Benches, Tables, Desks | *"Chair 2 steps ahead in walking path."* / *"आगे मेज है।"* |
| **Personal Items**| Mobile Phones, Laptops, Bags, Backpacks | *"Mobile phone on table."* / *"बैग सामने है।"* |
| **Infrastructure**| Walls, Closed Doors, Pillars, Poles, Stairs | *"Stairs or step ahead. Proceed carefully."* / *"आगे सीढ़ियाँ हैं।"* |

### 2. 9-Zone Spatial Corridor Matrix
The camera field of view is subdivided into 9 distinct spatial zones:
- `FAR_LEFT` (0% - 18% width)
- `LEFT` (18% - 35% width)
- `CENTER_LEFT` (35% - 45% width)
- `CENTER` (45% - 55% width — **Immediate Walking Path**)
- `CENTER_RIGHT` (55% - 65% width)
- `RIGHT` (65% - 82% width)
- `FAR_RIGHT` (82% - 100% width)
- `UPPER_HAZARD` (Overhead obstacles / low ceilings / hanging branches)
- `LOW_GROUND_HAZARD` (Curbs, potholes, drop-offs)

### 3. Step & Distance Estimation
- Translates object pixel heights and bounding box positions into metric distances ($0.3\text{ m}$ to $6.0\text{ m}$).
- Converts meters into human walking steps (standard average stride length: $0.65\text{ m}$), speaking in terms like *"3 steps ahead"* rather than raw decimals.

---

## 🎙️ Voice & Haptic Interaction Design

### 1. Single Ring Confirmation Chime
- When tapping the voice button, NETRA AI immediately emits a distinct **single prompt chime** (`ToneGenerator.TONE_PROP_PROMPT`) to confirm listening mode has activated.
- When speech recognition concludes, an acknowledgment chime sounds before processing begins.

### 2. Smart Conversation Muting (Zero Chatter)
- **Problem**: Continuous obstacle alarms talking over the user or AI answers.
- **Solution**: The moment speech listening starts or NETRA AI is answering, background obstacle announcements and routine alarms are muted.

### 3. Tamed Tactile Feedback
- **Urgent Hazards Only**: Vibrations only occur if an obstacle is within $< 1.0\text{ m}$ in the center walking corridor.
- **Gentle Tap**: Short 90ms micro-pulse replacing harsh repeated vibration loops.

### 4. Downward Tilt / Floor Facing Detection
- Using device **Gyroscope** and **Rotation Vector Sensors**, NETRA AI detects if the phone is facing down ($|\text{pitch}| > 55^\circ$ or $|\text{roll}| > 135^\circ$).
- When pointed at the ground, in a pocket, or face down on a table, all vibrations and alerts are suppressed.

---

## 🌐 Supported Languages & Localization

NETRA AI is built for multilingual accessibility across 3 primary languages:

| Feature | English 🇬🇧 🇺🇸 | Hindi (हिन्दी) 🇮🇳 | Marathi (मराठी) 🇮🇳 |
| :--- | :--- | :--- | :--- |
| **Speech-to-Text** | `en-US`, `en-IN` | `hi-IN` | `mr-IN` |
| **Text-to-Speech** | English Neural TTS | Hindi Neural TTS | Marathi Neural TTS |
| **Hazard Alerts** | *"Caution! Car 3 steps ahead."* | *"सावधान! आगे 3 कदम पर कार है।"* | *"सावध रहा! पुढे ३ पावलांवर गाडी आहे."* |
| **Detour Guidance**| *"Clear space on left, move left."*| *"बाईं ओर जगह है, बाएँ मुड़ें।"* | *"डावीकडे जागा आहे, डावीकडे वळा."* |
| **Path Status** | *"Path is clear for 8 steps."* | *"रास्ता 8 कदम पूरी तरह साफ है।"*| *"रस्ता ८ पावले पूर्णपणे मोकळा आहे."* |

---

## 🗺️ Offline Pedestrian Navigation (Pune City Dataset)

NETRA AI includes a built-in offline pedestrian navigation router utilizing OpenStreetMap (OSM) data centered around Pune (FC Road, JM Road, Deccan Gymkhana):

- **Tactile Paving Awareness**: Identifies sidewalks with tactile paving for guide canes.
- **Crossing & Hazard Warnings**: Alerts for zebra crossings, traffic lights, and curbs.
- **Key Pre-Loaded POIs**:
  - *Fergusson College Main Gate* (फर्ग्युसन कॉलेज)
  - *Poona Hospital & Research Centre* (पूना हॉस्पिटल)
  - *Apollo Pharmacy FC Road* (अपोलो फार्मसी)
  - *Deccan PMPML Bus Station* (डेक्कन बस स्थानक)
  - *Goodluck Cafe / Deccan Gymkhana* (कॅफे गूडलक)
  - *Chhatrapati Sambhaji Garden* (संभाजी उद्यान)

---

## 📱 User Interface & Navigation Tabs

The app uses a high-contrast dark theme designed with large touch targets ($> 48\text{ dp}$):

1. **Vision HUD (Tab 1)**:
   - Live camera preview with spatial bounding boxes.
   - Large central **Tap to Speak / Ask Anything** button.
   - Stop audio button and quick language selector (EN / HI / MR).
   - "OFFLINE VISION ACTIVE" quick toggle badge.
2. **NETRA AI Live (Tab 2)**:
   - Full conversational multimodal interface.
   - Live audio waveforms and direct responses.
   - Fast suggestion chips (*"How many steps can I walk?"*, *"Is path clear?"*, *"What's in front of me?"*).
3. **Pedestrian Guide (Tab 3)**:
   - Destination selector with category filters (Hospitals, Pharmacies, Transit, Colleges).
   - Turn-by-turn walking directions with compass bearings and distance counters.
4. **Telemetry & Sensors (Tab 4)**:
   - Real-time diagnostic readouts for Azimuth, Pitch, Roll, Accelerometer, Step Count, and ML inference FPS.

---

## 🛠️ Technology Stack & Dependencies

- **Language**: Kotlin 2.0 (100% Kotlin DSL)
- **UI Framework**: Jetpack Compose (Material Design 3)
- **Camera**: AndroidX CameraX (Camera2, Lifecycle, View)
- **On-Device ML**: Google ML Kit Object Detection & Text Recognition (OCR)
- **Networking**: Retrofit 2, OkHttp 3, Kotlinx Coroutines, Moshi
- **Sensors**: Android SensorManager (Rotation Vector, Accelerometer, Gyroscope, Step Detector)
- **Audio & Speech**: Android `TextToSpeech`, `SpeechRecognizer`, `ToneGenerator`
- **Testing**: Robolectric, Roborazzi Screenshot Verification, JUnit 4

---

## ⚙️ Configuration & Secrets

API keys are managed via `.env` (handled by the Secrets Gradle Plugin):

```bash
# In .env (or Secrets panel in AI Studio)
GEMINI_API_KEY=your_gemini_api_key_here
```

*Note: If no API key is present, the app automatically switches to the on-device Tier 4 reasoning engine without crashing or failing.*

---

## 🚀 Build & Compilation

To build and compile the APK using Gradle:

```bash
# Run unit tests
gradle :app:testDebugUnitTest

# Assemble Debug APK
gradle :app:assembleDebug
```

---

## 📄 License & Accessibility Statement

Built with ❤️ for assistive accessibility. Designed to empower blind and visually impaired individuals with independent, safe, and intuitive spatial mobility.
