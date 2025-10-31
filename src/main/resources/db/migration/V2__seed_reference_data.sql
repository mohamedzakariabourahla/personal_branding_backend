INSERT INTO niches (name) VALUES
    ('SaaS'),
    ('Personal Development'),
    ('Marketing'),
    ('Technology'),
    ('Health & Wellness')
ON CONFLICT (name) DO NOTHING;

INSERT INTO audiences (name) VALUES
    ('Entrepreneurs'),
    ('Marketing Leaders'),
    ('Software Engineers'),
    ('Freelancers'),
    ('Investors')
ON CONFLICT (name) DO NOTHING;

INSERT INTO tones (name) VALUES
    ('Professional'),
    ('Inspirational'),
    ('Educational'),
    ('Entertaining'),
    ('Bold')
ON CONFLICT (name) DO NOTHING;

INSERT INTO platforms (name) VALUES
    ('LinkedIn'),
    ('Twitter/X'),
    ('Instagram'),
    ('YouTube'),
    ('TikTok')
ON CONFLICT (name) DO NOTHING;

INSERT INTO countries (name, iso_code) VALUES
    ('United States', 'USA'),
    ('Canada', 'CAN'),
    ('United Kingdom', 'GBR'),
    ('Germany', 'DEU'),
    ('France', 'FRA')
ON CONFLICT (iso_code) DO NOTHING;

INSERT INTO posting_frequencies (name) VALUES
    ('Daily'),
    ('Weekly'),
    ('Biweekly'),
    ('Monthly')
ON CONFLICT (name) DO NOTHING;
