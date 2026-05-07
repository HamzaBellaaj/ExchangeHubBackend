CREATE TABLE document (
    id UUID PRIMARY KEY,
    candidature_id UUID NOT NULL,
    type_document VARCHAR(50) NOT NULL,
    file_name VARCHAR(255),
    file_url TEXT NOT NULL,
    storage_path TEXT NOT NULL,
    mime_type VARCHAR(100),
    size BIGINT,
    uploaded_at TIMESTAMP,
    CONSTRAINT fk_document_candidature FOREIGN KEY (candidature_id) REFERENCES candidature(id)
);

CREATE TABLE cv_profile (
    id UUID PRIMARY KEY,
    candidature_id UUID UNIQUE,
    nom_complet VARCHAR(255),
    email VARCHAR(255),
    telephone VARCHAR(50),
    titre_profil VARCHAR(255),
    resume TEXT,
    annees_experience INTEGER,
    parsed_at TIMESTAMP,
    CONSTRAINT fk_cv_candidature FOREIGN KEY (candidature_id) REFERENCES candidature(id)
);