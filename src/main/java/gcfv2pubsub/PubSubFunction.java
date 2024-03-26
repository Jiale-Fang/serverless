package gcfv2pubsub;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.cloud.functions.CloudEventsFunction;
import com.google.events.cloud.pubsub.v1.MessagePublishedData;
import com.google.events.cloud.pubsub.v1.Message;
import com.mashape.unirest.http.HttpResponse;
import com.mashape.unirest.http.JsonNode;
import com.mashape.unirest.http.Unirest;
import com.mashape.unirest.http.exceptions.UnirestException;
import io.cloudevents.CloudEvent;
import org.apache.http.HttpStatus;

import java.util.Base64;
import java.util.logging.Logger;

public class PubSubFunction implements CloudEventsFunction {
    private static final Logger logger = Logger.getLogger(PubSubFunction.class.getName());
    private static final String API_KEY = System.getenv("MAILGUN_API_KEY");
    private static final String Domain = "jialefang.site";
    private final ObjectMapper mapper = new ObjectMapper()
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

    @Override
    public void accept(CloudEvent event) {
        try {
            String cloudEventData = new String(event.getData().toBytes());
            MessagePublishedData data = mapper.readValue(cloudEventData, MessagePublishedData.class);
            Message message = data.getMessage();
            String encodedData = message.getData();
            String decodedData = new String(Base64.getDecoder().decode(encodedData));
            EmailLog emailLog = mapper.readValue(decodedData, EmailLog.class);

            boolean isSent = sendSimpleMessage(emailLog);
            emailLog.setIsSent(isSent);

            // Log the message
            logger.info("Pub/Sub send an email: " + emailLog);
            JdbcUtils.insertEmailLog(emailLog);

        } catch (Exception e) {
            logger.severe("Failed to process the CloudEvent: " + e.getMessage());
        }
    }

    /**
     * Send email
     *
     * @param emailLog emailLog entity
     * @return Successfully send email or not
     */
    public boolean sendSimpleMessage(EmailLog emailLog) {
        HttpResponse<JsonNode> response = null;
        try {
            response = Unirest.post("https://api.mailgun.net/v3/" + Domain + "/messages")
                    .basicAuth("api", API_KEY)
                    .queryString("from", emailLog.getSender())
                    .queryString("to", emailLog.getRecipient())
                    .queryString("subject", emailLog.getSubject())
                    .queryString("html", emailLog.getContent())
                    .asJson();
        } catch (UnirestException e) {
            logger.severe(e.getMessage());
            throw new RuntimeException(e);
        }
        if (response.getStatus() != HttpStatus.SC_OK) {
            logger.severe(response.getBody().toString());
            return false;
        }
        return true;
    }
}
