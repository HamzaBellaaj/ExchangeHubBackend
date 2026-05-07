CREATE TABLE experience (
    id UUID PRIMARY KEY,
    cv_profile_id UUID NOT NULL,
    poste VARCHAR(255),
    organisation VARCHAR(255),
    pays VARCHAR(100),
    date_debut DATE,
    date_fin DATE,
    description TEXT,
    current BOOLEAN DEFAULT FALSE,
    CONSTRAINT fk_experience_cv_profile FOREIGN KEY (cv_profile_id) REFERENCES cv_profile(id)
);

CREATE TABLE formation (
    id UUID PRIMARY KEY,
    cv_profile_id UUID NOT NULL,
    diplome VARCHAR(255),
    etablissement VARCHAR(255),
    pays VARCHAR(100),
    domaine VARCHAR(255),
    date_debut DATE,
    date_fin DATE,
    CONSTRAINT fk_formation_cv_profile FOREIGN KEY (cv_profile_id) REFERENCES cv_profile(id)
);

CREATE TABLE competence (
    id UUID PRIMARY KEY,
    cv_profile_id UUID NOT NULL,
    nom VARCHAR(255) NOT NULL,
    niveau VARCHAR(50),
    CONSTRAINT fk_competence_cv_profile FOREIGN KEY (cv_profile_id) REFERENCES cv_profile(id)
);

CREATE TABLE langue (
    id UUID PRIMARY KEY,
    cv_profile_id UUID NOT NULL,
    langue VARCHAR(100) NOT NULL,
    niveau VARCHAR(50) NOT NULL,
    CONSTRAINT fk_langue_cv_profile FOREIGN KEY (cv_profile_id) REFERENCES cv_profile(id)
);