-- Made-up sample data, not from a real study.
INSERT INTO respondent (id, age, gender, education_level, country) VALUES
    (1, 34, 'female', 'bachelor', 'Netherlands'),
    (2, 51, 'male', 'secondary', 'Belgium'),
    (3, 27, 'female', 'master', 'Netherlands'),
    (4, 63, 'male', 'primary', 'Germany'),
    (5, 45, 'other', 'bachelor', 'Belgium'),
    (6, 39, 'female', 'secondary', 'Germany');

INSERT INTO survey_response (respondent_id, wave, life_satisfaction, trust_in_government, income_bracket) VALUES
    (1, 1, 8, 3, 'middle'),
    (2, 1, 6, 2, 'low'),
    (3, 1, 7, 4, 'middle'),
    (4, 1, 5, 2, 'low'),
    (5, 1, 9, 3, 'high'),
    (6, 1, NULL, 1, 'middle'),
    (1, 2, 7, 3, 'middle'),
    (3, 2, 8, 4, 'high');
