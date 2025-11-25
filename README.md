# 🏖️ Spanish Beach Bot

**Intelligent multilingual Telegram bot for Spanish beach information**

[![Java 21](https://img.shields.io/badge/Java-21-orange?style=flat-square)](https://openjdk.org/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.5.5-brightgreen?style=flat-square)](https://spring.io/projects/spring-boot)
[![Telegram Bot API](https://img.shields.io/badge/Telegram%20Bot%20API-6.9.7-blue?style=flat-square)](https://core.telegram.org/bots/api)
[![Languages](https://img.shields.io/badge/Languages-English%20%7C%20Spanish-yellow?style=flat-square)](https://github.com)

---

## 👋 About

A modern Telegram bot built with **Java 21** and **Spring Boot 3.5.5**. Provides real-time information about Spanish beaches, including weather data, jellyfish alerts, and beach surface characteristics.

## ✨ Key Features

### 🌍 **Multilingual Support**
- 🇬🇧 **English** - full localization for international tourists
- 🇪🇸 **Español** - complete Spanish translation
- Language switching via `/language` or `/lang`
- Persistent language preferences per user

### 📍 **Beach Search**
- 🗺️ Accurate GPS coordinates via Nominatim OSM
- 📍 Full address and location confirmation
- ✍️ Smart typo correction for beach names
- 🇪🇸 Specialized for Spanish beaches

### 🌡️ **Weather Data**
- ☀️ Current air temperature (Open-Meteo API)
- 🌊 Water temperature with fallback systems
- 🏝️ Regional patterns for Mediterranean/Atlantic coasts
- 📊 Comfort level assessment with emoji indicators

### 🪼 **Jellyfish Monitoring**
- 🔍 Real-time data from 3 marine biology APIs (iNaturalist, GBIF, OBIS)
- ⚠️ 5-tier risk assessment (Very Low to Very High)
- 🦠 Species identification with danger classification
- 🚨 Safety recommendations based on current conditions

### 🏖️ **Beach Surface Analysis**
- 🏝️ Surface type detection (sand, rocks, pebbles, mixed)
- 📊 Data from multiple sources (database + OpenStreetMap)
- 🇪🇸 Regional info for Spanish coastlines

### ⚡ **Performance**
- 🚀 Parallel processing - all APIs called simultaneously
- 💾 Smart caching - 5-minute cache for repeated requests
- ⏱️ Progress notifications during search
- 🛡️ Graceful degradation - partial data on API failures
- 📈 Fast response (3-8 seconds average)

---

## 🚀 Quick Start

### **1. Requirements**
- **Java 21** (check: `java -version`)
- **Gradle** (included: `./gradlew`)
- Telegram Bot Token (get from [@BotFather](https://t.me/botfather))

### **2. Create Telegram Bot**
1. Find [@BotFather](https://t.me/botfather) in Telegram
2. Send `/newbot` command
3. Choose bot name and username (must end with `bot`)
4. Save the token

### **3. Configure**

**IMPORTANT:** Never commit real tokens/API keys to Git!

1. Copy example configuration:
```bash
cp src/main/resources/application.yml.example src/main/resources/application.yml
```

2. Edit `src/main/resources/application.yml` - add your token:
```yaml
telegram:
  bot:
    token: "YOUR_TELEGRAM_BOT_TOKEN_HERE"  # From @BotFather
    username: "YourBotUsername"             # Your bot name
```

**Note:** `application.yml` is protected via `.gitignore`

### **4. Run**

**Via Gradle:**
```bash
./gradlew bootRun
```

**Or build JAR and run:**
```bash
./gradlew build
java -jar build/libs/bot-0.0.1-SNAPSHOT.jar
```

### **5. Test**
1. Find your bot in Telegram by username
2. Send `/start`
3. Choose language via `/language`
4. Enter beach name: `Benidorm`, `Valencia`, `Marbella`

---

## 💻 Technical Stack

| Component | Technology | Version |
|-----------|------------|---------|
| **Language** | Java | 21 |
| **Framework** | Spring Boot | 3.5.5 |
| **Build** | Gradle | 8.x |
| **Telegram API** | TelegramBots | 6.9.7.1 |
| **Code Simplification** | Lombok | latest |
| **Logging** | Logback | Spring Boot default |

### **🔌 External APIs**

| Service | API | Purpose |
|---------|-----|---------|
| **Location** | Nominatim OSM | GPS coordinates, addresses |
| **Weather** | Open-Meteo | Air/water temperature |
| **Jellyfish** | iNaturalist + GBIF + OBIS | Marine biology data |
| **Beach Info** | OSM + database | Surface type, characteristics |

---

## 📱 Commands

| Command | Description |
|---------|-------------|
| `/start` | Welcome message (localized) |
| `/help` | Help and usage examples |
| `/language` or `/lang` | Language selection |

### **🏖️ Beach Search**
Simply type a beach name: `Benidorm`, `Valencia`, `Marbella`, `Playa de la Concha`

---

## 🗺️ Coverage

**Spanish Coastal Regions:**
- **Costa del Sol** - Málaga, Marbella, Torremolinos, Nerja
- **Costa Blanca** - Benidorm, Alicante, Calpe, Dénia  
- **Costa Brava** - Tossa de Mar, Lloret de Mar, Cadaqués
- **Balearic Islands** - Palma, Ibiza, Menorca
- **Canary Islands** - Las Canteras, Tenerife, Lanzarote
- **Northern Spain** - San Sebastián, Santander, Asturias

---

## 🎖️ Perfect For

- 🌍 **International tourists** visiting Spain
- 🏖️ **Beach vacation planning** - safety and comfort
- 🏪 **Tourism companies** - customer information services
- 📱 **Mobile apps** - bot integration
- ⚡ **Real-time systems** - quick beach data access

---

## 📞 Support

- 📚 Documentation: code comments in `/src`
- ⚙️ Configuration: `src/main/resources/application.yml`
- 📄 Logs: `/logs` directory
- ❓ Help: `/help` command in bot

---

**🌊 ¡Disfruta de las playas españolas de forma segura! 🏖️**  
**Enjoy Spanish beaches safely!**
