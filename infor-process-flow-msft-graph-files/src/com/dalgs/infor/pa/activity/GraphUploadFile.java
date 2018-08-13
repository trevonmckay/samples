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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.json.JSONException;
import org.json.JSONObject;

import com.lawson.bpm.adapter.sdk.BPMAdapter;
import com.lawson.bpm.adapter.sdk.BPMAdapterRuntime; 

public class GraphUploadFile implements BPMAdapter {

    private final int MEGABYTE = 1048576;
    private final String[] DEFAULT_SCOPES = { "https://graph.microsoft.com/.default" };

    private BPMAdapterRuntime _adapter;
    private JSONObject _cachedToken;
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

    public JSONObject uploadLargeFile(String driveId, String path, String fileName, byte[] data, String contentType) {
    	URL requestUrl = null;
		try {
			JSONObject session = _startUploadSession(driveId, path, fileName, "rename");
			String uploadUrl = session.getString("uploadUrl");
			requestUrl = new URL(uploadUrl);
		} catch (MalformedURLException ex) {
			_logError(ex);
		} catch (JSONException ex) {
			_logError(ex);
		}

        int chunkSize = 60 * MEGABYTE;
        int fileSize = data.length;

        List<byte[]> chunks = new ArrayList<byte[]>();
        int start = 0;
        while(start < fileSize) {
            int end = Math.min(fileSize, start + chunkSize);
            chunks.add(Arrays.copyOfRange(data, start, end));
            start += chunkSize;
        }

        JSONObject uploadResult = null;
        int bytesSent = 0;
        int chunkCount = chunks.size();
        int i = 0, retries = 0;
        while (i < chunkCount) {
            byte[] chunk = chunks.get(i);
            HttpURLConnection conn = null;
			try {
				conn = (HttpURLConnection)requestUrl.openConnection();
	            conn.setRequestMethod("PUT");
			} catch(ProtocolException ex) {
				ex.printStackTrace();
			} catch (IOException ex) {
				ex.printStackTrace();
			}
			
			if(conn == null)
			{
				this._logError("Failed to create a new HTTP connection to continue file upload.");
				return null;
			}
            
            _authenticateRequest(conn);

            int contentLength = chunk.length;
            start = bytesSent;
            int end = start + contentLength - 1;

			conn.setRequestProperty("Accept", "application/json");
            // conn.setRequestProperty("Content-Length", Integer.toString(contentLength));
            conn.setRequestProperty("Content-Range", MessageFormat.format("bytes {0,number,#}-{1,number,#}/{2,number,#}", start, end, fileSize));

            conn.setDoOutput(true);
            DataOutputStream out;
    		try {
    			out = new DataOutputStream(conn.getOutputStream());
    			out.write(chunk);
    	        out.flush();
    	        out.close();
    		} catch (IOException ex) {
    			// TODO Auto-generated catch block
    			ex.printStackTrace();
    		}
            
            int statusCode = -1;
			try {
				statusCode = conn.getResponseCode();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			
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
	            uploadResult = new JSONObject(responseBody);
			} catch(IOException ex) {
				_logError(ex);
			} catch (JSONException ex) {
				_logError(ex);
			}

            if(statusCode >= HttpURLConnection.HTTP_BAD_REQUEST)
            {
                if (retries >= 3)
                {
                    _logError(MessageFormat.format("Failed to upload {0}. Server retured {1}", fileName, statusCode));
                    return null;
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
        
        return uploadResult;
    }

    public JSONObject uploadFile(String driveId, String path, String fileName, String content, String contentType) {
        int maxFileSize = 4 * MEGABYTE;
        byte[] data = content.getBytes();
        int fileSize = data.length;

        if(fileSize > maxFileSize)
        {
        	return uploadLargeFile(driveId, path, fileName, data, contentType);
        }
        
        HttpURLConnection conn = null;
        try {
        	URL requestUrl = new URL(MessageFormat.format("https://graph.microsoft.com/v1.0/drives/{0}/items/{1}/{2}:/content", driveId, path, fileName));
            conn = (HttpURLConnection) requestUrl.openConnection();
            conn.setRequestMethod("PUT");
			conn.setRequestProperty("Accept", "application/json");
			conn.setRequestProperty("Connection", "Keep-Alive");
			conn.setRequestProperty("Cache-Control", "no-cache");
            conn.setRequestProperty("Content-Type", contentType);
        } catch(MalformedURLException ex) {
        	_logError(ex);
        } catch (ProtocolException ex) {
        	_logError(ex);
        } catch (IOException ex) {
        	_logError(ex);
        }

        _authenticateRequest(conn);
        
        conn.setDoOutput(true);
        DataOutputStream out;
		try {
			out = new DataOutputStream(conn.getOutputStream());
			out.writeBytes(content);
	        out.flush();
	        out.close();
		} catch (IOException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}

        int statusCode = -1;
		try {
			statusCode = conn.getResponseCode();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

        if(statusCode != 200 && statusCode != 201)
        {
            _logError(MessageFormat.format("Failed to upload {0}. Server retured {1}", fileName, statusCode));
        }
        
        JSONObject uploadResult = null;
        try {
        	InputStream stream;
			if(statusCode < HttpURLConnection.HTTP_BAD_REQUEST) {
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
            uploadResult = new JSONObject(responseBody);
        } catch (Exception e) {
            _logError(e);
        }
        
        return uploadResult;
    }

    public void uploadFile(BPMAdapterRuntime runtime) {
        this._adapter = runtime;
        uploadFile(this._driveId, this._path, this._fileName, this._fileContent, this._contentType);
    }

    private void _authenticateRequest(HttpURLConnection connection, String[] scopes) {
        JSONObject token = _getToken(this._clientId, this._clientSecret, this._tenantId, scopes);
		try {
			String accessToken = token.getString("access_token");
			connection.setRequestProperty("Authorization", "Bearer " + accessToken);
		} catch (JSONException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
    }

    private void _authenticateRequest(HttpURLConnection connection) {
        _authenticateRequest(connection, DEFAULT_SCOPES);
    }

    private JSONObject _getToken(String clientId, String clientSecret, String tenant, String[] scopes) {
    	if (this._cachedToken != null) {
    		return this._cachedToken;
    	}
    	
    	HttpURLConnection conn = null;
		try {
			URL requestUrl = new URL("https://login.microsoftonline.com/" + tenant + "/oauth2/v2.0/token");
			conn = (HttpURLConnection) requestUrl.openConnection();
			conn.setRequestMethod("POST");
			conn.setRequestProperty("Accept", "application/json");
			conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
		} catch (MalformedURLException ex) {
			// TODO Auto-generated catch block
			ex.printStackTrace();
		} catch (ProtocolException ex) {
			ex.printStackTrace();
		} catch (IOException ex) {
			ex.printStackTrace();
		}
		
		if (conn == null)
		{
			return null;
		}

        String scope = String.join(" ", scopes);

        Map<String, String> parameters = new HashMap<>();
        parameters.put("client_id", clientId);
        parameters.put("client_secret", clientSecret);
        parameters.put("scope", scope);
        parameters.put("grant_type", "client_credentials");

        String requestBody = "";
		try {
			requestBody = _getParamsString(parameters);
		} catch (UnsupportedEncodingException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
        
        conn.setDoOutput(true);
        DataOutputStream out;
		try {
			out = new DataOutputStream(conn.getOutputStream());
			out.writeBytes(requestBody);
	        out.flush();
	        out.close();
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
            streamReader.close();
        	
            String responseBody = sb.toString();
            tokenResult = new JSONObject(responseBody);
            this._cachedToken = tokenResult;
        } catch (Exception e) {
            _logError(e);
        }

        conn.disconnect();

        return tokenResult;
    }

    private JSONObject _startUploadSession(String driveId, String path, String fileName, String conflictResolution) throws MalformedURLException {
    	String urlString = MessageFormat.format("https://graph.microsoft.com/v1.0/drives/{0}/{1}/{2}:/createUploadSession", driveId, path, fileName);
    	URL requestUrl = new URL(urlString);
		
        HttpURLConnection conn = null;
		try {
			conn = (HttpURLConnection)requestUrl.openConnection();
			conn.setRequestMethod("POST");
			conn.setRequestProperty("Accept", "application/json");
			conn.setRequestProperty("Content-type", "application/json");
		} catch (IOException ex) {
			_logError(ex);
		}
		
		if(conn == null)
		{
			return null;
		}
		
        _authenticateRequest(conn);
        
        String requestBody = null;
        try {
        	JSONObject requestObject = new JSONObject();
            JSONObject itemRequest = new JSONObject();
			itemRequest.put("@microsoft.graph.conflictBehavior", conflictResolution);
			itemRequest.put("name", fileName);
			requestObject.put("item", itemRequest);
			requestBody = requestObject.toString();
		} catch (JSONException e2) {
			// TODO Auto-generated catch block
			e2.printStackTrace();
		}
        
        conn.setDoOutput(true);
        DataOutputStream out;
		try {
			out = new DataOutputStream(conn.getOutputStream());
			out.writeBytes(requestBody);
	        out.flush();
	        out.close();
		} catch (IOException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
        
        int statusCode = -1;
		try {
			statusCode = conn.getResponseCode();
		} catch (IOException ex) {
			this._logError(ex);
		}

        if(statusCode != 200 || statusCode != 201)
        {
            _logError(MessageFormat.format("Failed to initiate large file upload session. Server retured {0}", statusCode));
        }

        JSONObject sessionResult = null;
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
	        sessionResult = new JSONObject(responseBody);
        } catch(IOException ex) {
        	_logError(ex);
        } catch (JSONException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
        conn.disconnect();
        
        return sessionResult;
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