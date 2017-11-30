package SystembolagetDBUpdater;

import com.mysql.jdbc.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class ConnectionFactory {
    public static Connection getConnection(String url) throws SQLException {
        String user = "root";
        String password = "";
        Connection connection = (com.mysql.jdbc.Connection) DriverManager.getConnection(url, user, password);
        return connection;
    }
}
