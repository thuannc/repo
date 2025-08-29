//--------------------------
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import org.json.JSONObject;
import org.json.JSONArray;

public class ODAMobilePackageRenewalDetailed {
    public static void main(String[] args) {
        try {
            // Thông tin yêu cầu
            String customerId = "CUST123456";
            String productSpecId = "PROD_SPEC_MOBILE_PACKAGE_100MIN_250SMS_2GB";
            String odaToken = "your-oda-token-here";

            // Tạo payload chi tiết cho Service Order (TMF641)
            JSONObject payload = new JSONObject();
            payload.put("@type", "ServiceOrder");
            payload.put("id", "SO-123456789");
            payload.put("href", "https://api.tmforum-oda.com/service-order/SO-123456789");
            payload.put("externalId", "EXT-RENEW-001");
            payload.put("priority", "1");
            payload.put("description", "Gia hạn gói cước di động: 100 phút thoại nội mạng, 250 SMS, 2GB Data/ngày với throttling");
            payload.put("category", "Mobile Package Renewal");
            payload.put("requestedStartDate", "2025-08-29T00:00:00Z");
            payload.put("requestedCompletionDate", "2025-08-30T00:00:00Z");
            payload.put("expectedCompletionDate", "2025-08-30T23:59:59Z");
            payload.put("orderDate", "2025-08-29T15:00:00Z");

            // Related Party (Customer)
            JSONArray relatedParty = new JSONArray();
            relatedParty.put(new JSONObject()
                .put("@type", "Customer")
                .put("id", customerId)
                .put("href", "https://api.tmforum-oda.com/customer-management/customer/" + customerId)
                .put("name", "Customer Name")
                .put("role", "Customer"));
            payload.put("relatedParty", relatedParty);

            // Order Items với action "modify"
            JSONArray orderItems = new JSONArray();
            // Order Item cho Voice CFS
            orderItems.put(new JSONObject()
                .put("@type", "ServiceOrderItem")
                .put("id", "SOI-001")
                .put("action", "modify")
                .put("service", new JSONObject()
                    .put("@type", "CustomerFacingService")
                    .put("id", "existingVoiceServiceId")
                    .put("href", "https://api.tmforum-oda.com/service-inventory/service/existingVoiceServiceId")
                    .put("name", "Mobile Voice On-Net Service")
                    .put("serviceSpecification", new JSONObject()
                        .put("id", "CFS_MOBILE_VOICE_ONNET")
                        .put("href", "https://api.tmforum-oda.com/service-catalog/serviceSpecification/CFS_MOBILE_VOICE_ONNET"))
                    .put("serviceCharacteristic", new JSONArray()
                        .put(new JSONObject().put("name", "VoiceQuota").put("valueType", "integer").put("value", "100"))
                        .put(new JSONObject().put("name", "EffectiveDate").put("valueType", "dateTime").put("value", "2025-08-29T00:00:00Z")))
                    .put("supportingService", new JSONArray()
                        .put(new JSONObject()
                            .put("@type", "ResourceFacingService")
                            .put("id", "RFS_VOICE_NETWORK")
                            .put("href", "https://api.tmforum-oda.com/service-catalog/serviceSpecification/RFS_VOICE_NETWORK")
                            .put("name", "Voice Network RFS")
                            .put("serviceCharacteristic", new JSONArray()
                                .put(new JSONObject().put("name", "ChargingSystem").put("value", "Online Charging System (OCS)")))
                            .put("supportingResource", new JSONArray()
                                .put(new JSONObject().put("id", "RES_OCS").put("name", "Online Charging System")))))));
            // Tương tự cho SMS và Data (thêm tương tự như trên để ngắn gọn)

            payload.put("orderItem", orderItems);

            // Note và Product Ref
            JSONArray notes = new JSONArray();
            notes.put(new JSONObject()
                .put("text", "Gia hạn tự động theo yêu cầu khách hàng")
                .put("author", "System")
                .put("date", "2025-08-29T15:00:00Z"));
            payload.put("note", notes);
            payload.put("productOrderRef", new JSONObject()
                .put("id", productSpecId)
                .put("href", "https://api.tmforum-oda.com/product-catalog/productSpecification/" + productSpecId));

            // Gọi API
            String response = renewPackageODA(payload, odaToken);
            System.out.println("Phản hồi từ ODA system: " + response);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

/**
     * Gửi yêu cầu gia hạn gói cước qua TMF641 Service Ordering API
     */
    public static String renewPackageODA(JSONObject payload, String odaToken) throws Exception {
        // URL API cho Service Ordering (TMF641)
        String apiUrl = "https://api.tmforum-oda.com/service-order";
        URL url = new URL(apiUrl);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();

        // Cấu hình request theo chuẩn ODA
        conn.setRequestMethod("POST");
        conn.setRequestProperty("Content-Type", "application/json; charset=UTF-8");
        conn.setRequestProperty("Accept", "application/json");
        conn.setRequestProperty("Authorization", "Bearer " + odaToken);
        conn.setDoOutput(true);

        // Gửi payload
        try (OutputStream os = conn.getOutputStream()) {
            byte[] input = payload.toString().getBytes("utf-8");
            os.write(input, 0, input.length);
        }

        // Đọc phản hồi
        StringBuilder response = new StringBuilder();
        int responseCode = conn.getResponseCode();
        if (responseCode == 200 || responseCode == 202) {
            try (BufferedReader br = new BufferedReader(
                    new InputStreamReader(conn.getInputStream(), "utf-8"))) {
                String responseLine;
                while ((responseLine = br.readLine()) != null) {
                    response.append(responseLine.trim());
                }
            }

            // Xử lý phản hồi theo TM Forum acknowledgement pattern
            JSONObject jsonResponse = new JSONObject(response.toString());
            JSONObject acknowledgement = jsonResponse.getJSONObject("acknowledgement");
            boolean success = acknowledgement.getJSONObject("received").getBoolean("success");
            String message = jsonResponse.optString("message", "No message");

            if (success) {
                return "Gia hạn gói cước thành công: " + message;
            } else {
                return "Gia hạn thất bại: " + message;
            }
        } else {
            try (BufferedReader br = new BufferedReader(
                    new InputStreamReader(conn.getErrorStream(), "utf-8"))) {
                String responseLine;
                while ((responseLine = br.readLine()) != null) {
                    response.append(responseLine.trim());
                }
            }
            return "Lỗi khi gọi ODA API, mã trạng thái: " + responseCode + ", chi tiết: " + response.toString();
        }
    }
}
