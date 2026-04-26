# Task Manager - Reto 3

API REST con Spring Boot desplegada en Kubernetes (Minikube), con autenticación JWT y persistencia en PostgreSQL. Proyecto del **Master en Microservicios con Java Spring Boot**.

## 📋 Resumen del Reto

Llevar un microservicio Spring Boot desde código fuente hasta ejecución en Kubernetes (Minikube), validando la API REST en cada etapa del proceso.

## 🛠️ Tecnologías

| Componente | Versión |
|---|---|
| Spring Boot | 3.2.4 |
| Java | 17 (Eclipse Temurin) |
| PostgreSQL | 16-alpine |
| Spring Security + JWT | JJWT 0.12.6 |
| Docker | Multi-stage build |
| Kubernetes | Minikube v1.36.0 |
| kubectl | v1.33.1 |

## 📦 Imagen Docker

Imagen publicada en Docker Hub:

jinostrozach/task-manager:1.0.0

Pull desde Docker Hub:
```bash
docker pull jinostrozach/task-manager:1.0.0
```

## 🚀 Ejecución Local con Docker Compose

Levanta la app + PostgreSQL en un solo comando:

```bash
docker compose up -d
```

La API queda disponible en `http://localhost:8080`.

Para detener:

```bash
docker compose down
```

## ☸️ Despliegue en Kubernetes (Minikube)

### 1. Iniciar Minikube
```bash
minikube start --driver=docker
```

### 2. Aplicar manifiestos en orden
```bash
kubectl apply -f k8s/namespace.yaml
kubectl apply -f k8s/configmap.yaml
kubectl apply -f k8s/secret.yaml
kubectl apply -f k8s/postgres.yaml
kubectl apply -f k8s/app.yaml
```

### 3. Verificar pods
```bash
kubectl get pods -n task-manager
```

### 4. Exponer el servicio
```bash
minikube service task-manager-service -n task-manager
```

Minikube genera un tunnel local que mapea el LoadBalancer a una URL accesible desde Windows.

## 🔌 Endpoints Principales

### Auth Service
| Método | Endpoint | Descripción |
|---|---|---|
| POST | `/api/auth/register` | Registro de usuario (devuelve JWT) |
| POST | `/api/auth/login` | Login (devuelve JWT) |

### Task Service (requiere Bearer Token)
| Método | Endpoint | Descripción |
|---|---|---|
| GET | `/api/tasks` | Listar todas las tareas |
| GET | `/api/tasks/{id}` | Obtener tarea por ID |
| POST | `/api/tasks` | Crear nueva tarea |
| PUT | `/api/tasks/{id}` | Actualizar tarea |
| DELETE | `/api/tasks/{id}` | Eliminar tarea |

## 📂 Estructura del Proyecto

task-manager/
├── src/main/java/com/taskmanager/
│   ├── controller/      # AuthController, TaskController
│   ├── dto/             # AuthRequest/Response, TaskRequest/Response
│   ├── exception/       # GlobalExceptionHandler
│   ├── model/           # Task, User, TaskStatus, TaskPriority
│   ├── repository/      # TaskRepository, UserRepository
│   ├── security/        # JwtUtil, JwtAuthenticationFilter, SecurityConfig
│   └── service/         # AuthService, TaskService
├── k8s/                 # Manifiestos Kubernetes
│   ├── namespace.yaml
│   ├── configmap.yaml
│   ├── secret.yaml
│   ├── postgres.yaml
│   └── app.yaml
├── Dockerfile           # Multi-stage build
├── docker-compose.yml   # App + PostgreSQL
└── pom.xml

## 📄 Informe del Reto

Ver `Reto3-SpringBoot-Docker-Kubernetes.docx` con todas las evidencias y screenshots de la ejecución.

## 👤 Autor

**Juan Carlos Inostroza** - Master en Microservicios con Java Spring Boot


