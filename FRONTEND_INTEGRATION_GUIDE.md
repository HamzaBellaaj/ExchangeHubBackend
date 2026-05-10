# FRONTEND INTEGRATION GUIDE - ExchangeHub Backend

## 1. Resume architecture frontend/backend

ExchangeHub is a Spring Boot 3 backend exposing REST APIs for programme management, candidatures, documents, interviews, decisions, CV standardization, and n8n callbacks.

Frontend target: Angular, React, or Vue.

Recommended frontend architecture:
- Keycloak handles authentication.
- Frontend stores only the access token in memory or secure session storage.
- Every API call sends `Authorization: Bearer <access_token>`.
- Backend validates JWT as a resource server.
- Business permissions are enforced twice:
  - in `SecurityConfig` by role
  - in services by owner checks and workflow rules

Important current behavior:
- There is no dedicated backend login endpoint.
- There is no public users API in the current codebase.
- User provisioning is automatic on first authenticated request through `CurrentUserService`.
- Uploading a CV triggers automatic n8n analysis after the document is saved.

---

## 2. Auth Keycloak

### 2.1 Keycloak values extracted from backend config and project dossier

- Keycloak base URL: `http://[::1]:8080`
- Realm: `exchangehub`
- Client ID: `exchangehub-backend`
- Issuer URI: `http://[::1]:8080/realms/exchangehub`
- JWK set URI: `http://[::1]:8080/realms/exchangehub/protocol/openid-connect/certs`

### 2.2 Recommended login method for frontend

Recommended method:
- login via Keycloak integrated
- Authorization Code Flow with PKCE for Angular / React / Vue

Reason:
- this is the correct browser-based flow for a frontend SPA
- it avoids storing user passwords in the frontend
- it fits Spring Resource Server validation on the backend

### 2.3 JWT transport to backend

Send the token like this:

```http
Authorization: Bearer <access_token>
```

### 2.4 JWT claims used by the backend

Claims consumed by the backend:
- `email` - used by `CurrentUserService` to resolve the application user
- `roles` - used by `SecurityConfig` to map authorities
- `sub` - stored as `keycloakId`
- `given_name` - stored as `prenom` on first user provisioning
- `family_name` - stored as `nom` on first user provisioning

### 2.5 Roles in the backend

Exact roles:
- `CANDIDAT`
- `COORDINATEUR`
- `RESPONSABLE`
- `ADMIN`

### 2.6 JIT provisioning

`CurrentUserService` creates a local `User` row automatically if the JWT email is not found in the database.

Behavior:
- lookup user by `email`
- if not found, create a new `User`
- default role is `CANDIDAT`
- if `roles` claim exists, the first role is used to set the local role

Frontend impact:
- first authenticated request can create the application user automatically
- no separate registration flow is exposed in the backend

---

## 3. Base URL backend

Values extracted from `application.properties`:

- `server.port = 8081`
- local API base URL: `http://localhost:8081`
- Swagger UI: `http://localhost:8081/swagger-ui.html`
- OpenAPI docs: `http://localhost:8081/v3/api-docs`

Other useful URLs:
- n8n webhook base URL: `http://localhost:5678`
- n8n callback to backend: `http://localhost:8081/cv-standardization/import`

---

## 4. Endpoints by module

### 4.1 Auth / Current user

There is no dedicated controller for login, logout, or current user.

Current user resolution is internal only:
- `CurrentUserService` reads the JWT from `SecurityContextHolder`
- it extracts the `email` claim
- it loads or creates the local `User`

Frontend responsibility:
- authenticate with Keycloak
- attach the access token to every request

---

### 4.2 Programmes

Base path: `/programmes`

| Method | URI | Roles | Query / Body | Response | Notes |
|---|---|---|---|---|---|
| GET | `/programmes` | `CANDIDAT`, `COORDINATEUR`, `RESPONSABLE`, `ADMIN` | Query params: `statut`, `typeMobilite`, `pays` | `List<ProgrammeResponse>` | Filters are optional |
| GET | `/programmes/{id}` | `CANDIDAT`, `COORDINATEUR`, `RESPONSABLE`, `ADMIN` | Path variable `id` | `ProgrammeResponse` | Read one programme |
| POST | `/programmes` | `RESPONSABLE`, `ADMIN` | `CreateProgrammeRequest` | `ProgrammeResponse` | Create programme |
| PATCH | `/programmes/{id}` | `RESPONSABLE`, `ADMIN` | `UpdateProgrammeRequest` | `ProgrammeResponse` | Update programme |

Example request for create/update:

```json
{
  "titre": "Erasmus Espagne 2026",
  "description": "Mobilite semestrielle",
  "typeMobilite": "ETUDES",
  "pays": "Espagne",
  "universitePartenaire": "Universidad X",
  "dateDebut": "2026-09-01",
  "dateFin": "2027-01-31",
  "dateLimiteCandidature": "2026-06-15",
  "statut": "OUVERT",
  "coordinateurId": "uuid",
  "responsableId": "uuid"
}
```

---

### 4.3 Candidatures

Base path: `/candidatures`

| Method | URI | Roles | Query / Body | Response | Notes |
|---|---|---|---|---|---|
| POST | `/candidatures` | `CANDIDAT` | `CreateCandidatureRequest` | `Candidature` entity | Returns entity, not DTO |
| GET | `/candidatures` | `CANDIDAT`, `COORDINATEUR`, `RESPONSABLE`, `ADMIN` | Query params: `statut`, `programmeId`, `candidatId`, `includeArchived` | `List<CandidatureListResponse>` | Candidate sees only own records |
| GET | `/candidatures/{id}` | `CANDIDAT`, `COORDINATEUR`, `RESPONSABLE`, `ADMIN` | Path variable `id` | `CandidatureDetailResponse` | Candidate ownership checked in service |
| GET | `/candidatures/{candidatureId}/documents` | same as above | Path variable `candidatureId` | `List<DocumentResponse>` | Ownership checked in `DocumentService` |
| GET | `/candidatures/{id}/documents/validation` | same as above | Path variable `id` | `DocumentValidationResponse` | Validates required documents |
| PATCH | `/candidatures/{id}/statut` | `COORDINATEUR`, `RESPONSABLE`, `ADMIN` | `UpdateCandidatureStatusRequest` | `CandidatureStatusResponse` | Candidate forbidden |
| PATCH | `/candidatures/{id}/archive` | `COORDINATEUR`, `RESPONSABLE`, `ADMIN` | none | `CandidatureArchiveResponse` | Candidate forbidden |
| GET | `/candidatures/{candidatureId}/decision` | HTTP security allows all roles, service limits candidate by ownership | Path variable `candidatureId` | `DecisionResponse` | Useful for detail page |
| GET | `/candidatures/{candidatureId}/entretien` | HTTP security allows all roles, service limits candidate by ownership | Path variable `candidatureId` | `EntretienResponse` | Useful for detail page |

Example create candidature request:

```json
{
  "programmeId": "uuid-programme"
}
```

Example candidature detail response:

```json
{
  "id": "uuid-candidature",
  "statut": "SOUMISE",
  "submittedAt": "2026-05-10T10:00:00",
  "programmeId": "uuid-programme",
  "programmeTitre": "Erasmus Espagne 2026",
  "programmePays": "Espagne",
  "documents": []
}
```

Business note:
- a candidate can create one candidature per programme only
- duplicate `(candidatId, programmeId)` is rejected

---

### 4.4 Documents

Base path: `/documents`

| Method | URI | Roles | Body / Params | Response | Notes |
|---|---|---|---|---|---|
| POST | `/documents/upload` | `CANDIDAT`, `COORDINATEUR`, `ADMIN` | multipart form-data: `candidatureId`, `typeDocument`, `file` | `UploadDocumentResponse` | `RESPONSABLE` is blocked by service |
| GET | `/documents/{documentId}/signed-url` | `CANDIDAT`, `COORDINATEUR`, `RESPONSABLE`, `ADMIN` | Path variable `documentId` | `SignedUrlResponse` | Candidate ownership enforced in service |

Multipart upload example:

```http
POST /documents/upload
Content-Type: multipart/form-data
```

Fields:
- `candidatureId`: UUID
- `typeDocument`: `CV`, `RELEVE_NOTES`, `LETTRE_MOTIVATION`, `PASSEPORT`, `AUTRE`
- `file`: uploaded file

Example upload response:

```json
{
  "documentId": "uuid-document",
  "candidatureId": "uuid-candidature",
  "typeDocument": "CV",
  "fileName": "cv.pdf",
  "storagePath": "candidatures/uuid-candidature/uuid_cv.pdf",
  "fileUrl": "https://...",
  "mimeType": "application/pdf",
  "size": 123456,
  "uploadedAt": "2026-05-10T10:00:00"
}
```

Signed URL response:

```json
{
  "documentId": "uuid-document",
  "signedUrl": "https://...",
  "expiresIn": 600
}
```

Business note:
- signed URLs are temporary
- current expiry is 600 seconds for document service
- CV upload triggers automatic n8n analysis in the current code

---

### 4.5 Validation documents

Endpoint exposed through candidature controller:

| Method | URI | Roles | Response | Notes |
|---|---|---|---|---|
| GET | `/candidatures/{id}/documents/validation` | `CANDIDAT`, `COORDINATEUR`, `RESPONSABLE`, `ADMIN` | `DocumentValidationResponse` | Candidate ownership checked by service |

Validation logic currently implemented:
- `CV` is always required
- if programme mobility type is `ETUDES`, `RELEVE_NOTES` is also required

Response example:

```json
{
  "candidatureId": "uuid-candidature",
  "valid": false,
  "requiredDocuments": ["CV", "RELEVE_NOTES"],
  "uploadedDocuments": ["CV"],
  "missingDocuments": ["RELEVE_NOTES"]
}
```

---

### 4.6 Entretiens

Base path: none explicit on controller, routes are direct.

| Method | URI | Roles | Body / Params | Response | Notes |
|---|---|---|---|---|---|
| POST | `/entretiens` | `COORDINATEUR`, `ADMIN` | `CreateEntretienRequest` | `EntretienResponse` | Planning only |
| GET | `/entretiens/{id}` | `CANDIDAT`, `COORDINATEUR`, `RESPONSABLE`, `ADMIN` | Path variable `id` | `EntretienResponse` | Candidate ownership enforced in service |
| GET | `/candidatures/{candidatureId}/entretien` | `CANDIDAT`, `COORDINATEUR`, `RESPONSABLE`, `ADMIN` | Path variable `candidatureId` | `EntretienResponse` | Useful for candidature detail |
| PATCH | `/entretiens/{id}/statut` | `COORDINATEUR`, `ADMIN` | `UpdateEntretienStatusRequest` | `EntretienResponse` | Status update |

Create entretien request example:

```json
{
  "candidatureId": "uuid-candidature",
  "dateHeure": "2026-05-20T14:30:00",
  "mode": "VISIO",
  "lienVisio": "https://meet.example.com/abc",
  "lieu": null
}
```

Rules:
- `VISIO` requires `lienVisio`
- `PRESENTIEL` requires `lieu`
- `TELEPHONE` is not currently accepted by the service validation
- one interview per candidature only

---

### 4.7 Decisions

| Method | URI | Roles | Body / Params | Response | Notes |
|---|---|---|---|---|---|
| POST | `/decisions` | `RESPONSABLE`, `ADMIN` | `CreateDecisionRequest` | `DecisionResponse` | One decision per candidature |
| GET | `/candidatures/{candidatureId}/decision` | `CANDIDAT`, `COORDINATEUR`, `RESPONSABLE`, `ADMIN` by security; ownership checked for candidate | Path variable `candidatureId` | `DecisionResponse` | Useful for candidature detail |

Create decision request example:

```json
{
  "candidatureId": "uuid-candidature",
  "decision": "ACCEPTEE",
  "commentaire": "Dossier solide"
}
```

Decision values:
- `ACCEPTEE`
- `REFUSEE`
- `LISTE_ATTENTE`

Business note:
- a candidature can have only one final decision
- decision also updates candidature status through workflow

---

### 4.8 CV standardization / CV profile / analysis / n8n

Current code paths related to CV standardization:

| Method | URI | Roles | Notes |
|---|---|---|---|
| POST | `/n8n/cv-standardization/callback` | public in security config | n8n posts analyzed CV result to backend |
| POST | `/cv-standardization/import` | public in security config | legacy import endpoint for n8n callback |
| POST | `/candidatures/{id}/standardize-cv` | authorized in `SecurityConfig`, but no controller is currently present in the codebase | security rule exists, but route is not exposed by a controller |

Current actual automatic behavior:
- `POST /documents/upload` with `typeDocument = CV` triggers automatic n8n analysis via `N8nCvStandardizationService.triggerStandardizationAuto`
- the backend sends to n8n:
  - `candidatureId`
  - `documentId`
  - `signedUrl`
  - `callbackUrl`

n8n callback request shape:

```json
{
  "candidatureId": "uuid-candidature",
  "documentId": "uuid-document",
  "fullName": "Jean Dupont",
  "email": "jean@example.com",
  "phone": "+21600000000",
  "summary": "Profile summary",
  "experiences": [],
  "formations": [],
  "competences": [],
  "langues": [],
  "analyse": {
    "scoreGlobal": 82,
    "scoreExperience": 75,
    "scoreFormation": 80,
    "scoreLangues": 70,
    "scoreCompetences": 90,
    "pointsForts": "...",
    "pointsFaibles": "...",
    "recommandation": "Bonne candidature"
  }
}
```

Callback response example:

```json
{
  "candidatureId": "uuid-candidature",
  "documentId": "uuid-document",
  "cvProfileId": "uuid-profile",
  "statut": "ANALYSEE"
}
```

Important frontend consequence:
- frontend does not call the n8n callback
- frontend only uploads the CV and then refreshes the candidature detail until the CV profile appears

---

## 5. Workflows frontend principaux

### 5.1 Connexion utilisateur

Suggested frontend flow:
1. redirect user to Keycloak login
2. Keycloak returns access token
3. frontend stores token in memory or secure session storage
4. every API request sends `Authorization: Bearer <access_token>`
5. first backend call can auto-create the local user if it does not exist yet

JIT provisioning note:
- there is no explicit sign-up screen in the backend
- the backend creates the application user from the JWT on first request

---

### 5.2 Candidat cree une candidature

Recommended flow:
1. login with Keycloak
2. call `GET /programmes` to list open programmes
3. select a programme
4. call `POST /candidatures` with `programmeId`
5. upload CV and other documents through `POST /documents/upload`
6. CV upload automatically triggers n8n analysis
7. poll or refresh `GET /candidatures/{id}` and `GET /candidatures/{id}/documents/validation`
8. when analysis is done, fetch `CvProfile` data from candidature detail workflow and display it

UI note:
- the candidate should not manually trigger the analysis in the current version
- analysis is automatic after CV upload

---

### 5.3 Upload document

Upload flow:
1. select candidature
2. choose document type
3. upload file as multipart form-data
4. backend stores file in Supabase Storage
5. backend stores document metadata in PostgreSQL
6. if type is CV, backend automatically sends the CV to n8n

Constraints:
- `spring.servlet.multipart.max-file-size = 10MB`
- `spring.servlet.multipart.max-request-size = 10MB`

Supported document types:
- `CV`
- `RELEVE_NOTES`
- `LETTRE_MOTIVATION`
- `PASSEPORT`
- `AUTRE`

---

### 5.4 Consultation candidature

Candidate experience:
- list only own candidatures
- open candidature detail
- see documents attached
- see document validation status
- see interview if it exists
- see decision if it exists
- see CV profile and analysis after n8n callback

Staff experience:
- coordinateur sees broader candidature list
- responsable sees broader candidature list
- admin sees everything

---

### 5.5 Analyse CV

Current data model for frontend:
- `CvProfile`
- `Experience`
- `Formation`
- `Competence`
- `Langue`
- `AnalyseCv`

After callback:
- candidature status becomes `ANALYSEE`
- profile and child collections are replaced, not appended

Frontend display suggestion:
- profile card
- timeline of experiences
- education section
- skills chips
- language chips or table
- analysis scores and recommendation

---

### 5.6 Staff workflows

Coordinateur:
- consult candidatures
- update operational status to `EN_ANALYSE`, `ANALYSEE`, `ENTRETIEN_PLANIFIE`, `ENTRETIEN_TERMINE`
- plan interviews
- update interview status

Responsable:
- consult candidatures
- take final decision
- set `ACCEPTEE`, `REFUSEE`, or `LISTE_ATTENTE`
- create and update programmes only if allowed by service rules

Admin:
- full access
- manage programmes
- handle operational support
- can write final decisions and operational steps

---

## 6. Enums frontend

### 6.1 Role

Values:
- `CANDIDAT`
- `COORDINATEUR`
- `RESPONSABLE`
- `ADMIN`

Frontend use:
- route guards
- menu visibility
- button visibility
- page access

### 6.2 TypeDocument

Values:
- `CV`
- `RELEVE_NOTES`
- `LETTRE_MOTIVATION`
- `PASSEPORT`
- `AUTRE`

Frontend use:
- document upload selector
- required documents display

### 6.3 StatutCandidature

Values:
- `BROUILLON`
- `SOUMISE`
- `EN_ANALYSE`
- `ANALYSEE`
- `ENTRETIEN_PLANIFIE`
- `ENTRETIEN_TERMINE`
- `ACCEPTEE`
- `REFUSEE`
- `LISTE_ATTENTE`
- `ANNULEE`
- `ARCHIVEE`

Frontend use:
- workflow badges
- filters
- timeline

### 6.4 StatutProgramme

Values:
- `BROUILLON`
- `OUVERT`
- `FERME`
- `ARCHIVE`

Frontend use:
- programme list filters
- create/edit form

### 6.5 TypeMobilite

Values:
- `ETUDES`
- `ENSEIGNEMENT`
- `FORMATION`

Frontend use:
- programme form
- filter chips

### 6.6 ModeEntretien

Values:
- `VISIO`
- `PRESENTIEL`
- `TELEPHONE`

Frontend use:
- create interview form

Implementation note:
- backend validation currently accepts only `VISIO` and `PRESENTIEL`

### 6.7 StatutEntretien

Values:
- `PLANIFIE`
- `TERMINE`
- `ANNULE`
- `ABSENT`

Frontend use:
- interview detail and status management

### 6.8 TypeDecision

Values:
- `ACCEPTEE`
- `REFUSEE`
- `LISTE_ATTENTE`

Frontend use:
- final decision selector

### 6.9 DecisionFinale

Values:
- `ACCEPTEE`
- `REFUSEE`
- `LISTE_ATTENTE`

Frontend use:
- legacy/internal enum presence
- API currently uses `TypeDecision`, so prefer `TypeDecision` in the frontend

### 6.10 NiveauCompetence

Values:
- `DEBUTANT`
- `INTERMEDIAIRE`
- `AVANCE`
- `EXPERT`

Frontend use:
- parsed CV competences
- import mapping

### 6.11 NiveauLangue

Values:
- `A1`
- `A2`
- `B1`
- `B2`
- `C1`
- `C2`
- `NATIVE`

Frontend use:
- parsed CV languages
- import mapping

### 6.12 Recommendation

There is no recommendation enum in the backend.

- `recommandation` is a `String`
- it appears in `AnalyseCvDto` and persisted analysis

Frontend use:
- free text analysis recommendation

---

## 7. DTOs frontend

### 7.1 Main request/response DTOs

| DTO | Fields | Mandatory / notes |
|---|---|---|
| `CreateCandidatureRequest` | `programmeId: UUID` | mandatory |
| `CreateProgrammeRequest` | `titre`, `description`, `typeMobilite`, `pays`, `universitePartenaire`, `dateDebut`, `dateFin`, `dateLimiteCandidature`, `statut`, `coordinateurId`, `responsableId` | `titre`, `typeMobilite`, `pays` mandatory in service |
| `UpdateProgrammeRequest` | same as create | all optional |
| `UpdateCandidatureStatusRequest` | `statut: StatutCandidature` | mandatory |
| `CreateEntretienRequest` | `candidatureId`, `dateHeure`, `mode`, `lienVisio`, `lieu` | `candidatureId`, `dateHeure`, `mode` mandatory |
| `UpdateEntretienStatusRequest` | `statut: StatutEntretien` | mandatory |
| `CreateDecisionRequest` | `candidatureId`, `decision`, `commentaire` | `candidatureId`, `decision` mandatory |
| `UploadDocumentResponse` | see fields below | returned by upload |
| `SignedUrlResponse` | `documentId`, `signedUrl`, `expiresIn` | returned by signed URL endpoint |
| `DocumentValidationResponse` | `candidatureId`, `valid`, `requiredDocuments`, `uploadedDocuments`, `missingDocuments` | returned by validation endpoint |
| `CandidatureListResponse` | `id`, `statut`, `submittedAt`, `programmeId`, `programmeTitre`, `candidatId` | list view |
| `CandidatureDetailResponse` | `id`, `statut`, `submittedAt`, `programmeId`, `programmeTitre`, `programmePays`, `documents` | detail view |
| `CandidatureStatusResponse` | `id`, `statut`, `updatedAt` | after status update |
| `CandidatureArchiveResponse` | `id`, `statut`, `archivedAt`, `archivedById`, `archivedByNom` | after archive |
| `ProgrammeResponse` | see fields below | programme detail/list |
| `DocumentResponse` | see fields below | documents list |
| `EntretienResponse` | see fields below | interview detail |
| `DecisionResponse` | see fields below | decision detail |

### 7.2 Fields of the important DTOs

`ProgrammeResponse`
- `id: UUID`
- `titre: String`
- `description: String`
- `typeMobilite: TypeMobilite`
- `pays: String`
- `universitePartenaire: String`
- `dateDebut: LocalDate`
- `dateFin: LocalDate`
- `dateLimiteCandidature: LocalDate`
- `statut: StatutProgramme`
- `coordinateurId: UUID`
- `coordinateurNom: String`
- `responsableId: UUID`
- `responsableNom: String`
- `createdAt: LocalDateTime`
- `updatedAt: LocalDateTime`

`DocumentResponse`
- `documentId: UUID`
- `candidatureId: UUID`
- `typeDocument: TypeDocument`
- `fileName: String`
- `storagePath: String`
- `fileUrl: String`
- `mimeType: String`
- `size: Long`
- `uploadedAt: LocalDateTime`

`EntretienResponse`
- `id: UUID`
- `candidatureId: UUID`
- `dateHeure: LocalDateTime`
- `mode: ModeEntretien`
- `lienVisio: String`
- `lieu: String`
- `statut: StatutEntretien`
- `planifieParId: UUID`
- `planifieParNom: String`
- `createdAt: LocalDateTime`
- `updatedAt: LocalDateTime`

`DecisionResponse`
- `id: UUID`
- `candidatureId: UUID`
- `decision: TypeDecision`
- `commentaire: String`
- `responsableId: UUID`
- `responsableNom: String`
- `decidedAt: LocalDateTime`
- `createdAt: LocalDateTime`

`DocumentValidationResponse`
- `candidatureId: UUID`
- `valid: boolean`
- `requiredDocuments: List<TypeDocument>`
- `uploadedDocuments: List<TypeDocument>`
- `missingDocuments: List<TypeDocument>`

`UploadDocumentResponse`
- `documentId: UUID`
- `candidatureId: UUID`
- `typeDocument: TypeDocument`
- `fileName: String`
- `storagePath: String`
- `fileUrl: String`
- `mimeType: String`
- `size: Long`
- `uploadedAt: LocalDateTime`

### 7.3 CV standardization DTOs

`CvStandardizationTriggerResponse`
- `candidatureId: UUID`
- `documentId: UUID`
- `status: String`

`CvStandardizationCallbackRequest`
- `candidatureId: UUID`
- `documentId: UUID`
- `fullName: String`
- `email: String`
- `phone: String`
- `summary: String`
- `experiences: List<ExperienceDto>`
- `formations: List<FormationDto>`
- `competences: List<CompetenceDto>`
- `langues: List<LangueDto>`
- `analyse: AnalyseCvDto`

`CvStandardizationCallbackResponse`
- `candidatureId: UUID`
- `documentId: UUID`
- `cvProfileId: UUID`
- `statut: StatutCandidature`

`CvStandardizationImportRequest` (`dto/cv` package)
- `candidatureId: UUID`
- `documentId: UUID`
- `cvProfile: CvProfileDto`
- `formations: List<FormationDto>`
- `experiences: List<ExperienceDto>`
- `competences: List<CompetenceDto>`
- `langues: List<LangueDto>`
- `analyse: AnalyseCvDto`

`CvProfileDto`
- `fullName: String`
- `email: String`
- `phone: String`
- `address: String`
- `profileTitle: String`
- `summary: String`

`AnalyseCvDto` (`dto/cv` package)
- `academicScore: Double`
- `languageScore: Double`
- `mobilityReadinessScore: Double`
- `globalScore: Double`
- `strengths: List<String>`
- `weaknesses: List<String>`
- `keywords: List<String>`
- `recommendation: String`

`ExperienceDto` (`dto/cv` package)
- `titre: String`
- `organisation: String`
- `dateDebut: LocalDate`
- `dateFin: LocalDate`
- `description: String`

`FormationDto` (`dto/cv` package)
- `diplome: String`
- `etablissement: String`
- `dateDebut: LocalDate`
- `dateFin: LocalDate`
- `description: String`

`CompetenceDto` (`dto/cv` package)
- `nom: String`
- `type: String`
- `niveau: String`

`LangueDto` (`dto/cv` package)
- `nom: String`
- `niveau: String`

---

## 8. Security rules for frontend

### 8.1 Global security summary

Public endpoints:
- `OPTIONS /**`
- `/error`
- `/actuator/**`
- `/swagger-ui.html`
- `/swagger-ui/**`
- `/v3/api-docs/**`
- `POST /n8n/cv-standardization/callback`
- `POST /cv-standardization/import`

### 8.2 Access matrix

| Endpoint | CANDIDAT | COORDINATEUR | RESPONSABLE | ADMIN |
|---|---|---|---|---|
| GET `/programmes` | allowed | allowed | allowed | allowed |
| GET `/programmes/{id}` | allowed | allowed | allowed | allowed |
| POST `/programmes` | denied | denied | allowed | allowed |
| PATCH `/programmes/{id}` | denied | denied | allowed | allowed |
| POST `/candidatures` | allowed | denied | denied | denied |
| GET `/candidatures` | allowed, own records mainly | allowed | allowed | allowed |
| GET `/candidatures/{id}` | allowed if owner | allowed | allowed | allowed |
| GET `/candidatures/{id}/documents` | allowed if owner | allowed | allowed | allowed |
| GET `/candidatures/{id}/documents/validation` | allowed if owner | allowed | allowed | allowed |
| PATCH `/candidatures/{id}/statut` | denied | allowed | allowed | allowed |
| PATCH `/candidatures/{id}/archive` | denied | allowed | allowed | allowed |
| POST `/documents/upload` | allowed | allowed | denied by service | allowed |
| GET `/documents/{documentId}/signed-url` | allowed if owner | allowed | allowed | allowed |
| POST `/entretiens` | denied | allowed | denied | allowed |
| GET `/entretiens/{id}` | allowed if owner | allowed | allowed | allowed |
| PATCH `/entretiens/{id}/statut` | denied | allowed | denied | allowed |
| POST `/decisions` | denied | denied | allowed | allowed |
| GET `/candidatures/{candidatureId}/decision` | allowed if owner | allowed | allowed | allowed |
| GET `/candidatures/{candidatureId}/entretien` | allowed if owner | allowed | allowed | allowed |
| POST `/n8n/cv-standardization/callback` | public | public | public | public |
| POST `/cv-standardization/import` | public | public | public | public |

### 8.3 Important owner rules

Even when security config allows a route, services still enforce ownership:
- candidate can only access his own candidatures
- candidate can only access his own documents
- candidate can only access his own signed URLs

### 8.4 Important service-specific restriction

`POST /documents/upload`:
- security config allows `CANDIDAT`, `COORDINATEUR`, `ADMIN`
- service blocks `RESPONSABLE`

---

## 9. API error format

Global exception handling is centralized in `GlobalExceptionHandler`.

Returned error shape:

```json
{
  "timestamp": "2026-05-10T10:00:00",
  "status": 403,
  "error": "Forbidden",
  "message": "Acces refuse a ce document",
  "path": "/documents/uuid/signed-url"
}
```

Mapped statuses:
- `400` Bad Request
- `401` not explicitly formatted by the custom handler, usually depends on Spring Security
- `403` Forbidden
- `404` Not Found
- `409` Conflict
- `500` Internal Server Error

Message mapping in the handler:
- `BadRequestException` -> `400`
- `ResourceNotFoundException` -> `404`
- `ForbiddenException` or `AccessDeniedException` -> `403`
- `ConflictException` -> `409`
- unhandled exceptions -> `500` with message `Erreur interne du serveur`

---

## 10. Recommended frontend config

Example `environment.ts` or equivalent:

```ts
export const environment = {
  production: false,
  apiBaseUrl: 'http://localhost:8081',
  swaggerUrl: 'http://localhost:8081/swagger-ui.html',
  keycloakUrl: 'http://[::1]:8080',
  keycloakRealm: 'exchangehub',
  keycloakClientId: 'exchangehub-backend'
};
```

If your frontend runs in a browser that does not like IPv6 loopback, use the exact host that matches your Keycloak deployment, but keep the same realm and client ID.

---

## 11. Frontend services to create

### AuthService
Methods:
- login
- logout
- getAccessToken
- getCurrentRole
- decodeToken

Uses:
- Keycloak integration only

### ProgrammeService
Methods:
- getProgrammes(filters)
- getProgramme(id)
- createProgramme(request)
- updateProgramme(id, request)

Roles:
- public-ish read for all authenticated roles
- write for `RESPONSABLE` and `ADMIN`

### CandidatureService
Methods:
- createCandidature(programmeId)
- getCandidatures(filters)
- getCandidature(id)
- updateStatus(id, request)
- archive(id)
- getDocuments(candidatureId)
- validateDocuments(candidatureId)
- getDecision(candidatureId)
- getEntretien(candidatureId)

Roles:
- candidate own data
- staff wide access depending on role

### DocumentService
Methods:
- uploadDocument(candidatureId, typeDocument, file)
- getSignedUrl(documentId)
- getDocumentsByCandidature(candidatureId)
- validateDocuments(candidatureId)

Roles:
- candidate own documents
- staff access as configured

### EntretienService
Methods:
- createEntretien(request)
- getEntretien(id)
- getEntretienByCandidature(candidatureId)
- updateStatus(id, request)

Roles:
- `COORDINATEUR`, `ADMIN` for write
- read for owner and staff

### DecisionService
Methods:
- createDecision(request)
- getDecisionByCandidature(candidatureId)

Roles:
- `RESPONSABLE`, `ADMIN` for write

### CvAnalysisService
Methods:
- refreshCandidatureAnalysis(candidatureId)
- getCvProfileByCandidature(candidatureId)
- getAnalyseByCandidature(candidatureId)

Note:
- there is no dedicated read controller for CV profile in the current code
- frontend should read it from candidature detail plus related documents and analysis data once exposed or mapped in a future DTO

### UserService
Optional / future:
- getMyProfile
- getUsersByRole

Current state:
- no public users controller exists
- user provisioning is automatic via JWT

---

## 12. Pages frontend to create

### CANDIDAT
- login
- liste programmes
- creer candidature
- mes candidatures
- detail candidature
- upload documents
- analyse CV / profil CV
- documents validation

### COORDINATEUR
- dashboard
- toutes candidatures
- detail candidature
- documents
- analyse CV
- entretiens

### RESPONSABLE
- dashboard
- candidatures a decider
- detail candidature
- decision finale

### ADMIN
- dashboard admin
- programmes
- candidatures
- entretiens
- decisions
- supervision technique

---

## 13. Example frontend calls

### 13.1 Login / token use

Pseudo flow:

```ts
const token = keycloak.token;
await fetch(`${environment.apiBaseUrl}/programmes`, {
  headers: {
    Authorization: `Bearer ${token}`
  }
});
```

### 13.2 Create candidature

```ts
await fetch(`${environment.apiBaseUrl}/candidatures`, {
  method: 'POST',
  headers: {
    Authorization: `Bearer ${token}`,
    'Content-Type': 'application/json'
  },
  body: JSON.stringify({ programmeId })
});
```

### 13.3 Upload CV document

```ts
const formData = new FormData();
formData.append('candidatureId', candidatureId);
formData.append('typeDocument', 'CV');
formData.append('file', file);

await fetch(`${environment.apiBaseUrl}/documents/upload`, {
  method: 'POST',
  headers: {
    Authorization: `Bearer ${token}`
  },
  body: formData
});
```

### 13.4 Read signed URL

```ts
await fetch(`${environment.apiBaseUrl}/documents/${documentId}/signed-url`, {
  headers: {
    Authorization: `Bearer ${token}`
  }
});
```

### 13.5 Decision

```ts
await fetch(`${environment.apiBaseUrl}/decisions`, {
  method: 'POST',
  headers: {
    Authorization: `Bearer ${token}`,
    'Content-Type': 'application/json'
  },
  body: JSON.stringify({
    candidatureId,
    decision: 'ACCEPTEE',
    commentaire: 'Bon dossier'
  })
});
```

---

## 14. Points to verify before frontend development

1. There is no backend auth/login endpoint. Keycloak must be configured in the frontend.
2. `CurrentUserService` relies on the `email` claim. The Keycloak token must include it.
3. `roles` claim must be present for role mapping.
4. Candidate ownership rules are enforced in services, not only by route security.
5. CV upload currently starts automatic analysis via n8n.
6. The manual route `/candidatures/{id}/standardize-cv` is authorized in security but not exposed by a controller in the current codebase.
7. `RESPONSABLE` is blocked from document upload even if the route security is broad enough to let some roles through.
8. Signed URLs are temporary and should be consumed quickly.
9. Frontend should refresh candidature detail after CV upload to show analysis results.
10. Do not send Supabase service role key or database credentials to the frontend.
11. Keep n8n callback endpoints backend-to-backend only.
12. Frontend should treat `CreateCandidature` response as an entity-shaped JSON, not a DTO, because the controller returns `Candidature` directly.

---

## 15. Short implementation summary for frontend teams

Minimum frontend stack needed:
- Keycloak OIDC integration
- API client with bearer token interceptor
- role guards
- candidature list and detail screens
- programme list and editor screens for staff
- multipart upload component
- status badges and workflow timeline
- interview and decision screens for staff
- polling or refresh behavior after CV upload for n8n analysis

---

## 16. Suggested next step

Before starting frontend work, align on:
- Keycloak frontend client configuration
- exact shape of the candidature detail page
- whether CV profile and analysis should be exposed in a dedicated DTO or reused from the candidature detail view
- whether the manual n8n trigger route should remain hidden or be removed later
