# CinePremier Backend Services

This repository contains the backend services for the CinePremier cinema management and booking platform.

## Architecture

- **`cinemaAI/`**: Core backend service built with Spring Boot 3 (Java 21) providing RESTful APIs for authentication, movie catalog, showtimes, seat booking, concessions, payment (VNPay), reviews, loyalty points, and staff/admin operations.
- **`AiService/`**: AI recommendation service built with Python FastAPI and Ollama for personalized movie recommendations and conversational assistance.

## Getting Started

### 1. Spring Boot Core Service (`cinemaAI`)
```bash
cd cinemaAI
./mvnw clean install
./mvnw spring-boot:run
```

### 2. AI Recommendation Service (`AiService`)
```bash
cd AiService
cp .env.example .env
pip install -r requirements.txt
python -m uvicorn app.main:app --host 0.0.0.0 --port 8000 --reload
```
