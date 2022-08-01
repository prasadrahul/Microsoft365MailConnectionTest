package com.rahulnotebook.app;

import com.azure.core.credential.TokenRequestContext;
import com.azure.identity.ClientSecretCredential;
import com.azure.identity.ClientSecretCredentialBuilder;
import com.microsoft.graph.logger.DefaultLogger;
import com.microsoft.graph.logger.LoggerLevel;
import com.sun.mail.util.BASE64EncoderStream;
import okhttp3.*;
import org.json.JSONObject;

import javax.mail.*;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

/**
 * Mail Test
 *
 */
public class App 
{
    static final String CLIENT_ID = "CLIENT_ID";
    static final String CLIENT_SECRET = "CLIENT_SECRET" ;
    static final String TENANT_GUID = "TENANT_GUID";
    static final String MAIL_USER = "MAIL_USER";
    static final String MAIL_RECIPIENT = "MAIL_RECIPIENT";
    static final List<String> SCOPES = new ArrayList<>();
    static final String ACCESS_TOKEN_URL = "ACCESS_TOKEN_URL";
    static final String DEFAULT_SCOPE = "https://graph.microsoft.com/.default";
    static final String HOST_SERVER = "outlook.office365.com";

    public static void main(String[] args) throws IOException, MessagingException {
        String access_token_via_rest_call = getMircosoftAccessTokenViaRestAPI();
        performFetchEmail(access_token_via_rest_call);

/*        String access_token_via_masl_graph = getMircosoftAccessTokenViaMaslGraph();
        performFetchEmail(access_token_via_masl_graph);*/

//        performSendEmail();
    }

    private static void performFetchEmail() {

    }

    private static void performFetchEmail(String access_token_via_rest_call) throws MessagingException {

        Properties props = new Properties();

//            ---------- with imap --------------
        props.put("mail.imap.host", HOST_SERVER);
        props.put("mail.imap.port", "993");
        props.put("mail.imap.sasl.enable", true);
        props.put("mail.imap.sasl.mechanisms", "XOAUTH2");
        props.put("mail.imap.auth.login.disable", true);
        props.put("mail.imap.auth.plain.disable", false);
        props.put("mail.imap.ssl.enable", true);
        props.put("mail.debug.auth", true);
        props.put("mail.imaps.ssl.trust", "*");
        props.put("mail.imap.auth.xoauth2.disable", false);

        Session session = Session.getInstance(props, null);
        session.setDebug(true);
        Store store = session.getStore("imap");

//        String newToken = new String(BASE64EncoderStream.encode(String.format("user=%s\1auth=Bearer %s\1\1", MAIL_USER, access_token_via_rest_call).getBytes(StandardCharsets.US_ASCII)));

        store.connect(HOST_SERVER, MAIL_USER, access_token_via_rest_call);

        Folder emailFolderObj = store.getFolder("INBOX");
        emailFolderObj.open(Folder.READ_ONLY);
        Message[] messageobjs = emailFolderObj.getMessages();

        System.out.println("Total message : " + messageobjs.length);

        for (int i = 0, n = 2; i < n; i++) {
            Message indvidualmsg = messageobjs[i];
            System.out.println("Printing individual messages");
            System.out.println("No# " + (i + 1));
            System.out.println("Email Subject: " + indvidualmsg.getSubject());
            System.out.println("Sender: " + indvidualmsg.getFrom()[0]);
        }
//Now close all the objects
        emailFolderObj.close(false);
        store.close();

    }

    private static String getMircosoftAccessTokenViaMaslGraph() {

        SCOPES.add(DEFAULT_SCOPE);


        // Create default logger to only log errors
        DefaultLogger logger = new DefaultLogger();
        logger.setLoggingLevel(LoggerLevel.DEBUG);

        final ClientSecretCredential clientSecretCredential = new ClientSecretCredentialBuilder()
                .clientId(CLIENT_ID)
                .clientSecret(CLIENT_SECRET)
                .tenantId(TENANT_GUID)
                .build();

        String access_token = clientSecretCredential.getToken(new TokenRequestContext().setScopes(SCOPES).setTenantId(TENANT_GUID)).block().getToken();

        System.out.println("MASL access token" + access_token);

        return access_token;

    }

    private static String getMircosoftAccessTokenViaRestAPI() throws IOException {

        OkHttpClient client = new OkHttpClient().newBuilder().build();

        //        ---------- office365 - Application Permission -------------
        RequestBody body = new MultipartBody.Builder().setType(MultipartBody.FORM)
                .addFormDataPart("grant_type", "client_credentials")
                .addFormDataPart("Scope", DEFAULT_SCOPE)
                .addFormDataPart("Client_Id", CLIENT_ID)
                .addFormDataPart("Client_Secret", CLIENT_SECRET)
                .build();

        Request request = new Request.Builder()
                .url(ACCESS_TOKEN_URL)
                .method("POST", body)
                .addHeader("Cookie", "fpc=rnb_")
                .build();

        Response response = client.newCall(request).execute();


        JSONObject mainObject = new JSONObject(response.body().string());

        System.out.println("MainObject :: " + mainObject);

        Object token = mainObject.get("access_token");

        return token.toString();
    }
}
