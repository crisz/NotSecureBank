package com.notsecurebank.api;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;

import com.notsecurebank.model.Account;
import com.notsecurebank.model.User;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.apache.wink.json4j.JSONException;
import org.apache.wink.json4j.JSONObject;

import com.notsecurebank.util.OperationsUtil;

import static com.notsecurebank.util.ServletUtil.getUser;

@Path("transfer")
public class TransferAPI extends NotSecureBankAPI {

    private static final Logger LOG = LogManager.getLogger(TransferAPI.class);

    @POST
    @Produces("application/json")
    public Response transfer(String bodyJSON, @Context HttpServletRequest request) {
        LOG.info("transfer");

        JSONObject myJson = new JSONObject();
        Long creditActId;
        String fromAccount;
        double amount;
        String message;

        try {
            myJson = new JSONObject(bodyJSON);
            // Get the transaction parameters
            
            String inputCsrfToken = myJson.get("csrfToken").toString();
            String sessionCsrfToken = (String)request.getSession().getAttribute("csrfToken");
            if (!inputCsrfToken.equals(sessionCsrfToken)) {
                return Response.status(403).entity("{\"Error\": \"Invalid request\"}").build();
            }
            creditActId = Long.parseLong(myJson.get("toAccount").toString());
            fromAccount = myJson.get("fromAccount").toString();
            
            User user = getUser(request);
            boolean found = false;
            for (Account account: user.getAccounts()) {
                if (account.getAccountId() == Long.parseLong(fromAccount)) {
                    found = true;
                    break;
                }
            }
            
            if (!found) {
                return Response.status(403).entity("{\"Error\": \"Invalid fromAccount parameter\"}").build();
            }
            amount = Double.parseDouble(myJson.get("transferAmount").toString());
            message = OperationsUtil.doTransfer(request, creditActId, fromAccount, amount);
        } catch (JSONException e) {
            LOG.error(e.toString());
            return Response.status(500).entity("{\"Error\": \"Request is not in JSON format\"}").build();
        }

        if (message.startsWith("ERROR")) {
            return Response.status(500).entity("\"error\":\"" + message + "\"}").build();
        }

        return Response.status(200).entity("{\"success\":\"" + message + "\"}").build();
    }
}
