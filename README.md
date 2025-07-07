# 💉 Vaccine Scheduler

A Java-based command-line application that simulates a vaccine appointment system. Users can sign up as patients or caregivers, manage availability, reserve appointments, and track vaccine inventory. The system uses secure password hashing and a SQL database for persistent data storage.

---

## ✨ Key Features
- **User Management**: Create and authenticate both patients and caregivers using secure salted password hashing.
- **Caregiver Scheduling**: Caregivers can upload availability for specific dates.
- **Appointment Booking**: Patients can search caregiver schedules and reserve appointments based on vaccine availability.
- **Vaccine Inventory**: Supports adding and managing vaccine doses.
- **Appointment Management**: Patients and caregivers can view, cancel, and manage their scheduled appointments.
- **SQL-backed Data Persistence**: Fully integrated with a relational database using JDBC.

## 🛠 Tech Stack
- **Java**
- **SQL / JDBC**
- **Command-Line Interface**

## 📁 Example Commands
```bash
> create_patient john123 password123
> login_patient john123 password123
> search_caregiver_schedule 2025-07-10
> reserve 2025-07-10 Pfizer
> show_appointments
> logout

