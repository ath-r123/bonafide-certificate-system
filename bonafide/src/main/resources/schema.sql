-- Bonafide Certificate Management System - MySQL 8 schema
CREATE DATABASE IF NOT EXISTS bonafide_db CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE bonafide_db;

CREATE TABLE IF NOT EXISTS institute (
    institute_id   TINYINT      NOT NULL PRIMARY KEY,
    name           VARCHAR(150) NOT NULL,
    address        VARCHAR(255) NOT NULL,
    principal_name VARCHAR(100) NOT NULL,
    place          VARCHAR(100) NOT NULL,
    CONSTRAINT chk_institute_single_row CHECK (institute_id = 1)
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS department (
    dept_id   INT          NOT NULL AUTO_INCREMENT PRIMARY KEY,
    dept_name VARCHAR(100) NOT NULL,
    CONSTRAINT uq_department_name UNIQUE (dept_name)
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS course (
    course_id      INT          NOT NULL AUTO_INCREMENT PRIMARY KEY,
    dept_id        INT          NOT NULL,
    course_name    VARCHAR(100) NOT NULL,
    duration_years TINYINT      NOT NULL,
    CONSTRAINT uq_course_dept_name UNIQUE (dept_id, course_name),
    KEY idx_course_dept (dept_id),
    CONSTRAINT fk_course_dept FOREIGN KEY (dept_id) REFERENCES department(dept_id)
        ON DELETE RESTRICT ON UPDATE CASCADE
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS student (
    student_id       INT          NOT NULL AUTO_INCREMENT PRIMARY KEY,
    roll_no          VARCHAR(20)  NOT NULL,
    name             VARCHAR(100) NOT NULL,
    father_name      VARCHAR(100) NOT NULL,
    dob              DATE         NOT NULL,
    gender           ENUM('MALE','FEMALE','OTHER') NOT NULL,
    course_id        INT          NOT NULL,
    current_year     TINYINT      NOT NULL,
    current_semester TINYINT      NOT NULL,
    admission_year   SMALLINT     NOT NULL,
    email            VARCHAR(100) NOT NULL,
    phone            VARCHAR(15)  NOT NULL,
    address          VARCHAR(255) NOT NULL,
    status           ENUM('ACTIVE','PASSED_OUT') NOT NULL DEFAULT 'ACTIVE',
    created_at       TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_student_roll_no UNIQUE (roll_no),
    CONSTRAINT uq_student_email   UNIQUE (email),
    -- Indexes for the actual search paths: name prefix search, admission-year filter,
    -- and course filter (also backs the FK lookup).
    KEY idx_student_name (name),
    KEY idx_student_admission_year (admission_year),
    KEY idx_student_course (course_id),
    CONSTRAINT fk_student_course FOREIGN KEY (course_id) REFERENCES course(course_id)
        ON DELETE RESTRICT ON UPDATE CASCADE
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS certificate (
    certificate_id INT          NOT NULL AUTO_INCREMENT PRIMARY KEY,
    certificate_no VARCHAR(20)  NOT NULL,
    student_id     INT          NOT NULL,
    academic_year  CHAR(7)      NOT NULL,
    purpose        ENUM('BANK_LOAN','PASSPORT','SCHOLARSHIP','OTHER') NOT NULL,
    purpose_detail VARCHAR(150) NULL,
    issue_date     DATE         NOT NULL,
    file_path      VARCHAR(255) NOT NULL,
    CONSTRAINT uq_certificate_no UNIQUE (certificate_no),
    KEY idx_certificate_student (student_id),
    -- Certificate history is permanent: a student with certificates cannot be deleted.
    CONSTRAINT fk_certificate_student FOREIGN KEY (student_id) REFERENCES student(student_id)
        ON DELETE RESTRICT ON UPDATE CASCADE
) ENGINE=InnoDB;

-- Per-academic-year counter for BC/YYYY-YY/0001 numbering (row locked during issue).
CREATE TABLE IF NOT EXISTS certificate_seq (
    academic_year CHAR(7) NOT NULL PRIMARY KEY,
    last_seq      INT     NOT NULL
) ENGINE=InnoDB;

INSERT IGNORE INTO institute (institute_id, name, address, principal_name, place) VALUES
 (1, 'Sinhgad College of Engineering',
     'Vadgaon Budruk, Off Sinhgad Road, Pune - 411041, Maharashtra',
     'Dr. S. D. Lokhande', 'Pune');

INSERT IGNORE INTO department (dept_name) VALUES
 ('Computer Engineering'), ('Information Technology'), ('Electronics and Telecommunication'), ('Mechanical Engineering');

INSERT IGNORE INTO course (dept_id, course_name, duration_years)
SELECT d.dept_id, x.course_name, x.duration_years FROM department d JOIN (
  SELECT 'Computer Engineering' AS dept, 'B.E. Computer Engineering' AS course_name, 4 AS duration_years UNION ALL
  SELECT 'Computer Engineering', 'M.E. Computer Engineering', 2 UNION ALL
  SELECT 'Information Technology', 'B.E. Information Technology', 4 UNION ALL
  SELECT 'Electronics and Telecommunication', 'B.E. Electronics and Telecommunication', 4 UNION ALL
  SELECT 'Mechanical Engineering', 'B.E. Mechanical Engineering', 4
) x ON x.dept = d.dept_name;
