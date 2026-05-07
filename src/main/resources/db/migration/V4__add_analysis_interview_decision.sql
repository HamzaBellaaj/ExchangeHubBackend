CREATE TABLE analyse_cv (
    id UUID PRIMARY KEY,
    cv_profile_id UUID UNIQUE NOT NULL,
    score_global INTEGER,
    score_experience INTEGER,
    score_formation INTEGER,
    score_langues INTEGER,
    score_competences INTEGER,
    points_forts TEXT,
    points_faibles TEXT,
    recommandation TEXT,
    analyzed_at TIMESTAMP,
    CONSTRAINT fk_analyse_cv_profile FOREIGN KEY (cv_profile_id) REFERENCES cv_profile(id),
    CONSTRAINT chk_score_global CHECK (score_global BETWEEN 0 AND 100),
    CONSTRAINT chk_score_experience CHECK (score_experience BETWEEN 0 AND 100),
    CONSTRAINT chk_score_formation CHECK (score_formation BETWEEN 0 AND 100),
    CONSTRAINT chk_score_langues CHECK (score_langues BETWEEN 0 AND 100),
    CONSTRAINT chk_score_competences CHECK (score_competences BETWEEN 0 AND 100)
);

CREATE TABLE entretien (
    id UUID PRIMARY KEY,
    candidature_id UUID UNIQUE NOT NULL,
    date_entretien TIMESTAMP,
    mode VARCHAR(50),
    lien_visio TEXT,
    lieu VARCHAR(255),
    statut VARCHAR(50),
    planifie_par_id UUID,
    notes TEXT,
    created_at TIMESTAMP,
    updated_at TIMESTAMP,
    CONSTRAINT fk_entretien_candidature FOREIGN KEY (candidature_id) REFERENCES candidature(id),
    CONSTRAINT fk_entretien_planifie_par FOREIGN KEY (planifie_par_id) REFERENCES users(id)
);

CREATE TABLE decision (
    id UUID PRIMARY KEY,
    candidature_id UUID UNIQUE NOT NULL,
    decision VARCHAR(50) NOT NULL,
    responsable_id UUID,
    commentaire TEXT,
    decided_at TIMESTAMP,
    CONSTRAINT fk_decision_candidature FOREIGN KEY (candidature_id) REFERENCES candidature(id),
    CONSTRAINT fk_decision_responsable FOREIGN KEY (responsable_id) REFERENCES users(id)
);