-- Preserve the legacy tables. Only legacy full administrators receive ROLE_ADMIN.
UPDATE users SET role = 'ROLE_ADMIN' WHERE role IN ('ADMIN', 'SUPER_ADMIN', 'ROLE_ADMIN')
 OR id IN (SELECT ur.user_id FROM user_roles ur JOIN roles r ON r.id = ur.role_id WHERE r.name = 'SUPER_ADMIN');
UPDATE users SET role = 'ROLE_USER' WHERE role IS NULL OR role <> 'ROLE_ADMIN';
ALTER TABLE users ADD COLUMN token_version INT NOT NULL DEFAULT 0;
ALTER TABLE feedback ADD COLUMN question_id INT;
ALTER TABLE feedback ADD COLUMN user_answer TEXT;
ALTER TABLE feedback ADD COLUMN score INT;
ALTER TABLE feedback ADD COLUMN strengths TEXT;
ALTER TABLE feedback ADD COLUMN improvements TEXT;
ALTER TABLE feedback ADD COLUMN explanation TEXT;
CREATE INDEX idx_feedback_user_created ON feedback(user_id, created_at);
ALTER TABLE progress ADD COLUMN event_key VARCHAR(150);
ALTER TABLE progress ADD CONSTRAINT uq_progress_event UNIQUE(user_id, event_key);
ALTER TABLE onboarding_data ADD COLUMN description TEXT;
CREATE TABLE quests (
 id INT AUTO_INCREMENT PRIMARY KEY, code VARCHAR(50) NOT NULL UNIQUE,
 title VARCHAR(255) NOT NULL, target_value INT NOT NULL, xp_reward INT NOT NULL, period VARCHAR(20) NOT NULL
);
INSERT INTO quests(code, title, target_value, xp_reward, period) VALUES
 ('DAILY', 'Answer 20 different questions today', 20, 900, 'DAILY'),
 ('MONTHLY', 'Complete 12 lessons this month', 12, 30000, 'MONTHLY');
CREATE TABLE quest_claims (
 id INT AUTO_INCREMENT PRIMARY KEY, user_id INT NOT NULL, quest_id INT NOT NULL,
 period_start DATE NOT NULL, claimed_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
 CONSTRAINT uq_quest_claim UNIQUE(user_id, quest_id, period_start),
 FOREIGN KEY(user_id) REFERENCES users(id), FOREIGN KEY(quest_id) REFERENCES quests(id)
);
CREATE TABLE achievements (
 id INT AUTO_INCREMENT PRIMARY KEY, user_id INT NOT NULL, code VARCHAR(60) NOT NULL,
 title VARCHAR(255) NOT NULL, earned_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
 CONSTRAINT uq_user_achievement UNIQUE(user_id, code), FOREIGN KEY(user_id) REFERENCES users(id)
);

-- Preserve already-claimed legacy rewards when upgrading an existing database.
INSERT INTO quest_claims(user_id, quest_id, period_start)
 SELECT u.id, q.id, u.last_daily_quest_claim FROM users u JOIN quests q ON q.code = 'DAILY'
 WHERE u.last_daily_quest_claim IS NOT NULL;
INSERT INTO quest_claims(user_id, quest_id, period_start)
 SELECT u.id, q.id, CAST(CONCAT(EXTRACT(YEAR FROM u.last_monthly_quest_claim), '-',
 LPAD(EXTRACT(MONTH FROM u.last_monthly_quest_claim), 2, '0'), '-01') AS DATE)
 FROM users u JOIN quests q ON q.code = 'MONTHLY' WHERE u.last_monthly_quest_claim IS NOT NULL;
