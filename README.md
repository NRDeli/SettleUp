# 🧾 SettleUp — Microservices-Based Group Expense Manager

**SettleUp** is a full-stack, microservices-driven application that helps groups track shared expenses, record contributions, and automatically compute settlements to balance dues among members.  

Built using **Spring Boot (Java 17)** for the backend and **React + Vite + Tailwind CSS** for the frontend, SettleUp emphasizes modularity, event-driven design, and modern DevOps practices with **Docker Compose**, **PostgreSQL**, and **RabbitMQ**.

---

## 🧩 Project Overview

SettleUp consists of three backend microservices and one frontend UI:

| Component | Port | Description |
|------------|------|-------------|
| **membership-service** | 8081 | Manages groups and members |
| **expense-service** | 8082 | Handles expense creation and validation |
| **settlement-service** | 8083 | Computes settlements among group members |
| **frontend (React)** | 5173 | User interface for group management and expense visualization |

All services communicate through **REST APIs** and **RabbitMQ** events, while persisting data in **PostgreSQL** databases.  
Each microservice is independently deployable and containerized.

---

## 🏗️ System Architecture

```
                             ┌────────────────────┐
                             │    Frontend (5173) │
                             │ React + Vite +     │
                             │ Tailwind CSS       │
                             └─────────┬──────────┘
                                       │
                                       ▼
        ┌───────────────┐       ┌───────────────┐       ┌────────────────┐
        │Membership Svc │<----->│Expense Svc    │<----->│Settlement Svc │
        │:8081          │  REST │:8082          │ Rabbit│:8083           │
        └──────┬────────┘       └───────────────┘ Queue └──────┬─────────┘
               │                         │                          │
               ▼                         ▼                          ▼
         ┌──────────────┐          ┌──────────────┐           ┌──────────────┐
         │ PostgreSQL DB│          │ PostgreSQL DB│           │ PostgreSQL DB│
         └──────────────┘          └──────────────┘           └──────────────┘
```

Communication pattern:
- `membership-service` validates group existence and members.
- `expense-service` emits **expense-created** events.
- `settlement-service` listens to those events and computes **settlement plans** asynchronously.

---

## ⚙️ Tech Stack

| Layer | Technologies |
|-------|---------------|
| **Backend** | Java 17, Spring Boot 3.3.x, Maven, JPA (Hibernate), REST |
| **Frontend** | React, Vite, Tailwind CSS |
| **Database** | PostgreSQL |
| **Messaging** | RabbitMQ |
| **DevOps / Infra** | Docker, Docker Compose |
| **Testing** | JUnit 5, Mockito |
| **Monitoring (optional)** | Prometheus, Grafana |

---

## 🚀 Getting Started

### 🧰 Prerequisites

Before running SettleUp locally, ensure the following are installed:

- [Java 17 or newer](https://adoptium.net/)
- [Apache Maven 3.9+](https://maven.apache.org/)
- [Node.js 18+](https://nodejs.org/)
- [Docker & Docker Compose](https://docs.docker.com/compose/)
- (optional) [pnpm](https://pnpm.io/) for faster frontend builds

---

### 📂 Clone the Repository

```bash
git clone https://github.com/<your-username>/SettleUp.git
cd SettleUp
```

---

### 🏗️ Build Backend Services

Each backend service uses Maven. You can build them individually or all at once:

```bash
cd membership-service && mvn -DskipTests package && cd ..
cd expense-service    && mvn -DskipTests package && cd ..
cd settlement-service && mvn -DskipTests package && cd ..
```

---

### 💅 Build the Frontend

```bash
cd frontend
npm install         # or pnpm install
npm run build
cd ..
```

---

### 🐳 Run with Docker Compose

At the root of the project, ensure you have `docker-compose.yml` and then run:

```bash
docker compose up -d
```

To view logs interactively:
```bash
docker compose up
```

To stop all containers:
```bash
docker compose down
```

---

### 🌐 Service Endpoints

| Service | URL | Description |
|----------|-----|-------------|
| Membership | [http://localhost:8081](http://localhost:8081) | Manage groups & members |
| Expense | [http://localhost:8082](http://localhost:8082) | Record & view expenses |
| Settlement | [http://localhost:8083](http://localhost:8083) | Compute who owes whom |
| Frontend | [http://localhost:5173](http://localhost:5173) | React user interface |

---

## 🔐 Environment Variables

Create a `.env` file in the project root (or service-specific `.env` files):

```ini
# Database
POSTGRES_USER=settleup
POSTGRES_PASSWORD=settleup
POSTGRES_DB=settleup

# RabbitMQ
RABBITMQ_DEFAULT_USER=guest
RABBITMQ_DEFAULT_PASS=guest

# JDBC URLs
MEMBERSHIP_DB_URL=jdbc:postgresql://db:5432/settleup
EXPENSE_DB_URL=jdbc:postgresql://db:5432/settleup
SETTLEMENT_DB_URL=jdbc:postgresql://db:5432/settleup
```

Each service can read the values via `application.yml`:
```yaml
spring:
  datasource:
    url: ${SPRING_DATASOURCE_URL}
    username: ${SPRING_DATASOURCE_USERNAME}
    password: ${SPRING_DATASOURCE_PASSWORD}
```

---

## 🧪 Testing APIs

### Example: Health Check
```bash
curl -i http://localhost:8081/actuator/health
```

### Create a Group
```bash
curl -X POST http://localhost:8081/api/groups   -H "Content-Type: application/json"   -d '{"name":"Trip to NYC"}'
```

### Add Member
```bash
curl -X POST http://localhost:8081/api/groups/1/members   -H "Content-Type: application/json"   -d '{"name":"Alice","email":"alice@example.com"}'
```

### Record Expense
```bash
curl -X POST http://localhost:8082/api/expenses   -H "Content-Type: application/json"   -d '{"groupId":1,"payerId":1,"amount":120.0,"description":"Dinner"}'
```

### Generate Settlement Plan
```bash
curl -X GET "http://localhost:8083/api/settlements/plan?groupId=1"
```

*(Adjust URLs and JSON keys according to your implementation.)*

---

## 🧠 Development Mode

Run services individually for faster debugging.

**Example: Membership Service**
```bash
cd membership-service
mvn spring-boot:run
```

**Frontend**
```bash
cd frontend
npm run dev
```

---

## 🧪 Testing

**Backend Tests**
```bash
mvn test
```

**Frontend Tests**
```bash
npm test
```

---

## 📁 Project Structure

```
SettleUp/
 ├─ membership-service/
 ├─ expense-service/
 ├─ settlement-service/
 ├─ frontend/
 ├─ docker-compose.yml
 ├─ .env.example
 ├─ README.md
 └─ .gitignore
```

---

## 🧯 Troubleshooting

| Issue | Possible Fix |
|-------|---------------|
| **Whitelabel Error Page (404)** | Ensure you're using `/api/...` endpoints. Add controller mappings if missing. |
| **Dependency not found (common module)** | Run `mvn install` inside `common/` (if applicable). |
| **Port already in use** | Stop any apps using ports 8081–8083 or change them in `application.yml`. |
| **RabbitMQ / DB connection errors** | Check your `.env` file and ensure `depends_on` is set in `docker-compose.yml`. |
| **Frontend 404** | Run `npm run dev` instead of `npm run build` during active development. |

---

## 🧭 Version Control & Git Workflow

### Initialize Repository (via IntelliJ)

1. In IntelliJ → **VCS → Create Git Repository**
2. Choose your project’s **root folder** → Click **Create**
3. Add `.gitignore` and `README.md` (this file)
4. Stage and commit:
   ```bash
   git add .
   git commit -m "chore: initialize SettleUp project"
   ```

### Push to GitHub

1. Create a new **empty** repository on GitHub named `SettleUp`
2. Add it as remote:
   ```bash
   git remote add origin https://github.com/<your-username>/SettleUp.git
   git branch -M main
   git push -u origin main
   ```

### Typical Workflow

```bash
# Create a new branch
git checkout -b feat/settlement-logic

# Stage & commit changes
git add .
git commit -m "feat: improved settlement computation logic"

# Push to remote
git push -u origin feat/settlement-logic

# Merge via GitHub Pull Request
```

### Tagging Releases

```bash
git tag -a v0.0.1 -m "Initial release"
git push origin v0.0.1
```

---

## 💡 Contribution Guidelines

1. Fork the repository
2. Create a new feature branch (`git checkout -b feat/<feature-name>`)
3. Commit and push changes
4. Open a Pull Request describing:
   - The motivation for the change
   - Relevant screenshots (for frontend)
   - Test cases if applicable

Use **conventional commits**:
```
feat: add group settlement calculation
fix: resolve expense duplication issue
chore: update docker-compose configuration
```

---

## 📜 License

This project is licensed under the **MIT License**.  
Feel free to use, modify, and distribute it as long as attribution is given.

---

## 🧠 Future Enhancements

- Add authentication (Spring Security + JWT)
- Introduce email notifications for settlements
- Add analytics dashboards (Grafana)
- Deploy to AWS ECS or GCP Cloud Run
- CI/CD integration with GitHub Actions

---

## 🐺 Built by the Pack @ NC State University
> A project developed as part of **Software Analysis & Design / DevOps coursework** to explore microservices, distributed communication, and real-world system design.
