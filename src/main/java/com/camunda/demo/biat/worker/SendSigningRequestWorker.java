package com.camunda.demo.biat.worker;

import java.awt.Desktop;
import java.net.URI;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Map;

import org.apache.commons.codec.binary.Base64;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;

import com.camunda.demo.biat.storage.StorageProperties;
import com.docusign.esign.api.EnvelopesApi;
import com.docusign.esign.client.ApiClient;
import com.docusign.esign.client.ApiException;
import com.docusign.esign.client.auth.OAuth.OAuthToken;
import com.docusign.esign.client.auth.OAuth.UserInfo;
import com.docusign.esign.model.CarbonCopy;
import com.docusign.esign.model.Document;
import com.docusign.esign.model.EnvelopeDefinition;
import com.docusign.esign.model.EnvelopeSummary;
import com.docusign.esign.model.Recipients;
import com.docusign.esign.model.SignHere;
import com.docusign.esign.model.Signer;
import com.docusign.esign.model.Tabs;

import io.camunda.zeebe.spring.client.annotation.JobWorker;
import io.camunda.zeebe.spring.client.annotation.Variable;
import io.camunda.zeebe.spring.client.annotation.VariablesAsType;

/**
 * Starter class for JWTConsoleApp application.
 */

@Component
public class SendSigningRequestWorker {

    private final Path rootLocation;

    @Value("${docuSign.clientId}")
    private String clientId;// = "6266fae0-62bc-4a20-ac9c-f379af347a0c";

    @Value("${docuSign.userId}")
    private String userId;// = "31dcb2b0-b207-4f98-adfd-efa0969b6f96";

    @Value("${docuSign.rsaKeyFile}")
    private String rsaKeyFile;// =
                              // "/Users/stefan/git/customDemoBITA/biat/src/main/resources/docusign_private.key";

    // private String signerEmail = "stefan.wiese@camunda.com";
    // private String signerName = "Stefan Wiese";
    // private String ccEmail = "stefan.wiese@me.com";
    // private String ccName = "Stefan Wiese";

    @Autowired
    public SendSigningRequestWorker(StorageProperties properties) {
        this.rootLocation = Paths.get(properties.getLocation());
    }

    static String DevCenterPage = "https://developers.docusign.com/platform/auth/consent";

    @JobWorker(type = "sendSigningRequest", fetchAllVariables = true)
    public Map<String, Object> sendSigningRequest(@VariablesAsType EmployeeOnboardingData data, @Variable String managerEmail) throws Exception {

        try {
            // Get access token and accountId
            ApiClient apiClient = new ApiClient("https://demo.docusign.net/restapi");
            apiClient.setOAuthBasePath("account-d.docusign.com");
            ArrayList<String> scopes = new ArrayList<String>();
            scopes.add("signature");
            scopes.add("impersonation");

            URL rsaKeyFileUrl = this.getClass().getClassLoader().getResource(rsaKeyFile);
            byte[] privateKeyBytes = Files.readAllBytes(Paths.get(rsaKeyFileUrl.toURI()));

            OAuthToken oAuthToken = apiClient.requestJWTUserToken(
                    clientId,
                    userId,
                    scopes,
                    privateKeyBytes,
                    3600);
            String accessToken = oAuthToken.getAccessToken();
            UserInfo userInfo = apiClient.getUserInfo(accessToken);
            String accountId = userInfo.getAccounts().get(0).getAccountId();

            // Create envelopeDefinition object
            EnvelopeDefinition envelope = new EnvelopeDefinition();
            envelope.setEmailSubject("Please sign this document set");
            envelope.setStatus("sent");

            // Create tabs object
            SignHere signHere = new SignHere();
            signHere.setDocumentId("1");
            signHere.setPageNumber("1");
            signHere.setXPosition("380");
            signHere.setYPosition("220");
            Tabs tabs = new Tabs();
            tabs.setSignHereTabs(Arrays.asList(signHere));
            // Set recipients
            Signer signer = new Signer();
            signer.setEmail(data.getEmail());
            signer.setName(data.getEmployeeName());
            signer.recipientId("1");
            signer.setTabs(tabs);
            CarbonCopy cc = new CarbonCopy();
            cc.setEmail(managerEmail);
            cc.setName("Manager");
            cc.recipientId("2");
            Recipients recipients = new Recipients();
            recipients.setSigners(Arrays.asList(signer));
            recipients.setCarbonCopies(Arrays.asList(cc));
            envelope.setRecipients(recipients);

            // Load a file from disk.

            Resource pdf = new FileSystemResource(
                    this.rootLocation.resolve(data.getEmployeeName() + "_" + data.getDepartment() + ".pdf"));

            String pdfAsbase64 = new String(Base64.encodeBase64(pdf.getContentAsByteArray()));

            // Add document
            Document document = new Document();
            document.setName(data.getEmployeeName() + "_" + data.getDepartment() + ".pdf");
            document.setDocumentBase64(pdfAsbase64);
            document.setFileExtension("pdf");
            document.setDocumentId("1");
            envelope.setDocuments(Arrays.asList(document));

            // Send envelope
            apiClient.addDefaultHeader("Authorization", "Bearer " + accessToken);
            EnvelopesApi envelopesApi = new EnvelopesApi(apiClient);
            EnvelopeSummary results = envelopesApi.createEnvelope(accountId, envelope);
            System.out.println("Successfully sent envelope with envelopeId " + results.getEnvelopeId());
            return Map.of("envelopeId", results.getEnvelopeId());
        } catch (ApiException exp) {
            if (exp.getMessage().contains("consent_required")) {
                try {
                    System.out.println(
                            "Consent required, please provide consent in browser window and then run this app again.");
                    Desktop.getDesktop().browse(new URI(
                            "https://account-d.docusign.com/oauth/auth?response_type=code&scope=impersonation%20signature&client_id="
                                    + clientId + "&redirect_uri=" + DevCenterPage));

                    return Map.of("envelopeId", null);
                } catch (Exception e) {
                    e.printStackTrace();
                    System.out.print("Error!!!  ");
                    System.out.print(e.getMessage());
                    throw e;
                }
            } else {
                exp.printStackTrace();
                throw exp;
            }
        } catch (Exception e) {
            e.printStackTrace();
            System.out.print("Error!!!  ");
            System.out.print(e.getMessage());
            throw e;
        }
    }
}