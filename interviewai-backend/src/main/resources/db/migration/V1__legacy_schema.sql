CREATE TABLE chapters (
    id INT NOT NULL AUTO_INCREMENT,
    course_id INT NOT NULL,
    chapter_number INT DEFAULT NULL,
    name varchar(255) DEFAULT NULL,
    description text DEFAULT NULL,
    status varchar(30) NOT NULL DEFAULT 'NOT_STARTED',
    completed_at datetime DEFAULT NULL,
    PRIMARY KEY (id)
);

CREATE TABLE choices (
    id INT NOT NULL AUTO_INCREMENT,
    question_id INT NOT NULL,
    choice_text varchar(255) DEFAULT NULL,
    PRIMARY KEY (id)
);

CREATE TABLE feedback (
    id INT NOT NULL AUTO_INCREMENT,
    user_id INT DEFAULT NULL,
    course_id INT DEFAULT NULL,
    message text DEFAULT NULL,
    created_at timestamp NOT NULL DEFAULT current_timestamp(),
    PRIMARY KEY (id)
);

CREATE TABLE generated_courses (
    id INT NOT NULL AUTO_INCREMENT,
    user_id INT NOT NULL,
    course_title varchar(255) DEFAULT NULL,
    status varchar(50) DEFAULT NULL,
    created_at datetime DEFAULT current_timestamp(),
    PRIMARY KEY (id)
);

CREATE TABLE interview_prep (
    id INT NOT NULL AUTO_INCREMENT,
    user_id INT NOT NULL,
    technical_course_id INT DEFAULT NULL,
    softskills_course_id INT DEFAULT NULL,
    created_at timestamp NOT NULL DEFAULT current_timestamp(),
    PRIMARY KEY (id)
);

CREATE TABLE onboarding_data (
    user_id INT NOT NULL,
    interview_type varchar(50) NOT NULL,
    language varchar(50) NOT NULL,
    timeline varchar(50) NOT NULL,
    context text DEFAULT NULL,
    cv_path varchar(500) DEFAULT NULL,
    created_at timestamp NOT NULL DEFAULT current_timestamp(),
    updated_at timestamp NOT NULL DEFAULT current_timestamp() ON UPDATE current_timestamp(),
    PRIMARY KEY (user_id)
);

CREATE TABLE permissions (
    id INT NOT NULL AUTO_INCREMENT,
    name varchar(50) NOT NULL,
    description text DEFAULT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uq_permissions_name UNIQUE (name)
);

CREATE TABLE progress (
    id INT NOT NULL AUTO_INCREMENT,
    user_id INT NOT NULL,
    course_id INT NOT NULL,
    xp INT DEFAULT 0,
    last_updated timestamp NOT NULL DEFAULT current_timestamp(),
    PRIMARY KEY (id)
);

CREATE TABLE questions (
    id INT NOT NULL AUTO_INCREMENT,
    chapter_id INT NOT NULL,
    question text DEFAULT NULL,
    question_type varchar(50) DEFAULT 'MULTIPLE_CHOICE',
    correct_answer varchar(255) DEFAULT NULL,
    explanation text DEFAULT NULL,
    status varchar(30) NOT NULL DEFAULT 'NOT_STARTED',
    updated_at datetime NOT NULL DEFAULT current_timestamp() ON UPDATE current_timestamp(),
    PRIMARY KEY (id)
);

CREATE TABLE role_permissions (
    role_id INT NOT NULL,
    permission_id INT NOT NULL,
    PRIMARY KEY (role_id,permission_id)
);

CREATE TABLE roles (
    id INT NOT NULL AUTO_INCREMENT,
    name varchar(50) NOT NULL,
    description text DEFAULT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uq_roles_name UNIQUE (name)
);

CREATE TABLE user_goals (
    id INT NOT NULL AUTO_INCREMENT,
    user_id INT NOT NULL,
    goal_name varchar(255) NOT NULL,
    goal_type varchar(50) NOT NULL,
    target_value INT NOT NULL,
    start_value INT DEFAULT 0,
    current_value INT DEFAULT 0,
    is_active BOOLEAN DEFAULT 1,
    created_at timestamp NOT NULL DEFAULT current_timestamp(),
    updated_at timestamp NOT NULL DEFAULT current_timestamp() ON UPDATE current_timestamp(),
    PRIMARY KEY (id)
);

CREATE TABLE user_roles (
    user_id INT NOT NULL,
    role_id INT NOT NULL,
    PRIMARY KEY (user_id,role_id)
);

CREATE TABLE users (
    id INT NOT NULL AUTO_INCREMENT,
    username varchar(100) NOT NULL,
    email varchar(255) DEFAULT NULL,
    password_hash varchar(255) DEFAULT NULL,
    role varchar(20) DEFAULT 'CANDIDATE',
    created_at timestamp NOT NULL DEFAULT current_timestamp(),
    last_course_id INT DEFAULT NULL,
    last_chapter_id INT DEFAULT NULL,
    last_daily_quest_claim date DEFAULT NULL,
    last_monthly_quest_claim date DEFAULT NULL,
    is_active BOOLEAN DEFAULT 1,
    two_factor_enabled BOOLEAN DEFAULT 0,
    daily_quest_last_reset datetime DEFAULT CURRENT_TIMESTAMP,
    daily_quest_claims_count INT DEFAULT 0,
    PRIMARY KEY (id),
    CONSTRAINT uq_users_username UNIQUE (username)
);

CREATE INDEX idx_chapters_course_id ON chapters (course_id);

CREATE INDEX idx_choices_question_id ON choices (question_id);

CREATE INDEX idx_interview_prep_idx_user_id ON interview_prep (user_id);

CREATE INDEX idx_interview_prep_idx_technical_course_id ON interview_prep (technical_course_id);

CREATE INDEX idx_interview_prep_idx_softskills_course_id ON interview_prep (softskills_course_id);

CREATE INDEX idx_onboarding_data_idx_interview_type ON onboarding_data (interview_type);

CREATE INDEX idx_onboarding_data_idx_language ON onboarding_data (language);

CREATE INDEX idx_progress_user_id ON progress (user_id);

CREATE INDEX idx_progress_course_id ON progress (course_id);

CREATE INDEX idx_questions_chapter_id ON questions (chapter_id);

CREATE INDEX idx_role_permissions_permission_id ON role_permissions (permission_id);

CREATE INDEX idx_user_goals_user_id ON user_goals (user_id);

CREATE INDEX idx_user_roles_role_id ON user_roles (role_id);

CREATE INDEX idx_users_fk_users_last_course ON users (last_course_id);

CREATE INDEX idx_users_fk_users_last_chapter ON users (last_chapter_id);

ALTER TABLE chapters ADD CONSTRAINT chapters_ibfk_1 FOREIGN KEY (course_id) REFERENCES generated_courses (id) ON DELETE CASCADE;

ALTER TABLE choices ADD CONSTRAINT choices_ibfk_1 FOREIGN KEY (question_id) REFERENCES questions (id) ON DELETE CASCADE;

ALTER TABLE interview_prep ADD CONSTRAINT interview_prep_ibfk_1 FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE;

ALTER TABLE interview_prep ADD CONSTRAINT interview_prep_ibfk_2 FOREIGN KEY (technical_course_id) REFERENCES generated_courses (id) ON DELETE SET NULL;

ALTER TABLE interview_prep ADD CONSTRAINT interview_prep_ibfk_3 FOREIGN KEY (softskills_course_id) REFERENCES generated_courses (id) ON DELETE SET NULL;

ALTER TABLE onboarding_data ADD CONSTRAINT onboarding_data_ibfk_1 FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE;

ALTER TABLE progress ADD CONSTRAINT progress_ibfk_1 FOREIGN KEY (user_id) REFERENCES users (id);

ALTER TABLE progress ADD CONSTRAINT progress_ibfk_2 FOREIGN KEY (course_id) REFERENCES generated_courses (id) ON DELETE CASCADE;

ALTER TABLE questions ADD CONSTRAINT questions_ibfk_1 FOREIGN KEY (chapter_id) REFERENCES chapters (id) ON DELETE CASCADE;

ALTER TABLE role_permissions ADD CONSTRAINT role_permissions_ibfk_1 FOREIGN KEY (role_id) REFERENCES roles (id) ON DELETE CASCADE;

ALTER TABLE role_permissions ADD CONSTRAINT role_permissions_ibfk_2 FOREIGN KEY (permission_id) REFERENCES permissions (id) ON DELETE CASCADE;

ALTER TABLE user_goals ADD CONSTRAINT user_goals_ibfk_1 FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE;

ALTER TABLE user_roles ADD CONSTRAINT user_roles_ibfk_1 FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE;

ALTER TABLE user_roles ADD CONSTRAINT user_roles_ibfk_2 FOREIGN KEY (role_id) REFERENCES roles (id) ON DELETE CASCADE;

ALTER TABLE users ADD CONSTRAINT fk_users_last_chapter FOREIGN KEY (last_chapter_id) REFERENCES chapters (id) ON DELETE SET NULL;

ALTER TABLE users ADD CONSTRAINT fk_users_last_course FOREIGN KEY (last_course_id) REFERENCES generated_courses (id) ON DELETE SET NULL;
