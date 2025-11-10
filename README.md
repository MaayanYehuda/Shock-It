# 🥑 Shock-It – Agricultural Market Application

Shock-It is a **full-stack agricultural marketplace platform** designed to connect farmers and local markets.  
The system allows farmers to open and manage markets, invite other farmers to participate, and display their products to buyers — all through a modern Android app and a connected backend server.

Using the **Google Maps API**, the application displays all upcoming markets on an interactive map, allowing users to easily explore nearby markets and plan their participation.

---

## 🧩 Project Overview

The project consists of two main components:

### **1. Server (Neo4j & Node.js)**
Located in the `/server` directory.

- Built with **Node.js** and **Express.js**
- Uses a **Neo4j graph database** to store and manage relationships between farmers, markets, and products
- Provides a REST API consumed by the Android application
- Handles market creation, invitations, participation, and product management

### **2. Android Application**
Located in the `/android` directory.

- Developed using **Kotlin** in **Android Studio**
- Communicates with the server via **HTTP requests (REST API)**
- Provides a user-friendly interface for farmers to manage markets and view participants
- Implements modern UI design and architecture best practices

---

## ⚙️ Technologies Used

| Layer | Technologies |
|-------|---------------|
| **Backend (Server)** | Node.js, Express.js, Neo4j |
| **Frontend (Android App)** | Kotlin, Android Studio |
| **Database** | Neo4j Graph Database |
| **Version Control** | Git, GitHub |
| **Tools** | Visual Studio Code, Android Studio |

---

## 🧠 System Architecture

[Android App] ⇄ [Express Server] ⇄ [Neo4j Database]
- The Android app sends requests to the Node.js server.
- The server processes logic and communicates with the Neo4j database.
- Data is returned to the app to display relevant market and farmer information.

---

## 🚀 How to Run Locally

### **Server**
1. Navigate to the `server` directory:
  ```bash
   cd server 
   ```
2. Install dependencies:
  ```bash
   npm install 
   ```
3. Run the server:
  ```bash
   node server.js 
   ```

### **Android App**
1. Open the /android folder in Android Studio.
2. Sync Gradle.
3. Run the app on an emulator or physical device.

👥 Team

Developed collaboratively by:
Maayan Yehuda & Harel Rov
Both team members contributed to the design, development, and integration of the server, database, and Android application.

💡 Project Purpose

This project was developed as a final-year software engineering project at ORT Braude College.
It aims to make agricultural trading more accessible, digital, and efficient — by connecting farmers through a simple and intuitive mobile experience.

📁 Repository Structure
Shock-It/
├── server/     # Node.js + Neo4j backend
└── android/    # Kotlin Android app

🏁 License

This project is for educational and portfolio purposes.
All rights reserved © 2025 Maayan Yehuda & Harel Rov.
