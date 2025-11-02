# 🗝️ chat-e2e — End-to-End Encrypted Chat

A modern proof of concept for a fully **end-to-end encrypted chat**,  
built with **Spring Boot (Java 21)**, **React + Vite (TypeScript)**, and **PostgreSQL + Redis**.  
Runs locally via **Docker Compose** and can easily be deployed to **AWS or GCP**.

---

## ⚙️ Tech Stack

**Backend:** Spring Boot 3 (Maven)  
**Frontend:** React + Vite + TypeScript  
**Database:** PostgreSQL  
**Cache / PubSub:** Redis  
**Infrastructure:** Terraform (AWS / GCP), Docker Compose  
**CI/CD:** GitHub Actions (Build, Test, Deploy)

---

## 🧭 Architecture Overview

- **Frontend:**
    - React + Vite + WebCrypto (E2E encryption handled in the browser)
    - IndexedDB for local key storage
    - Communicates with backend via REST + WebSocket

- **Backend (Spring Boot):**
    - Auth (PAKE + 2FA), Vault (cipher vault), Messaging (REST / WS), Media (S3 / GCS)
    - PostgreSQL for persistent data, Redis for realtime events

→ See `docs/architecture/` for PlantUML diagrams (System, Components, Sequences, ERD, Deployment)

---

## 🧑‍💻 Local Development

**Requirements:**
- Java 21 (Temurin or OpenJDK)
- Maven 3.9+
- Node 20+ with pnpm 9+
- Docker & Docker Compose

```bash
# Start backend
cd backend
mvn spring-boot:run

# Start frontend
cd ../frontend
pnpm install
pnpm dev
