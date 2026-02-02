# 🅿️ Parking Access Request System

An automated parking access request system built using **Camunda Platform 8**, **Java/Spring Boot**, and **PostgreSQL**.

---

## 🚀 Project Overview

This project implements a complete business process for parking access requests. It includes:

* User form handling
* Database verification
* BPMN decision logic
* Automated email notifications

The system leverages Camunda for workflow orchestration, Spring Boot for backend services, and PostgreSQL for persistent storage.

---

## 🛠 Core Components

* **Camunda Forms** 
* **HTML Website** 
* **REST Endpoint** 
* **BPMN Process** 
* **PostgreSQL** 
* **Email Service** 

---

## ⚙️ Configuration Steps

### 1️⃣ Database Setup (PostgreSQL)

Create a database named `parking` and execute the following schema and sample data setup:

```sql
-- SCHEMA
CREATE SCHEMA IF NOT EXISTS parking;

-- PARKING SPOTS
CREATE TABLE parking.parking_spots (
    category     VARCHAR(20) PRIMARY KEY,
    total_spots  INTEGER NOT NULL CHECK (total_spots >= 0),
    free_spots   INTEGER NOT NULL CHECK (free_spots >= 0),
    CHECK (category IN ('normal', 'disabled')),
    CHECK (free_spots <= total_spots)
);

INSERT INTO parking.parking_spots (category, total_spots, free_spots) VALUES
('normal', 100, 20),
('disabled', 20, 10);

-- STUDENTS
CREATE TABLE parking.students (
    id                BIGSERIAL PRIMARY KEY,
    first_name        VARCHAR(100) NOT NULL,
    last_name         VARCHAR(100) NOT NULL,
    index_number      VARCHAR(5) NOT NULL UNIQUE,
    student_id_valid  BOOLEAN NOT NULL,
    is_disabled       BOOLEAN NOT NULL
);

INSERT INTO parking.students (first_name, last_name, index_number, student_id_valid, is_disabled) VALUES
('Jan', 'Kowalski', 'S1234', TRUE, FALSE),
('Anna', 'Nowak', 'S1235', TRUE, TRUE);

-- STAFF
CREATE TABLE parking.staff (
    id                 BIGSERIAL PRIMARY KEY,
    employee_id        VARCHAR(5) NOT NULL UNIQUE,
    first_name         VARCHAR(100) NOT NULL,
    last_name          VARCHAR(100) NOT NULL,
    employee_id_valid  BOOLEAN NOT NULL
);

INSERT INTO parking.staff (employee_id, first_name, last_name, employee_id_valid) VALUES
('E0001', 'Paweł', 'Mazur', TRUE),
('E0002', 'Karolina', 'Zielińska', TRUE);

-- CANDIDATES
CREATE TABLE parking.candidates (
    id            BIGSERIAL PRIMARY KEY,
    first_name    VARCHAR(100) NOT NULL,
    last_name     VARCHAR(100) NOT NULL,
    index_number  VARCHAR(5) NOT NULL UNIQUE,
    city          VARCHAR(100),
    score         NUMERIC(5,2),
    qualified     BOOLEAN NOT NULL,
    email         VARCHAR(50) NOT NULL UNIQUE
);

INSERT INTO parking.candidates (first_name, last_name, index_number, city, score, qualified, email) VALUES
('Michał', 'Lis', 'C0001', 'Warszawa', 85.50, TRUE, 'm1@pl.pl'),
('Ewa', 'Kaczmarek', 'C0002', 'Poznań', 78.00, FALSE, 'e2@pl.pl');

-- ASSIGNED SPOTS
CREATE TABLE parking.assigned_spots (
    id          BIGSERIAL PRIMARY KEY,
    first_name  VARCHAR(100) NOT NULL,
    last_name   VARCHAR(100) NOT NULL,
    person_id   VARCHAR(5) NOT NULL UNIQUE
);

INSERT INTO parking.assigned_spots (first_name, last_name, person_id) VALUES
('Jan', 'Kowalski', 'S1234'),
('Paweł', 'Mazur', 'E0001');
```

---

### 2️⃣ Application Configuration (`application.properties`)

Update the connection parameters in `src/main/resources/application.properties`:

**Database Connection:**

```properties
spring.datasource.url=jdbc:postgresql://195.150.230.208:5432/YOUR_DB_USERNAME
spring.datasource.username=YOUR_DB_USERNAME
spring.datasource.password=YOUR_DB_PASSWORD
```

**Email API:**

```properties
mail.api.key=YOUR_EMAIL_API_KEY
mail.sender.address=your_email@example.com
```

**Camunda 8 SaaS:**

```properties
camunda.client.mode=saas
camunda.client.auth.client-id=YOUR_CLIENT_ID
camunda.client.auth.client-secret=YOUR_CLIENT_SECRET
camunda.client.cloud.cluster-id=YOUR_CLUSTER_ID
camunda.client.cloud.region=YOUR_REGION
```

---

### 3️⃣ Timer Start Event Configuration

To automate the student allocation ranking, you must configure the Timer Start Event within the Camunda Modeler as follows:

* **Select the Event:** Click on the Timer Start Event labeled "Ranking Evaluation - Date".
* **Configure Timer Type:** In the Properties Panel on the right, locate the Timer section.
* **Set Execution Date:**

    * Set the Type to Date.
    * In the Value field, enter the specific execution timestamp using the ISO 8601 format (e.g., 2025-12-17T07:05:00Z).

---

### 4️⃣ Running the Application

1. Clone the repository.
2. Configure the database and update `application.properties`.
3. Build and run using Maven:

```bash
mvn package exec:java
```

4. Open in browser: [http://localhost:8080/](http://localhost:8080/)
