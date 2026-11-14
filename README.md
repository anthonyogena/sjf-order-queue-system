# SJF Order Queue System

A decoupled Food & Beverage (F&B) kitchen order management system powered by a **Shortest Job First (SJF) with Dynamic Aging** scheduling algorithm. Short orders move to the front of the queue for rapid turnaround, while waiting orders gradually gain priority over time to prevent starvation.

---

## Architecture & Tech Stack

```text
sjf-order-queue-system/
├── backend/          # Java 21 + Spring Boot 4.1.1 REST API + PostgreSQL
├── frontend/         # Decoupled client UI (React / Vite)
└── README.md
```

### Priority Scoring Formula
Every `PENDING` order is ranked by ascending priority score (lowest score = highest priority):

$$\text{Priority Score} = \text{prepTimeMinutes} - \left(\frac{\text{waitSeconds}}{60} \times 0.5\right)$$

* **Short orders** start with a lower base score and get served immediately.
* **Aging factor (`0.5` pts/min)** reduces the score as an order waits, guaranteeing older complex orders never starve indefinitely.

---

## Local Setup

### 1. Prerequisites
* **Java 21** (JDK 21+)
* **PostgreSQL 15+** running locally on port `5432`
* **Node.js 20+** (for `frontend/`)

### 2. Database & Backend (`backend/`)
1. Create a PostgreSQL database named `sjf_order_queue` (or match your `.env` URL).
2. Copy the environment template inside `backend/`:
   ```bash
   cd backend
   cp .env.example .env
   ```
3. Fill in your local PostgreSQL credentials in `backend/.env`:
   ```env
   DB_URL=jdbc:postgresql://localhost:5432/sjf_order_queue
   DB_USERNAME=postgres
   DB_PASSWORD=your_postgres_password
   ```
4. Start the Spring Boot server (runs on `http://localhost:8080`):
   ```bash
   ./mvnw spring-boot:run
   ```

### 3. Frontend (`frontend/`)
1. Navigate to `frontend/` and install dependencies:
   ```bash
   cd frontend
   npm install
   npm run dev
   ```
2. Configure the frontend API base URL to `http://localhost:8080/api`.

---

## REST API Reference

Base URL: `http://localhost:8080/api`

| Method | Endpoint | Description |
| :--- | :--- | :--- |
| `POST` | `/orders` | Create a new order (`PENDING`) |
| `GET` | `/orders` | List all orders (supports optional `?status=` filter) |
| `GET` | `/orders/queue` | Fetch `PENDING` orders sorted by SJF + Aging priority |
| `GET` | `/orders/{id}` | Get details for a single order |
| `PATCH` | `/orders/{id}/start` | Transition order from `PENDING` $\rightarrow$ `IN_PROGRESS` |
| `PATCH` | `/orders/{id}/complete` | Transition order from `IN_PROGRESS` $\rightarrow$ `COMPLETED` |
| `PATCH` | `/orders/{id}/cancel` | Cancel a `PENDING` order (`CANCELLED`) |
| `GET` | `/orders/metrics` | Retrieve real-time queue KPIs & wait time statistics |

### Sample Payloads

#### `POST /api/orders` — Request
```json
{
  "customerName": "Alex",
  "items": "1x Iced Latte, 1x Croissant",
  "estimatedPrepTimeMinutes": 4
}
```

#### `GET /api/orders/queue` — Response (`200 OK`)
```json
[
  {
    "id": 12,
    "customerName": "Alex",
    "items": "1x Iced Latte, 1x Croissant",
    "estimatedPrepTimeMinutes": 4,
    "status": "PENDING",
    "waitSeconds": 90,
    "priorityScore": 3.25,
    "createdAt": "2026-11-14T13:30:00Z",
    "startedAt": null,
    "completedAt": null
  }
]
```

#### `GET /api/orders/metrics` — Response (`200 OK`)
```json
{
  "pendingCount": 4,
  "inProgressCount": 2,
  "completedCount": 19,
  "averageWaitTimeSeconds": 142.5,
  "averageTurnaroundTimeSeconds": 385.0
}
```

#### Error Response Format (`400` / `404` / `409`)
```json
{
  "timestamp": "2026-11-14T13:35:00Z",
  "status": 409,
  "error": "Conflict",
  "message": "Cannot start order 12 because its current status is COMPLETED",
  "path": "/api/orders/12/start"
}
```

---

## Git Workflow for Contributors

1. **Never initialize a nested Git repo** inside `frontend/` (if a scaffolding tool creates `frontend/.git`, delete it immediately).
2. Branch off `main` using descriptive names:
   ```bash
   git checkout -b feat/pos-order-entry
   ```
3. Push your branch and open a **Pull Request** into `main` referencing your GitHub Issue (e.g., `Closes #2`).
