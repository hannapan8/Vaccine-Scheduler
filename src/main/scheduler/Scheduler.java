package scheduler;

import scheduler.db.ConnectionManager;
import scheduler.model.Caregiver;
import scheduler.model.Patient;
import scheduler.model.Vaccine;
import scheduler.util.Util;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Date;

public class Scheduler {

    // objects to keep track of the currently logged-in user
    // Note: it is always true that at most one of currentCaregiver and currentPatient is not null
    //       since only one user can be logged-in at a time
    private static Caregiver currentCaregiver = null;
    private static Patient currentPatient = null;

    public static void main(String[] args) {
        // printing greetings text
        System.out.println();
        System.out.println("Welcome to the COVID-19 Vaccine Reservation Scheduling Application!");
        System.out.println("*** Please enter one of the following commands ***");
        System.out.println("> create_patient <username> <password>");  //TODO: implement create_patient (Part 1)
        System.out.println("> create_caregiver <username> <password>");
        System.out.println("> login_patient <username> <password>");  // TODO: implement login_patient (Part 1)
        System.out.println("> login_caregiver <username> <password>");
        System.out.println("> search_caregiver_schedule <date>");  // TODO: implement search_caregiver_schedule (Part 2)
        System.out.println("> reserve <date> <vaccine>");  // TODO: implement reserve (Part 2)
        System.out.println("> upload_availability <date>");
        System.out.println("> cancel <appointment_id>");  // TODO: implement cancel (extra credit)
        System.out.println("> add_doses <vaccine> <number>");
        System.out.println("> show_appointments");  // TODO: implement show_appointments (Part 2)
        System.out.println("> logout");  // TODO: implement logout (Part 2)
        System.out.println("> quit");
        System.out.println();

        // read input from user
        BufferedReader r = new BufferedReader(new InputStreamReader(System.in));
        while (true) {
            System.out.print("> ");
            String response = "";
            try {
                response = r.readLine();
            } catch (IOException e) {
                System.out.println("Please try again!");
            }
            // split the user input by spaces
            String[] tokens = response.split(" ");
            // check if input exists
            if (tokens.length == 0) {
                System.out.println("Please try again!");
                continue;
            }
            // determine which operation to perform
            String operation = tokens[0];
            if (operation.equals("create_patient")) {
                createPatient(tokens);
            } else if (operation.equals("create_caregiver")) {
                createCaregiver(tokens);
            } else if (operation.equals("login_patient")) {
                loginPatient(tokens);
            } else if (operation.equals("login_caregiver")) {
                loginCaregiver(tokens);
            } else if (operation.equals("search_caregiver_schedule")) {
                searchCaregiverSchedule(tokens);
            } else if (operation.equals("reserve")) {
                reserve(tokens);
            } else if (operation.equals("upload_availability")) {
                uploadAvailability(tokens);
            } else if (operation.equals("cancel")) {
                cancel(tokens);
            } else if (operation.equals("add_doses")) {
                addDoses(tokens);
            } else if (operation.equals("show_appointments")) {
                showAppointments(tokens);
            } else if (operation.equals("logout")) {
                logout(tokens);
            } else if (operation.equals("quit")) {
                System.out.println("Bye!");
                return;
            } else {
                System.out.println("Invalid operation name!");
            }
        }
    }

    private static void createPatient(String[] tokens) {
        // create_patient <username> <password>
        // check 1: the length for tokens need to be exactly 3 to include all information (with the operation name)
        if (tokens.length != 3) {
            System.out.println("Create patient failed");
            return;
        }
        String username = tokens[1];
        String password = tokens[2];
        // check 2: check if the username has been taken already
        if (usernameExistsPatient(username)) {
            System.out.println("Username taken, try again");
            return;
        }
        byte[] salt = Util.generateSalt();
        byte[] hash = Util.generateHash(password, salt);
        // create the patient
        try {
            Patient patient = new Patient.PatientBuilder(username, salt, hash).build();
            // save to caregiver information to our database
            patient.saveToDB();
            System.out.println("Created user " + username);
        } catch (SQLException e) {
            System.out.println("Create patient failed");
        }
    }

    private static boolean usernameExistsPatient(String username) {
        ConnectionManager cm = new ConnectionManager();
        Connection con = cm.createConnection();

        String selectUsername = "SELECT * FROM Patients WHERE Name = ?";
        try {
            PreparedStatement statement = con.prepareStatement(selectUsername);
            statement.setString(1, username);
            ResultSet resultSet = statement.executeQuery();
            return resultSet.isBeforeFirst();
        } catch (SQLException e) {
            System.out.println("Error occurred when checking name");
        } finally {
            cm.closeConnection();
        }
        return true;
    }

    private static void createCaregiver(String[] tokens) {
        // create_caregiver <username> <password>
        // check 1: the length for tokens need to be exactly 3 to include all information (with the operation name)
        if (tokens.length != 3) {
            System.out.println("Failed to create user.");
            return;
        }
        String username = tokens[1];
        String password = tokens[2];
        // check 2: check if the username has been taken already
        if (usernameExistsCaregiver(username)) {
            System.out.println("Username taken, try again");
            return;
        }
        byte[] salt = Util.generateSalt();
        byte[] hash = Util.generateHash(password, salt);
        // create the caregiver
        try {
            Caregiver caregiver = new Caregiver.CaregiverBuilder(username, salt, hash).build(); 
            // save to caregiver information to our database
            caregiver.saveToDB();
            System.out.println("Created user " + username);
        } catch (SQLException e) {
            System.out.println("Failed to create user.");
        }
    }

    private static boolean usernameExistsCaregiver(String username) {
        ConnectionManager cm = new ConnectionManager();
        Connection con = cm.createConnection();

        String selectUsername = "SELECT * FROM Caregivers WHERE Username = ?";
        try {
            PreparedStatement statement = con.prepareStatement(selectUsername);
            statement.setString(1, username);
            ResultSet resultSet = statement.executeQuery();
            // returns false if the cursor is not before the first record or if there are no rows in the ResultSet.
            return resultSet.isBeforeFirst();
        } catch (SQLException e) {
            System.out.println("Error occurred when checking username");
        } finally {
            cm.closeConnection();
        }
        return true;
    }

    private static void loginPatient(String[] tokens) {
        // login_patient <username> <password>
        // check 1: if someone's already logged-in, they need to log out first
        if (currentCaregiver != null || currentPatient != null) {
            System.out.println("User already logged in, try again");
            return;
        }
        // check 2: the length for tokens need to be exactly 3 to include all information (with the operation name)
        if (tokens.length != 3) {
            System.out.println("Login patient failed");
            return;
        }
        String username = tokens[1];
        String password = tokens[2];

        Patient patient = null;
        try {
            patient = new Patient.PatientGetter(username, password).get();
        } catch (SQLException e) {
            System.out.println("Login patient failed");
        }
        // check if the login was successful
        if (patient == null) {
            System.out.println("Login patient failed");
        } else {
            System.out.println("Logged in as " + username);
            currentPatient = patient;
        }
    }

    private static void loginCaregiver(String[] tokens) {
        // login_caregiver <username> <password>
        // check 1: if someone's already logged-in, they need to log out first
        if (currentCaregiver != null || currentPatient != null) {
            System.out.println("User already logged in.");
            return;
        }
        // check 2: the length for tokens need to be exactly 3 to include all information (with the operation name)
        if (tokens.length != 3) {
            System.out.println("Login failed.");
            return;
        }
        String username = tokens[1];
        String password = tokens[2];

        Caregiver caregiver = null;
        try {
            caregiver = new Caregiver.CaregiverGetter(username, password).get();
        } catch (SQLException e) {
            System.out.println("Login failed.");
        }
        // check if the login was successful
        if (caregiver == null) {
            System.out.println("Login failed.");
        } else {
            System.out.println("Logged in as: " + username);
            currentCaregiver = caregiver;
        }
    }

    private static void searchCaregiverSchedule(String[] tokens) {
        // part 2
        // checks if no user is logged in yet and if there are any other errors
        if (currentCaregiver == null && currentPatient == null) {
            System.out.println("Please login first");
            return;
        } else if (tokens.length != 2) {
            System.out.println("Please try again");
            return;
        }

        // get date
        String date = tokens[1];
        Date d = null;
        try {
            d = Date.valueOf(date);
        } catch (IllegalArgumentException e) {
            System.out.println("Please try again");
            return;
        }

        ConnectionManager cm = new ConnectionManager();
        Connection con = cm.createConnection();

        // get caregivers
        String checkSched = "SELECT C.Username FROM Caregivers C, Availabilities A WHERE A.Username = C.Username AND Time = ? ORDER BY C.Username";
        try {
            PreparedStatement statement = con.prepareStatement(checkSched);
            statement.setDate(1, d);
            ResultSet resultSet = statement.executeQuery();

            System.out.println("Caregivers:");
            boolean foundCaregiver = false;

            while (resultSet.next()) {
                foundCaregiver = true;
                System.out.println(resultSet.getString("Username"));
            }

            if (!foundCaregiver) {
                System.out.println("No caregivers available");
            }

        } catch (IllegalArgumentException e) {
            System.out.println("Please try again");
        } catch (SQLException e) {
            System.out.println("Error occurred when searching for caregiver");
        }

        // get vaccines
        String checkVaccine = "SELECT * FROM Vaccines V ORDER BY V.Name";
        try {
            PreparedStatement statement = con.prepareStatement(checkVaccine);
            ResultSet resultSet = statement.executeQuery();

            System.out.println("Vaccines:");
            boolean foundVaccine = false;

            while (resultSet.next()) {
                foundVaccine = true;
                String vaccine = resultSet.getString("Name");
                int numOfVaccine = resultSet.getInt("Doses");
                System.out.println(vaccine + " " + numOfVaccine);
            }

            if (!foundVaccine) {
                System.out.println("No vaccines available");
            }

        } catch (IllegalArgumentException e) {
            System.out.println("Please try again");
        } catch (SQLException e) {
            System.out.println("Error occurred when searching for vaccine");
        } finally {
            cm.closeConnection();
        }
    }

    private static void reserve(String[] tokens) {
        if (currentCaregiver == null && currentPatient == null) {
            System.out.println("Please login first");
            return;
        } else if (currentCaregiver != null) {
            System.out.println("Please login as a patient");
            return;
        } else if (tokens.length != 3) {
            System.out.println("Please try again");
            return;
        }

        // check valid date
        String date = tokens[1];
        String vaccine = tokens[2];
        Date d = null;

        try {
            d = Date.valueOf(date);
        } catch (IllegalArgumentException e) {
            System.out.println("Please try again");
            return;
        }

        ConnectionManager cm = new ConnectionManager();
        Connection con = cm.createConnection();

        String checkVaccine = "SELECT V.Doses FROM Vaccines V WHERE V.Name = ?";
        int doses = 0;
        try {
            PreparedStatement statement = con.prepareStatement(checkVaccine);
            statement.setString(1, vaccine);
            ResultSet resultSet = statement.executeQuery();

            if (!resultSet.next() || resultSet.getInt("Doses") <= 0) {
                System.out.println("Not enough available doses");
                return;
            }
            doses = resultSet.getInt("Doses");

        } catch (IllegalArgumentException e) {
            System.out.println("Please try again");
        } catch (SQLException e) {
            System.out.println("Error occurred when searching for vaccine");
        }

        String checkCaregiver = "SELECT A.Username FROM Availabilities A WHERE Time = ? ORDER BY A.Username";
        String caregiver = null;
        try {
            PreparedStatement statement = con.prepareStatement(checkCaregiver);
            statement.setDate(1, d);
            ResultSet resultSet = statement.executeQuery();

            if(!resultSet.next()) {
                System.out.println("No caregiver is available");
                return;
            }

            caregiver = resultSet.getString("Username");
        } catch (IllegalArgumentException e) {
            System.out.println("Please try again");
        } catch (SQLException e) {
            System.out.println("Error occurred when searching for caregiver");
        }

        String getAppointment = "SELECT MAX(aid) AS max FROM Appointments";
        int newAppointmentID = 1;
        try {
            PreparedStatement statement = con.prepareStatement(getAppointment);
            ResultSet resultSet = statement.executeQuery();

            if (resultSet.next()) {
               newAppointmentID = resultSet.getInt("max") + 1;
            }
        } catch (IllegalArgumentException e) {
            System.out.println("Please try again");
        } catch (SQLException e) {
            System.out.println("Error occurred when searching for appointment id");
        }

        String insertAppointment = "INSERT INTO Appointments VALUES (?, ?, ?, ?, ?)";
        try {
            PreparedStatement statement = con.prepareStatement(insertAppointment);
            statement.setInt(1, newAppointmentID);
            statement.setString(2, vaccine);
            statement.setString(3, currentPatient.getUsername());
            statement.setString(4, caregiver);
            statement.setDate(5, d);
            statement.executeUpdate();
        } catch (IllegalArgumentException e) {
            System.out.println("Please try again");
        } catch (SQLException e) {
            System.out.println("Error occurred when updating appointments");
        }

        String deleteAvailability = "DELETE FROM Availabilities WHERE Username = ? AND Time = ?";
        try {
            PreparedStatement statement = con.prepareStatement(deleteAvailability);
            statement.setString(1, caregiver);
            statement.setDate(2, d);
            statement.executeUpdate();
        } catch (IllegalArgumentException e) {
            System.out.println("Please try again");
        } catch (SQLException e) {
            System.out.println("Error occurred when updating availabilities");
        } finally {
            cm.closeConnection();
        }

        Vaccine updateDoses = null;
        try {
            updateDoses = new Vaccine.VaccineGetter(vaccine).get();
            updateDoses.decreaseAvailableDoses(1);
        } catch (SQLException e) {
            System.out.println("Error occurred when updating vaccine");
        }

        System.out.println("Appointment ID " + newAppointmentID + ", Caregiver username " + caregiver);
    }

    private static void uploadAvailability(String[] tokens) {
        // upload_availability <date>
        // check 1: check if the current logged-in user is a caregiver
        if (currentCaregiver == null) {
            System.out.println("Please login as a caregiver first!");
            return;
        }
        // check 2: the length for tokens need to be exactly 2 to include all information (with the operation name)
        if (tokens.length != 2) {
            System.out.println("Please try again!");
            return;
        }
        String date = tokens[1];
        try {
            Date d = Date.valueOf(date);
            currentCaregiver.uploadAvailability(d);
            System.out.println("Availability uploaded!");
        } catch (IllegalArgumentException e) {
            System.out.println("Please enter a valid date!");
        } catch (SQLException e) {
            System.out.println("Error occurred when uploading availability");
        }
    }

    private static void cancel(String[] tokens) {
        if (currentCaregiver == null && currentPatient == null) {
            System.out.println("Please login first");
            return;
        } else if (tokens.length != 2) {
            System.out.println("Please try again");
            return;
        }

        int appointmentId = Integer.parseInt(tokens[1]);

        ConnectionManager cm = new ConnectionManager();
        Connection con = cm.createConnection();

        try {
            String getAppointment = "SELECT vaccine_name, patient_name, caregiver_name, Time FROM Appointments WHERE aid = ?";
            PreparedStatement statement = con.prepareStatement(getAppointment);
            statement.setInt(1, appointmentId);
            ResultSet resultSet = statement.executeQuery();

            if (!resultSet.next()) {
                System.out.println("Appointment ID " + appointmentId + " does not exist");
                return;
            }

            String vaccineName = resultSet.getString("vaccine_name");
            String patientName = resultSet.getString("patient_name");
            String caregiverName = resultSet.getString("caregiver_name");
            Date appointmentDate = resultSet.getDate("Time");

            if ((currentPatient != null && !currentPatient.getUsername().equals(patientName)) &&
                    (currentCaregiver != null && !currentCaregiver.getUsername().equals(caregiverName))) {
                System.out.println("Please try again");
                return;
            }

            String deleteAppointment = "DELETE FROM Appointments WHERE aid = ?";
            PreparedStatement deleteStatement = con.prepareStatement(deleteAppointment);
            deleteStatement.setInt(1, appointmentId);
            deleteStatement.executeUpdate();

            String updateVaccine = "UPDATE Vaccines SET Doses = Doses + 1 WHERE Name = ?";
            PreparedStatement updateVaccineStatement = con.prepareStatement(updateVaccine);
            updateVaccineStatement.setString(1, vaccineName);
            updateVaccineStatement.executeUpdate();

            String checkAvailability = "SELECT COUNT(*) FROM Availabilities WHERE Time = ? AND Username = ?";
            PreparedStatement checkAvailabilityStatement = con.prepareStatement(checkAvailability);
            checkAvailabilityStatement.setDate(1, appointmentDate);
            checkAvailabilityStatement.setString(2, caregiverName);
            ResultSet availabilityResultSet = checkAvailabilityStatement.executeQuery();

            if (availabilityResultSet.next() && availabilityResultSet.getInt(1) == 0) {
                String addAvailability = "INSERT INTO Availabilities (Time, Username) VALUES (?, ?)";
                PreparedStatement addAvailabilityStmt = con.prepareStatement(addAvailability);
                addAvailabilityStmt.setDate(1, appointmentDate);
                addAvailabilityStmt.setString(2, caregiverName);
                addAvailabilityStmt.executeUpdate();
            }

            System.out.println("Appointment ID " + appointmentId + " has been successfully canceled");

        } catch (SQLException e) {
            System.out.println("Please try again");
        } finally {
            cm.closeConnection();
        }
    }



    private static void addDoses(String[] tokens) {
        // add_doses <vaccine> <number>
        // check 1: check if the current logged-in user is a caregiver
        if (currentCaregiver == null) {
            System.out.println("Please login as a caregiver first!");
            return;
        }
        // check 2: the length for tokens need to be exactly 3 to include all information (with the operation name)
        if (tokens.length != 3) {
            System.out.println("Please try again!");
            return;
        }
        String vaccineName = tokens[1];
        int doses = Integer.parseInt(tokens[2]);
        Vaccine vaccine = null;
        try {
            vaccine = new Vaccine.VaccineGetter(vaccineName).get();
        } catch (SQLException e) {
            System.out.println("Error occurred when adding doses");
        }
        // check 3: if getter returns null, it means that we need to create the vaccine and insert it into the Vaccines
        //          table
        if (vaccine == null) {
            try {
                vaccine = new Vaccine.VaccineBuilder(vaccineName, doses).build();
                vaccine.saveToDB();
            } catch (SQLException e) {
                System.out.println("Error occurred when adding doses");
            }
        } else {
            // if the vaccine is not null, meaning that the vaccine already exists in our table
            try {
                vaccine.increaseAvailableDoses(doses);
            } catch (SQLException e) {
                System.out.println("Error occurred when adding doses");
            }
        }
        System.out.println("Doses updated!");
    }

    private static void showAppointments(String[] tokens) {
        if (currentCaregiver == null && currentPatient == null) {
            System.out.println("Please login first");
            return;
        } else if (tokens.length != 1){
            System.out.println("Please try again");
            return;
        }

        ConnectionManager cm = new ConnectionManager();
        Connection con = cm.createConnection();

        if (currentCaregiver != null) {
            String apptInfo = "SELECT aid, vaccine_name, Time, patient_name FROM Appointments WHERE caregiver_name = ? ORDER BY aid";
            try {
                PreparedStatement statement = con.prepareStatement(apptInfo);
                statement.setString(1, currentCaregiver.getUsername());
                ResultSet resultSet = statement.executeQuery();

                boolean hasAppointment = false;

                while(resultSet.next()) {
                    hasAppointment = true;
                    String toPrint = resultSet.getInt(1) + " " + resultSet.getString(2) + " " +
                                     resultSet.getDate(3) + " " + resultSet.getString(4);
                    System.out.println(toPrint);
                }

                if (!hasAppointment) {
                    System.out.println("No appointments scheduled");
                }

                cm.closeConnection();
            } catch (SQLException e) {
                System.out.println("Error occurred when getting caregiver information");
            }
        } else {
            String apptInfo = "SELECT aid, vaccine_name, Time, caregiver_name FROM Appointments WHERE patient_name = ? ORDER BY aid";
            try {
                PreparedStatement statement = con.prepareStatement(apptInfo);
                statement.setString(1, currentPatient.getUsername());
                ResultSet resultSet = statement.executeQuery();

                boolean hasAppointment = false;

                while(resultSet.next()) {
                    hasAppointment = true;
                    String toPrint = resultSet.getInt(1) + " " + resultSet.getString(2) + " " +
                                     resultSet.getDate(3) + " " + resultSet.getString(4);
                    System.out.println(toPrint);
                }

                if (!hasAppointment) {
                    System.out.println("No appointments scheduled");
                }

                cm.closeConnection();
            } catch (SQLException e) {
                System.out.println("Error occurred when getting patient information");
            }
        }

    }

    private static void logout(String[] tokens) {
        if (currentCaregiver == null && currentPatient == null) {
            System.out.println("Please login first");
            return;
        } else if (tokens.length != 1) {
            System.out.println("Please try again");
            return;
        }

        if(currentCaregiver != null) {
            currentCaregiver = null;
        } else {
            currentPatient = null;
        }

        System.out.println("Successfully logged out");

    }
}
