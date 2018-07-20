package com.lawson.bpm.adapter.sdk;

import java.net.HttpURLConnection;
import java.net.URL;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class GraphGetDriveItem implements BPMAdapter {

    private final int MEGABYTE = 4194304;
    private final String[] DEFAULT_SCOPES = { "https://graph.microsoft.com/.default" };

    private BPMAdapterRuntime _adapter;
    private String _clientId;
    private String _clientSecret;
    private String _tenantId;
    private String _driveId;
    private String _path;

    public void setClientId(String val) {
        if(val == null || val.isEmpty())
        {
            _logError("The property 'ClientId' is empty or null.");
        }

        this._clientId = val;
    }

    public void setClientSecret(String val) {
        if(val == null || val.isEmpty())
        {
            _logError("The property 'ClientSecret' is empty or null.");
        }

        this._clientSecret = val;
    }

    public void setTenantId(String val) {
        if(val == null || val.isEmpty())
        {
            _logError("The property 'TenantId' is empty or null.");
        }

        this._tenantId = val;
    }

    public void setDriveId(String val) {
        if(val == null || val.isEmpty())
        {
            _logError("The property 'TenantId' is empty or null.");
        }

        this._driveId = val;
    }

    public void setPath(String val) {
        if(val == null || val.isEmpty())
        {
            _logError("The property 'TenantId' is empty or null.");
        }

        this._path = val;
    }

    public GraphGetDriveItem() {

    }

    public void getItem(String driveId, String path) {
        URL requestUrl = new URL("https://graph.microsoft.com/v1.0/drives/" + driveId + "/items/" + path);
        HttpURLConnection con = (HttpURLConnection) requestUrl.openConnection();
        con.setRequestMethod("GET");

        _authenticateRequest(con);

        int statusCode = con.getResponseCode();

        if(statusCode != 200)
        {
            _logError(MessageFormat.format("Failed to get the file at {0}. Server retured {1}", path, statusCode));
            return;
        }

        BufferedReader in = new BufferedReader(new InputStreamReader(con.getInputStream()));
        String inputLine;
        StringBuffer responseBody = new StringBuffer();
        while ((inputLine = in.readLine()) != null) {
            responseBody.append(inputLine);
        }
        in.close();
        con.disconnect();
    }

    public void getItem(BPMAdapterRuntime runtime) {
        this._adapter = runtime;
        getItem(this._driveId, this._path);
    }

    private void _authenticateRequest(HttpURLConnection con, String[] scopes) {
        String accessToken = _getToken(this._clientId, this._clientSecret, this._tenantId, scopes);
        con.setRequestProperty("Authorization", "Bearer " + accessToken);
    }

    private void _authenticateRequest(HttpURLConnection con) {
        _authenticateRequest(con, DEFAULT_SCOPES);
    }

    private void _getToken(String clientId, String clientSecret, String tenant, String[] scopes) {
        URL requestUrl = new URL("https://login.microsoftonline.com/" + tenant + "/oauth2/v2.0/token");
        HttpURLConnection con = (HttpURLConnection) requestUrl.openConnection();
        con.setRequestMethod("POST");
        con.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");

        String scope = String.join(" ", scopes);

        Map<String, String> parameters = new HashMap<>();
        parameters.put("client_id", clientId);
        parameters.put("client_secret", clientSecret);
        parameters.put("scope", scope);
        parameters.put("grant_type", "client_credentials");

        String requestBody = ParameterStringBuilder._getParamsString(parameters);
        
        con.setDoOutput(true);
        DataOutputStream out = new DataOutputStream(con.getOutputStream());
        out.writeBytes(requestBody);
        out.flush();
        out.close();

        int statusCode = con.getResponseCode();

        if (statusCode != 200)
        {
            _logError("Failed to retrieve access token. Server returned " + statusCode);
            return;
        }

        BufferedReader in = new BufferedReader(new InputStreamReader(con.getInputStream()));
        String inputLine;
        StringBuffer responseBody = new StringBuffer();
        while ((inputLine = in.readLine()) != null) {
            responseBody.append(inputLine);
        }
        in.close();
        con.disconnect();
    }

    private String _encodeURIComponent(String s, String encoding) {
        String result;

        try {
            result = URLEncoder.encode(s, encoding)
                    .replaceAll("\\+", "%20")
                    .replaceAll("\\%21", "!")
                    .replaceAll("\\%27", "'")
                    .replaceAll("\\%28", "(")
                    .replaceAll("\\%29", ")")
                    .replaceAll("\\%7E", "~");
        } catch (UnsupportedEncodingException e) {
            result = s;
        }

        return result;
    }

    private String _getParamsString(Map<String, String> params) 
      throws UnsupportedEncodingException{
        StringBuilder result = new StringBuilder();
 
        for (Map.Entry<String, String> entry : params.entrySet()) {
          result.append(URLEncoder.encode(entry.getKey(), "UTF-8"));
          result.append("=");
          result.append(_encodeURIComponent(entry.getValue(), "UTF-8"));
          result.append("&");
        }
 
        String resultString = result.toString();
        return resultString.length() > 0
          ? resultString.substring(0, resultString.length() - 1)
          : resultString;
    }

    private void _log(String message) {
        if (this._adapter) {
            _adapter.writeToLog(message);
        }
    }

    private void _logError(String message) {
        if (this._adapter) {
            _adapter.writeToErrorLog(message);
        }
    }

}