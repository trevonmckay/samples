package com.lawson.bpm.adapter.sdk;

import java.net.HttpURLConnection;
import java.net.URL;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class GraphUploadFile implements BPMAdapter {

    private final int MEGABYTE = 4194304;
    private final String[] DEFAULT_SCOPES = { "https://graph.microsoft.com/.default" };

    private BPMAdapterRuntime _adapter;
    private String _clientId;
    private String _clientSecret;
    private String _tenantId;
    private String _driveId;
    private String _path;
    private String _fileName;
    private String _fileContent;
    private String _contentType;

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

    public void setFileName(String val) {
        if(val == null || val.isEmpty())
        {
            _logError("The property 'TenantId' is empty or null.");
        }

        this._fileName = val;
    }

    public void setFileContent(String val) {
        this._fileContent = val;
    }

    public void setContentType(String val) {
        this._contentType = val;
    }

    public GraphUploadFile() {

    }

    private void _startUploadSession(String driveId, String path, String fileName, String conflictResolution) {
        URL requestUrl = new URL(MessageFormat.format("https://graph.microsoft.com/v1.0/drives/{0}/{1}/createUploadSession", driveId, path));
        HttpURLConnection conn = (HttpURLConnection)requestUrl.openConnection();
        conn.setRequestMethod("POST");
        conn.setRequestProperty("Content-type", "application/json");

        _authenticateRequest(conn);

        String requestBodyFormat = "{\"@microsoft.graph.conflictBehavior\":{0},\"name\":{1}}";
        String requestBody = MessageFormat(requestBodyFormat, conflictResolution, fileName);

        int statusCode = con.getResponseCode();

        if(statusCode != 200 || statusCode != 201)
        {
            _logError(MessageFormat.format("Failed to initiate large file upload session. Server retured {0}", statusCode));
            return;
        }

        BufferedReader in = new BufferedReader(new InputStreamReader(conn.getInputStream()));
        String inputLine;
        StringBuffer responseBody = new StringBuffer();
        while ((inputLine = in.readLine()) != null) {
            responseBody.append(inputLine);
        }
        in.close();
        conn.disconnect();
    }

    public void uploadLargeFile(String driveId, String path, String fileName, byte[] data, String contentType) {
        _startUploadSession(driveId, path, fileName, "rename");
        URL requestUrl = new URL("");

        int chunkSize = 60 * MEGABYTE;
        int fileSize = data.length;

        List<byte[]> chunks = new ArrayList<byte[]>();
        int start = 0;
        while(start < fileSize) {
            int end = Math.min(fileSize, start + chunkSize);
            chunks.add(Arrays.copyOfRange(data, start, end));
            start += chunkSize;
        }

        int bytesSent = 0;
        int chunkCount = chunks.size();
        int i, retries = 0;
        while (i < chunkCount) {
            byte[] chunk = chunks.get(i);
            HttpURLConnection conn = (HttpURLConnection)requestUrl.openConnection();
            conn.setRequestMethod("PUT");
            
            _authenticateRequest(conn);

            int contentLength = chunk.length;
            start = bytesSent;
            int end = bytesSent + contentLength;

            request.setRequestHeader("Content-Length", contentLength);
            request.setRequestHeader("Content-Range", MessageFormat.format("bytes ${0}-${1}/${2}", start, end, fileSize));

            int statusCode = con.getResponseCode();

            if(statusCode != 202)
            {
                if (retries >= 3)
                {
                    _logError(MessageFormat.format("Failed to upload {0}. Server retured {1}", fileName, statusCode));
                    return;
                }
                retries++;
            }
            else
            {
                _log(MessageFormat.format("Sent {0} of {1} bytes...", bytesSent, fileSize));
                retries = 0;
                i++;
            }
        }
    }

    public void uploadFile(String driveId, String path, String filename, String content, String contentType) {
        int maxFileSize = 4 * MEGABYTE;
        byte[] data = content.getBytes();
        int fileSize = data.length;

        if(fileSize > maxFileSize)
        {
            return uploadLargeFile(driveId, path, fileName, content, contentType);
        }

        URL requestUrl = new URL(MessageFormat.format("https://graph.microsoft.com/v1.0/drives/{0}/items/{1}/{2}:/content", driveId, path, filename));
        HttpURLConnection con = (HttpURLConnection) requestUrl.openConnection();
        con.setRequestMethod("PUT");
        con.setRequestProperty("Content-Type", contentType);

        _authenticateRequest(con);

        int statusCode = con.getResponseCode();

        if(statusCode != 200 || statusCode != 201)
        {
            _logError(MessageFormat.format("Failed to upload {0}. Server retured {1}", filename, statusCode));
            return;
        }
    }

    public void uploadFile(BPMAdapterRuntime runtime) {
        this._adapter = runtime;
        uploadFile(this._driveId, this._path, this._fileName, this._fileContent, this._contentType);
    }

    public void getGroupDrives(String groupId) {
        URL requestUrl = new URL("https://graph.microsoft.com/v1.0/groups/" + groupId + "/drives");
        HttpURLConnection con = (HttpURLConnection) requestUrl.openConnection();
        con.setRequestMethod("GET");

        _authenticateRequest(con);

        int statusCode = con.getResponseCode();

        if(statusCode != 200)
        {
            _logError(MessageFormat.format("Failed to retrive drives for the group {0}. Server retured {1}", groupId, statusCode));
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