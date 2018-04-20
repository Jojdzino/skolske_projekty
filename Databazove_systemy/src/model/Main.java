package model;

import controller.ControllerUsers;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import org.postgresql.ds.PGConnectionPoolDataSource;

import java.sql.Connection;
import java.sql.SQLException;

public class Main extends Application {

    public PGConnectionPoolDataSource dataSource;

    @Override
    public void start(Stage primaryStage) throws Exception{
        openConnection();
        //opens first window
        ControllerUsers usersC=new ControllerUsers(dataSource);
        FXMLLoader fxmlLoader=new FXMLLoader((getClass().getResource("../sample/users.fxml")));
        fxmlLoader.setController(usersC);
        Parent root = fxmlLoader.load();
        primaryStage.setTitle("Informačný systém banky");
        primaryStage.setScene(new Scene(root, 500, 250));
        primaryStage.show();
    }


    public static void main(String[] args) {
        launch(args);
    }
    //opens connection to database
    public void openConnection () throws SQLException {
        dataSource = new PGConnectionPoolDataSource();
        dataSource.setServerName("localhost");
        dataSource.setDatabaseName("Updated_project");
        dataSource.setUser("postgres");
        dataSource.setPassword("Deny123");

        Connection conn = dataSource.getConnection();
    }
}
