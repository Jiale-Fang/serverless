package gcfv2pubsub;

import java.sql.*;
import java.util.logging.Logger;

public class JdbcUtils {
    private static final Logger logger = Logger.getLogger(PubSubFunction.class.getName());
    private static final String url = System.getenv("DB_URL");
    private static final String user = System.getenv("DB_USER");
    private static final String password = System.getenv("DB_PASSWORD");

    public static void insertEmailLog(EmailLog emailLog) {
        String sql = "INSERT INTO tb_email_log (id, recipient, sender, subject, content, create_time, is_sent, err_msg) VALUES (?, ?, ?, ?, ?, ?, ?, ?)";
        try (Connection conn = DriverManager.getConnection(url, user, password);
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, emailLog.getId());
            pstmt.setString(2, emailLog.getRecipient());
            pstmt.setString(3, emailLog.getSender());
            pstmt.setString(4, emailLog.getSubject());
            pstmt.setString(5, emailLog.getContent());
            pstmt.setTimestamp(6, new Timestamp(System.currentTimeMillis()));
            pstmt.setBoolean(7, emailLog.getIsSent());
            pstmt.setString(8, emailLog.getErrMsg());


            int affectedRows = pstmt.executeUpdate();
            if (affectedRows == 0) {
                logger.severe("Insert emailLog fail... " + emailLog);
            } else {
                logger.info("Successfully insert EmailLog:" + emailLog);
            }

        } catch (SQLException e) {
            logger.severe(e.getMessage());
        }
    }
}
