CREATE TABLE users (
    id UUID PRIMARY KEY,
    keycloak_id VARCHAR(255) UNIQUE NOT NULL,
    nom VARCHAR(100),
    prenom VARCHAR(100),
    email VARCHAR(255) UNIQUE NOT NULL,
    role VARCHAR(50) NOT NULL,
    actif BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMP,
    updated_at TIMESTAMP
);

CREATE TABLE programme (
    id UUID PRIMARY KEY,
    titre VARCHAR(255),
    description TEXT,
    type_mobilite VARCHAR(50),
    pays VARCHAR(100),
    universite_partenaire VARCHAR(255),
    date_debut DATE,
    date_fin DATE,
    date_limite_candidature DATE,
    statut VARCHAR(50),
    coordinateur_id UUID,
    responsable_id UUID,
    created_at TIMESTAMP,
    updated_at TIMESTAMP,
    CONSTRAINT fk_coordinateur FOREIGN KEY (coordinateur_id) REFERENCES users(id),
    CONSTRAINT fk_responsable FOREIGN KEY (responsable_id) REFERENCES users(id)
);

CREATE TABLE candidature (
    id UUID PRIMARY KEY,
    candidat_id UUID,
    programme_id UUID,
    statut VARCHAR(50),
    submitted_at TIMESTAMP,
    archived_at TIMESTAMP,
    archived_by UUID,
    created_at TIMESTAMP,
    updated_at TIMESTAMP,
    CONSTRAINT fk_candidat FOREIGN KEY (candidat_id) REFERENCES users(id),
    CONSTRAINT fk_programme FOREIGN KEY (programme_id) REFERENCES programme(id),
    CONSTRAINT unique_candidature UNIQUE (candidat_id, programme_id)
);