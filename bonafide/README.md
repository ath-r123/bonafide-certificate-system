# Bonafide Certificate Management System

Java 21 console application for student record management and automated bonafide certificate
generation. MySQL 8.x + JDBC only — no ORM, no framework, no GUI toolkit.

## Requirements

JDK 21, Maven 3.8+, MySQL 8.0 or later.

## Setup

1. Create the database, tables and seed data:
   `mysql -u root -p < src/main/resources/schema.sql`
2. Create the application user:
   ```sql
   CREATE USER 'bonafide'@'localhost' IDENTIFIED BY 'your_password';
   GRANT SELECT, INSERT, UPDATE, DELETE ON bonafide_db.* TO 'bonafide'@'localhost';
   ```
3. `cp db.properties.example db.properties` (Windows: `copy`) and set the real credentials.
   `db.properties` is git-ignored, so credentials never reach the repository or the source.
4. Edit the seeded `institute`, `department` and `course` rows in `schema.sql` for your college.

## Build and run

```
mvn clean package
java -jar target/bonafide.jar
```

Run from the project root: `db.properties` and the `certificates/` output folder are resolved
against the working directory. The only entry point is `com.bonafide.ui.ConsoleApp`; this is a
multi-package project, so compiling a single file on its own will not work (in VS Code use the
Extension Pack for Java and open this folder as the project root, not the Code Runner extension).

Without Maven, put the Connector/J jar in `lib/` and run:

```
javac -d build/classes $(find src/main/java -name '*.java')
cp src/main/resources/certificate-template.html build/classes/
java -cp "build/classes:lib/mysql-connector-j-9.4.0.jar" com.bonafide.ui.ConsoleApp
```

## Design notes

* `student` references `course`, `course` references `department`, so a student row never
  duplicates department data.
* Certificate numbers (`BC/2025-26/0001`) come from a per-academic-year counter row that is
  incremented inside the issuing transaction, so concurrent sessions cannot share a number.
* Issuing a certificate is one transaction: the number, the `certificate` row and the HTML file
  either all succeed or all roll back, and the file is written before the commit so a committed
  row always had its file on disk.
* Students with certificate history cannot be deleted. The DAO checks first and the
  `ON DELETE RESTRICT` foreign key is the final safety layer. Certificates are never deleted.
* Certificates are HTML: open in a browser and use Print / Save as PDF.
