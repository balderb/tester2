-- Small, made-up sample dataset so the commands have something to work with.
CREATE TABLE respondent (
    id              INTEGER PRIMARY KEY,
    age             INTEGER NOT NULL,
    gender          VARCHAR(20),
    education_level VARCHAR(30),
    country         VARCHAR(40)
);

CREATE TABLE survey_response (
    respondent_id       INTEGER NOT NULL REFERENCES respondent (id),
    wave                INTEGER NOT NULL,
    life_satisfaction   INTEGER,
    trust_in_government INTEGER,
    income_bracket      VARCHAR(20),
    PRIMARY KEY (respondent_id, wave)
);
