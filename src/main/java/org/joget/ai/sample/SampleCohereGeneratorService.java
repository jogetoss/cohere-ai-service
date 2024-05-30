package org.joget.ai.sample;

import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpEntityEnclosingRequestBase;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.joget.ai.model.AiAppGeneratorServiceAbstract;
import org.joget.ai.model.AiConversation;
import org.joget.apps.app.service.AppPluginUtil;
import org.joget.apps.app.service.AppUtil;
import org.joget.commons.util.LogUtil;
import org.json.JSONArray;
import org.json.JSONObject;

public class SampleCohereGeneratorService extends AiAppGeneratorServiceAbstract {
    
    @Override
    public String getName() {
        return "SampleCohereGeneratorService";
    }

    @Override
    public String getVersion() {
        return Activator.VERSION;
    }

    @Override
    public String getLabel() {
        return AppPluginUtil.getMessage(getName() + ".label", getClassName(), Activator.AI_MESSAGE_PATH);
    }

    @Override
    public String getDescription() {
        return AppPluginUtil.getMessage(getName() + ".desc", getClassName(), Activator.AI_MESSAGE_PATH);
    }
    
    @Override
    public String getClassName() {
        return getClass().getName();
    }

    @Override
    public String getIcon() {
        return "[CONTEXT_PATH]/plugin/org.joget.ai.CohereGeneratorService/images/cohereai_logo.webp";
    }

    @Override
    public String getPropertyOptions() {
        return AppUtil.readPluginResource(getClass().getName(), "/properties/ai/"+getName()+".json", null, true, Activator.AI_MESSAGE_PATH);
    }

    /**
     * This is the only required method to return the response from AI service
     * based on the provided instructions, sample and prompt
     * @param messages
     * @return 
     */
    @Override
    public String getResponse(Collection<AiConversation> messages) {
        String content = null;
        String key = getPropertyString("apiKey");
        String model = getPropertyString("model");
        String debug = getPropertyString("debug");
        String domain = getPropertyString("proxyDomain");
        
        if (domain.isEmpty()) {
            domain = "https://api.cohere.ai";
        }
        if (!domain.endsWith("/")) {
            domain += "/";
        }
        
        JSONObject obj = preparePayload(messages);
        obj.put("model", model);
        obj.put("temperature", 0.01);
        
        String jsonResponse = callAPI(domain + "v1/chat", key, null, obj, "true".equalsIgnoreCase(debug));
        if (jsonResponse != null && !jsonResponse.isEmpty()) {
            JSONObject result = new JSONObject(jsonResponse);
            if (result.has("text")) {
                content = extractCode(result.getString("text"));
            }
        }
        
        return content;
    }
    
    /**
     * Extract and unwrap the definition from AI response
     * 
     * @param respond
     * @return 
     */
    public String extractCode(String respond) {
        String newRespond = respond;
        if (newRespond != null && !newRespond.isEmpty() && newRespond.contains("```")) {
            newRespond = newRespond.substring(newRespond.indexOf("```") + 3);
            if (newRespond.trim().isEmpty()) {
                newRespond = respond;
            }
            if (newRespond.indexOf("```") > 0) {
                newRespond = newRespond.substring(0, newRespond.indexOf("```"));
            }
            //remove starting new line if there exist
            newRespond = newRespond.replaceFirst("^[\\n\\r]", "");
        }
        
        return newRespond;
    }
    
    /**
     * Convert the provided instructions, sample and prompt to 
     * AI service payload
     * @param messages
     * @return 
     */
    public JSONObject preparePayload(Collection<AiConversation> messages) {
        JSONObject obj = new JSONObject();
        
        //set expectation, combine first and last as message. other as documents
        if (messages != null && !messages.isEmpty()) {
            JSONArray chatHistories = new JSONArray();
            String message = "";
            ArrayList<AiConversation> messagesList = (ArrayList<AiConversation>) messages;
            for (int i = 0; i < messagesList.size(); i++) {
                AiConversation m = messagesList.get(i);
                if (i == messagesList.size() - 1) {
                    message += m.getMessage();
                } else {
                    JSONObject chat = new JSONObject();
                    chat.put("role", "USER");
                    chat.put("message", m.getMessage());
                    chatHistories.put(chat);
                }
            }
            obj.put("message", message);
            obj.put("chat_history", chatHistories);
        }
        
        if ("true".equalsIgnoreCase(getPropertyString("webSearch"))) {
            JSONArray connectors = new JSONArray();
            JSONObject webserach = new JSONObject();
            webserach.put("id", "web-search");
            connectors.put(webserach);
            obj.put("connectors", connectors);
        }
        
        return obj;
    }
    
    /**
     * Call the AI service API to retrieve response
     * 
     * @param url
     * @param apiKey
     * @param headers
     * @param payload
     * @param isDebug
     * @return 
     */
    public String callAPI(String url, String apiKey, Map<String, String> headers, JSONObject payload, boolean isDebug) {
        CloseableHttpClient client = null;
        HttpRequestBase request = null;
        
        try {
            HttpClientBuilder httpClientBuilder = HttpClients.custom();
            URL urlObj = new URL(url);
            client = httpClientBuilder.build();
            request = new HttpPost(urlObj.toURI());
            
            request.setHeader("accept", "application/json");
            request.setHeader("Content-Type", "application/json");
            if (apiKey != null && !apiKey.isEmpty()) {
                request.setHeader("Authorization", "Bearer " + apiKey);
            }
            
            if (headers != null) {
                for (String key : headers.keySet()) {
                    request.setHeader(key, headers.get(key));
                }
            }
            
            StringEntity requestEntity = new StringEntity(payload.toString(4), "UTF-8");
            ((HttpEntityEnclosingRequestBase) request).setEntity(requestEntity);
            if (isDebug) {
                LogUtil.info(SampleCohereGeneratorService.class.getName(), "----- Request Start -----");
                LogUtil.info(SampleCohereGeneratorService.class.getName(), payload.toString(4));
                LogUtil.info(SampleCohereGeneratorService.class.getName(), "----- Request End -----");
            }
            
            HttpResponse response = client.execute(request);
            String jsonResponse =  EntityUtils.toString(response.getEntity(), "UTF-8");
            if (isDebug) {
                LogUtil.info(SampleCohereGeneratorService.class.getName(), "----- Response Start -----");
                LogUtil.info(SampleCohereGeneratorService.class.getName(), jsonResponse);
                LogUtil.info(SampleCohereGeneratorService.class.getName(), "----- Response End -----");
            }
            return jsonResponse;
        } catch (Exception e) {
            LogUtil.error(SampleCohereGeneratorService.class.getName(), e, "");
        } finally {
            try {
                if (request != null) {
                    request.releaseConnection();
                }
                if (client != null) {
                    client.close();
                }
            } catch (IOException ex) {
                LogUtil.error(SampleCohereGeneratorService.class.getName(), ex, "");
            }
        }
        return null;
    }
}
