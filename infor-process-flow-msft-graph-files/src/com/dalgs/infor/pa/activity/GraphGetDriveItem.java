package com.dalgs.infor.pa.activity;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.ProtocolException;
import java.net.URL;
import java.net.URLEncoder;
import java.text.MessageFormat;
import java.util.HashMap;
import java.util.Map;

import org.json.JSONException;
import org.json.JSONObject;

import com.lawson.bpm.adapter.sdk.BPMAdapter;
import com.lawson.bpm.adapter.sdk.BPMAdapterException;
import com.lawson.bpm.adapter.sdk.BPMAdapterRuntime;

public class GraphGetDriveItem implements BPMAdapter {

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

    public JSONObject getItem(String driveId, String path) {
    	path = path.replace(" ", "%20");
    	
    	HttpURLConnection conn = null;
    	try {
    		URL requestUrl = new URL("https://graph.microsoft.com/v1.0/drives/" + driveId + "/" + path);
    		conn = (HttpURLConnection)requestUrl.openConnection();
    		conn.setRequestMethod("GET");
    	} catch (MalformedURLException ex) {
    		_logError(ex);
    	} catch (ProtocolException ex) {
    		_logError(ex);
    	} catch (IOException ex) {
    		_logError(ex);
    	}
    	
    	if (conn == null) {
    		_logError("Unable to create a HTTP connection.");
    		return null;
    	}

        this._authenticateRequest(conn);
        conn.setRequestProperty("Accept", "application/json");

        int statusCode = -1;
        try {
			statusCode = conn.getResponseCode();
		} catch (IOException ex) {
			_logError(ex);
		}

        if(statusCode != 200)
        {
            _logError(MessageFormat.format("Failed to get the file at {0}. Server retured {1}", path, statusCode));
        }
        
        JSONObject itemResult = null;
        try {
        	InputStream stream;
            if (statusCode < HttpURLConnection.HTTP_BAD_REQUEST) {
            	stream = conn.getInputStream();
            } else {
            	stream = conn.getErrorStream();
            }
            
	        InputStreamReader streamReader = new InputStreamReader(stream);
	        BufferedReader buffReader = new BufferedReader(streamReader);
	        String inputLine;
	        StringBuffer sb = new StringBuffer();
	        while ((inputLine = buffReader.readLine()) != null) {
	            sb.append(inputLine);
	        }
	        buffReader.close();
	        streamReader.close();
	        
	        String responseBody = sb.toString();
	        itemResult = new JSONObject(responseBody);
        } catch(IOException ex) {
        	_logError(ex);
        } catch (JSONException ex) {
			_logError(ex);
		}
        
        conn.disconnect();
        
        return itemResult;
    }

    public void getItem(BPMAdapterRuntime runtime) throws IOException {
        this._adapter = runtime;
        JSONObject result = getItem(this._driveId, this._path);
        
        try {
    		if (result != null) {
    			this._adapter.setConnectorOutputVariables(true, 0, 200, "Success", result);
    		} 
		} catch (BPMAdapterException e) {
			_logError(e);
		}
    }

    private void _authenticateRequest(HttpURLConnection connection, String[] scopes) {
        JSONObject tokenResult = _getToken(this._clientId, this._clientSecret, this._tenantId, scopes);
		try {
			String accessToken = tokenResult.getString("access_token");
			connection.setRequestProperty("Authorization", "Bearer " + accessToken);			
		} catch (JSONException ex) {
			_logError(ex);
		}
    }

    private void _authenticateRequest(HttpURLConnection connection) {
        _authenticateRequest(connection, DEFAULT_SCOPES);
    }

    private JSONObject _getToken(String clientId, String clientSecret, String tenant, String[] scopes) {
    	HttpURLConnection conn = null;
    	try {
    		URL requestUrl = new URL("https://login.microsoftonline.com/" + tenant + "/oauth2/v2.0/token");
    		conn = (HttpURLConnection)requestUrl.openConnection();
    		conn.setRequestMethod("POST");
    		conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
    	} catch (MalformedURLException ex) {
    		_logError(ex);
    	} catch (ProtocolException ex) {
    		_logError(ex);
    	} catch (IOException ex) {
    		_logError(ex);
    	}

        String scope = String.join(" ", scopes);

        Map<String, String> parameters = new HashMap<>();
        parameters.put("client_id", clientId);
        parameters.put("client_secret", clientSecret);
        parameters.put("scope", scope);
        parameters.put("grant_type", "client_credentials");

        try {
	        String requestBody = _getParamsString(parameters);
	        
	        conn.setDoOutput(true);
	        DataOutputStream out = new DataOutputStream(conn.getOutputStream());
	        out.writeBytes(requestBody);
	        out.flush();
	        out.close();
        } catch (UnsupportedEncodingException ex) {
        	_logError(ex);
        } catch (IOException ex) {
        	_logError(ex);
        }

        int statusCode = -1;
		try {
			statusCode = conn.getResponseCode();
		} catch (IOException ex) {
			_logError(ex);
		}

        if (statusCode != 200)
        {
            _logError("Failed to retrieve access token. Server returned " + statusCode);
            return null;
        }

        JSONObject tokenResult = null;
        try {
        	InputStreamReader streamReader = new InputStreamReader(conn.getInputStream());
        	BufferedReader buffReader = new BufferedReader(streamReader);
            String inputLine;
            StringBuffer sb = new StringBuffer();
            while ((inputLine = buffReader.readLine()) != null) {
                sb.append(inputLine);
            }
            buffReader.close();
        	
            String responseBody = sb.toString();
            tokenResult = new JSONObject(responseBody);
            streamReader.close();
        } catch (Exception e) {
            _logError(e.getMessage());
        }

        conn.disconnect();

        return tokenResult;
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
        if (this._adapter != null) {
            _adapter.writeToLog(message);
        }
    }

    private void _logError(String message) {
        if (this._adapter != null) {
            _adapter.writeToErrorLog(message);
        }
    }
    
    private void _logError(Exception ex) {
    	ex.printStackTrace();
    	this._logError(ex.getMessage());
    }

}