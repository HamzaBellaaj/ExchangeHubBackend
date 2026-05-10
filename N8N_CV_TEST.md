# Tests n8n CV

## Workflow n8n conseille

Webhook
-> Extract from File
-> AI Agent
-> Code Parse JSON
-> HTTP Request vers Spring

Le dernier noeud HTTP doit appeler :

```text
POST http://host.docker.internal:8081/cv-standardization/import
Body JSON: {{ $json }}
```

Le callback Spring reste :

```text
http://localhost:8081/cv-standardization/import
```

## Tests Postman minimum

1. `GET /programmes`
2. `POST /candidatures`
3. `POST /documents/upload` avec `typeDocument=CV`
4. `GET /documents/{id}/signed-url` avec un token `ADMIN`
5. Verifier que n8n se lance apres upload CV
6. `POST /cv-standardization/import` avec un JSON mock n8n
7. `GET /candidatures/{id}`
8. Verifier que le statut passe a `ANALYSEE`

## Points a verifier

- `ADMIN` peut generer une signed URL.
- `CANDIDAT` ne lit pas les documents des autres.
- Personne ne lance n8n manuellement.
- n8n se lance seulement apres upload CV.
- Le callback n8n n'est pas bloque.
- Les donnees CV sont sauvegardees.
- Le bucket Supabase reste prive.
